package com.amisapps.amismind.data.firebase

import com.google.firebase.firestore.FirebaseFirestore

object FirebaseUtils {
    // TODO: Change to user-specific collection path once Firebase Auth is integrated.
    // For example: "/users/{UID}/notes"
    // For now, using a common collection for simplicity.
    val notesCollection = FirebaseFirestore.getInstance().collection("notes")
}
