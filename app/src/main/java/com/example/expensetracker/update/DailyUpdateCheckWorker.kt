package com.example.expensetracker.update

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.expensetracker.AppContainer
import java.util.concurrent.TimeUnit

/**
 * Checks for a new release once a day and notifies once per version, so an update is noticed even when
 * the app is mostly used through its notifications and rarely opened.
 */
class DailyUpdateCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = AppContainer(applicationContext)
        when (val check = container.updateChecker.check()) {
            is UpdateCheck.Available -> {
                val preferences = container.updatePreferences
                if (shouldAnnounce(check.release, preferences.lastAnnouncedVersionCode)) {
                    UpdateNotification(applicationContext).show(check.release)
                    preferences.lastAnnouncedVersionCode = check.release.versionCode
                }
            }
            // Not retried: tomorrow's run is soon enough, and before the repo is public this fails daily.
            is UpdateCheck.Failed -> Log.w(TAG, "Daily update check failed", check.cause)
            UpdateCheck.UpToDate -> Unit
        }
        return Result.success()
    }

    companion object {
        private const val TAG = "DailyUpdateCheck"
        private const val WORK_NAME = "daily-update-check"

        /** Safe to call on every app open: an existing schedule is kept, not restarted. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<DailyUpdateCheckWorker>(1, TimeUnit.DAYS)
                // Waits for a connection instead of running offline and failing.
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
