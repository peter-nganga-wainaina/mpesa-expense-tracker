package com.example.expensetracker.sms

import java.time.LocalDateTime

/** M-Pesa messages already sitting in the phone's SMS inbox. */
fun interface MpesaInbox {
    /** Messages from M-Pesa received at or after [since], oldest first. */
    suspend fun messagesSince(since: LocalDateTime): List<InboxMessage>
}

/**
 * Brings M-Pesa messages the receiver never saw into the queue: the user's chosen history on first
 * run, and a short look-back on every app open and daily to catch anything missed since.
 * Re-reading messages that are already saved is harmless, because transaction codes dedupe.
 */
class InboxBackfill(
    private val inbox: MpesaInbox,
    private val ingest: TransactionIngest,
    private val now: () -> LocalDateTime,
) {
    /** Imports the last [days] days of messages. Returns how many transactions were new. */
    suspend fun importLast(days: Int): Int {
        require(days >= 0) { "Can't import a negative number of days: $days" }
        if (days == 0) return 0
        return ingest.backfill(inbox.messagesSince(now().minusDays(days.toLong())))
    }

    /** The safety net: re-checks the last few days. Returns how many transactions were new. */
    suspend fun sweep(): Int = importLast(SWEEP_DAYS)

    companion object {
        /** Choices offered at first-run setup; 0 means start empty. */
        val FIRST_IMPORT_OPTIONS = listOf(0, 7, 14, 30)
        const val DEFAULT_FIRST_IMPORT_DAYS = 14

        /** Long enough to cover a phone left off over a weekend; overlap with earlier sweeps is free. */
        const val SWEEP_DAYS = 3
    }
}
