package com.example.expensetracker

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import com.example.expensetracker.categorize.Categorizer
import com.example.expensetracker.data.AppDatabase
import com.example.expensetracker.data.RoomCategoryStore
import com.example.expensetracker.data.RoomTransactionStore
import com.example.expensetracker.data.SetupState
import com.example.expensetracker.data.ThemePreference
import com.example.expensetracker.notify.CategoryPrompt
import com.example.expensetracker.sms.InboxBackfill
import com.example.expensetracker.sms.SmsInbox
import com.example.expensetracker.sms.TransactionIngest
import com.example.expensetracker.suggest.ChainedSuggester
import com.example.expensetracker.suggest.MerchantMemorySuggester
import com.example.expensetracker.suggest.MostUsedSuggester
import com.example.expensetracker.update.GitHubReleases
import com.example.expensetracker.update.UpdateChecker
import com.example.expensetracker.update.UpdateInstaller
import com.example.expensetracker.update.UpdatePreferences
import java.time.LocalDateTime

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
    val backfill = InboxBackfill(SmsInbox(app), ingest, now = LocalDateTime::now)
    val setup = SetupState(app)
    val themePreference = ThemePreference(app)

    private val releases = GitHubReleases()
    val updateChecker = UpdateChecker(releases, installedVersionCode = installedVersionCode(app))
    val updateInstaller = UpdateInstaller(releases)
    val updatePreferences = UpdatePreferences(app)
}

private fun installedVersionCode(context: Context): Long =
    PackageInfoCompat.getLongVersionCode(context.packageManager.getPackageInfo(context.packageName, 0))
