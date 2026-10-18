package com.example.expensetracker.data

import com.example.expensetracker.categorize.Category
import com.example.expensetracker.manual.ManualEntry
import com.example.expensetracker.manual.ManualEntryStore
import com.example.expensetracker.manual.PaymentSource
import com.example.expensetracker.sms.MpesaTransaction
import com.example.expensetracker.sms.MpesaTransactionType
import com.example.expensetracker.sms.ReceivedSms
import com.example.expensetracker.sms.TransactionStore
import com.example.expensetracker.unparsed.UnparsedMessage
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomTransactionStore(private val db: AppDatabase) : TransactionStore, ManualEntryStore {

    override suspend fun insertIfNew(transaction: MpesaTransaction, sms: ReceivedSms): Boolean =
        db.transactions().insert(transaction.toEntity(sms)) != -1L

    override suspend fun saveUnparsed(transactionCode: String, sms: ReceivedSms) =
        db.unparsedMessages().insert(UnparsedMessageEntity(transactionCode, sms.body, sms.receivedAt.toString()))

    override suspend fun save(entry: ManualEntry) = db.transactions().upsert(entry.toEntity())

    override suspend fun delete(code: String) {
        // Fail loudly rather than silently ignore: the UI only offers Delete on manual entries.
        check(db.transactions().deleteManual(code) == 1) { "No manual entry to delete with code $code" }
    }

    override suspend fun setCategory(transactionCode: String, categoryId: Long) =
        db.transactions().setCategory(transactionCode, categoryId)

    /** Every transaction, newest first, with its category; updates as transactions arrive or get filed. */
    fun observeHistory(): Flow<List<StoredTransaction>> =
        db.transactions().observeHistory().map { rows -> rows.map { it.toStoredTransaction() } }

    /** Unreadable receipts the user hasn't dismissed, newest first. */
    fun observeUnparsed(): Flow<List<UnparsedMessage>> =
        db.unparsedMessages().observeWaiting().map { rows -> rows.map { it.toUnparsedMessage() } }

    suspend fun dismissUnparsed(code: String) = db.unparsedMessages().dismiss(code)

    /** Uncategorized transactions, newest first, updating as they arrive or get filed. */
    fun observeUncategorized(): Flow<List<StoredTransaction>> =
        db.transactions().observeUncategorized().map { rows -> rows.map { StoredTransaction(it.toTransaction(), null, it.body, PaymentSource.valueOf(it.source)) } }
}

/**
 * A saved transaction with its category (null while it waits to be categorized), its original SMS
 * (null for manual entries), and how it was paid.
 */
data class StoredTransaction(
    val transaction: MpesaTransaction,
    val category: Category?,
    val originalMessage: String?,
    val source: PaymentSource,
) {
    /** Typed in by the user (editable and deletable), as opposed to read from an SMS. */
    val isManual: Boolean get() = transaction.type == MpesaTransactionType.MANUAL
}

fun TransactionWithCategory.toStoredTransaction(): StoredTransaction {
    val categoryId = transaction.categoryId
    // The foreign key guarantees a name for every categoryId; a missing one means the query is wrong.
    val category = categoryId?.let { Category(it, checkNotNull(categoryName) { "Category $it has no name" }) }
    return StoredTransaction(transaction.toTransaction(), category, transaction.body, PaymentSource.valueOf(transaction.source))
}

fun UnparsedMessageEntity.toUnparsedMessage() = UnparsedMessage(code, body, LocalDateTime.parse(receivedAt))

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
    source = PaymentSource.MPESA.name,
)

/** A manual entry's row: already categorized, no SMS, the note standing in as the counterparty. */
fun ManualEntry.toEntity() = TransactionEntity(
    code = code,
    type = MpesaTransactionType.MANUAL.name,
    amountCents = amountCents,
    costCents = 0,
    counterparty = note,
    phone = null,
    reference = null,
    occurredAt = occurredAt.toString(),
    body = null,
    source = source.name,
    categoryId = categoryId,
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
