package com.example.expensetracker.insights

import com.example.expensetracker.categorize.Category
import com.example.expensetracker.data.StoredTransaction
import java.time.YearMonth

/** One category's slice of a month: its total and its share of the month, in whole percent. */
data class CategoryTotal(val category: Category, val totalCents: Long, val percent: Int)

/**
 * A month at a glance: categorized totals, largest first, and how many transactions still wait for a
 * category (counted, not totaled, so the total never silently includes guesses).
 */
data class MonthSummary(
    val month: YearMonth,
    val categories: List<CategoryTotal>,
    val totalCents: Long,
    val uncategorizedCount: Int,
)

/**
 * Totals the [direction] transaction amounts of [month] by category. Fees are not included: they are
 * recorded separately and whether to show them is a later decision. The uncategorized count covers
 * every direction, since anything waiting in the queue could belong to either side.
 */
fun summarize(month: YearMonth, direction: Direction, transactions: List<StoredTransaction>): MonthSummary {
    val inMonth = transactions.filter { it.isIn(month) }
    val totals = inMonth
        .filter { it.direction == direction }
        .mapNotNull { stored -> stored.category?.let { it to stored.transaction.amountCents } }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, amounts) -> amounts.sum() }
    val totalCents = totals.values.sum()
    val percents = wholePercents(totals.mapKeys { it.key.id }, totalCents)
    return MonthSummary(
        month = month,
        categories = totals
            .map { (category, cents) -> CategoryTotal(category, cents, percents.getValue(category.id)) }
            .sortedWith(compareByDescending<CategoryTotal> { it.totalCents }.thenBy { it.category.name }),
        totalCents = totalCents,
        uncategorizedCount = inMonth.count { it.category == null },
    )
}

/**
 * Money in minus money out for [month]: the difference of the two totals Insights shows, so it adds up
 * on screen. Like them, it leaves out uncategorized transactions; transfers count in neither.
 */
fun netCents(month: YearMonth, transactions: List<StoredTransaction>): Long =
    summarize(month, Direction.IN, transactions).totalCents - summarize(month, Direction.OUT, transactions).totalCents

// Stored transactions always have a time: the arrival time stands in when the SMS has none.
internal fun StoredTransaction.isIn(month: YearMonth): Boolean = YearMonth.from(checkNotNull(transaction.occurredAt)) == month

/**
 * Whole-number percentages that always add up to exactly 100 (largest remainder method), so the
 * list never shows 99% or 101% in total because of rounding.
 */
internal fun <K> wholePercents(amounts: Map<K, Long>, total: Long): Map<K, Int> {
    if (total == 0L) return amounts.mapValues { 0 }
    val exact = amounts.mapValues { (_, cents) -> cents * 100.0 / total }
    val floors = exact.mapValues { (_, value) -> value.toInt() }
    val leftover = 100 - floors.values.sum()
    val roundedUp = exact.entries.sortedByDescending { it.value - it.value.toInt() }.take(leftover).map { it.key }.toSet()
    return floors.mapValues { (key, floor) -> if (key in roundedUp) floor + 1 else floor }
}

/** The month the user may go back to: not before their first transaction. */
fun canGoBack(month: YearMonth, earliest: YearMonth?): Boolean = earliest != null && month > earliest

/** No future months: there is nothing to show there yet. */
fun canGoForward(month: YearMonth, current: YearMonth): Boolean = month < current

/** The month of the oldest transaction, or null when there are none. */
fun earliestMonth(transactions: List<StoredTransaction>): YearMonth? =
    transactions.mapNotNull { it.transaction.occurredAt?.let(YearMonth::from) }.minOrNull()
