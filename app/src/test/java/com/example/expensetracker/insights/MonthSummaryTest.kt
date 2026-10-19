package com.example.expensetracker.insights

import com.example.expensetracker.categorize.Category
import com.example.expensetracker.data.StoredTransaction
import com.example.expensetracker.manual.PaymentSource
import com.example.expensetracker.sms.MpesaTransaction
import com.example.expensetracker.sms.MpesaTransactionType
import com.example.expensetracker.sms.MpesaTransactionType.FULIZA_DRAWDOWN
import com.example.expensetracker.sms.MpesaTransactionType.PAID
import com.example.expensetracker.sms.MpesaTransactionType.RECEIVED
import com.example.expensetracker.sms.MpesaTransactionType.WITHDRAW
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

    private fun stored(cents: Long, category: Category?, day: Int = 10, month: Int = 10, type: MpesaTransactionType = PAID, fee: Long = 0) = StoredTransaction(
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

    @Test
    fun `amounts are totaled per category`() {
        val summary = summarize(october, Direction.OUT, listOf(stored(100_00, food), stored(250_00, food), stored(80_00, transport)))

        assertEquals(listOf(350_00L, 80_00L), summary.categories.map { it.totalCents })
        assertEquals(430_00L, summary.totalCents)
    }

    @Test
    fun `categories are listed largest first`() {
        val summary = summarize(october, Direction.OUT, listOf(stored(50_00, food), stored(900_00, bills), stored(300_00, transport)))

        assertEquals(listOf("Bills", "Transport", "Food"), summary.categories.map { it.category.name })
    }

    @Test
    fun `only the chosen month counts`() {
        val summary = summarize(
            october,
            Direction.OUT,
            listOf(stored(100_00, food, day = 31, month = 10), stored(999_00, food, day = 1, month = 11), stored(5_00, food, day = 30, month = 9)),
        )

        assertEquals(100_00L, summary.totalCents)
    }

    @Test
    fun `uncategorized transactions are counted but not added to the total`() {
        val summary = summarize(october, Direction.OUT, listOf(stored(100_00, food), stored(500_00, null), stored(70_00, null)))

        assertEquals(2, summary.uncategorizedCount)
        assertEquals(100_00L, summary.totalCents)
    }

    @Test
    fun `each direction totals only its own transactions`() {
        val month = listOf(stored(100_00, food), stored(2_000_00, bills, type = RECEIVED), stored(500_00, transport, type = WITHDRAW))

        assertEquals(listOf("Food"), summarize(october, Direction.OUT, month).categories.map { it.category.name })
        assertEquals(listOf("Bills"), summarize(october, Direction.IN, month).categories.map { it.category.name })
    }

    @Test
    fun `net is money in minus money out, leaving out transfers and uncategorized transactions`() {
        val month = listOf(
            stored(300_00, food),
            stored(1_000_00, bills, type = RECEIVED),
            stored(500_00, transport, type = WITHDRAW),
            stored(40_00, null),
            stored(60_00, null, type = RECEIVED),
        )

        assertEquals(700_00L, netCents(october, month))
    }

    @Test
    fun `fees are their own line in money out, counted in its total`() {
        val summary = summarize(october, Direction.OUT, listOf(stored(1_100_00, food, fee = 23_00), stored(500_00, transport, fee = 7_00)))

        assertEquals(30_00L, summary.feesCents)
        assertEquals(listOf(1_100_00L, 500_00L), summary.categories.map { it.totalCents })
        assertEquals(1_630_00L, summary.totalCents)
    }

    @Test
    fun `fees count whether or not their transaction is categorized or a transfer`() {
        val summary = summarize(october, Direction.OUT, listOf(stored(1_100_00, null, fee = 23_00), stored(2_000_00, null, type = WITHDRAW, fee = 29_00)))

        assertEquals(52_00L, summary.feesCents)
        assertEquals(52_00L, summary.totalCents)
    }

    @Test
    fun `a Fuliza access fee is not a fee here, since the repayment already includes it`() {
        val summary = summarize(october, Direction.OUT, listOf(stored(948_44, null, type = FULIZA_DRAWDOWN, fee = 9_49)))

        assertEquals(0L, summary.feesCents)
    }

    @Test
    fun `money in has no fees line`() {
        val summary = summarize(october, Direction.IN, listOf(stored(3_000_00, bills, type = RECEIVED), stored(100_00, food, fee = 23_00)))

        assertEquals(0L, summary.feesCents)
        assertEquals(3_000_00L, summary.totalCents)
    }

    @Test
    fun `the fees line gets its share of money out, and all the percentages add up to 100`() {
        val summary = summarize(october, Direction.OUT, listOf(stored(100_00, food, fee = 200_00), stored(100_00, transport)))

        assertEquals(50, summary.feesPercent)
        assertEquals(listOf(25, 25), summary.categories.map { it.percent })
    }

    @Test
    fun `a share too small to round to 1 percent shows as under 1 percent, not 0`() {
        assertEquals("<1%", percentLabel(percent = 0, cents = 44_00))
        assertEquals("0%", percentLabel(percent = 0, cents = 0))
        assertEquals("1%", percentLabel(percent = 1, cents = 120_00))
        assertEquals("59%", percentLabel(percent = 59, cents = 12_657_00))
    }

    @Test
    fun `percentages add up to exactly 100`() {
        // Thirds don't round evenly: naive rounding would show 33 + 33 + 33 = 99.
        val summary = summarize(october, Direction.OUT, listOf(stored(100_00, food), stored(100_00, transport), stored(100_00, bills)))

        assertEquals(100, summary.categories.sumOf { it.percent })
    }

    @Test
    fun `each percentage reflects its share`() {
        val summary = summarize(october, Direction.OUT, listOf(stored(750_00, food), stored(250_00, transport)))

        assertEquals(listOf(75, 25), summary.categories.map { it.percent })
    }

    @Test
    fun `a month with nothing in it is empty`() {
        val summary = summarize(october, Direction.OUT, emptyList())

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
