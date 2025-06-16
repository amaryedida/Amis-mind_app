package com.amisapps.amismind.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ContextMenu
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.amisapps.amismind.R
import com.amisapps.amismind.data.database.model.Note
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NoteAdapter(private val listener: OnNoteItemClickListener) :
    ListAdapter<Note, NoteAdapter.NoteViewHolder>(NoteDiffCallback()) {

    interface OnNoteItemClickListener {
        fun onNoteItemClick(note: Note)
        // We'll let MainActivity handle context menu item clicks directly
        // by using adapter.getNoteAt(position)
    }

    fun getNoteAt(position: Int): Note? {
        return getItem(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.note_item, parent, false)
        return NoteViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val currentNote = getItem(position)
        holder.bind(currentNote)
    }

    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.noteItemTitleTextView)
        private val contentTextView: TextView = itemView.findViewById(R.id.noteItemContentTextView)
        private val timestampTextView: TextView = itemView.findViewById(R.id.noteItemTimestampTextView)
        private val imageView: ImageView = itemView.findViewById(R.id.noteItemImageView)
        private var currentNote: Note? = null

        init {
            itemView.setOnClickListener {
                currentNote?.let {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) { // Check position validity
                        listener.onNoteItemClick(it)
                    }
                }
            }
            itemView.setOnCreateContextMenuListener(this) // For context menu
        }

        fun bind(note: Note) {
            currentNote = note // Store the current note for context menu
            titleTextView.text = note.title
            contentTextView.text = note.content
            timestampTextView.text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(note.updatedAt))

            if (note.imageUri != null) {
                try {
                    imageView.setImageURI(android.net.Uri.parse(note.imageUri))
                    imageView.visibility = View.VISIBLE
                } catch (e: Exception) {
                    // Log error or set placeholder if URI is invalid or image loading fails
                    imageView.visibility = View.GONE
                }
            } else {
                imageView.visibility = View.GONE
            }
        }

        // Called when context menu is being built for this item
        override fun onCreateContextMenu(
            menu: ContextMenu?,
            v: View?,
            menuInfo: ContextMenu.ContextMenuInfo?
        ) {
            currentNote?.let { // Ensure there's a note associated
                // Set note or its ID to the menu, or rely on adapter position in MainActivity
                // For simplicity, MainActivity will get the note using adapter.getNoteAt(adapterPosition)
                // which means menuInfo (AdapterContextMenuInfo) will be crucial.
                val activity = itemView.context as? android.app.Activity
                activity?.menuInflater?.inflate(R.menu.menu_note_item_context, menu)

                // You can pass the note ID or position through the menu item itself if needed,
                // but typically AdapterContextMenuInfo in onContextItemSelected is used.
                 menu?.setHeaderTitle(it.title) // Optional: set title for context menu
            }
        }
    }

    class NoteDiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem == newItem
        }
    }
}
