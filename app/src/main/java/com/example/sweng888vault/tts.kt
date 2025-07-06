package com.example.sweng888vault // Or your relevant package

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.Toast
import java.util.Locale

class TTS(private val context: Context, private val initializationListener: OnInitListener? = null) :
    TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var language: Locale = Locale.US // Default language

    interface OnInitListener {
        fun onTTSInitialized(success: Boolean)
        fun onTTSError(errorInfo: String)
    }

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e("TTS_Custom", "Exception during TTS instantiation: ${e.message}", e)
            initializationListener?.onTTSError("Failed to create TextToSpeech instance.")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(language)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS_Custom", "Language ($language) not supported or missing data.")
                isInitialized = false
                initializationListener?.onTTSError("Language ($language) not supported or missing data.")
            } else {
                Log.i("TTS_Custom", "TTS Initialized successfully with language: $language")
                isInitialized = true
                initializationListener?.onTTSInitialized(true)
            }
        } else {
            Log.e("TTS_Custom", "TTS Initialization failed with status: $status")
            isInitialized = false
            initializationListener?.onTTSError("TTS Initialization failed with status: $status")
        }

        // Optional: Set up an utterance progress listener
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d("TTS_Custom", "Speech started: $utteranceId")
            }

            override fun onDone(utteranceId: String?) {
                Log.d("TTS_Custom", "Speech completed: $utteranceId")
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                Log.e("TTS_Custom", "Speech error: $utteranceId")
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                Log.e("TTS_Custom", "Speech error with code $errorCode: $utteranceId")
            }
        })
    }

    fun setLanguage(locale: Locale): Boolean {
        this.language = locale
        if (isInitialized) {
            val result = tts?.setLanguage(language)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w("TTS_Custom", "Could not set new language ($language) after initialization.")
                return false
            }
            return true
        }
        // If not initialized, language will be set during onInit
        return true // Tentatively true, actual check happens in onInit
    }

    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        if (isInitialized && tts != null) {
            if (text.isNotEmpty()) {
                val utteranceId = this.hashCode().toString() + System.currentTimeMillis()
                tts?.speak(text, queueMode, null, utteranceId)
            } else {
                Log.w("TTS_Custom", "Attempted to speak empty text.")
            }
        } else {
            Log.e("TTS_Custom", "TTS not initialized or null, cannot speak.")
            Toast.makeText(context, "Text-to-Speech is not ready.", Toast.LENGTH_SHORT).show()
        }
    }

    fun isSpeaking(): Boolean {
        return tts?.isSpeaking ?: false
    }

    fun stop() {
        tts?.stop()
    }

    // Call this when the TTS service is no longer needed (e.g., in Activity's onDestroy)
    fun shutdown() {
        if (tts != null) {
            Log.i("TTS_Custom", "Shutting down TTS engine.")
            tts?.stop()
            tts?.shutdown()
            tts = null // Help garbage collection
            isInitialized = false
        }
    }
}
