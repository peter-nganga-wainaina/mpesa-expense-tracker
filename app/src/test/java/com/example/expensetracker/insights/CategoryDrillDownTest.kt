package com.example.expensetracker.insights

import com.example.expensetracker.categorize.Category
import com.example.expensetracker.data.StoredTransaction
import com.example.expensetracker.manual.PaymentSource
import com.example.expensetracker.sms.MpesaTransaction
import com.example.expensetracker.sms.MpesaTransactionType
import com.example.expensetracker.sms.MpesaTransactionType.FULIZA_DRAWDOWN
import com.example.expensetracker.sms.MpesaTransactionType.PAID
import com.example.expensetracker.sms.MpesaTransactionType.RECEIVED
import com.example.expensetracker.sms.MpesaTransactionType.SENT
import com.example.expensetracker.sms.MpesaTransactionType.WITHDRAW
import java.time.LocalDateTime
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryDrillDownTest {

    private val food = Category(1, "Food")
    private val savings = Category(2, "Savings")
    private val october = YearMonth.of(2026, 10)
    private var nextCode = 0

    private fun stored(cents: Long, category: Category?, day: Int, month: Int = 10, type: MpesaTransactionType = PAID, fee: Long = 0) = StoredTransaction(
        transaction = MpesaTransaction(
            code = "TX%08d".format(nextCode++),
            type = type,
            amountCents = cents,
            costCents = fee,
            counterparty = "SHOP",
            phone = null,
            reference = null,
            occurredAt = LocalDateTime.of(2026, month, day, 12, 0),
        ),
        category = category,
        originalMessage = "",
        source = PaymentSource.MPESA,
    )

    private val all = listOf(
        stored(100_00, food, day = 3),
        stored(3_500_00, savings, day = 18),
        stored(1_200_00, savings, day = 25, type = RECEIVED),
        stored(250_00, food, day = 20),
        stored(999_00, food, day = 30, month = 9),
        stored(40_00, null, day = 21),
    )

    @Test
    fun `lists only that category's transactions for that month`() {
        val amounts = transactionsIn(october, Direction.OUT, food.id, all).map { it.transaction.amountCents }

        assertEquals(setOf(100_00L, 250_00L), amounts.toSet())
    }

    @Test
    fun `newest come first`() {
        val days = transactionsIn(october, Direction.OUT, food.id, all).map { it.transaction.occurredAt!!.dayOfMonth }

        assertEquals(listOf(20, 3), days)
    }

    @Test
    fun `the listed transactions add up to the category's total in the month summary`() {
        val summary = summarize(october, Direction.OUT, all)

        for (total in summary.categories) {
            assertEquals(total.totalCents, transactionsIn(october, Direction.OUT, total.category.id, all).sumOf { it.transaction.amountCents })
        }
    }

    @Test
    fun `money in and money out of one category are listed separately`() {
        val moneyOut = transactionsIn(october, Direction.OUT, savings.id, all).map { it.transaction.amountCents }
        val moneyIn = transactionsIn(october, Direction.IN, savings.id, all).map { it.transaction.amountCents }

        assertEquals(listOf(3_500_00L), moneyOut)
        assertEquals(listOf(1_200_00L), moneyIn)
    }

    @Test
    fun `a category with nothing that month lists nothing`() {
        assertTrue(transactionsIn(YearMonth.of(2026, 9), Direction.OUT, savings.id, all).isEmpty())
    }

    private val withFees = listOf(
        stored(1_500_00, food, day = 5, type = SENT, fee = 15_00),
        stored(2_000_00, null, day = 18, type = WITHDRAW, fee = 29_00),
        stored(948_44, savings, day = 12, type = FULIZA_DRAWDOWN, fee = 9_49),
        stored(500_00, food, day = 20),
        stored(1_000_00, food, day = 30, month = 9, type = SENT, fee = 12_00),
    )

    @Test
    fun `the fees line lists the month's transactions that carried a fee, newest first`() {
        val fees = feesIn(october, withFees).map { it.feeCents }

        assertEquals(listOf(29_00L, 15_00L), fees)
    }

    @Test
    fun `the listed fees add up to the fees line in the month summary`() {
        val summary = summarize(october, Direction.OUT, withFees)

        assertEquals(summary.feesCents, feesIn(october, withFees).sumOf { it.feeCents })
    }

    @Test
    fun `Fuliza access fees left out of the fees line are totaled so the screen can say where they went`() {
        assertEquals(9_49L, fulizaAccessFeesCents(october, withFees))
        assertEquals(0L, fulizaAccessFeesCents(YearMonth.of(2026, 9), withFees))
    }
}
