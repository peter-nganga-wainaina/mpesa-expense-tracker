package com.example.expensetracker.insights

import com.example.expensetracker.data.StoredTransaction
import com.example.expensetracker.sms.MpesaTransactionType
import java.time.YearMonth

/** What makes up one category's [direction] total for a month: its transactions, newest first. */
fun transactionsIn(month: YearMonth, direction: Direction, categoryId: Long, transactions: List<StoredTransaction>): List<StoredTransaction> =
    transactions
        .filter { it.category?.id == categoryId && it.direction == direction && it.isIn(month) }
        .sortedByDescending { it.transaction.occurredAt }

/** What makes up the month's "M-Pesa fees" line: transactions whose fee counts, newest first. */
fun feesIn(month: YearMonth, transactions: List<StoredTransaction>): List<StoredTransaction> =
    transactions
        .filter { it.feeCents > 0 && it.isIn(month) }
        .sortedByDescending { it.transaction.occurredAt }

/** The Fuliza access fees the fees line leaves out (they're inside the repayments), so the screen can say so. */
fun fulizaAccessFeesCents(month: YearMonth, transactions: List<StoredTransaction>): Long =
    transactions
        .filter { it.transaction.type == MpesaTransactionType.FULIZA_DRAWDOWN && it.isIn(month) }
        .sumOf { it.transaction.costCents }
