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
    suspend fun insertIfNew(transaction: MpesaTransaction, receivedAt: LocalDateTime): Boolean

    /** Keeps a receipt the parser couldn't read, so it is never silently dropped. */
    suspend fun saveUnparsed(body: String, receivedAt: LocalDateTime)

    suspend fun setCategory(transactionCode: String, categoryId: Long)
}

/**
 * What happens to one M-Pesa message after it arrives: parse it, save it, then prompt for a category.
 * Shared by the SMS receiver and, later, the inbox backfill.
 */
class TransactionIngest(
    private val store: TransactionStore,
    private val suggester: CategorySuggester,
    private val notifier: TransactionNotifier,
) {
    /** [receivedAt] is when the SMS arrived, used when the message itself carries no time. */
    suspend fun ingest(body: String, receivedAt: LocalDateTime) {
        when (val result = MpesaParser.parse(body)) {
            is ParseResult.Parsed -> {
                val transaction = result.transaction
                if (store.insertIfNew(transaction, receivedAt)) {
                    notifier.promptForCategory(transaction, suggester.suggest(transaction, QUICK_PICKS))
                }
            }
            ParseResult.Unrecognized -> store.saveUnparsed(body, receivedAt)
            ParseResult.Ignored -> Unit
        }
    }

    private companion object {
        /** Android shows at most 3 notification actions: 2 quick picks plus "Other…". */
        const val QUICK_PICKS = 2
    }
}
