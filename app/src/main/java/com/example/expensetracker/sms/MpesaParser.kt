package com.example.expensetracker.sms

import com.example.expensetracker.sms.MpesaTransactionType.AIRTIME
import com.example.expensetracker.sms.MpesaTransactionType.FULIZA_DRAWDOWN
import com.example.expensetracker.sms.MpesaTransactionType.FULIZA_REPAYMENT
import com.example.expensetracker.sms.MpesaTransactionType.PAID
import com.example.expensetracker.sms.MpesaTransactionType.PAYBILL
import com.example.expensetracker.sms.MpesaTransactionType.RECEIVED
import com.example.expensetracker.sms.MpesaTransactionType.SENT
import com.example.expensetracker.sms.MpesaTransactionType.UNKNOWN
import com.example.expensetracker.sms.MpesaTransactionType.WITHDRAW
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Turns the body of an M-Pesa SMS into a [ParseResult].
 *
 * Each field is extracted independently by its landmarks (a keyword before it, the next landmark
 * after it) rather than by matching the whole sentence. A wording change then only breaks the fields
 * whose landmarks changed, and a message with a code and an amount but no known type keyword still
 * comes through as [UNKNOWN] instead of being lost.
 */
object MpesaParser {

    fun parse(body: String): ParseResult {
        val text = body.replace(WHITESPACE, " ").trim()
        // Every money movement starts with its transaction code, so a message without one is a notice.
        val code = CODE.find(text)?.groupValues?.get(1) ?: return ParseResult.Ignored
        // A balance check carries a code and an amount but moves no money.
        if (BALANCE_CHECK.containsMatchIn(text)) return ParseResult.Ignored

        val amount = AMOUNT.find(text)?.groupValues?.get(1) ?: return ParseResult.Unrecognized
        val type = TYPE_KEYWORDS.firstOrNull { (keyword, _) -> text.contains(keyword, ignoreCase = true) }?.second
            ?: UNKNOWN
        val party = counterparty(text, type)

        return ParseResult.Parsed(
            MpesaTransaction(
                code = code.uppercase(),
                type = type,
                amountCents = toCents(amount),
                costCents = COST.find(text)?.let { toCents(it.groupValues[1]) } ?: 0,
                counterparty = party.name,
                phone = party.phone,
                reference = reference(text, type),
                occurredAt = dateTime(text),
            )
        )
    }

    private data class Party(val name: String?, val phone: String? = null)

    private fun counterparty(text: String, type: MpesaTransactionType): Party = when (type) {
        PAYBILL, SENT -> splitPhone(between(text, "sent to "))
        PAID -> Party(between(text, "paid to "))
        RECEIVED -> splitPhone(between(text, " from "))
        WITHDRAW -> Party(between(text, " from ")?.substringAfter(" - "))
        AIRTIME -> Party("Airtime", AIRTIME_FOR.find(text)?.groupValues?.get(1))
        FULIZA_DRAWDOWN, FULIZA_REPAYMENT -> Party("Fuliza")
        UNKNOWN -> Party(null)
    }

    /** Paybill account number or withdrawal agent number. */
    private fun reference(text: String, type: MpesaTransactionType): String? = when (type) {
        PAYBILL -> between(text, " for account ")
        WITHDRAW -> between(text, " from ")?.substringBefore(" - ")
        else -> null
    }

    /** The text after [start], up to the next landmark (a date, "for account", or the balance). */
    private fun between(text: String, start: String): String? {
        val from = text.indexOf(start, ignoreCase = true).takeIf { it >= 0 }?.plus(start.length) ?: return null
        val to = FIELD_END.find(text, from)?.range?.first ?: return null
        return text.substring(from, to).trim().trimEnd('.').trim().takeIf { it.isNotEmpty() }
    }

    /** "ALICE MWANGI 0700000001" -> name and phone. The phone may be masked or absent. */
    private fun splitPhone(segment: String?): Party {
        if (segment == null) return Party(null)
        val match = TRAILING_PHONE.find(segment) ?: return Party(segment)
        return Party(segment.substring(0, match.range.first).trim(), match.groupValues[1])
    }

    private fun dateTime(text: String): LocalDateTime? =
        DATE_TIME.find(text)?.let { m ->
            LocalDateTime.parse("${m.groupValues[1]} ${m.groupValues[2].uppercase()}", DATE_TIME_FORMAT)
        }

    /** "1,100.00" -> 110000. Money never goes through floating point. */
    private fun toCents(amount: String): Long =
        amount.replace(",", "").toBigDecimal().movePointRight(2).longValueExact()

    private fun regex(pattern: String) = Regex(pattern, RegexOption.IGNORE_CASE)

    private val WHITESPACE = Regex("""\s+""")
    private val CODE = Regex("""^([A-Z0-9]{10}) """)
    private val AMOUNT = regex("""Ksh ?([\d,]+\.\d{2})""")
    private val COST = regex("""(?:Transaction cost,|Access Fee charged) Ksh ?([\d,]+\.\d{2})""")
    private val DATE_TIME = regex("""(\d{1,2}/\d{1,2}/\d{2}) (?:at )?(\d{1,2}:\d{2} [AP]M)""")
    private val DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("d/M/yy h:mm a", Locale.ENGLISH)
    private val FIELD_END = regex(""" (?:on \d{1,2}/\d{1,2}/\d{2}|for account |New M-PESA balance)""")
    private val TRAILING_PHONE = Regex(""" ([\d*+]{10,13})$""")
    private val AIRTIME_FOR = regex("""airtime for ([\d*+]{10,13})""")

    // First keyword found decides the type. Order matters where keywords overlap:
    // paybills also say "sent to", so "for account" comes first.
    private val TYPE_KEYWORDS = listOf(
        "Fuliza M-PESA amount is" to FULIZA_DRAWDOWN,
        "pay your outstanding Fuliza" to FULIZA_REPAYMENT,
        "for account" to PAYBILL,
        "paid to" to PAID,
        "sent to" to SENT,
        "received" to RECEIVED,
        "Withdraw" to WITHDRAW,
        "of airtime" to AIRTIME,
    )
    private val BALANCE_CHECK = regex("""Your account balance was""")
}
