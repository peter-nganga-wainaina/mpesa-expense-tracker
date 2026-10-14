package com.example.expensetracker.sms

/** The only sender ID M-Pesa confirmations come from. */
const val MPESA_SENDER_ID = "MPESA"

/** One part of an incoming SMS, as delivered by the system. */
data class SmsPart(val sender: String?, val body: String?, val timestampMillis: Long)

/** A complete M-Pesa SMS: all parts joined, stamped with when it arrived. */
data class MpesaSms(val body: String, val receivedAtMillis: Long)

/**
 * The full M-Pesa message carried by one SMS broadcast, or null if none of it is from M-Pesa.
 * A long confirmation arrives as several parts, in order, and must be parsed as one message.
 */
fun mpesaSms(parts: List<SmsPart>): MpesaSms? {
    val mpesaParts = parts.filter { it.sender == MPESA_SENDER_ID }
    if (mpesaParts.isEmpty()) return null
    return MpesaSms(
        body = mpesaParts.joinToString("") { it.body.orEmpty() },
        receivedAtMillis = mpesaParts.first().timestampMillis,
    )
}
