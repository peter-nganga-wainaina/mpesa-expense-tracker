package com.example.expensetracker.insights

import com.example.expensetracker.data.StoredTransaction
import java.time.YearMonth

/** What makes up one category's [direction] total for a month: its transactions, newest first. */
fun transactionsIn(month: YearMonth, direction: Direction, categoryId: Long, transactions: List<StoredTransaction>): List<StoredTransaction> =
    transactions
        .filter { it.category?.id == categoryId && it.direction == direction && it.isIn(month) }
        .sortedByDescending { it.transaction.occurredAt }
