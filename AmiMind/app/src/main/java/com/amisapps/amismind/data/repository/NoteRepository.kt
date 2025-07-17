package com.amisapps.amismind.data.repository

import android.util.Log
import com.amisapps.amismind.data.database.dao.NoteDao
import com.amisapps.amismind.data.database.model.Note
import com.amisapps.amismind.data.firebase.FirebaseUtils
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class NoteRepository(
    private val noteDao: NoteDao,
    private val firestore: FirebaseFirestore = FirebaseUtils.notesCollection // Uses the default notes collection
) {

    private val TAG = "NoteRepository"

    suspend fun saveNote(note: Note) {
        withContext(Dispatchers.IO) {
            try {
                if (note.firebaseId == null) { // New note
                    // For new notes, ensure local createdAt/updatedAt are set before toMap() if not using serverTimestamp directly in map
                    note.createdAt = System.currentTimeMillis() // Ensure local model has it
                    note.updatedAt = System.currentTimeMillis()

                    val documentReference = firestore.add(note.toMap()).await()
                    note.firebaseId = documentReference.id
                    // Firestore will set its own server timestamps.
                    // The local insertOrUpdateNote below will save the note with its new firebaseId.
                    // The snapshot listener will later sync server timestamps back to Room.
                    Log.d(TAG, "New note added to Firestore with ID: ${note.firebaseId}")
                } else { // Existing note
                    note.updatedAt = System.currentTimeMillis() // Update local model's timestamp
                    firestore.document(note.firebaseId!!).set(note.toMap(), SetOptions.merge()).await()
                    Log.d(TAG, "Note updated in Firestore with ID: ${note.firebaseId}")
                }
                // Save to Room after successful Firestore operation (or let sync handle it)
                // For quicker UI update, save to Room directly. Sync will overwrite if there are discrepancies (e.g. server timestamps).
                noteDao.insertOrUpdateNote(note)
            } catch (e: Exception) {
                Log.e(TAG, "Error saving note to Firestore", e)
                // Optionally, rethrow or handle error (e.g., save to a local "dirty" queue)
                // For now, if Firestore fails, we might have only local changes or no changes.
                // If it's a new note and add() failed, firebaseId will be null.
                // If it's an existing note, local Room might be updated if insertOrUpdateNote is called outside.
                // For now, we'll let it fail and log. A robust solution needs more error handling.
                if (note.firebaseId != null) { // If it was an existing note, try to update Room anyway with local changes.
                    noteDao.insertOrUpdateNote(note)
                }
            }
        }
    }

    suspend fun deleteNote(note: Note) {
        withContext(Dispatchers.IO) {
            try {
                if (note.firebaseId != null) {
                    firestore.document(note.firebaseId!!).delete().await()
                    Log.d(TAG, "Note deleted from Firestore: ${note.firebaseId}")
                }
                noteDao.deleteNote(note) // Delete from Room regardless of Firestore status for now
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting note from Firestore", e)
                // Still delete from Room? Or handle error? For now, we proceed with Room deletion.
                noteDao.deleteNote(note)
            }
        }
    }

    suspend fun archiveOrUnarchiveNote(note: Note, isArchived: Boolean) {
        withContext(Dispatchers.IO) {
            val originalNoteId = note.id // Room ID
            note.isArchived = isArchived
            note.updatedAt = System.currentTimeMillis() // Local timestamp update

            try {
                if (note.firebaseId != null) {
                    // Only send fields that change to Firestore to avoid overwriting server timestamps with local ones
                    firestore.document(note.firebaseId!!)
                        .update(mapOf(
                            "archived" to note.isArchived,
                            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                        ))
                        .await()
                    Log.d(TAG, "Note archive status updated in Firestore: ${note.firebaseId}")
                }
                // Update Room using the specific DAO method that updates archive status and potentially timestamp
                // The note object passed to DAO should have the updated fields.
                // The DAO method might need to fetch the note by id, update fields, then save.
                // Or, updateArchiveStatus can directly take all necessary fields.
                 noteDao.updateArchiveStatus(originalNoteId, note.isArchived) // Assuming this also updates timestamp if DAO is designed so
                 // If not, we might need noteDao.insertOrUpdateNote(note) after fetching the full note and applying changes.
                 // For simplicity, let's assume updateArchiveStatus is sufficient or sync will correct timestamps.
            } catch (e: Exception) {
                Log.e(TAG, "Error updating note archive status in Firestore", e)
                 noteDao.updateArchiveStatus(originalNoteId, note.isArchived) // Try local update anyway
            }
        }
    }


    fun syncNotes() {
        // Ensure this is called from a scope that can handle long-running listeners, like viewModelScope
        // or a dedicated service/singleton scope.
        firestore.addSnapshotListener { snapshots, error ->
            if (error != null) {
                Log.e(TAG, "Firestore snapshot listener error", error)
                return@addSnapshotListener
            }

            if (snapshots == null) {
                Log.w(TAG, "Firestore snapshots are null")
                return@addSnapshotListener
            }

            CoroutineScope(Dispatchers.IO).launch { // Use CoroutineScope for DAO operations
                for (documentChange in snapshots.documentChanges) {
                    val document = documentChange.document
                    try {
                        val syncedNote = Note.fromMap(document.data, document.id)
                        when (documentChange.type) {
                            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                noteDao.insertOrUpdateNote(syncedNote)
                                Log.d(TAG, "Synced and upserted note from Firestore: ${syncedNote.firebaseId}")
                            }
                            DocumentChange.Type.REMOVED -> {
                                // We need a way to delete from Room by firebaseId if the local 'id' is unknown
                                // or not yet synced. If 'syncedNote' has its Room 'id' field populated correctly
                                // (e.g., if 'fromMap' could query Room by firebaseId to get local id), then this is fine.
                                // Otherwise, add noteDao.deleteNoteByFirebaseId(syncedNote.firebaseId!!)
                                val localNote = noteDao.getNoteByFirebaseId(syncedNote.firebaseId!!)
                                localNote?.let { noteDao.deleteNote(it) } ?: Log.w(TAG, "Tried to delete non-existent local note for firebaseId: ${syncedNote.firebaseId}")
                                Log.d(TAG, "Synced and deleted note from Firestore: ${syncedNote.firebaseId}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing document change for ${document.id}", e)
                    }
                }
            }
        }
    }
}
