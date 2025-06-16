package com.amisapps.amismind.ui.addeditnote

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.amisapps.amismind.data.database.AppDatabase
import com.amisapps.amismind.data.database.model.Note
import com.amisapps.amismind.data.repository.NoteRepository
import kotlinx.coroutines.launch

class AddEditNoteViewModel(application: Application) : AndroidViewModel(application) {

    private val noteRepository: NoteRepository
    private val _currentNote = MutableLiveData<Note?>() // Used to populate UI from Room
    val currentNote: LiveData<Note?> = _currentNote

    private val _currentImageUri = MutableLiveData<String?>()
    val currentImageUri: LiveData<String?> = _currentImageUri

    private val _saveFinished = MutableLiveData<Boolean>()
    val saveFinished: LiveData<Boolean> = _saveFinished

    init {
        val noteDao = AppDatabase.getDatabase(application).noteDao()
        noteRepository = NoteRepository(noteDao)
        // No initial sync needed here as MainActivity's ViewModel handles global sync.
        // This ViewModel loads specific note from Room (which is being synced).
    }

    fun loadNote(noteId: Long) {
        if (noteId == -1L) {
            _currentNote.value = null
            _currentImageUri.value = null // Ensure image URI is also cleared for new note
            return
        }
        viewModelScope.launch {
            // Fetch from DAO. Room is our single source of truth for UI.
            // Firestore sync updates Room in the background via NoteRepository in MainViewModel.
            val note = noteRepository.noteDao.getNoteById(noteId) // Direct DAO access for loading
            _currentNote.value = note
            _currentImageUri.value = note?.imageUri
        }
    }

    fun setImageUri(uriString: String?) {
        _currentImageUri.value = uriString
    }

    fun saveNote(title: String, content: String, existingNoteId: Long?, currentImageUriValue: String?) {
        viewModelScope.launch {
            val noteToSave: Note
            if (existingNoteId != null && existingNoteId != -1L) {
                // Existing note: Try to fetch it to preserve firebaseId and createdAt
                var existingNote = noteRepository.noteDao.getNoteById(existingNoteId)
                if (existingNote != null) {
                    existingNote.title = title
                    existingNote.content = content
                    existingNote.imageUri = currentImageUriValue
                    // existingNote.updatedAt will be set by Repository/Firestore
                    noteToSave = existingNote
                } else {
                    // Should not happen if ID is valid, but as fallback create new
                    noteToSave = Note(title = title, content = content, imageUri = currentImageUriValue)
                    // This new note won't have the original firebaseId if existingNote was null.
                    // Consider error logging or different handling.
                }
            } else {
                // New note
                noteToSave = Note(title = title, content = content, imageUri = currentImageUriValue)
            }
            noteRepository.saveNote(noteToSave)
            _saveFinished.value = true
        }
    }

    fun resetSaveFinished() {
        _saveFinished.value = false
    }
}
