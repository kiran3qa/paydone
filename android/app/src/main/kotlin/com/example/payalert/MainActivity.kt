package com.example.payalert

import android.content.Context
import android.content.Intent
import android.provider.Settings
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val CHANNEL = "pay_speaker/native"
    private val PREFS = "pay_speaker_prefs"
    private val KEY_LISTENING = "listeningEnabled"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "setListeningEnabled" -> {
                        val enabled = call.argument<Boolean>("enabled") ?: false
                        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                        prefs.edit().putBoolean(KEY_LISTENING, enabled).apply()
                        result.success(null)
                    }
                    "getListeningEnabled" -> {
                        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                        val enabled = prefs.getBoolean(KEY_LISTENING, false)
                        result.success(enabled)
                    }
                    "openNotificationAccessSettings" -> {
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        result.success(null)
                    }
                    "isNotificationListenerEnabled" -> {
                        val pkg = packageName
                        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
                        val enabled = flat != null && flat.contains(pkg)
                        result.success(enabled)
                    }
                    "testSpeak" -> {
                        val text = call.argument<String>("text") ?: ""
                        PayTts.speak(this, text)
                        result.success(null)
                    }
                    else -> result.notImplemented()
                }
            }
    }
}
