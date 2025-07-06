package com.example.sweng888vault.util

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import android.widget.Toast
import java.io.File

object MediaManager {

    private var mediaPlayer: MediaPlayer? = null
    private var isPaused = false

    fun playAudio(context: Context, file: File) {
        if (!file.exists()) {
            Toast.makeText(context, "Audio file not found", Toast.LENGTH_SHORT).show()
            return
        }

        // If paused, resume instead of playing from beginning
        if (isPaused && mediaPlayer != null) {
            mediaPlayer?.start()
            isPaused = false
            return
        }

        stopAudio()

        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(file.absolutePath)
                prepare()
                start()
                setOnCompletionListener {
                    Toast.makeText(context, "Playback completed", Toast.LENGTH_SHORT).show()
                    stopAudio()
                }
            } catch (e: Exception) {
                Log.e("MediaManager", "Playback failed: ${e.message}", e)
                Toast.makeText(context, "Error playing audio", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun pauseAudio() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                isPaused = true
            }
        }
    }

    fun stopAudio() {
        mediaPlayer?.let {
            if (it.isPlaying || isPaused) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
        isPaused = false
    }

    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true
}


