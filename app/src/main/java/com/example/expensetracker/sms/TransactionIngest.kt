package com.example.expensetracker.sms

import java.time.LocalDateTime

/** Shows the user a newly parsed transaction so they can categorize it. */
fun interface TransactionNotifier {
    fun promptForCategory(transaction: MpesaTransaction)
}

/** Where ingested messages are kept. The database is the source of truth; notifications are shortcuts. */
interface TransactionStore {
    /**
     * Saves [transaction] as uncategorized unless one with the same code exists. Returns whether it was
     * new, so a re-delivered message isn't notified twice; insert-and-report keeps the check atomic.
     */
    suspend fun insertIfNew(transaction: MpesaTransaction, receivedAt: LocalDateTime): Boolean

    /** Keeps a receipt the parser couldn't read, so it is never silently dropped. */
    suspend fun saveUnparsed(body: String, receivedAt: LocalDateTime)
}

/**
 * What happens to one M-Pesa message after it arrives: parse it, save it, then prompt for a category.
 * Shared by the SMS receiver and, later, the inbox backfill.
 */
class TransactionIngest(
    private val store: TransactionStore,
    private val notifier: TransactionNotifier,
) {
    /** [receivedAt] is when the SMS arrived, used when the message itself carries no time. */
    suspend fun ingest(body: String, receivedAt: LocalDateTime) {
        when (val result = MpesaParser.parse(body)) {
            is ParseResult.Parsed ->
                if (store.insertIfNew(result.transaction, receivedAt)) notifier.promptForCategory(result.transaction)
            ParseResult.Unrecognized -> store.saveUnparsed(body, receivedAt)
            ParseResult.Ignored -> Unit
        }
    }
}
