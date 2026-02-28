package com.example.payalert

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {

        if (intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        try {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

            if (messages.isNullOrEmpty()) return

            val fullMessageBody = StringBuilder()
            var senderAddress: String? = null

            for (sms in messages) {
                fullMessageBody.append(sms.messageBody)
                senderAddress = sms.displayOriginatingAddress
            }

            val messageBody = fullMessageBody.toString()
            Log.d("SmsReceiver", "SMS from: $senderAddress")
            Log.d("SmsReceiver", "Message: $messageBody")

            // Check if this is a CREDIT message
            if (SmsParser.isCreditMessage(messageBody)) {

                val amount = SmsParser.extractAmount(messageBody)

                if (!amount.isNullOrEmpty()) {

                    val speakText = "Rupees $amount credited, thank you."

                    TtsSpeaker.speak(context, speakText)

                    Log.d("SmsReceiver", "Speaking: $speakText")
                }
            }

        } catch (e: Exception) {
            Log.e("SmsReceiver", "Error processing SMS", e)
        }
    }
}
