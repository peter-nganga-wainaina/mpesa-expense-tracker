package com.example.expensetracker.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.expensetracker.MainActivity
import com.example.expensetracker.R
import com.example.expensetracker.categorize.Category
import com.example.expensetracker.sms.MpesaTransaction
import com.example.expensetracker.sms.TransactionNotifier

/**
 * Posts a notification asking the user to categorize a transaction: one button per quick pick, which
 * files it without opening the app, plus "Other…" for the full picker.
 */
class CategoryPrompt(private val context: Context) : TransactionNotifier {

    override fun promptForCategory(transaction: MpesaTransaction, quickPicks: List<Category>) {
        // Without the permission, the transaction still waits in the queue.
        if (!context.canPostNotifications()) return
        ensureChannel()

        val openPicker = openPicker(transaction.code)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Categorize this transaction")
            .setContentText(describe(transaction))
            .setContentIntent(openPicker)
            .setAutoCancel(true)
            .apply {
                quickPicks.forEach { addAction(0, it.name, CategoryActionReceiver.intent(context, transaction.code, it.id)) }
            }
            .addAction(0, "Other…", openPicker)
            .build()
        NotificationManagerCompat.from(context).notify(notificationId(transaction.code), notification)
    }

    override fun dismiss(transactionCode: String) =
        NotificationManagerCompat.from(context).cancel(notificationId(transactionCode))

    /** Activities may only be opened by a user's tap, so the picker is reached through the notification. */
    private fun openPicker(transactionCode: String): PendingIntent = PendingIntent.getActivity(
        context,
        transactionCode.hashCode(),
        MainActivity.pickerIntent(context, transactionCode),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    // Keyed by transaction code, so the prompt can be found again to dismiss it.
    private fun notificationId(transactionCode: String) = transactionCode.hashCode()

    private fun ensureChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Transactions to categorize", NotificationManager.IMPORTANCE_HIGH)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "category_prompts"
    }
}
