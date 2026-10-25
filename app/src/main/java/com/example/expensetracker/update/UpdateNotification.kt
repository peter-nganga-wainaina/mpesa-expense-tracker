package com.example.expensetracker.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.expensetracker.MainActivity
import com.example.expensetracker.R
import com.example.expensetracker.notify.canPostNotifications

/**
 * Tells the user a new version is out. Tapping it only opens the app, where the banner offers the
 * update: downloading starts from a deliberate tap, never from the background.
 */
class UpdateNotification(private val context: Context) {

    fun show(release: ReleaseInfo) {
        if (!context.canPostNotifications()) return
        ensureChannel()
        val openApp = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Update available")
            .setContentText("Version ${release.versionName} is ready to install. Open the app to update.")
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    // Default importance: worth a sound-free mention, unlike the categorize prompts that need a reply.
    private fun ensureChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "App updates", NotificationManager.IMPORTANCE_DEFAULT)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "app_updates"
        // One update notification at a time; a newer version replaces the older one.
        const val NOTIFICATION_ID = 1
    }
}
