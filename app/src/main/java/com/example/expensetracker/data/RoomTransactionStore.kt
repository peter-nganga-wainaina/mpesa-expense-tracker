package com.example.expensetracker.data

import com.example.expensetracker.categorize.Category
import com.example.expensetracker.sms.MpesaTransaction
import com.example.expensetracker.sms.MpesaTransactionType
import com.example.expensetracker.sms.TransactionStore
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomTransactionStore(private val db: AppDatabase) : TransactionStore {

    override suspend fun insertIfNew(transaction: MpesaTransaction, receivedAt: LocalDateTime): Boolean =
        db.transactions().insert(transaction.toEntity(receivedAt)) != -1L

    override suspend fun saveUnparsed(transactionCode: String, body: String, receivedAt: LocalDateTime) =
        db.unparsedMessages().insert(UnparsedMessageEntity(transactionCode, body, receivedAt.toString()))

    override suspend fun setCategory(transactionCode: String, categoryId: Long) =
        db.transactions().setCategory(transactionCode, categoryId)

    /** Every transaction, newest first, with its category; updates as transactions arrive or get filed. */
    fun observeHistory(): Flow<List<HistoryItem>> =
        db.transactions().observeHistory().map { rows -> rows.map { it.toHistoryItem() } }

    /** Uncategorized transactions, newest first, updating as they arrive or get filed. */
    fun observeUncategorized(): Flow<List<MpesaTransaction>> =
        db.transactions().observeUncategorized().map { rows -> rows.map { it.toTransaction() } }
}

/** A transaction as History shows it: with its category, or null while it waits to be categorized. */
data class HistoryItem(val transaction: MpesaTransaction, val category: Category?)

fun TransactionWithCategory.toHistoryItem(): HistoryItem {
    val categoryId = transaction.categoryId
    // The foreign key guarantees a name for every categoryId; a missing one means the query is wrong.
    val category = categoryId?.let { Category(it, checkNotNull(categoryName) { "Category $it has no name" }) }
    return HistoryItem(transaction.toTransaction(), category)
}

/** A new, uncategorized row. Falls back to [receivedAt] when the message carried no time of its own. */
fun MpesaTransaction.toEntity(receivedAt: LocalDateTime) = TransactionEntity(
    code = code,
    type = type.name,
    amountCents = amountCents,
    costCents = costCents,
    counterparty = counterparty,
    phone = phone,
    reference = reference,
    occurredAt = (occurredAt ?: receivedAt).toString(),
)

/** Every stored row has a time, so the result's [MpesaTransaction.occurredAt] is never null. */
fun TransactionEntity.toTransaction() = MpesaTransaction(
    code = code,
    type = MpesaTransactionType.valueOf(type),
    amountCents = amountCents,
    costCents = costCents,
    counterparty = counterparty,
    phone = phone,
    reference = reference,
    occurredAt = LocalDateTime.parse(occurredAt),
)
