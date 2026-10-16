package com.example.expensetracker.suggest

import com.example.expensetracker.categorize.Category
import com.example.expensetracker.categorize.CategoryStore
import com.example.expensetracker.sms.MpesaTransaction

/** Ranks categories for a transaction. Only suggests; the user always makes the choice. */
fun interface CategorySuggester {
    suspend fun suggest(transaction: MpesaTransaction, limit: Int): List<Category>
}

/** The user's most-used categories, with unused ones in the order they were created. */
class MostUsedSuggester(private val categories: CategoryStore) : CategorySuggester {

    override suspend fun suggest(transaction: MpesaTransaction, limit: Int): List<Category> {
        val uses = categories.useCounts()
        return categories.categories()
            .sortedWith(compareByDescending<Category> { uses[it.id] ?: 0 }.thenBy { it.id })
            .take(limit)
    }
}
