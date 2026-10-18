package com.example.expensetracker.data

import com.example.expensetracker.categorize.Category
import com.example.expensetracker.manual.ManualEntry
import com.example.expensetracker.manual.PaymentSource
import com.example.expensetracker.sms.MpesaTransaction
import com.example.expensetracker.sms.MpesaTransactionType.SENT
import com.example.expensetracker.sms.ReceivedSms
import com.example.expensetracker.unparsed.UnparsedMessage
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionEntityTest {

    private val receivedAt = LocalDateTime.of(2025, 12, 4, 8, 9)
    private val sms = ReceivedSms("TL1A2B3C4D Confirmed. Ksh1,100.00 sent to ALICE MWANGI on 4/12/25 at 8:07 AM.", receivedAt)

    @Test
    fun `a new transaction is stored uncategorized`() {
        assertNull(transaction().toEntity(sms).categoryId)
    }

    @Test
    fun `the time in the message is used when present`() {
        val entity = transaction(occurredAt = LocalDateTime.of(2025, 12, 4, 8, 7)).toEntity(sms)

        assertEquals(LocalDateTime.of(2025, 12, 4, 8, 7), LocalDateTime.parse(entity.occurredAt))
    }

    @Test
    fun `the SMS arrival time is used when the message has no time`() {
        val entity = transaction(occurredAt = null).toEntity(sms)

        assertEquals(receivedAt, LocalDateTime.parse(entity.occurredAt))
    }

    @Test
    fun `stored times sort chronologically as text`() {
        val times = listOf(
            LocalDateTime.of(2025, 9, 30, 23, 59),
            LocalDateTime.of(2025, 10, 1, 0, 0),
            LocalDateTime.of(2025, 12, 4, 8, 7),
        )
        val stored = times.map { transaction(occurredAt = it).toEntity(sms).occurredAt }

        assertTrue(stored == stored.sorted())
    }

    @Test
    fun `a stored transaction reads back unchanged`() {
        val original = transaction(occurredAt = LocalDateTime.of(2025, 12, 4, 8, 7))

        assertEquals(original, original.toEntity(sms).toTransaction())
    }

    @Test
    fun `a transaction stored without its own time reads back with the arrival time`() {
        assertEquals(receivedAt, transaction(occurredAt = null).toEntity(sms).toTransaction().occurredAt)
    }

    @Test
    fun `an uncategorized transaction appears in history without a category`() {
        val row = TransactionWithCategory(transaction().toEntity(sms), categoryName = null)

        assertNull(row.toStoredTransaction().category)
    }

    @Test
    fun `a categorized transaction appears in history with its category`() {
        val entity = transaction().toEntity(sms).copy(categoryId = 7)

        assertEquals(Category(7, "Groceries"), TransactionWithCategory(entity, "Groceries").toStoredTransaction().category)
    }

    @Test
    fun `a stored transaction keeps its original message`() {
        val row = TransactionWithCategory(transaction().toEntity(sms), categoryName = null)

        assertEquals(sms.body, row.toStoredTransaction().originalMessage)
    }

    @Test
    fun `an M-Pesa transaction is stored as paid by M-Pesa`() {
        val row = TransactionWithCategory(transaction().toEntity(sms), categoryName = null)

        assertEquals(PaymentSource.MPESA, row.toStoredTransaction().source)
    }

    @Test
    fun `a manual entry reads back with its source, note and category, and no SMS`() {
        val entry = ManualEntry("MANUAL-1", 45000, "Mama Mboga", PaymentSource.CASH, receivedAt, categoryId = 3)

        val stored = TransactionWithCategory(entry.toEntity(), categoryName = "Food").toStoredTransaction()

        assertEquals(PaymentSource.CASH, stored.source)
        assertEquals(Category(3, "Food"), stored.category)
        assertEquals("Mama Mboga", stored.transaction.counterparty)
        assertEquals(45000L, stored.transaction.amountCents)
        assertNull(stored.originalMessage)
    }

    @Test
    fun `an unreadable receipt reads back with its text and arrival time`() {
        val row = UnparsedMessageEntity("TL9Z8Y7X6W", "TL9Z8Y7X6W Confirmed. Something new.", receivedAt.toString())

        assertEquals(UnparsedMessage("TL9Z8Y7X6W", "TL9Z8Y7X6W Confirmed. Something new.", receivedAt), row.toUnparsedMessage())
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
