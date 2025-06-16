package com.amisapps.amismind.data.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    @get:Exclude var id: Long = 0, // Exclude Room's ID from Firestore mapping if firebaseId is primary there
    var title: String = "",
    var content: String = "",
    @ServerTimestamp @get:Exclude var createdAt FirestoreTimestamp: Date? = null, // For Firestore
    @ServerTimestamp @get:Exclude var updatedAtFirestoreTimestamp: Date? = null, // For Firestore
    var imageUri: String? = null,
    var isArchived: Boolean = false,
    var firebaseId: String? = null,

    // Local timestamps, managed by app, synced from Firestore's timestamp
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
) {
    // Nullary constructor for Firestore deserialization
    constructor() : this(0, "", "", null, null, null, false, null, 0, 0)

    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "title" to title,
            "content" to content,
            "createdAt" to FieldValue.serverTimestamp(), // Use server timestamp for new/updated
            "updatedAt" to FieldValue.serverTimestamp(),
            "imageUri" to imageUri,
            "isArchived" to isArchived,
            "firebaseId" to firebaseId // though this is often the doc ID itself
            // Do not include local 'id', 'createdAt', 'updatedAt' if they are different from Firestore fields
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>, documentId: String): Note {
            val note = Note(
                title = map["title"] as? String ?: "",
                content = map["content"] as? String ?: "",
                imageUri = map["imageUri"] as? String?,
                isArchived = map["isArchived"] as? Boolean ?: false,
                firebaseId = documentId // Use documentId from Firestore
            )
            // Handle Timestamps from Firestore
            (map["createdAt"] as? com.google.firebase.Timestamp)?.toDate()?.time?.let {
                note.createdAt = it
            }
            (map["updatedAt"] as? com.google.firebase.Timestamp)?.toDate()?.time?.let {
                note.updatedAt = it
            }
            // If createdAt/updatedAt are null from map (e.g. during optimistic update before server sets them),
            // local values will retain System.currentTimeMillis() or previous value.
            // This might need refinement based on how server timestamps are handled on optimistic writes.
            return note
        }
    }
}
