package com.example.flashcardapp.ui.deck

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.flashcardapp.data.db.AppDatabase
import com.example.flashcardapp.data.db.dao.DeckDao
import com.example.flashcardapp.data.db.entity.Deck
import kotlinx.coroutines.launch

class DeckViewModel(application: Application) : AndroidViewModel(application) {

    private val deckDao: DeckDao
    val allDecks: LiveData<List<Deck>>

    init {
        deckDao = AppDatabase.getDatabase(application).deckDao()
        allDecks = deckDao.getAllDecks()
    }

    fun insert(deck: Deck) = viewModelScope.launch {
        deckDao.insert(deck)
    }
}
