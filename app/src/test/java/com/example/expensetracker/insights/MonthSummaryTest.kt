package com.example.expensetracker.insights

import com.example.expensetracker.categorize.Category
import com.example.expensetracker.data.StoredTransaction
import com.example.expensetracker.sms.MpesaTransaction
import com.example.expensetracker.sms.MpesaTransactionType.PAID
import java.time.LocalDateTime
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MonthSummaryTest {

    private val food = Category(1, "Food")
    private val transport = Category(2, "Transport")
    private val bills = Category(3, "Bills")
    private val october = YearMonth.of(2026, 10)
    private var nextCode = 0

    private fun stored(cents: Long, category: Category?, day: Int = 10, month: Int = 10) = StoredTransaction(
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
    )

    @Test
    fun `amounts are totaled per category`() {
        val summary = summarize(october, listOf(stored(100_00, food), stored(250_00, food), stored(80_00, transport)))

        assertEquals(listOf(350_00L, 80_00L), summary.categories.map { it.totalCents })
        assertEquals(430_00L, summary.totalCents)
    }

    @Test
    fun `categories are listed largest first`() {
        val summary = summarize(october, listOf(stored(50_00, food), stored(900_00, bills), stored(300_00, transport)))

        assertEquals(listOf("Bills", "Transport", "Food"), summary.categories.map { it.category.name })
    }

    @Test
    fun `only the chosen month counts`() {
        val summary = summarize(
            october,
            listOf(stored(100_00, food, day = 31, month = 10), stored(999_00, food, day = 1, month = 11), stored(5_00, food, day = 30, month = 9)),
        )

        assertEquals(100_00L, summary.totalCents)
    }

    @Test
    fun `uncategorized transactions are counted but not added to the total`() {
        val summary = summarize(october, listOf(stored(100_00, food), stored(500_00, null), stored(70_00, null)))

        assertEquals(2, summary.uncategorizedCount)
        assertEquals(100_00L, summary.totalCents)
    }

    @Test
    fun `percentages add up to exactly 100`() {
        // Thirds don't round evenly: naive rounding would show 33 + 33 + 33 = 99.
        val summary = summarize(october, listOf(stored(100_00, food), stored(100_00, transport), stored(100_00, bills)))

        assertEquals(100, summary.categories.sumOf { it.percent })
    }

    @Test
    fun `each percentage reflects its share`() {
        val summary = summarize(october, listOf(stored(750_00, food), stored(250_00, transport)))

        assertEquals(listOf(75, 25), summary.categories.map { it.percent })
    }

    @Test
    fun `a month with nothing in it is empty`() {
        val summary = summarize(october, emptyList())

        assertTrue(summary.categories.isEmpty())
        assertEquals(0L, summary.totalCents)
        assertEquals(0, summary.uncategorizedCount)
    }

    @Test
    fun `months can be browsed back to the first transaction, and not into the future`() {
        val earliest = YearMonth.of(2026, 8)

        assertTrue(canGoBack(october, earliest))
        assertFalse(canGoBack(earliest, earliest))
        assertFalse(canGoForward(october, current = october))
        assertTrue(canGoForward(YearMonth.of(2026, 9), current = october))
    }

    @Test
    fun `with no transactions there is nowhere to go back to`() {
        assertNull(earliestMonth(emptyList()))
        assertFalse(canGoBack(october, null))
    }

    @Test
    fun `the earliest month is that of the oldest transaction`() {
        assertEquals(YearMonth.of(2026, 9), earliestMonth(listOf(stored(1, food, month = 10), stored(1, null, day = 3, month = 9))))
    }
}
