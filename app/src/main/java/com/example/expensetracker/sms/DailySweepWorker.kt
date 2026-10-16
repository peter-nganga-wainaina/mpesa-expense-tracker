package com.example.expensetracker.sms

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.expensetracker.AppContainer
import java.util.concurrent.TimeUnit

/**
 * Runs the inbox sweep once a day, so missed M-Pesa messages reach the queue even if the app is
 * never opened. WorkManager survives reboots and picks a battery-friendly moment within the day.
 */
class DailySweepWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        AppContainer(applicationContext).backfill.sweep()
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "daily-inbox-sweep"

        /** Safe to call on every app open: an existing schedule is kept, not restarted. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<DailySweepWorker>(1, TimeUnit.DAYS).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
