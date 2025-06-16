package com.amisapps.amismind.ui.share

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.amisapps.amismind.R // R class import
import com.amisapps.amismind.ui.addeditnote.AddEditNoteActivity // Activity import

class ShareReceiverActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true) {
            handleSharedImage()
        } else {
            Toast.makeText(this, "Unsupported content to share.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun handleSharedImage() {
        val imageUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        if (imageUri != null) {
            val launchIntent = Intent(this, AddEditNoteActivity::class.java).apply {
                action = Intent.ACTION_SEND // Optional: carry action for context
                putExtra(AddEditNoteActivity.EXTRA_SHARED_IMAGE_URI, imageUri.toString())
                // Ensure this new note doesn't try to load an existing note by ID
                // AddEditNoteActivity should prioritize EXTRA_SHARED_IMAGE_URI for new note creation
                // and not attempt to load EXTRA_NOTE_ID if EXTRA_SHARED_IMAGE_URI is present.
                // Or, ensure EXTRA_NOTE_ID is not passed or is -1L.
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) // Clear task for a clean new note
            }
            startActivity(launchIntent)
        } else {
            Toast.makeText(this, "Could not retrieve shared image.", Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}
