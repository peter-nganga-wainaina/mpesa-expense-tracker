package com.example.expensetracker.suggest

import com.example.expensetracker.categorize.Category
import com.example.expensetracker.sms.MpesaTransaction

/** The user's past choices, looked up by counterparty. */
fun interface MerchantHistory {
    /**
     * The category of the most recent categorized transaction with this counterparty, ignoring case,
     * or null if there is none. Keyed by name, not phone number, because numbers can be masked.
     */
    suspend fun lastCategoryFor(counterparty: String): Category?
}

/** Suggests whatever the user last chose for this counterparty, e.g. CARREFOUR -> Groceries. */
class MerchantMemorySuggester(private val history: MerchantHistory) : CategorySuggester {

    override suspend fun suggest(transaction: MpesaTransaction, limit: Int): List<Category> {
        if (limit <= 0) return emptyList()
        val counterparty = transaction.counterparty ?: return emptyList()
        return listOfNotNull(history.lastCategoryFor(counterparty))
    }
}
