package com.example.sweng888vault.util // Or your app's util package

import android.webkit.MimeTypeMap
import java.io.File

object MimeTypeUtil { // Crucially, it's an 'object'

    fun getMimeType(file: File): String? { // Basic version using Android's MimeTypeMap
        val extension = file.extension.lowercase()
        return if (extension.isNotEmpty()) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        } else {
            null
        }
    }

    fun getMimeTypeExplicit(file: File): String { // Your explicit version
        return when (file.extension.lowercase()) {
            "pdf" -> "application/pdf"
            "txt" -> "text/plain"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/x-wav"
            "m4a" -> "audio/mp4" // Corrected from audio/m4a if that was a typo, mp4 container for audio
            "aac" -> "audio/aac"
            "ogg" -> "audio/ogg"
            "mp4" -> "video/mp4"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "ppt" -> "application/vnd.ms-powerpoint"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "zip" -> "application/zip"
            "rar" -> "application/x-rar-compressed"
            // Add more common types as needed
            else -> getMimeType(file) ?: "*/*" // Fallback to system or generic
        }
    }
}