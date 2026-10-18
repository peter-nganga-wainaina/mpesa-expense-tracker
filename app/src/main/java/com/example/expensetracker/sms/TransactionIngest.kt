package com.example.expensetracker.sms

import com.example.expensetracker.categorize.Category
import com.example.expensetracker.suggest.CategorySuggester
import java.time.LocalDateTime

/** Prompts the user to categorize transactions, and withdraws prompts that are no longer needed. */
interface TransactionNotifier {
    fun promptForCategory(transaction: MpesaTransaction, quickPicks: List<Category>)
    fun dismiss(transactionCode: String)
}

/** Where ingested messages are kept. The database is the source of truth; notifications are shortcuts. */
interface TransactionStore {
    /**
     * Saves [transaction] as uncategorized unless one with the same code exists. Returns whether it was
     * new, so a re-delivered message isn't notified twice; insert-and-report keeps the check atomic.
     */
    suspend fun insertIfNew(transaction: MpesaTransaction, sms: ReceivedSms): Boolean

    /**
     * Keeps a receipt the parser couldn't read so it is never silently dropped. Kept once per
     * [transactionCode], like transactions: a later sweep may read the same message back.
     */
    suspend fun saveUnparsed(transactionCode: String, sms: ReceivedSms)

    suspend fun setCategory(transactionCode: String, categoryId: Long)
}

/** An M-Pesa SMS: its full text and when it arrived, whether live or read back from the inbox. */
data class ReceivedSms(val body: String, val receivedAt: LocalDateTime)

/**
 * What happens to an M-Pesa message: parse it, save it, and for a live message, prompt for a category.
 * Shared by the SMS receiver and the inbox backfill.
 */
class TransactionIngest(
    private val store: TransactionStore,
    private val suggester: CategorySuggester,
    private val notifier: TransactionNotifier,
) {
    /** A message that just arrived. [receivedAt] is used when the message itself carries no time. */
    suspend fun ingest(body: String, receivedAt: LocalDateTime) {
        val transaction = saveIfNew(ReceivedSms(body, receivedAt)) ?: return
        notifier.promptForCategory(transaction, suggester.suggest(transaction, QUICK_PICKS))
    }

    /**
     * Messages read back from the inbox. They wait in the queue without notifications: a burst of
     * prompts for old spending would be noise. Returns how many transactions were new.
     */
    suspend fun backfill(messages: List<ReceivedSms>): Int =
        messages.count { saveIfNew(it) != null }

    /** Saves the message; returns its transaction only if it wasn't already stored. */
    private suspend fun saveIfNew(sms: ReceivedSms): MpesaTransaction? =
        when (val result = MpesaParser.parse(sms.body)) {
            is ParseResult.Parsed -> result.transaction.takeIf { store.insertIfNew(it, sms) }
            is ParseResult.Unrecognized -> {
                store.saveUnparsed(result.code, sms)
                null
            }
            ParseResult.Ignored -> null
        }

    private companion object {
        /** Android shows at most 3 notification actions: 2 quick picks plus "Other…". */
        const val QUICK_PICKS = 2
    }
}
