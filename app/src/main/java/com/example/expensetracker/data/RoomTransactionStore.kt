package com.example.expensetracker.data

import com.example.expensetracker.categorize.Category
import com.example.expensetracker.sms.MpesaTransaction
import com.example.expensetracker.sms.MpesaTransactionType
import com.example.expensetracker.sms.ReceivedSms
import com.example.expensetracker.sms.TransactionStore
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomTransactionStore(private val db: AppDatabase) : TransactionStore {

    override suspend fun insertIfNew(transaction: MpesaTransaction, sms: ReceivedSms): Boolean =
        db.transactions().insert(transaction.toEntity(sms)) != -1L

    override suspend fun saveUnparsed(transactionCode: String, sms: ReceivedSms) =
        db.unparsedMessages().insert(UnparsedMessageEntity(transactionCode, sms.body, sms.receivedAt.toString()))

    override suspend fun setCategory(transactionCode: String, categoryId: Long) =
        db.transactions().setCategory(transactionCode, categoryId)

    /** Every transaction, newest first, with its category; updates as transactions arrive or get filed. */
    fun observeHistory(): Flow<List<StoredTransaction>> =
        db.transactions().observeHistory().map { rows -> rows.map { it.toStoredTransaction() } }

    /** Uncategorized transactions, newest first, updating as they arrive or get filed. */
    fun observeUncategorized(): Flow<List<StoredTransaction>> =
        db.transactions().observeUncategorized().map { rows -> rows.map { StoredTransaction(it.toTransaction(), null, it.body) } }
}

/** A saved transaction with its category (null while it waits to be categorized) and its original SMS. */
data class StoredTransaction(val transaction: MpesaTransaction, val category: Category?, val originalMessage: String)

fun TransactionWithCategory.toStoredTransaction(): StoredTransaction {
    val categoryId = transaction.categoryId
    // The foreign key guarantees a name for every categoryId; a missing one means the query is wrong.
    val category = categoryId?.let { Category(it, checkNotNull(categoryName) { "Category $it has no name" }) }
    return StoredTransaction(transaction.toTransaction(), category, transaction.body)
}

/** A new, uncategorized row. Falls back to the arrival time when the message carried no time of its own. */
fun MpesaTransaction.toEntity(sms: ReceivedSms) = TransactionEntity(
    code = code,
    type = type.name,
    amountCents = amountCents,
    costCents = costCents,
    counterparty = counterparty,
    phone = phone,
    reference = reference,
    occurredAt = (occurredAt ?: sms.receivedAt).toString(),
    body = sms.body,
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
