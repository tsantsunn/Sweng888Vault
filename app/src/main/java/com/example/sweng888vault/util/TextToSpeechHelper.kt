package com.example.sweng888vault.util

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.Toast
import java.io.File
import java.util.*

class TextToSpeechHelper(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.getDefault()) // Use default system language
            isReady = if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "The Language specified is not supported!")
                Toast.makeText(context, "TTS language not supported for your device's locale.", Toast.LENGTH_LONG).show()
                false
            } else {
                Log.i("TTS", "TTS Engine Initialized successfully.")
                true
            }
        } else {
            isReady = false
            Log.e("TTS", "TTS Initialization Failed!")
            Toast.makeText(context, "TTS initialization failed.", Toast.LENGTH_SHORT).show()
        }
    }

    fun speak(text: String, utteranceId: String = UUID.randomUUID().toString()) {
        if (isReady) {
            // Check if text exceeds max input length
            if (text.length >= TextToSpeech.getMaxSpeechInputLength()) {
                Log.w("TextToSpeechHelper", "Text length exceeds TTS max input length. Consider chunking.")
                // Optionally, you could truncate or try to split here, but it's better handled by the caller
                // For now, we'll try to speak it anyway, the engine might truncate or error.
            }
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } else {
            Log.w("TextToSpeechHelper", "TTS not ready. Cannot speak.")
            // Optionally, re-initialize or queue the request if you build such logic
            Toast.makeText(context, "TTS is not ready. Please try again shortly.", Toast.LENGTH_SHORT).show()
        }
    }

    fun stop() {
        if (isReady) {
            tts?.stop()
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null // Help garbage collection
        isReady = false
        Log.i("TTS", "TTS Engine Shut down.")
    }

    fun synthesizeToFile(text: String, file: File, onComplete: (Boolean) -> Unit) {
        if (!isReady) {
            Log.e("TTS", "TTS not initialized or not ready for synthesizeToFile")
            onComplete(false)
            return
        }

        val utteranceId = "synthesize_${System.currentTimeMillis()}"
        val params = Bundle()
        // No specific params needed for default synthesis to file usually,
        // but you could add things like KEY_PARAM_VOLUME if supported and needed.

        // It's good practice to set the listener before calling synthesizeToFile
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {
                Log.i("TTS", "Utterance started: $id")
            }

            override fun onDone(id: String?) {
                if (id == utteranceId) {
                    Log.i("TTS", "Utterance done (file synthesized): $id")
                    // Ensure the callback is on the main thread if it updates UI
                    (context as? android.app.Activity)?.runOnUiThread {
                        onComplete(true)
                    } ?: onComplete(true) // If context is not an Activity, call directly
                }
            }

            override fun onError(id: String?) {
                Log.e("TTS", "Utterance error (file synthesis failed): $id")
                (context as? android.app.Activity)?.runOnUiThread {
                    onComplete(false)
                } ?: onComplete(false)
            }

            override fun onError(utteranceId: String?, errorCode: Int) { // Overload for newer API levels
                super.onError(utteranceId, errorCode)
                Log.e("TTS", "Utterance error with code $errorCode (file synthesis failed): $utteranceId")
                (context as? android.app.Activity)?.runOnUiThread {
                    onComplete(false)
                } ?: onComplete(false)
            }
        })

        val result = tts?.synthesizeToFile(text, params, file, utteranceId)

        if (result == TextToSpeech.SUCCESS) {
            Log.i("TTS", "synthesizeToFile call successful for utteranceId: $utteranceId")
            // onComplete will be called by onDone in UtteranceProgressListener
        } else {
            Log.e("TTS", "synthesizeToFile call failed with result: $result")
            onComplete(false)
        }
    }

    fun isSpeaking(): Boolean {
        return if (isReady) {
            tts?.isSpeaking ?: false
        } else {
            false
        }
    }
}