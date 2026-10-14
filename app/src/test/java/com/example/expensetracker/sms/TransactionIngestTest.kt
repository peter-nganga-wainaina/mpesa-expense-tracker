package com.example.expensetracker.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionIngestTest {

    private val prompted = mutableListOf<MpesaTransaction>()
    private val unparsed = mutableListOf<String>()
    private val ingest = TransactionIngest(
        notifier = { prompted += it },
        recordUnparsed = { unparsed += it },
    )

    @Test
    fun `a parsed transaction prompts for a category`() {
        ingest.ingest(
            "TL1A2B3C4D Confirmed. Ksh1,100.00 sent to ALICE  MWANGI 0700000001 on 4/12/25 at 8:07 AM. " +
                "New M-PESA balance is Ksh25,000.00. Transaction cost, Ksh23.00."
        )

        assertEquals(listOf("TL1A2B3C4D"), prompted.map { it.code })
        assertTrue(unparsed.isEmpty())
    }

    @Test
    fun `a receipt with no readable amount is kept as unparsed`() {
        val body = "TL9Z8Y7X6W Confirmed. Something new happened on 4/12/25 at 8:07 AM."

        ingest.ingest(body)

        assertEquals(listOf(body), unparsed)
        assertTrue(prompted.isEmpty())
    }

    @Test
    fun `a notice without a transaction code does nothing`() {
        ingest.ingest("Failed. You do not have enough money in your M-PESA account to send Ksh5,000.00.")

        assertTrue(prompted.isEmpty())
        assertTrue(unparsed.isEmpty())
    }
}
