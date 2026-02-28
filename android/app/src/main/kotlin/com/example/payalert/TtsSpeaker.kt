package com.example.payalert

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

object TtsSpeaker {

    private var tts: TextToSpeech? = null
    private var isReady = false

    private fun init(context: Context) {
        if (tts != null) return

        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("en", "IN")
                isReady = true
            }
        }
    }

    fun speak(context: Context, text: String) {
        init(context)
        if (isReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "payalert")
        }
    }
}
