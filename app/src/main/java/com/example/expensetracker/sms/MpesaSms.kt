package com.example.expensetracker.sms

/** The only sender ID M-Pesa confirmations come from. */
const val MPESA_SENDER_ID = "MPESA"

/** One part of an incoming SMS, as delivered by the system. */
data class SmsPart(val sender: String?, val body: String?)

/**
 * The full M-Pesa message carried by one SMS broadcast, or null if none of it is from M-Pesa.
 * A long confirmation arrives as several parts, in order, and must be parsed as one message.
 */
fun mpesaBody(parts: List<SmsPart>): String? =
    parts.filter { it.sender == MPESA_SENDER_ID }
        .takeIf { it.isNotEmpty() }
        ?.joinToString("") { it.body.orEmpty() }
