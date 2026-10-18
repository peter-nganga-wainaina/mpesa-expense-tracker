package com.example.expensetracker.notify

import com.example.expensetracker.sms.MpesaTransaction
import com.example.expensetracker.sms.MpesaTransactionType.AIRTIME
import com.example.expensetracker.sms.MpesaTransactionType.FULIZA_DRAWDOWN
import com.example.expensetracker.sms.MpesaTransactionType.FULIZA_REPAYMENT
import com.example.expensetracker.sms.MpesaTransactionType.MANUAL
import com.example.expensetracker.sms.MpesaTransactionType.PAID
import com.example.expensetracker.sms.MpesaTransactionType.PAYBILL
import com.example.expensetracker.sms.MpesaTransactionType.RECEIVED
import com.example.expensetracker.sms.MpesaTransactionType.SENT
import com.example.expensetracker.sms.MpesaTransactionType.UNKNOWN
import com.example.expensetracker.sms.MpesaTransactionType.WITHDRAW
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** One-line summary for the notification, e.g. "Ksh1,100.00 sent to ALICE MWANGI". */
fun describe(transaction: MpesaTransaction): String {
    val amount = formatKsh(transaction.amountCents)
    val party = transaction.counterparty ?: "unknown"
    return when (transaction.type) {
        PAID, PAYBILL -> "$amount paid to $party"
        SENT -> "$amount sent to $party"
        RECEIVED -> "$amount received from $party"
        WITHDRAW -> "$amount withdrawn at $party"
        AIRTIME -> "$amount of airtime"
        FULIZA_DRAWDOWN -> "$amount borrowed from Fuliza"
        FULIZA_REPAYMENT -> "$amount Fuliza repayment"
        UNKNOWN -> "$amount M-Pesa transaction"
        MANUAL -> if (transaction.counterparty == null) "$amount added by hand" else "$amount · ${transaction.counterparty}"
    }
}

/** 110000 -> "Ksh1,100.00", matching how M-Pesa writes amounts. */
fun formatKsh(cents: Long): String = "Ksh" + KSH_FORMAT.format(cents.toBigDecimal().movePointLeft(2))

/** "13 Oct, 10:40 PM". English month names, matching the app's English UI whatever the phone's locale. */
fun formatOccurredAt(time: LocalDateTime): String = OCCURRED_AT_FORMAT.format(time)

private val OCCURRED_AT_FORMAT = DateTimeFormatter.ofPattern("d MMM, h:mm a", Locale.ENGLISH)
private val KSH_FORMAT = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
