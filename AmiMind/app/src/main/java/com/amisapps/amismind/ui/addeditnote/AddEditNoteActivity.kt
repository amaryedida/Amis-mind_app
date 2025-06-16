package com.amisapps.amismind.ui.addeditnote

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.amisapps.amismind.R
import com.amisapps.amismind.databinding.ActivityAddEditNoteBinding

class AddEditNoteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditNoteBinding
    private lateinit var viewModel: AddEditNoteViewModel
    private var currentNoteId: Long = -1L

    companion object {
        const val EXTRA_NOTE_ID = "com.amisapps.amismind.EXTRA_NOTE_ID" // Updated package
        const val EXTRA_SHARED_IMAGE_URI = "com.amisapps.amismind.EXTRA_SHARED_IMAGE_URI" // Updated package
        private const val PICK_IMAGE_REQUEST_CODE = 101
        private const val RECORD_AUDIO_PERMISSION_REQUEST_CODE = 102
    }

    private var speechRecognizer: SpeechRecognizer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this).get(AddEditNoteViewModel::class.java)

        setSupportActionBar(binding.addEditNoteToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val sharedImageUriString = intent.getStringExtra(EXTRA_SHARED_IMAGE_URI)

        if (sharedImageUriString != null) {
            // Shared image takes precedence, treat as a new note with this image
            currentNoteId = -1L // Ensure it's treated as a new note
            supportActionBar?.title = "Add Note from Share"
            viewModel.setImageUri(sharedImageUriString) // This will trigger the observer for image display
            // Clear currentNote LiveData in ViewModel if it's not already null for a new note
            // This is to prevent previously loaded existing note data from sticking around.
            // viewModel.loadNote(-1L) // Or a more direct way to reset currentNote in ViewModel
        } else {
            // Regular note loading logic
            currentNoteId = intent.getLongExtra(EXTRA_NOTE_ID, -1L)
            if (savedInstanceState == null && currentNoteId != -1L) {
                viewModel.loadNote(currentNoteId)
            } else if (currentNoteId == -1L) {
                supportActionBar?.title = "Add Note"
            }
        }

        viewModel.currentNote.observe(this) { note ->
            // This observer will primarily populate fields when an existing note is loaded.
            // For shared images leading to new notes, 'note' will be null initially.
            note?.let {
                // Only set text if it's different, to avoid issues if user started typing
                // and a delayed loadNote call finishes.
                if (binding.noteTitleEditText.text.toString().isEmpty() || binding.noteTitleEditText.text.toString() != it.title) {
                    binding.noteTitleEditText.setText(it.title)
                }
                if (binding.noteContentEditText.text.toString().isEmpty() || binding.noteContentEditText.text.toString() != it.content) {
                    binding.noteContentEditText.setText(it.content)
                }
                supportActionBar?.title = "Edit Note"
            }
        }

        viewModel.currentImageUri.observe(this) { uriString ->
            if (uriString != null) {
                binding.noteImageView.setImageURI(Uri.parse(uriString))
                binding.noteImageView.visibility = View.VISIBLE
            } else {
                binding.noteImageView.visibility = View.GONE
            }
        }

        viewModel.saveFinished.observe(this) { finished ->
            if (finished) {
                Toast.makeText(this, "Note saved", Toast.LENGTH_SHORT).show()
                viewModel.resetSaveFinished() // Reset the flag
                finish()
            }
        }

        binding.attachImageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK) // Or Intent.ACTION_GET_CONTENT
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST_CODE)
        }

        binding.recordVoiceButton.setOnClickListener {
            checkAndRequestAudioPermission()
        }
    }

    private fun checkAndRequestAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                RECORD_AUDIO_PERMISSION_REQUEST_CODE
            )
        } else {
            startVoiceRecognition()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                startVoiceRecognition()
            } else {
                Toast.makeText(this, R.string.permission_record_audio_denied, Toast.LENGTH_LONG).show()
            }
        }
    }


    private fun startVoiceRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, R.string.speech_recognition_not_available, Toast.LENGTH_LONG).show()
            return
        }

        speechRecognizer?.destroy() // Destroy any existing instance
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { Log.d("SpeechRecognizer", "Ready for speech") }
                override fun onBeginningOfSpeech() { Log.d("SpeechRecognizer", "Beginning of speech") }
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { Log.d("SpeechRecognizer", "End of speech") }

                override fun onError(error: Int) {
                    Log.e("SpeechRecognizer", "Error: $error")
                    Toast.makeText(this@AddEditNoteActivity, getString(R.string.speech_recognition_error) + " Code: $error", Toast.LENGTH_SHORT).show()
                    this@AddEditNoteActivity.speechRecognizer?.destroy() // Clean up after error
                    this@AddEditNoteActivity.speechRecognizer = null
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (matches != null && matches.isNotEmpty()) {
                        val currentContent = binding.noteContentEditText.text.toString()
                        val newContent = if (currentContent.isEmpty()) matches[0] else "$currentContent ${matches[0]}"
                        binding.noteContentEditText.setText(newContent)
                        binding.noteContentEditText.setSelection(newContent.length) // Move cursor to end
                    }
                    this@AddEditNoteActivity.speechRecognizer?.destroy() // Clean up after results
                    this@AddEditNoteActivity.speechRecognizer = null
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speak_now))
        }
        speechRecognizer?.startListening(speechIntent)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data?.data != null) {
            val imageUri = data.data
            viewModel.setImageUri(imageUri.toString())
        }
    }

    override fun onDestroy() {
        speechRecognizer?.destroy() // Ensure cleanup on activity destroy
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_add_edit_note, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save_note -> {
                val title = binding.noteTitleEditText.text.toString().trim()
                val content = binding.noteContentEditText.text.toString().trim()

                if (title.isEmpty() && content.isEmpty()) {
                    Toast.makeText(this, "Cannot save empty note", Toast.LENGTH_SHORT).show()
                } else {
                    val imageUriToSave = viewModel.currentImageUri.value
                    viewModel.saveNote(title, content, if (currentNoteId == -1L) null else currentNoteId, imageUriToSave)
                }
                true
            }
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
