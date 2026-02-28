package com.example.pay_speaker

import android.content.Context
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

object TtsSpeaker : TextToSpeech.OnInitListener {

    private const val TAG = "TtsSpeaker"

    private var tts: TextToSpeech? = null
    private var appContext: Context? = null
    private var isReady: Boolean = false
    private var isInitInProgress: Boolean = false

    // If speak() is called before TTS is ready, store messages here and flush later
    private val pendingQueue: ConcurrentLinkedQueue<String> = ConcurrentLinkedQueue()

    /**
     * Call once from MainActivity (recommended), but not required.
     */
    fun init(context: Context) {
        val ctx = context.applicationContext
        appContext = ctx

        if (isReady || isInitInProgress) return

        isInitInProgress = true
        tts = TextToSpeech(ctx, this)
    }

    /**
     * Safe to call from anywhere (including BroadcastReceiver).
     * It will auto-init if needed.
     */
    fun speak(context: Context, text: String) {
        if (text.isBlank()) return

        // Ensure initialized
        if (tts == null || appContext == null) {
            init(context)
        }

        if (!isReady) {
            // Queue until ready
            pendingQueue.add(text)
            Log.d(TAG, "TTS not ready yet, queued: $text")
            return
        }

        speakNow(text)
    }

    override fun onInit(status: Int) {
        isInitInProgress = false

        if (status != TextToSpeech.SUCCESS) {
            Log.e(TAG, "TTS init failed: $status")
            isReady = false
            return
        }

        val engine = tts ?: run {
            isReady = false
            return
        }

        // Language: English (India). Fallback to US if not available.
        val langResult = engine.setLanguage(Locale("en", "IN"))
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "en-IN not supported, falling back to en-US")
            engine.setLanguage(Locale.US)
        }

        engine.setSpeechRate(1.0f)
        engine.setPitch(1.0f)

        isReady = true
        Log.d(TAG, "TTS ready")

        // Flush queued items
        flushPendingQueue()
    }

    private fun flushPendingQueue() {
        if (!isReady) return
        while (true) {
            val msg = pendingQueue.poll() ?: break
            speakNow(msg)
        }
    }

    private fun speakNow(text: String) {
        val engine = tts ?: return

        val params = android.os.Bundle().apply {
            // STREAM_MUSIC is usually best; switch to STREAM_ALARM if you want louder priority behavior
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
        }

        // QUEUE_ADD so multiple credits can be spoken in sequence
        engine.speak(text, TextToSpeech.QUEUE_ADD, params, "sms_credit_${System.currentTimeMillis()}")
        Log.d(TAG, "Speaking: $text")
    }

    /**
     * Optional: Call from MainActivity.onDestroy()
     */
    fun shutdown() {
        try {
            pendingQueue.clear()
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "shutdown error", e)
        } finally {
            tts = null
            isReady = false
            isInitInProgress = false
            appContext = null
        }
    }
}
