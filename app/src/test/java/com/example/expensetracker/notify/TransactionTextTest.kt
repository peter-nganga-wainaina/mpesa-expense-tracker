package com.example.expensetracker.notify

import com.example.expensetracker.sms.MpesaTransaction
import com.example.expensetracker.sms.MpesaTransactionType
import com.example.expensetracker.sms.MpesaTransactionType.PAID
import com.example.expensetracker.sms.MpesaTransactionType.PAYBILL
import com.example.expensetracker.sms.MpesaTransactionType.RECEIVED
import com.example.expensetracker.sms.MpesaTransactionType.SENT
import com.example.expensetracker.sms.MpesaTransactionType.WITHDRAW
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionTextTest {

    @Test
    fun `formats cents the way M-Pesa writes amounts`() {
        assertEquals("Ksh1,100.00", formatKsh(110000))
        assertEquals("Ksh948.44", formatKsh(94844))
        assertEquals("Ksh0.05", formatKsh(5))
        assertEquals("Ksh1,234,567.00", formatKsh(123456700))
    }

    @Test
    fun `shows when a transaction happened as day, month and 12-hour time`() {
        assertEquals("13 Oct, 10:40 PM", formatOccurredAt(LocalDateTime.of(2026, 10, 13, 22, 40)))
        assertEquals("4 Dec, 8:07 AM", formatOccurredAt(LocalDateTime.of(2025, 12, 4, 8, 7)))
    }

    @Test
    fun `every transaction type is described with its amount`() {
        for (type in MpesaTransactionType.entries) {
            val text = describe(transaction(type, 94844, counterparty = "SOMEONE"))
            assertTrue("$type: $text", "Ksh948.44" in text)
        }
    }

    @Test
    fun `the counterparty is named for payments, sends, receipts and withdrawals`() {
        for (type in listOf(PAID, PAYBILL, SENT, RECEIVED, WITHDRAW)) {
            val text = describe(transaction(type, 10000, counterparty = "JAVA HOUSE"))
            assertTrue("$type: $text", "JAVA HOUSE" in text)
        }
    }

    @Test
    fun `money sent and money received read differently`() {
        assertNotEquals(
            describe(transaction(SENT, 10000, counterparty = "ALICE")),
            describe(transaction(RECEIVED, 10000, counterparty = "ALICE")),
        )
    }

    @Test
    fun `a transaction with no known counterparty is still described`() {
        for (type in MpesaTransactionType.entries) {
            assertTrue(type.name, "Ksh50.00" in describe(transaction(type, 5000, counterparty = null)))
        }
    }

    private fun transaction(type: MpesaTransactionType, amountCents: Long, counterparty: String?) =
        MpesaTransaction(
            code = "TL1A2B3C4D",
            type = type,
            amountCents = amountCents,
            costCents = 0,
            counterparty = counterparty,
            phone = null,
            reference = null,
            occurredAt = null,
        )
}
