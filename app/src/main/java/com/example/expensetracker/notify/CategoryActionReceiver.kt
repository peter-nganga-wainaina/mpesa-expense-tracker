package com.example.expensetracker.notify

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.expensetracker.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Handles a quick-pick button tap on a category prompt: files the transaction without opening the app. */
class CategoryActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Only this app's own notifications can send here (not exported), so missing extras are a bug.
        val code = checkNotNull(intent.getStringExtra(EXTRA_CODE)) { "Category action without a transaction code" }
        val categoryId = intent.getLongExtra(EXTRA_CATEGORY_ID, -1).also {
            check(it >= 0) { "Category action without a category" }
        }

        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                AppContainer(context).categorizer.categorize(code, categoryId)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val EXTRA_CODE = "transactionCode"
        private const val EXTRA_CATEGORY_ID = "categoryId"

        fun intent(context: Context, transactionCode: String, categoryId: Long): PendingIntent {
            val intent = Intent(context, CategoryActionReceiver::class.java)
                .putExtra(EXTRA_CODE, transactionCode)
                .putExtra(EXTRA_CATEGORY_ID, categoryId)
            // Each button needs its own request code, or Android reuses one PendingIntent for all of them.
            return PendingIntent.getBroadcast(
                context,
                "$transactionCode/$categoryId".hashCode(),
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
    }
}
