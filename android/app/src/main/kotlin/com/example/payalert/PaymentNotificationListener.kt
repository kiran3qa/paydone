package com.example.payalert

import android.content.SharedPreferences
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.preference.PreferenceManager
import java.util.LinkedHashMap

class PaymentNotificationListener : NotificationListenerService() {
    private lateinit var sharedPreferences: SharedPreferences
    private val dedupeCache = LRUCache<String, Boolean>(50)
    
    private val allowedPackages = setOf(
        "com.google.android.apps.nbu.paisa.user",
        "net.one97.paytm",
        "net.one97.paytm.business"
    )
    
    private val acceptKeywords = setOf(
        "received",
        "credited",
        "payment received",
        "money received"
    )
    
    private val rejectKeywords = setOf(
        "debited",
        "sent",
        "paid",
        "spent",
        "transfer",
        "payment to"
    )
    
    override fun onCreate() {
        super.onCreate()
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
    }
    
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            // Check if listening is enabled
            if (!isListeningEnabled()) {
                return
            }
            
            // Check if package is allowed
            val packageName = sbn.packageName
            if (!allowedPackages.contains(packageName)) {
                return
            }
            
            // Deduplicate using sbn.key
            if (dedupeCache.containsKey(sbn.key)) {
                Log.d("PaymentNotificationListener", "Duplicate notification skipped: ${sbn.key}")
                return
            }
            dedupeCache.put(sbn.key, true)
            
            // Extract notification text
            val notification = sbn.notification
            val extras = notification.extras
            
            val title = extras.getString("android.title", "")
            val text = extras.getString("android.text", "")
            val bigText = extras.getString("android.bigText", "")
            
            val fullText = buildFullText(title, text, bigText)
            
            Log.d("PaymentNotificationListener", "Full text: $fullText")
            
            // Check if it's a credit notification
            if (!isPaymentCredit(fullText)) {
                Log.d("PaymentNotificationListener", "Not a payment credit notification")
                return
            }
            
            // Extract amount
            val amount = extractAmount(fullText)
            
            // Build speak text
            val speakText = if (amount != null) {
                "Payment received: $amount rupees"
            } else {
                "Payment received"
            }
            
            // Speak using PayTts
            PayTts.speak(applicationContext, speakText)
            Log.d("PaymentNotificationListener", "Spoke: $speakText")
            
        } catch (e: Exception) {
            Log.e("PaymentNotificationListener", "Error processing notification: ${e.message}", e)
        }
    }
    
    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Not needed for this implementation
    }
    
    private fun isListeningEnabled(): Boolean {
        return sharedPreferences.getBoolean("listeningEnabled", false)
    }
    
    private fun buildFullText(title: String?, text: String?, bigText: String?): String {
        val parts = mutableListOf<String>()
        if (!title.isNullOrEmpty()) parts.add(title)
        if (!text.isNullOrEmpty()) parts.add(text)
        if (!bigText.isNullOrEmpty()) parts.add(bigText)
        return parts.joinToString(" ")
    }
    
    private fun isPaymentCredit(fullText: String): Boolean {
        val lowerText = fullText.lowercase()
        
        // Check if any reject keyword is present
        for (keyword in rejectKeywords) {
            if (keyword in lowerText) {
                return false
            }
        }
        
        // Check if any accept keyword is present
        for (keyword in acceptKeywords) {
            if (keyword in lowerText) {
                return true
            }
        }
        
        return false
    }
    
    private fun extractAmount(fullText: String): String? {
        // Regex to match ₹ or Rs followed by amount
        val patterns = listOf(
            "₹\\s*([0-9,]+(?:\\.[0-9]{2})?)",  // ₹ symbol
            "Rs\\.?\\s*([0-9,]+(?:\\.[0-9]{2})?)",  // Rs or Rs.
            "rs\\.?\\s*([0-9,]+(?:\\.[0-9]{2})?)"   // rs or rs. (lowercase)
        )
        
        for (pattern in patterns) {
            val regex = Regex(pattern, RegexOption.IGNORE_CASE)
            val match = regex.find(fullText)
            if (match != null) {
                val amount = match.groupValues[1]
                return amount.replace(",", "")  // Remove commas for cleaner speech
            }
        }
        
        return null
    }
}

/**
 * Simple LRU Cache implementation with fixed capacity
 */
class LRUCache<K, V>(private val capacity: Int) : LinkedHashMap<K, V>(capacity, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
        return size > capacity
    }
}
