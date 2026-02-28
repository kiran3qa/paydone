package com.example.payalert

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * PayTts - Singleton for managing TextToSpeech instance
 * Ensures only one TextToSpeech instance is created and properly managed
 */
object PayTts : TextToSpeech.OnInitListener {
    private var textToSpeech: TextToSpeech? = null
    private var isReady = false
    private val lock = Any()

    /**
     * Initialize TextToSpeech with application context
     * Safe to call multiple times - only initializes once
     */
    fun init(context: Context) {
        synchronized(lock) {
            if (textToSpeech == null) {
                try {
                    textToSpeech = TextToSpeech(context.applicationContext, this)
                    Log.d("PayTts", "TextToSpeech initialization started")
                } catch (e: Exception) {
                    Log.e("PayTts", "Error initializing TextToSpeech: ${e.message}", e)
                }
            }
        }
    }

    /**
     * TextToSpeech initialization callback
     */
    override fun onInit(status: Int) {
        synchronized(lock) {
            if (status == TextToSpeech.SUCCESS) {
                try {
                    // Try to set language to en-IN (India English), fallback to en-US
                    val languageResult = textToSpeech?.setLanguage(Locale("en", "IN"))
                    if (languageResult == TextToSpeech.LANG_MISSING_DATA || languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.w("PayTts", "en-IN not supported, falling back to en-US")
                        textToSpeech?.setLanguage(Locale.US)
                    }
                    isReady = true
                    Log.d("PayTts", "TextToSpeech ready with language set")
                } catch (e: Exception) {
                    Log.e("PayTts", "Error setting language: ${e.message}", e)
                    isReady = false
                }
            } else {
                Log.e("PayTts", "TextToSpeech initialization failed with status: $status")
                isReady = false
            }
        }
    }

    /**
     * Speak text with optional rate and pitch control
     * @param context Android context (used for lazy initialization)
     * @param text Text to speak
     * @param rate Speech rate (1.0f = normal, < 1.0f = slower, > 1.0f = faster)
     * @param pitch Pitch (1.0f = normal, < 1.0f = lower, > 1.0f = higher)
     */
    fun speak(context: Context, text: String, rate: Float = 1.0f, pitch: Float = 1.0f) {
        synchronized(lock) {
            try {
                // Initialize if not already done
                if (textToSpeech == null) {
                    init(context)
                }

                // Queue speech even if not ready yet
                textToSpeech?.apply {
                    setSpeechRate(rate)
                    setPitch(pitch)
                    speak(text, TextToSpeech.QUEUE_FLUSH, null)
                    Log.d("PayTts", "Speaking: '$text' (rate=$rate, pitch=$pitch)")
                }
            } catch (e: Exception) {
                Log.e("PayTts", "Error speaking: ${e.message}", e)
            }
        }
    }

    /**
     * Shutdown and cleanup TextToSpeech instance
     * Should be called when the app is being destroyed to avoid memory leaks
     */
    fun shutdown() {
        synchronized(lock) {
            try {
                textToSpeech?.stop()
                textToSpeech?.shutdown()
                textToSpeech = null
                isReady = false
                Log.d("PayTts", "TextToSpeech shut down successfully")
            } catch (e: Exception) {
                Log.e("PayTts", "Error shutting down TextToSpeech: ${e.message}", e)
            }
        }
    }
}
