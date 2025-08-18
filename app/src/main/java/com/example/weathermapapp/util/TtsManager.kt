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
            // Try to set the default language.
            val defaultLocale = Locale.getDefault()
            when (tts?.setLanguage(defaultLocale)) {
                TextToSpeech.LANG_AVAILABLE, TextToSpeech.LANG_COUNTRY_AVAILABLE, TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE -> {
                    Log.d("TtsManager", "Default language '${defaultLocale.toLanguageTag()}' is supported.")
                    isInitialized = true
                    onInitializationFinished(true)
                    return
                }
            }

            Log.w("TtsManager", "Default language '${defaultLocale.toLanguageTag()}' not supported. Trying fallbacks.")

            // Fallback 1: US English
            val usLocale = Locale.US
            when (tts?.setLanguage(usLocale)) {
                TextToSpeech.LANG_AVAILABLE, TextToSpeech.LANG_COUNTRY_AVAILABLE, TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE -> {
                    Log.d("TtsManager", "Fallback language '${usLocale.toLanguageTag()}' is supported.")
                    isInitialized = true
                    onInitializationFinished(true)
                    return
                }
            }

            Log.w("TtsManager", "Fallback language '${usLocale.toLanguageTag()}' not supported. Searching for any available language.")

            // Fallback 2: Any available language
            tts?.availableLanguages?.firstOrNull()?.let { availableLocale ->
                tts?.language = availableLocale
                Log.d("TtsManager", "Found and set available language: '${availableLocale.toLanguageTag()}'")
                isInitialized = true
                onInitializationFinished(true)
                return
            }

            Log.e("TtsManager", "No supported language found on the device.")
            isInitialized = false
            onInitializationFinished(false)

        } else {
            Log.e("TtsManager", "TTS Engine initialization failed with status: $status")
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
