package com.example.expensetracker.sms

/** Shows the user a newly parsed transaction so they can categorize it. */
fun interface TransactionNotifier {
    fun promptForCategory(transaction: MpesaTransaction)
}

/**
 * What happens to one M-Pesa message after it arrives: parse it, then prompt for a category or keep
 * the unreadable body. Shared by the SMS receiver and, later, the inbox backfill.
 */
class TransactionIngest(
    private val notifier: TransactionNotifier,
    /** Keeps a receipt the parser couldn't read, so it is never silently dropped. */
    private val recordUnparsed: (body: String) -> Unit,
) {
    fun ingest(body: String) {
        when (val result = MpesaParser.parse(body)) {
            is ParseResult.Parsed -> notifier.promptForCategory(result.transaction)
            ParseResult.Unrecognized -> recordUnparsed(body)
            ParseResult.Ignored -> Unit
        }
    }
}
