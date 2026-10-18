package com.example.expensetracker.manual

import java.time.LocalDateTime
import java.util.UUID

/** How a transaction was paid. M-Pesa ones come from SMS; the rest are added by hand. */
enum class PaymentSource(val label: String) {
    MPESA("M-Pesa"),
    CASH("Cash"),
    CARD("Card"),
    OTHER("Other");

    val isManual: Boolean get() = this != MPESA

    companion object {
        /** The choices offered in the add form. */
        val MANUAL = entries.filter { it.isManual }
    }
}

/** Spending added by hand: what the form produces, ready to save. */
data class ManualEntry(
    val code: String,
    val amountCents: Long,
    /** "What for", e.g. "Mama Mboga". Doubles as the counterparty, so merchant memory works for it. */
    val note: String?,
    val source: PaymentSource,
    val occurredAt: LocalDateTime,
    val categoryId: Long,
) {
    init {
        require(amountCents > 0) { "Amount must be positive: $amountCents" }
        require(source.isManual) { "Manual entries can't be M-Pesa" }
        require(isManualCode(code)) { "Not a manual entry code: $code" }
    }
}

/**
 * Reads a typed amount: "450", "1,250.50", "Ksh 300". Returns cents, or null for anything that isn't
 * a positive amount with at most two decimals (money never goes through floating point).
 */
fun parseKshAmount(text: String): Long? {
    val cleaned = text.trim().removePrefix("Ksh").removePrefix("KSh").replace(",", "").replace(" ", "")
    if (!AMOUNT.matches(cleaned)) return null
    return cleaned.toBigDecimal().movePointRight(2).longValueExact().takeIf { it > 0 }
}

/** Cents back into what [parseKshAmount] reads, for editing: 45000 -> "450", 125050 -> "1250.50". */
fun editableAmount(cents: Long): String =
    cents.toBigDecimal().movePointLeft(2).stripTrailingZeros().let { if (it.scale() > 0) it.setScale(2) else it }.toPlainString()

// Up to 9 shilling digits: far above any M-Pesa limit, and safely inside a Long once in cents.
private val AMOUNT = Regex("""\d{1,9}(\.\d{1,2})?""")

/**
 * A new manual entry's ID. M-Pesa codes are exactly 10 letters and digits; these contain "-", so the
 * two can never collide in the shared transaction table.
 */
fun newManualCode(uuid: UUID = UUID.randomUUID()): String = "$MANUAL_PREFIX$uuid"

fun isManualCode(code: String): Boolean = code.startsWith(MANUAL_PREFIX)

private const val MANUAL_PREFIX = "MANUAL-"

/** What the add form holds while the user types; [check] turns it into an entry or says what's missing. */
data class ManualEntryForm(
    val amountText: String = "",
    val note: String = "",
    val source: PaymentSource = PaymentSource.CASH,
    val occurredAt: LocalDateTime,
    val categoryId: Long? = null,
)

sealed interface FormCheck {
    data class Ready(val entry: ManualEntry) : FormCheck
    /** What still stops saving, in the order the form shows it. */
    data class Incomplete(val missingAmount: Boolean, val missingCategory: Boolean) : FormCheck
}

/** Validates the form for a new entry ([existingCode] null) or an edit of an existing one. */
fun ManualEntryForm.check(existingCode: String? = null, newCode: () -> String = ::newManualCode): FormCheck {
    val cents = parseKshAmount(amountText)
    if (cents == null || categoryId == null) return FormCheck.Incomplete(cents == null, categoryId == null)
    return FormCheck.Ready(
        ManualEntry(
            code = existingCode ?: newCode(),
            amountCents = cents,
            note = note.trim().replace(Regex("""\s+"""), " ").ifEmpty { null },
            source = source,
            occurredAt = occurredAt,
            categoryId = categoryId,
        )
    )
}
