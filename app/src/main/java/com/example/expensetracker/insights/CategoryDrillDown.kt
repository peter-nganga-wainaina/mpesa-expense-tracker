package com.example.expensetracker.insights

import com.example.expensetracker.data.StoredTransaction
import java.time.YearMonth

/** What makes up one category's total for a month: its transactions, newest first. */
fun transactionsIn(month: YearMonth, categoryId: Long, transactions: List<StoredTransaction>): List<StoredTransaction> =
    transactions
        // Stored transactions always have a time: the arrival time stands in when the SMS has none.
        .filter { it.category?.id == categoryId && YearMonth.from(checkNotNull(it.transaction.occurredAt)) == month }
        .sortedByDescending { it.transaction.occurredAt }
