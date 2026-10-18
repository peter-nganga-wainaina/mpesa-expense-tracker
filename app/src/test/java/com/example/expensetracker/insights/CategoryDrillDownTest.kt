package com.example.expensetracker.insights

import com.example.expensetracker.categorize.Category
import com.example.expensetracker.data.StoredTransaction
import com.example.expensetracker.manual.PaymentSource
import com.example.expensetracker.sms.MpesaTransaction
import com.example.expensetracker.sms.MpesaTransactionType.PAID
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

    private fun stored(cents: Long, category: Category?, day: Int, month: Int = 10) = StoredTransaction(
        transaction = MpesaTransaction(
            code = "TX%08d".format(nextCode++),
            type = PAID,
            amountCents = cents,
            costCents = 0,
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
        stored(250_00, food, day = 20),
        stored(999_00, food, day = 30, month = 9),
        stored(40_00, null, day = 21),
    )

    @Test
    fun `lists only that category's transactions for that month`() {
        val amounts = transactionsIn(october, food.id, all).map { it.transaction.amountCents }

        assertEquals(setOf(100_00L, 250_00L), amounts.toSet())
    }

    @Test
    fun `newest come first`() {
        val days = transactionsIn(october, food.id, all).map { it.transaction.occurredAt!!.dayOfMonth }

        assertEquals(listOf(20, 3), days)
    }

    @Test
    fun `the listed transactions add up to the category's total in the month summary`() {
        val summary = summarize(october, all)

        for (total in summary.categories) {
            assertEquals(total.totalCents, transactionsIn(october, total.category.id, all).sumOf { it.transaction.amountCents })
        }
    }

    @Test
    fun `a category with nothing that month lists nothing`() {
        assertTrue(transactionsIn(YearMonth.of(2026, 9), savings.id, all).isEmpty())
    }
}
