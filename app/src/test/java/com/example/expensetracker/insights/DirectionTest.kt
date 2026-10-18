package com.example.expensetracker.insights

import com.example.expensetracker.sms.MpesaTransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class DirectionTest {

    @Test
    fun `received money and a Fuliza borrow are money in`() {
        assertEquals(Direction.IN, directionOf(MpesaTransactionType.RECEIVED))
        assertEquals(Direction.IN, directionOf(MpesaTransactionType.FULIZA_DRAWDOWN))
    }

    @Test
    fun `a withdrawal is a transfer to her own cash`() {
        assertEquals(Direction.TRANSFER, directionOf(MpesaTransactionType.WITHDRAW))
    }

    @Test
    fun `every other type, including manual entries and unknown formats, is money out`() {
        val inOrTransfer = setOf(MpesaTransactionType.RECEIVED, MpesaTransactionType.FULIZA_DRAWDOWN, MpesaTransactionType.WITHDRAW)

        for (type in MpesaTransactionType.entries - inOrTransfer) {
            assertEquals("$type", Direction.OUT, directionOf(type))
        }
    }
}
