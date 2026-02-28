package com.example.payalert

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

object TtsSpeaker {
    private var tts: TextToSpeech? = null
    private var isReady = false

    // ✅ was private — make it public so MainActivity can call it
    fun init(context: Context) {
        if (tts != null) return

        tts = TextToSpeech(context.applicationContext) { status ->
            isReady = (status == TextToSpeech.SUCCESS)
            if (isReady) {
                tts?.language = Locale("en", "IN")
            }
        }
    }

    fun speak(context: Context, text: String, rate: Float = 1.0f, pitch: Float = 1.0f) {
        if (tts == null) init(context)
        if (!isReady) return

        tts?.setSpeechRate(rate)
        tts?.setPitch(pitch)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "payalert_tts")
    }

    // ✅ add this so MainActivity can call TtsSpeaker.shutdown()
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}
