package com.example.expensetracker.data

import com.example.expensetracker.sms.MpesaTransaction
import com.example.expensetracker.sms.MpesaTransactionType
import com.example.expensetracker.sms.TransactionStore
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomTransactionStore(private val db: AppDatabase) : TransactionStore {

    override suspend fun insertIfNew(transaction: MpesaTransaction, receivedAt: LocalDateTime): Boolean =
        db.transactions().insert(transaction.toEntity(receivedAt)) != -1L

    override suspend fun saveUnparsed(body: String, receivedAt: LocalDateTime) =
        db.unparsedMessages().insert(UnparsedMessageEntity(body = body, receivedAt = receivedAt.toString()))

    override suspend fun setCategory(transactionCode: String, categoryId: Long) =
        db.transactions().setCategory(transactionCode, categoryId)

    /** Uncategorized transactions, newest first, updating as they arrive or get filed. */
    fun observeUncategorized(): Flow<List<MpesaTransaction>> =
        db.transactions().observeUncategorized().map { rows -> rows.map { it.toTransaction() } }
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
