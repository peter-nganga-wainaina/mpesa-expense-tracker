package com.example.expensetracker.testing

import com.example.expensetracker.categorize.Category
import com.example.expensetracker.categorize.CategoryStore
import com.example.expensetracker.sms.MpesaTransaction
import com.example.expensetracker.sms.TransactionNotifier
import com.example.expensetracker.sms.TransactionStore
import java.time.LocalDateTime

/** In-memory stand-in for the transaction tables, deduplicating by code like the real one. */
class FakeTransactionStore : TransactionStore {
    val saved = linkedMapOf<String, MpesaTransaction>()
    val unparsed = mutableListOf<String>()
    val categoryOf = mutableMapOf<String, Long>()

    override suspend fun insertIfNew(transaction: MpesaTransaction, receivedAt: LocalDateTime): Boolean =
        saved.putIfAbsent(transaction.code, transaction) == null

    override suspend fun saveUnparsed(body: String, receivedAt: LocalDateTime) {
        unparsed += body
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
