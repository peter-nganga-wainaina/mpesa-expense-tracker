package com.example.expensetracker.data

import com.example.expensetracker.sms.MpesaTransaction
import com.example.expensetracker.sms.TransactionStore
import java.time.LocalDateTime

class RoomTransactionStore(private val db: AppDatabase) : TransactionStore {

    override suspend fun insertIfNew(transaction: MpesaTransaction, receivedAt: LocalDateTime): Boolean =
        db.transactions().insert(transaction.toEntity(receivedAt)) != -1L

    override suspend fun saveUnparsed(body: String, receivedAt: LocalDateTime) =
        db.unparsedMessages().insert(UnparsedMessageEntity(body = body, receivedAt = receivedAt.toString()))
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
