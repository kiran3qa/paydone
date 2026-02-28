package com.example.payalert

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val CHANNEL = "pay_speaker/native"
    private val PREFS = "pay_speaker_prefs"
    private val KEY_LISTENING = "listeningEnabled"

    // ✅ Added: request code
    private val REQ_SMS_PERMISSION = 1001

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        // ✅ Added: init TTS early (optional but recommended)
        TtsSpeaker.init(this)

        // ✅ Added: request RECEIVE_SMS permission at runtime
        requestSmsPermissionIfNeeded()

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

    // ✅ Added: permission helper (minimal)
    private fun requestSmsPermissionIfNeeded() {
        // Runtime permissions apply from Android 6.0 (API 23+)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return

        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECEIVE_SMS),
                REQ_SMS_PERMISSION
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // ✅ Added: optional cleanup
        TtsSpeaker.shutdown()
    }
}
