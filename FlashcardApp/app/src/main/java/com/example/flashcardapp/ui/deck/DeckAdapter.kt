package com.example.flashcardapp.ui.deck

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.flashcardapp.R
import com.example.flashcardapp.data.db.entity.Deck

class DeckAdapter(private var decks: List<Deck>) : RecyclerView.Adapter<DeckAdapter.DeckViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeckViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.deck_list_item, parent, false)
        return DeckViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: DeckViewHolder, position: Int) {
        val currentDeck = decks[position]
        holder.deckNameTextView.text = currentDeck.deckName
    }

    override fun getItemCount() = decks.size

    fun setDecks(decks: List<Deck>) {
        this.decks = decks
        notifyDataSetChanged()
    }

    class DeckViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deckNameTextView: TextView = itemView.findViewById(R.id.deckNameTextView)
    }
}
