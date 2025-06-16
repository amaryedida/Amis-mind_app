package com.example.flashcardapp

import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.flashcardapp.data.db.entity.Deck
import com.example.flashcardapp.databinding.ActivityMainBinding // ViewBinding
import com.example.flashcardapp.ui.deck.DeckAdapter
import com.example.flashcardapp.ui.deck.DeckViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding // ViewBinding
    private lateinit var deckViewModel: DeckViewModel
    private lateinit var deckAdapter: DeckAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater) // ViewBinding
        setContentView(binding.root) // ViewBinding

        // Initialize ViewModel
        deckViewModel = ViewModelProvider(this).get(DeckViewModel::class.java)

        // Setup RecyclerView
        deckAdapter = DeckAdapter(emptyList())
        binding.decksRecyclerView.adapter = deckAdapter
        binding.decksRecyclerView.layoutManager = LinearLayoutManager(this)

        // Observe LiveData
        deckViewModel.allDecks.observe(this) { decks ->
            decks?.let { deckAdapter.setDecks(it) }
        }

        // FAB OnClickListener
        binding.addDeckFab.setOnClickListener {
            showAddDeckDialog()
        }
    }

    private fun showAddDeckDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add New Deck")

        val input = EditText(this)
        input.hint = "Enter deck name"
        builder.setView(input)

        builder.setPositiveButton("Add") { dialog, _ ->
            val deckName = input.text.toString().trim()
            if (deckName.isNotEmpty()) {
                deckViewModel.insert(Deck(deckName = deckName))
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }
        builder.show()
    }
}
