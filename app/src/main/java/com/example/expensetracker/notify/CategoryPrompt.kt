package com.example.expensetracker.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.expensetracker.MainActivity
import com.example.expensetracker.R
import com.example.expensetracker.sms.MpesaTransaction
import com.example.expensetracker.sms.TransactionNotifier

/** Posts a notification asking the user to categorize a transaction. Category buttons come later. */
class CategoryPrompt(private val context: Context) : TransactionNotifier {

    override fun promptForCategory(transaction: MpesaTransaction) {
        // Without the permission (Android 13+) notify() is a no-op; the transaction will still reach
        // the uncategorized queue once storage exists.
        if (!canPostNotifications()) return
        ensureChannel()

        val openApp = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Categorize this transaction")
            .setContentText(describe(transaction))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        // Keyed by transaction code, so a repeated message replaces its notification instead of stacking.
        NotificationManagerCompat.from(context).notify(transaction.code.hashCode(), notification)
    }

    private fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun ensureChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Transactions to categorize", NotificationManager.IMPORTANCE_HIGH)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "category_prompts"
    }
}
