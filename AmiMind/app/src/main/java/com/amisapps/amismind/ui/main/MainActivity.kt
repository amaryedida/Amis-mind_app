package com.amisapps.amismind.ui.main

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView.AdapterContextMenuInfo
import com.amisblog.amismind.data.database.model.Note
import com.amisblog.amismind.databinding.ActivityMainBinding
import com.amisblog.amismind.ui.addeditnote.AddEditNoteActivity

    private lateinit var binding: ActivityMainBinding
    private lateinit var mainViewModel: MainViewModel
    private lateinit var noteAdapter: NoteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize ViewModel
        mainViewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        // Initialize Adapter
        noteAdapter = NoteAdapter(this)

        // Setup RecyclerView
        binding.notesRecyclerView.apply {
            adapter = noteAdapter
            layoutManager = GridLayoutManager(this@MainActivity, 2) // 2 columns
        }

        // Observe LiveData
        mainViewModel.activeNotes.observe(this) { notes ->
            notes?.let {
                noteAdapter.submitList(it)
                Log.d("MainActivity", "Active notes updated: ${it.size} notes")
            }
        }

        // FAB OnClickListener
        binding.addNoteFab.setOnClickListener {
            val intent = Intent(this, AddEditNoteActivity::class.java)
            startActivity(intent)
            Log.d("MainActivity", "Add FAB clicked, launching AddEditNoteActivity")
        }
        registerForContextMenu(binding.notesRecyclerView) // Register RecyclerView for context menu
    }

    override fun onNoteItemClick(note: Note) {
        val intent = Intent(this, AddEditNoteActivity::class.java).apply {
            putExtra(AddEditNoteActivity.EXTRA_NOTE_ID, note.id)
        }
        startActivity(intent)
        Log.d("MainActivity", "Clicked on note ID: ${note.id}, Title: ${note.title}, launching AddEditNoteActivity")
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = item.menuInfo as? AdapterContextMenuInfo
        val position = info?.position ?: return super.onContextItemSelected(item)
        val note = noteAdapter.getNoteAt(position)

        if (note != null) {
            when (item.itemId) {
                R.id.action_archive_note -> {
                    mainViewModel.archiveNote(note)
                    Toast.makeText(this, "Note '${note.title}' archived", Toast.LENGTH_SHORT).show()
                    return true
                }
                R.id.action_delete_note -> {
                    showDeleteConfirmationDialog(note)
                    return true
                }
                else -> return super.onContextItemSelected(item)
            }
        }
        return super.onContextItemSelected(item)
    }

    private fun showDeleteConfirmationDialog(note: Note) {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_delete_note_title)
            .setMessage(getString(R.string.dialog_delete_note_message))
            .setPositiveButton(R.string.dialog_yes) { _, _ ->
                mainViewModel.deleteNote(note)
                Toast.makeText(this, "Note '${note.title}' deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.dialog_no, null)
            .show()
    }
}
