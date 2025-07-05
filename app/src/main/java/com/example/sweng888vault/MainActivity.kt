package com.example.sweng888vault // Ensure this package is correct
// REMOVED: import androidx.compose.ui.semantics.text
// R class should be generated in the same package, or ensure correct import if different
// import com.example.sweng888vault.R
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sweng888vault.databinding.ActivityMainBinding
import com.example.sweng888vault.util.FileStorageManager
import java.io.File

class MainActivity : AppCompatActivity() {

    // View Binding variable
    private lateinit var binding: ActivityMainBinding

    private lateinit var fileAdapter: FileAdapter
    private var currentRelativePath: String = ""

    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    val fileName = getFileNameFromUri(uri)
                    if (fileName != null) {
                        FileStorageManager.saveFile(this, uri, fileName, currentRelativePath)?.let {
                            Toast.makeText(this, "File '$fileName' saved", Toast.LENGTH_SHORT).show()
                            loadFilesAndFolders()
                        } ?: run {
                            Toast.makeText(this, "Failed to save file '$fileName'", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Could not determine file name", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate the layout using View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup ActionBar (optional, but good for context)
        // Ensure you have a Toolbar with id 'toolbar' in your activity_main.xml layout
        setSupportActionBar(binding.toolbar)

        setupRecyclerView()
        loadFilesAndFolders() // Initial load for the root directory
        updateActionBar() // Initial ActionBar setup

        binding.buttonCreateFolder.setOnClickListener {
            showCreateFolderDialog()
        }

        //Shows popup menu of adding file from phone or scanning documents
        binding.buttonAddFile.setOnClickListener { view ->
           showPopupMenu(view)
        }

        // Handle "Up" navigation more broadly with OnBackPressedDispatcher
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentRelativePath.isNotEmpty()) {
                    navigateUpInFileSystem()
                } else {
                    // If already at root, allow default back press behavior (e.g., exit activity)
                    isEnabled = false // Disable this callback
                    onBackPressedDispatcher.onBackPressed() // Trigger default behavior
                    isEnabled = true // Re-enable for next time (if activity is not finished)
                }
            }
        })
    }


//Handles the PopupMenu when "Add File" is clicked
    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(this@MainActivity, view)
        popupMenu.menuInflater.inflate(R.menu.popup_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.addFromPhone -> {
                    openFilePicker()
                    true
                }
                R.id.scanDocument -> {
                    //PLACEHOLDER
                    Toast.makeText(this, "Scan Document selected", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun setupRecyclerView() {
        fileAdapter = FileAdapter(
            onItemClick = { file ->
                if (file.isDirectory) {
                    currentRelativePath = if (currentRelativePath.isEmpty()) {
                        file.name
                    } else {
                        "$currentRelativePath${File.separator}${file.name}"
                    }
                    loadFilesAndFolders()
                    updateActionBar()
                } else {
                    openFileWithProvider(file) // Use FileProvider for opening
                }
            },
            onItemDelete = { file ->
                showDeleteConfirmationDialog(file)
            }
        )
        binding.recyclerViewFiles.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = fileAdapter
            // Optional: Add item decoration for dividers
            // addItemDecoration(DividerItemDecoration(this@MainActivity, LinearLayoutManager.VERTICAL))
        }
    }

    private fun loadFilesAndFolders() {
        val items = FileStorageManager.listItems(this, currentRelativePath)
        fileAdapter.submitList(items.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))) // Sort folders first, then by name
    }

    private fun updateActionBar() {
        if (currentRelativePath.isEmpty()) {
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
            // It's good practice to use string resources for titles
            supportActionBar?.title = getString(com.example.sweng888vault.R.string.app_name) // Or a specific title like "My Vault"
        } else {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            val currentFolderName = currentRelativePath.substringAfterLast(File.separatorChar, currentRelativePath)
            supportActionBar?.title = currentFolderName
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        if (currentRelativePath.isNotEmpty()) {
            navigateUpInFileSystem()
            return true
        }
        return super.onSupportNavigateUp()
    }

    private fun navigateUpInFileSystem() {
        val lastSeparator = currentRelativePath.lastIndexOf(File.separatorChar)
        currentRelativePath = if (lastSeparator > -1) {
            currentRelativePath.substring(0, lastSeparator)
        } else {
            "" // Back to root
        }
        loadFilesAndFolders()
        updateActionBar()
    }

    private fun showCreateFolderDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Create New Folder") // Consider using string resources

        val input = EditText(this).apply {
            hint = "Folder Name" // Consider using string resources
        }
        builder.setView(input)

        builder.setPositiveButton("Create") { dialog, _ -> // Consider using string resources
            val folderName = input.text.toString().trim()
            if (folderName.isNotEmpty()) {
                if (folderName.any { it in ILLEGAL_CHARACTERS_FOR_FILENAME }) {
                    Toast.makeText(this, "Folder name contains invalid characters", Toast.LENGTH_SHORT).show() // Consider using string resources
                    return@setPositiveButton
                }

                if (FileStorageManager.createFolder(this, folderName, currentRelativePath)) {
                    Toast.makeText(this, "Folder '$folderName' created", Toast.LENGTH_SHORT).show()
                    loadFilesAndFolders()
                } else {
                    Toast.makeText(this, "Failed to create folder '$folderName'", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Folder name cannot be empty", Toast.LENGTH_SHORT).show() // Consider using string resources
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() } // Consider using string resources
        builder.show()
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        try {
            filePickerLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No app found to pick files.", Toast.LENGTH_LONG).show() // Consider using string resources
            Log.e("MainActivity", "No file picker found", e)
        }
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        var fileName: String? = null
        try {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) {
                        fileName = cursor.getString(displayNameIndex)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error getting file name from URI", e)
        }
        if (fileName == null) {
            fileName = uri.lastPathSegment?.substringAfterLast('/')
        }
        return fileName?.replace(Regex("[$ILLEGAL_CHARACTERS_FOR_FILENAME]"), "_")
    }

    private fun openFileWithProvider(file: File) {
        val authority = "${applicationContext.packageName}.fileprovider"
        val fileUri: Uri = try {
            FileProvider.getUriForFile(this, authority, file)
        } catch (e: IllegalArgumentException) {
            Log.e("MainActivity", "File URI generation failed for: ${file.absolutePath}", e)
            Toast.makeText(this, "Error: Could not share file.", Toast.LENGTH_LONG).show()
            return
        }

        // Declare mimeType outside the 'apply' block
        val extension = file.extension.lowercase()
        val resolvedMimeType = MimeTypeUtil.getMimeType(extension) // Renamed to avoid confusion if needed

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, resolvedMimeType) // Use the variable declared outside
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No app found to open this file type: ${file.extension}", Toast.LENGTH_LONG).show()
            // Now you can access resolvedMimeType here
            Log.w("MainActivity", "No app to open ${file.absolutePath} with MIME $resolvedMimeType", e)
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open file.", Toast.LENGTH_LONG).show()
            Log.e("MainActivity", "Error opening file: ${file.absolutePath}", e)
        }
    }

    private fun showDeleteConfirmationDialog(file: File) {
        AlertDialog.Builder(this)
            .setTitle("Delete Item") // Consider using string resources
            .setMessage("Are you sure you want to delete '${file.name}'?") // Consider using string resources
            .setPositiveButton("Delete") { _, _ -> // Consider using string resources
                if (FileStorageManager.deleteItem(file)) {
                    Toast.makeText(this, "'${file.name}' deleted", Toast.LENGTH_SHORT).show()
                    loadFilesAndFolders()
                } else {
                    Toast.makeText(this, "Failed to delete '${file.name}'", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null) // Consider using string resources
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    companion object {
        private const val ILLEGAL_CHARACTERS_FOR_FILENAME = "/\\:*?\"<>|"
    }
}

// Optional: Create a MimeTypeUtil.kt for better MIME type handling
object MimeTypeUtil {
    fun getMimeType(extension: String): String {
        return when (extension.lowercase()) {
            "pdf" -> "application/pdf"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "mp4" -> "video/mp4"
            "3gp" -> "video/3gpp"
            "mkv" -> "video/x-matroska"
            "webm" -> "video/webm"
            "txt" -> "text/plain"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "ppt" -> "application/vnd.ms-powerpoint"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "zip" -> "application/zip"
            "rar" -> "application/x-rar-compressed"
            // Add more as needed
            else -> "*/*" // Generic fallback
        }
    }
}