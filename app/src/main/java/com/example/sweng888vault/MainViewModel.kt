package com.example.sweng888vault

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException

class MainViewModel(application: Application) : AndroidViewModel(application), TTS.OnInitListener {

    private val _ttsStatus = MutableLiveData<String>()
    val ttsStatus: LiveData<String> = _ttsStatus

    private val _showToast = MutableLiveData<Event<String>>()
    val showToast: LiveData<Event<String>> = _showToast

    private var ttsManager: TTS = TTS(application.applicationContext, this)
    private var isTtsReady = false

    // --- TTS.OnInitListener Implementation ---
    override fun onTTSInitialized(success: Boolean) {
        if (success) {
            isTtsReady = true
            Log.i("MainViewModel_TTS", "TTS Engine initialized successfully.")
            // You could post an event or status if the Activity needs to know explicitly
            // _ttsStatus.postValue("TTS Ready") // Example
        } else {
            isTtsReady = false
            Log.e("MainViewModel_TTS", "TTS Engine initialization failed.")
            _ttsStatus.postValue("TTS Initialization Failed")
        }
    }

    override fun onTTSError(errorInfo: String) {
        isTtsReady = false
        Log.e("MainViewModel_TTS", "TTS Initialization Error: $errorInfo")
        _ttsStatus.postValue("TTS Error: $errorInfo")
    }
    // --- End of TTS.OnInitListener Implementation ---

    fun speakFileContent(file: File) {
        if (!isTtsReady) {
            _showToast.value = Event("Text-to-Speech engine is not ready yet.")
            if (ttsManager == null || !isTtsReady) { // Simplified check for re-init
                ttsManager = TTS(getApplication<Application>().applicationContext, this)
            }
            return
        }

        if (!file.isFile || !isTextBasedFile(file)) {
            _showToast.value = Event("Cannot read content from this file type: ${file.extension}")
            return
        }

        viewModelScope.launch {
            val content = readFileContent(file)
            if (content != null) {
                if (content.isNotEmpty()) {
                    ttsManager.speak(content)
                } else {
                    _showToast.value = Event("File is empty.")
                }
            } else {
                _showToast.value = Event("Error reading file.")
            }
        }
    }

    private fun isTextBasedFile(file: File): Boolean {
        val extension = file.extension.lowercase()
        return extension == "txt" || extension == "log" || extension == "md" // Add other text-based extensions
    }

    private suspend fun readFileContent(file: File): String? {
        return withContext(Dispatchers.IO) { // Perform file I/O on a background thread
            val content = StringBuilder()
            try {
                BufferedReader(FileReader(file)).use { br ->
                    var line: String?
                    while (br.readLine().also { line = it } != null) {
                        content.append(line).append("\n")
                    }
                }
                content.toString()
            } catch (e: IOException) {
                Log.e("MainViewModel_File", "Error reading file: ${file.absolutePath}", e)
                null
            }
        }
    }

    fun stopSpeaking() {
        if (isTtsReady) {
            ttsManager.stop()
        }
    }

    // Called when the ViewModel is cleared (e.g., when the Activity is finished)
    override fun onCleared() {
        Log.d("MainViewModel", "onCleared called, shutting down TTS.")
        ttsManager.shutdown()
        super.onCleared()
    }
}

/**
 * Used as a wrapper for data that is exposed via a LiveData that represents an event.
 * Helps to avoid events being triggered multiple times on configuration change.
 */
open class Event<out T>(private val content: T) {
    var hasBeenHandled = false
        private set // Allow external read but not write

    /**
     * Returns the content and prevents its use again.
     */
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    /**
     * Returns the content, even if it's already been handled.
     */
    fun peekContent(): T = content
}