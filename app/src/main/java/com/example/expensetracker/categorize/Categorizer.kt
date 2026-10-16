package com.example.expensetracker.categorize

import com.example.expensetracker.sms.TransactionNotifier
import com.example.expensetracker.sms.TransactionStore

/** Files a transaction under a category, from a notification button or the in-app picker alike. */
class Categorizer(
    private val store: TransactionStore,
    private val notifier: TransactionNotifier,
) {
    suspend fun categorize(transactionCode: String, categoryId: Long) {
        store.setCategory(transactionCode, categoryId)
        // Once filed, its prompt is stale wherever the choice was made.
        notifier.dismiss(transactionCode)
    }
}
