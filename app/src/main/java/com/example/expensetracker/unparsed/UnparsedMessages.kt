package com.example.expensetracker.unparsed

import com.example.expensetracker.data.StoredTransaction
import java.time.LocalDateTime

/** An M-Pesa receipt the parser couldn't read, waiting for the user to look at it. */
data class UnparsedMessage(val code: String, val body: String, val receivedAt: LocalDateTime)

/** The To categorize banner's text, or null when there's nothing to show (the banner hides). */
fun unparsedBannerText(waiting: Int): String? = when {
    waiting <= 0 -> null
    waiting == 1 -> "1 M-Pesa message couldn't be read"
    else -> "$waiting M-Pesa messages couldn't be read"
}

/** Whether she has already typed this message in as spending. */
sealed interface MessageStatus {
    data object NotAdded : MessageStatus
    data class Added(val entry: StoredTransaction) : MessageStatus
}

/**
 * An entry typed in from an unreadable message takes the message's own M-Pesa code, so the message
 * and its entry find each other by code. Saving again replaces that entry instead of adding a second
 * one, and if a future parser reads the real transaction, its insert is skipped: no double count.
 */
fun entryCodeFor(message: UnparsedMessage): String = message.code

fun statusOf(message: UnparsedMessage, transactionsByCode: Map<String, StoredTransaction>): MessageStatus =
    transactionsByCode[entryCodeFor(message)]?.let { MessageStatus.Added(it) } ?: MessageStatus.NotAdded
