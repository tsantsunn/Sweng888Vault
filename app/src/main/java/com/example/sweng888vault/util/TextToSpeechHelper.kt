package com.example.sweng888vault.util

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
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
            val result = tts?.setLanguage(Locale.getDefault())
            isReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED

            if (!isReady) {
                Toast.makeText(context, "TTS language not supported", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "TTS initialization failed", Toast.LENGTH_SHORT).show()
        }
    }

    fun speak(text: String) {
        if (isReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            Log.w("TextToSpeechHelper", "TTS not ready")
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }

    fun stop() {
        tts?.stop()
    }

    fun synthesizeToFile(text: String, file: File, onComplete: (Boolean) -> Unit) {
        if (!isReady) {
            Log.e("TTS", "TTS not initialized or not ready")
            onComplete(false)
            return
        }

        val params = Bundle()
        val utteranceId = System.currentTimeMillis().toString()

        tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {}
            override fun onDone(utteranceId: String) {
                onComplete(true)
            }

            override fun onError(utteranceId: String) {
                onComplete(false)
            }
        })

        val result = tts?.synthesizeToFile(text, params, file, utteranceId)
        if (result != TextToSpeech.SUCCESS) {
            Log.e("TTS", "synthesizeToFile failed")
            onComplete(false)
        }
    }
}
