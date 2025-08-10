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

    fun speak(text: String, baseUtteranceId: String = UUID.randomUUID().toString()) {
        if (!isReady) {
            Log.w("TextToSpeechHelper", "TTS not ready. Cannot speak.")
            Toast.makeText(context, "TTS is not ready. Please try again shortly.", Toast.LENGTH_SHORT).show()
            return
        }

        val maxLength = TextToSpeech.getMaxSpeechInputLength()
        if (text.length <= maxLength) {
            // Safe to speak directly
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, baseUtteranceId)
        } else {
            Log.w("TextToSpeechHelper", "Text exceeds max length. Splitting into chunks...")

            // Split text into safe chunks
            val chunks = text.chunked(maxLength - 100) // 100-char buffer to avoid hard limit issues

            for ((index, chunk) in chunks.withIndex()) {
                val utteranceId = "$baseUtteranceId-$index"
                val queueMode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
                tts?.speak(chunk, queueMode, null, utteranceId)
            }
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