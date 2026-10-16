package com.example.expensetracker

import android.content.Context
import com.example.expensetracker.categorize.Categorizer
import com.example.expensetracker.data.AppDatabase
import com.example.expensetracker.data.RoomCategoryStore
import com.example.expensetracker.data.RoomTransactionStore
import com.example.expensetracker.notify.CategoryPrompt
import com.example.expensetracker.sms.TransactionIngest
import com.example.expensetracker.suggest.ChainedSuggester
import com.example.expensetracker.suggest.MerchantMemorySuggester
import com.example.expensetracker.suggest.MostUsedSuggester

/** Wires the app's pieces together in one place, for the receivers and the UI alike. Cheap to create. */
class AppContainer(context: Context) {
    private val app = context.applicationContext
    private val db = AppDatabase.get(app)
    private val notifier = CategoryPrompt(app)

    val transactions = RoomTransactionStore(db)
    val categories = RoomCategoryStore(db)
    // Past choice for this merchant first, then the user's most-used categories.
    private val suggester = ChainedSuggester(MerchantMemorySuggester(categories), MostUsedSuggester(categories))

    val ingest = TransactionIngest(transactions, suggester, notifier)
    val categorizer = Categorizer(transactions, notifier)
}
