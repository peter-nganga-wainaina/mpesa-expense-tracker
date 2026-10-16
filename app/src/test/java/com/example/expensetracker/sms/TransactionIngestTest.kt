package com.example.expensetracker.sms

import com.example.expensetracker.categorize.Category
import com.example.expensetracker.testing.FakeNotifier
import com.example.expensetracker.testing.FakeTransactionStore
import java.time.LocalDateTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionIngestTest {

    private val store = FakeTransactionStore()
    private val notifier = FakeNotifier()
    private val picks = listOf(Category(1, "Food"), Category(2, "Transport"))
    private val ingest = TransactionIngest(store, suggester = { _, limit -> picks.take(limit) }, notifier = notifier)

    private fun ingest(body: String) = runBlocking { ingest.ingest(body, RECEIVED_AT) }

    @Test
    fun `a new transaction is saved and prompts for a category`() {
        ingest(SENT_MESSAGE)

        assertEquals(setOf("TL1A2B3C4D"), store.saved.keys)
        assertEquals(setOf("TL1A2B3C4D"), notifier.showing.keys)
    }

    @Test
    fun `the prompt offers the suggested categories as quick picks`() {
        ingest(SENT_MESSAGE)

        assertEquals(picks, notifier.showing.getValue("TL1A2B3C4D").quickPicks)
    }

    @Test
    fun `a message delivered twice is saved and prompted only once`() {
        ingest(SENT_MESSAGE)
        ingest(SENT_MESSAGE)

        assertEquals(1, store.saved.size)
        assertEquals(1, notifier.promptCount)
    }

    @Test
    fun `a receipt with no readable amount is kept as unparsed, without a prompt`() {
        val body = "TL9Z8Y7X6W Confirmed. Something new happened on 4/12/25 at 8:07 AM."

        ingest(body)

        assertEquals(listOf(body), store.unparsed)
        assertTrue(store.saved.isEmpty())
        assertEquals(0, notifier.promptCount)
    }

    @Test
    fun `a notice without a transaction code is neither saved nor prompted`() {
        ingest("Failed. You do not have enough money in your M-PESA account to send Ksh5,000.00.")

        assertTrue(store.saved.isEmpty())
        assertTrue(store.unparsed.isEmpty())
        assertEquals(0, notifier.promptCount)
    }

    private companion object {
        val RECEIVED_AT: LocalDateTime = LocalDateTime.of(2025, 12, 4, 8, 7)
        const val SENT_MESSAGE =
            "TL1A2B3C4D Confirmed. Ksh1,100.00 sent to ALICE  MWANGI 0700000001 on 4/12/25 at 8:07 AM. " +
                "New M-PESA balance is Ksh25,000.00. Transaction cost, Ksh23.00."
    }
}
