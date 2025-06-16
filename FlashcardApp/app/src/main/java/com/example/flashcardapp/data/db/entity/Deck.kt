package com.example.flashcardapp.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "decks")
data class Deck(
    @PrimaryKey(autoGenerate = true)
    val deckId: Long = 0,
    val deckName: String
)
