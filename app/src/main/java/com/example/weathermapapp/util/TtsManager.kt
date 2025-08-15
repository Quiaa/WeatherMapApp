package com.example.weathermapapp.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TtsManager(
    context: Context,
    private val onInitializationFinished: (Boolean) -> Unit
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        try {
            tts = TextToSpeech(context, this)
        } catch (e: Exception) {
            Log.e("TtsManager", "Failed to create TextToSpeech instance", e)
            onInitializationFinished(false)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val defaultLocale = Locale.getDefault()
            val result = tts?.setLanguage(defaultLocale)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w("TtsManager", "Default language not supported, falling back to US English")
                val fallbackResult = tts?.setLanguage(Locale.US)
                if (fallbackResult == TextToSpeech.LANG_MISSING_DATA || fallbackResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TtsManager", "Fallback language (US English) also not supported.")
                    isInitialized = false
                    onInitializationFinished(false)
                } else {
                    isInitialized = true
                    onInitializationFinished(true)
                }
            } else {
                isInitialized = true
                onInitializationFinished(true)
            }
        } else {
            Log.e("TtsManager", "TTS Initialization failed with status: $status")
            isInitialized = false
            onInitializationFinished(false)
        }
    }

    fun speak(text: String) {
        if (isInitialized) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            Log.e("TtsManager", "TTS not initialized, cannot speak")
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        isInitialized = false
    }
}
