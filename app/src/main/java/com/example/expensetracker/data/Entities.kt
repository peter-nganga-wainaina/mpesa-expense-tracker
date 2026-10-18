package com.example.expensetracker.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Times are stored as ISO-8601 local date-times ("2025-12-04T08:07"): M-Pesa reports Kenyan local
 * time with no zone, and the text form sorts chronologically and reads clearly in a DB browser.
 */
@Entity(
    tableName = "transactions",
    foreignKeys = [ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["categoryId"])],
    indices = [Index("categoryId")],
)
data class TransactionEntity(
    /** The M-Pesa transaction code; unique per money movement, so it doubles as the dedupe key. */
    @PrimaryKey val code: String,
    /** An [com.example.expensetracker.sms.MpesaTransactionType] name. */
    val type: String,
    val amountCents: Long,
    val costCents: Long,
    val counterparty: String?,
    val phone: String?,
    val reference: String?,
    val occurredAt: String,
    /**
     * The full SMS as received, so the user can check what the app read and it can be re-parsed later.
     * Null for entries added by hand.
     */
    val body: String?,
    /** A [com.example.expensetracker.manual.PaymentSource] name: MPESA for SMS, else how it was paid. */
    val source: String,
    /** Null until the user picks a category. */
    val categoryId: Long? = null,
)

@Entity(tableName = "categories", indices = [Index("name", unique = true)])
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Unique ignoring case, so "food" can't be added beside "Food". */
    @ColumnInfo(collate = ColumnInfo.NOCASE) val name: String,
)

/** A receipt the parser couldn't read. Keyed by its transaction code, so each is kept once. */
@Entity(tableName = "unparsed_messages")
data class UnparsedMessageEntity(
    @PrimaryKey val code: String,
    val body: String,
    val receivedAt: String,
    /**
     * The user has dealt with it. A flag rather than a delete: the sweep re-reads recent SMS, and
     * the insert keeps the existing row, so a dismissed message stays dismissed.
     */
    val dismissed: Boolean = false,
)
