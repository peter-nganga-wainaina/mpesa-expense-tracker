package com.example.expensetracker.insights

import com.example.expensetracker.data.StoredTransaction
import com.example.expensetracker.sms.MpesaTransactionType

/** Which way money moved: into her pockets, out of them, or between two of her own (counted in neither). */
enum class Direction { IN, OUT, TRANSFER }

/**
 * The default direction of each type, kept in this one place. A withdrawal is a transfer because the
 * cash she then spends is entered by hand, and counting both would double it. A Fuliza borrow is money
 * in and its repayment money out, so a month nets to what actually left her (payment plus fee).
 */
fun directionOf(type: MpesaTransactionType): Direction = when (type) {
    MpesaTransactionType.RECEIVED, MpesaTransactionType.FULIZA_DRAWDOWN -> Direction.IN
    MpesaTransactionType.WITHDRAW -> Direction.TRANSFER
    MpesaTransactionType.PAID,
    MpesaTransactionType.PAYBILL,
    MpesaTransactionType.SENT,
    MpesaTransactionType.AIRTIME,
    MpesaTransactionType.FULIZA_REPAYMENT,
    MpesaTransactionType.UNKNOWN,
    MpesaTransactionType.MANUAL -> Direction.OUT
}

val StoredTransaction.direction: Direction get() = directionOf(transaction.type)
