package com.example.sweng888vault.util // Or your preferred package

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

object FileStorageManager {

    private const val ROOT_DIRECTORY_NAME = "UserContent" // Main directory for your app's content

    /**
     * Gets the root directory for storing user content within the app's internal storage.
     * Creates it if it doesn't exist.
     */
    private fun getRootContentDirectory(context: Context): File {
        val rootDir = File(context.filesDir, ROOT_DIRECTORY_NAME)
        if (!rootDir.exists()) {
            rootDir.mkdirs()
        }
        return rootDir
    }

    /**
     * Creates a new folder within a given parent path (relative to the root content directory).
     * @param context Context
     * @param folderName The name of the new folder.
     * @param parentRelativePath The relative path from the root content directory where the folder should be created.
     *                           Pass an empty string or null to create in the root.
     * @return True if folder was created successfully or already exists, false otherwise.
     */
    fun createFolder(context: Context, folderName: String, parentRelativePath: String? = null): Boolean {
        if (folderName.isBlank() || folderName.contains(File.separatorChar)) {
            Log.e("FileStorageManager", "Invalid folder name.")
            return false
        }
        val parentDir = if (parentRelativePath.isNullOrEmpty()) {
            getRootContentDirectory(context)
        } else {
            File(getRootContentDirectory(context), parentRelativePath)
        }

        if (!parentDir.exists() && !parentDir.mkdirs()) {
            Log.e("FileStorageManager", "Could not create parent directory: ${parentDir.absolutePath}")
            return false
        }

        val newFolder = File(parentDir, folderName)
        return try {
            if (newFolder.exists()) {
                Log.i("FileStorageManager", "Folder already exists: ${newFolder.absolutePath}")
                true // Or false if you want to explicitly prevent overwriting/indicate it exists
            } else {
                newFolder.mkdirs()
            }
        } catch (e: SecurityException) {
            Log.e("FileStorageManager", "SecurityException creating folder: ${newFolder.absolutePath}", e)
            false
        } catch (e: IOException) {
            Log.e("FileStorageManager", "IOException creating folder: ${newFolder.absolutePath}", e)
            false
        }
    }

    /**
     * Lists files and folders within a given relative path.
     * @param context Context
     * @param relativePath The relative path from the root content directory.
     *                     Pass an empty string or null to list content of the root.
     * @return List of File objects.
     */
    fun listItems(context: Context, relativePath: String? = null): List<File> {
        val directory = if (relativePath.isNullOrEmpty()) {
            getRootContentDirectory(context)
        } else {
            File(getRootContentDirectory(context), relativePath)
        }

        return directory.listFiles()?.toList() ?: emptyList()
    }

    /**
     * Saves a file (from a Uri, typically obtained from a file picker) into a specified folder.
     * @param context Context
     * @param sourceUri The Uri of the file to save.
     * @param destinationFileName The desired name for the saved file.
     * @param destinationRelativePath The relative path within the root content directory to save the file.
     * @return The File object of the saved file, or null on failure.
     */
    fun saveFile(
        context: Context,
        sourceUri: Uri,
        destinationFileName: String,
        destinationRelativePath: String
    ): File? {
        val destinationDir = File(getRootContentDirectory(context), destinationRelativePath)
        if (!destinationDir.exists() && !destinationDir.mkdirs()) {
            Log.e("FileStorageManager", "Could not create destination directory: ${destinationDir.absolutePath}")
            return null
        }

        val destinationFile = File(destinationDir, destinationFileName)

        try {
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Log.i("FileStorageManager", "File saved successfully: ${destinationFile.absolutePath}")
            return destinationFile
        } catch (e: IOException) {
            Log.e("FileStorageManager", "Error saving file", e)
            return null
        } catch (e: SecurityException) {
            Log.e("FileStorageManager", "Security error saving file", e)
            return null
        }
    }

    /**
     * Gets a File object for a given path relative to the app's content root.
     */
    fun getFile(context: Context, relativePath: String, fileName: String): File {
        return File(File(getRootContentDirectory(context), relativePath), fileName)
    }

    // Add methods for deleting files/folders, renaming, moving, etc. as needed
    fun deleteItem(item: File): Boolean {
        return try {
            if (item.isDirectory) {
                item.deleteRecursively() // Deletes folder and its contents
            } else {
                item.delete()
            }
        } catch (e: SecurityException) {
            Log.e("FileStorageManager", "SecurityException deleting item: ${item.absolutePath}", e)
            false
        }
    }
}