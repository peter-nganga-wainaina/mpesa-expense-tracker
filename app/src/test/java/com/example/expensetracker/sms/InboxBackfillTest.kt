package com.example.expensetracker.sms

import com.example.expensetracker.testing.FakeNotifier
import com.example.expensetracker.testing.FakeTransactionStore
import java.time.LocalDateTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class InboxBackfillTest {

    private val now = LocalDateTime.of(2026, 10, 15, 12, 0)
    private val store = FakeTransactionStore()
    private val notifier = FakeNotifier()
    private val ingest = TransactionIngest(store, suggester = { _, _ -> emptyList() }, notifier = notifier)

    /** An inbox holding one M-Pesa payment per age, filtering by time like the real one. */
    private fun backfillWithMessagesAged(vararg ages: Long): InboxBackfill {
        val messages = ages.mapIndexed { i, daysAgo -> InboxMessage(payment(code = "TX%08d".format(i)), now.minusDays(daysAgo)) }
        return InboxBackfill(inbox = { since -> messages.filter { it.receivedAt >= since } }, ingest = ingest, now = { now })
    }

    @Test
    fun `the first import brings in messages within the chosen window only`() = runBlocking {
        val backfill = backfillWithMessagesAged(1, 13, 15, 40)

        assertEquals(2, backfill.importLast(14))
        assertEquals(2, store.saved.size)
    }

    @Test
    fun `starting fresh imports nothing`() = runBlocking {
        val backfill = backfillWithMessagesAged(1, 2)

        assertEquals(0, backfill.importLast(0))
        assertTrue(store.saved.isEmpty())
    }

    @Test
    fun `imported transactions wait in the queue without notifications`() = runBlocking {
        backfillWithMessagesAged(1, 2).importLast(7)

        assertEquals(0, notifier.promptCount)
    }

    @Test
    fun `the sweep catches recent messages the receiver missed`() = runBlocking {
        val backfill = backfillWithMessagesAged(0, 2, 10)

        assertEquals(2, backfill.sweep())
    }

    @Test
    fun `a sweep after the first import adds nothing already imported`() = runBlocking {
        val backfill = backfillWithMessagesAged(0, 2, 10)
        backfill.importLast(14)

        assertEquals(0, backfill.sweep())
        assertEquals(3, store.saved.size)
    }

    @Test
    fun `a negative window is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { runBlocking { backfillWithMessagesAged().importLast(-1) } }
    }

    @Test
    fun `the default first import is one of the offered choices`() {
        assertTrue(InboxBackfill.DEFAULT_FIRST_IMPORT_DAYS in InboxBackfill.FIRST_IMPORT_OPTIONS)
    }

    private fun payment(code: String) =
        "$code Confirmed. Ksh100.00 paid to SHOP. on 1/10/26 at 9:00 AM. " +
            "New M-PESA balance is Ksh1,000.00. Transaction cost, Ksh0.00."
}
