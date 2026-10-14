package com.example.expensetracker.sms

import java.time.LocalDateTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionIngestTest {

    /** In-memory stand-in for the database, deduplicating by code like the real one. */
    private class FakeStore : TransactionStore {
        val saved = linkedMapOf<String, MpesaTransaction>()
        val unparsed = mutableListOf<String>()

        override suspend fun insertIfNew(transaction: MpesaTransaction, receivedAt: LocalDateTime): Boolean =
            saved.putIfAbsent(transaction.code, transaction) == null

        override suspend fun saveUnparsed(body: String, receivedAt: LocalDateTime) {
            unparsed += body
        }
    }

    private val store = FakeStore()
    private val prompted = mutableListOf<MpesaTransaction>()
    private val ingest = TransactionIngest(store = store, notifier = { prompted += it })

    private fun ingest(body: String) = runBlocking { ingest.ingest(body, RECEIVED_AT) }

    @Test
    fun `a new transaction is saved and prompts for a category`() {
        ingest(SENT_MESSAGE)

        assertEquals(setOf("TL1A2B3C4D"), store.saved.keys)
        assertEquals(listOf("TL1A2B3C4D"), prompted.map { it.code })
    }

    @Test
    fun `a message delivered twice is saved and prompted only once`() {
        ingest(SENT_MESSAGE)
        ingest(SENT_MESSAGE)

        assertEquals(1, store.saved.size)
        assertEquals(1, prompted.size)
    }

    @Test
    fun `a receipt with no readable amount is kept as unparsed, without a prompt`() {
        val body = "TL9Z8Y7X6W Confirmed. Something new happened on 4/12/25 at 8:07 AM."

        ingest(body)

        assertEquals(listOf(body), store.unparsed)
        assertTrue(store.saved.isEmpty())
        assertTrue(prompted.isEmpty())
    }

    @Test
    fun `a notice without a transaction code is neither saved nor prompted`() {
        ingest("Failed. You do not have enough money in your M-PESA account to send Ksh5,000.00.")

        assertTrue(store.saved.isEmpty())
        assertTrue(store.unparsed.isEmpty())
        assertTrue(prompted.isEmpty())
    }

    private companion object {
        val RECEIVED_AT: LocalDateTime = LocalDateTime.of(2025, 12, 4, 8, 7)
        const val SENT_MESSAGE =
            "TL1A2B3C4D Confirmed. Ksh1,100.00 sent to ALICE  MWANGI 0700000001 on 4/12/25 at 8:07 AM. " +
                "New M-PESA balance is Ksh25,000.00. Transaction cost, Ksh23.00."
    }
}
