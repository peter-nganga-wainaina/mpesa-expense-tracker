package com.example.expensetracker.testing

import com.example.expensetracker.categorize.Category
import com.example.expensetracker.categorize.CategoryStore
import com.example.expensetracker.sms.MpesaTransaction
import com.example.expensetracker.sms.ReceivedSms
import com.example.expensetracker.sms.TransactionNotifier
import com.example.expensetracker.sms.TransactionStore

/** In-memory stand-in for the transaction tables, deduplicating like the real ones. */
class FakeTransactionStore : TransactionStore {
    val saved = linkedMapOf<String, MpesaTransaction>()
    /** The original SMS kept with each saved transaction, by code. */
    val originalMessages = mutableMapOf<String, String>()
    /** Unreadable receipts by transaction code. */
    val unparsed = linkedMapOf<String, String>()
    val categoryOf = mutableMapOf<String, Long>()

    override suspend fun insertIfNew(transaction: MpesaTransaction, sms: ReceivedSms): Boolean {
        if (transaction.code in saved) return false
        saved[transaction.code] = transaction
        originalMessages[transaction.code] = sms.body
        return true
    }

    override suspend fun saveUnparsed(transactionCode: String, sms: ReceivedSms) {
        unparsed.putIfAbsent(transactionCode, sms.body)
    }

    override suspend fun setCategory(transactionCode: String, categoryId: Long) {
        categoryOf[transactionCode] = categoryId
    }
}

/** Records what's showing in the notification shade. */
class FakeNotifier : TransactionNotifier {
    data class Prompt(val transaction: MpesaTransaction, val quickPicks: List<Category>)

    val showing = linkedMapOf<String, Prompt>()
    var promptCount = 0
        private set

    override fun promptForCategory(transaction: MpesaTransaction, quickPicks: List<Category>) {
        promptCount++
        showing[transaction.code] = Prompt(transaction, quickPicks)
    }

    override fun dismiss(transactionCode: String) {
        showing.remove(transactionCode)
    }
}

/** In-memory categories, with names unique ignoring case like the real table. */
class FakeCategoryStore(vararg names: String) : CategoryStore {
    private val all = names.mapIndexed { i, name -> Category(id = i + 1L, name = name) }.toMutableList()
    var uses: Map<Long, Int> = emptyMap()

    override suspend fun categories(): List<Category> = all.toList()

    override suspend fun useCounts(): Map<Long, Int> = uses

    override suspend fun add(name: String): Category? {
        if (all.any { it.name.equals(name, ignoreCase = true) }) return null
        return Category(id = all.size + 1L, name = name).also { all += it }
    }

    fun named(name: String): Category = all.single { it.name == name }
}
