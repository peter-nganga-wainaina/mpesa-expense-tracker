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
    /** Null until the user picks a category. */
    val categoryId: Long? = null,
)

@Entity(tableName = "categories", indices = [Index("name", unique = true)])
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Unique ignoring case, so "food" can't be added beside "Food". */
    @ColumnInfo(collate = ColumnInfo.NOCASE) val name: String,
)

@Entity(tableName = "unparsed_messages")
data class UnparsedMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val body: String,
    val receivedAt: String,
)
