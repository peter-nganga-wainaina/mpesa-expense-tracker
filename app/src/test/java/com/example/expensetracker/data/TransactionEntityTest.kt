package com.example.expensetracker.data

import com.example.expensetracker.sms.MpesaTransaction
import com.example.expensetracker.sms.MpesaTransactionType.SENT
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionEntityTest {

    private val receivedAt = LocalDateTime.of(2025, 12, 4, 8, 9)

    @Test
    fun `a new transaction is stored uncategorized`() {
        assertNull(transaction().toEntity(receivedAt).categoryId)
    }

    @Test
    fun `the time in the message is used when present`() {
        val entity = transaction(occurredAt = LocalDateTime.of(2025, 12, 4, 8, 7)).toEntity(receivedAt)

        assertEquals(LocalDateTime.of(2025, 12, 4, 8, 7), LocalDateTime.parse(entity.occurredAt))
    }

    @Test
    fun `the SMS arrival time is used when the message has no time`() {
        val entity = transaction(occurredAt = null).toEntity(receivedAt)

        assertEquals(receivedAt, LocalDateTime.parse(entity.occurredAt))
    }

    @Test
    fun `stored times sort chronologically as text`() {
        val times = listOf(
            LocalDateTime.of(2025, 9, 30, 23, 59),
            LocalDateTime.of(2025, 10, 1, 0, 0),
            LocalDateTime.of(2025, 12, 4, 8, 7),
        )
        val stored = times.map { transaction(occurredAt = it).toEntity(receivedAt).occurredAt }

        assertTrue(stored == stored.sorted())
    }

    private fun transaction(occurredAt: LocalDateTime? = null) = MpesaTransaction(
        code = "TL1A2B3C4D",
        type = SENT,
        amountCents = 110000,
        costCents = 2300,
        counterparty = "ALICE MWANGI",
        phone = "0700000001",
        reference = null,
        occurredAt = occurredAt,
    )
}
