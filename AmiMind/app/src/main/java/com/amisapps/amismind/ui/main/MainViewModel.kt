package com.amisapps.amismind.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.amisapps.amismind.data.database.AppDatabase
import com.amisapps.amismind.data.database.model.Note
import com.amisapps.amismind.data.repository.NoteRepository
import kotlinx.coroutines.launch


class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val noteRepository: NoteRepository
    val activeNotes: LiveData<List<Note>> // Still observe local DAO

    init {
        val noteDao = AppDatabase.getDatabase(application).noteDao()
        noteRepository = NoteRepository(noteDao) // FirebaseUtils.notesCollection is default in repo
        activeNotes = noteDao.getAllActiveNotes()
        noteRepository.syncNotes() // Start syncing with Firestore
    }

    fun archiveNote(note: Note) {
        viewModelScope.launch {
            noteRepository.archiveOrUnarchiveNote(note, true)
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            noteRepository.deleteNote(note)
        }
    }
}
