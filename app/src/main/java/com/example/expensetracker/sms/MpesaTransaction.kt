package com.example.expensetracker.sms

import java.time.LocalDateTime

enum class MpesaTransactionType {
    PAID,
    PAYBILL,
    SENT,
    RECEIVED,
    WITHDRAW,
    AIRTIME,
    FULIZA_DRAWDOWN,
    FULIZA_REPAYMENT,

    /** Has a transaction code and an amount, but no known type keyword: likely a new M-Pesa format. */
    UNKNOWN,

    /** Added by hand (cash, card, other) rather than read from an SMS; the parser never produces it. */
    MANUAL,
}

data class MpesaTransaction(
    val code: String,
    val type: MpesaTransactionType,
    val amountCents: Long,
    /** Transaction cost, or the access fee for a Fuliza drawdown. */
    val costCents: Long,
    /** Null when it can't be located, e.g. for an [MpesaTransactionType.UNKNOWN] transaction. */
    val counterparty: String?,
    /** Full or masked (`0700***003`); null when the message has no number. */
    val phone: String?,
    /** Paybill account number or withdrawal agent number. */
    val reference: String?,
    /** Null when the message carries no transaction time; callers fall back to the SMS timestamp. */
    val occurredAt: LocalDateTime?,
)

sealed interface ParseResult {
    data class Parsed(val transaction: MpesaTransaction) : ParseResult

    /** Moves no money: anything without a transaction code (failures, notices) and balance checks. */
    data object Ignored : ParseResult

    /** Has a transaction code but no readable amount: a receipt in a format the parser can't read. */
    data class Unrecognized(val code: String) : ParseResult
}
