package com.example.flashcardapp.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.flashcardapp.data.db.entity.Deck

@Dao
interface DeckDao {
    @Insert
    suspend fun insert(deck: Deck)

    @Query("SELECT * FROM decks ORDER BY deckName ASC")
    fun getAllDecks(): LiveData<List<Deck>>
}
