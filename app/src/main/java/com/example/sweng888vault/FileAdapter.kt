package com.example.sweng888vault // Or your adapter package

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class FileAdapter(
    private val onItemClick: (File) -> Unit,
    private val onItemDelete: (File) -> Unit, // Callback for delete action
    private val onTextToSpeech: (File) -> Unit
) : ListAdapter<File, FileAdapter.FileViewHolder>(FileDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file, parent, false) // Create item_file.xml layout
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val file = getItem(position)
        holder.bind(file, onItemClick, onItemDelete, onTextToSpeech)
    }

    class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.fileIcon)
        private val name: TextView = itemView.findViewById(R.id.fileName)
        private val menuButton: ImageButton = itemView.findViewById(R.id.menuButton)


        fun bind(
            file: File,
            onItemClick: (File) -> Unit,
            onItemDelete: (File) -> Unit,
            onTextToSpeech: (File) -> Unit
        ) {
            name.text = file.name
            if (file.isDirectory) {
                icon.setImageResource(R.drawable.folder) // Create this drawable
            } else {
                // Set icons based on file type (pdf, image, video etc.)
                when (file.extension.lowercase()) {
                    "pdf" -> icon.setImageResource(R.drawable.pdfico) // Create this
                    "jpg", "jpeg", "png" -> icon.setImageResource(R.drawable.imageico) // Create this
                    "mp4", "mov", "avi" -> icon.setImageResource(R.drawable.videoico) // Create this
                    else -> icon.setImageResource(R.drawable.fileico) // Generic file icon
                }
            }
            itemView.setOnClickListener { onItemClick(file) }
            // Show popup menu on menuButton click
            menuButton.setOnClickListener { view ->
                val popup = PopupMenu(view.context, view)
                popup.menuInflater.inflate(R.menu.file_item_menu, popup.menu)

                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.menu_delete -> {
                            onItemDelete(file)
                            true
                        }

                        R.id.menu_tts -> {
                            onTextToSpeech(file)
                            true
                        }

                        else -> false
                    }
                }

                popup.show()
            }
        }
    }
}

class FileDiffCallback : DiffUtil.ItemCallback<File>() {
    override fun areItemsTheSame(oldItem: File, newItem: File): Boolean {
        return oldItem.absolutePath == newItem.absolutePath
    }

    override fun areContentsTheSame(oldItem: File, newItem: File): Boolean {
        // For files, you might want to compare lastModified or size if relevant
        return oldItem.name == newItem.name && oldItem.isDirectory == newItem.isDirectory && oldItem.length() == newItem.length()
    }
}