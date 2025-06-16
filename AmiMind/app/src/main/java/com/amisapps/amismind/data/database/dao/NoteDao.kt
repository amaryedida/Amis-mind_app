package com.amisapps.amismind.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.amisapps.amismind.data.database.model.Note

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateNote(note: Note): Long

    @Query("SELECT * FROM notes WHERE isArchived = 0 ORDER BY updatedAt DESC")
    fun getAllActiveNotes(): LiveData<List<Note>>

    @Query("SELECT * FROM notes WHERE isArchived = 1 ORDER BY updatedAt DESC")
    fun getAllArchivedNotes(): LiveData<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteById(noteId: Long): Note?

    @Query("SELECT * FROM notes WHERE firebaseId = :firebaseId")
    suspend fun getNoteByFirebaseId(firebaseId: String): Note?

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteNoteById(noteId: Long)

    @Query("UPDATE notes SET isArchived = :isArchived WHERE id = :noteId")
    suspend fun updateArchiveStatus(noteId: Long, isArchived: Boolean)
}
