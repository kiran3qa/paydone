package com.example.payalert

import java.util.Locale

object SmsParser {

    // Words that strongly indicate a CREDIT (money received)
    private val creditKeywords = listOf(
        "credited", "credit", "received", "deposit", "deposited", "added", "successfully received",
        "cr", "cr.", "credited to", "received in", "received from", "payment received"
    )

    // Words that indicate NOT credit (debit/expense)
    private val debitKeywords = listOf(
        "debited", "debit", "spent", "withdrawn", "purchase", "paid", "sent", "transfer",
        "dr", "dr.", "atm", "pos", "upi/p2p", "bill", "charges", "fee", "txn at", "card"
    )

    // Common non-transaction SMS patterns (avoid false triggers)
    private val noiseKeywords = listOf(
        "otp", "one time password", "verification", "verify", "code",
        "offer", "discount", "cashback", "reward", "promo", "sale",
        "balance", "available balance", "statement", "alert:", "dear customer"
    )

    /**
     * Returns true if the SMS likely represents an incoming CREDIT.
     * Heuristics:
     * - Must contain a credit keyword
     * - Must not contain debit keywords
     * - Must not look like OTP/promotional noise
     */
    fun isCreditMessage(message: String?): Boolean {
        if (message.isNullOrBlank()) return false

        val msg = normalize(message)

        // Hard reject OTP / verification messages (most common false positive)
        if (containsAny(msg, listOf("otp", "one time password", "verification", "verify", "code"))) {
            return false
        }

        // Reject if it contains strong debit indicators
        if (containsAny(msg, debitKeywords)) {
            return false
        }

        // Require at least one credit indicator
        val hasCredit = containsAny(msg, creditKeywords)
        if (!hasCredit) return false

        // If it's mostly noise/promotional, reject (but allow if "credited" is present)
        if (containsAny(msg, noiseKeywords) && !msg.contains("credited")) {
            return false
        }

        return true
    }

    /**
     * Extracts the amount from common Indian banking SMS formats.
     * Supports:
     *  - ₹500
     *  - Rs 500 / Rs.500
     *  - INR 1,200.50
     * Returns numeric string without currency symbols/commas, e.g. "1200.50"
     */
    fun extractAmount(message: String?): String? {
        if (message.isNullOrBlank()) return null

        // Keep original for regex (but normalize spaces)
        val text = message.replace("\n", " ").replace("\r", " ")

        // 1) Look for currency + amount patterns
        // Examples: "Rs. 500", "INR 1,200.50", "₹750", "Rs500"
        val currencyRegex = Regex(
            pattern = """(?i)\b(?:rs\.?|inr|₹)\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)\b"""
        )
        val m1 = currencyRegex.find(text)
        val amt1 = m1?.groups?.get(1)?.value
        if (!amt1.isNullOrBlank()) {
            return amt1.replace(",", "").trim()
        }

        // 2) Fallback: sometimes messages say "credited by 500" without Rs/INR
        // Example: "Amount 500 credited" or "credited by 500.00"
        val creditedByRegex = Regex(
            pattern = """(?i)\b(?:credited\s*(?:by|with)?|credit\s*(?:by|with)?|received\s*(?:by|with)?|deposit(?:ed)?\s*(?:by|with)?)\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)\b"""
        )
        val m2 = creditedByRegex.find(text)
        val amt2 = m2?.groups?.get(1)?.value
        if (!amt2.isNullOrBlank()) {
            return amt2.replace(",", "").trim()
        }

        return null
    }

    // ---------- Helpers ----------

    private fun normalize(s: String): String =
        s.lowercase(Locale.ROOT)
            .replace("\n", " ")
            .replace("\r", " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

    private fun containsAny(haystack: String, needles: List<String>): Boolean {
        for (n in needles) {
            val needle = n.lowercase(Locale.ROOT).trim()
            if (needle.isNotEmpty() && haystack.contains(needle)) return true
        }
        return false
    }
}
