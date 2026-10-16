package com.example.expensetracker.data

import android.content.Context

/** Whether first-run setup has been completed. One flag, so plain SharedPreferences is enough. */
class SetupState(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("setup", Context.MODE_PRIVATE)

    val isComplete: Boolean get() = prefs.getBoolean(KEY_COMPLETE, false)

    fun markComplete() = prefs.edit().putBoolean(KEY_COMPLETE, true).apply()

    private companion object {
        const val KEY_COMPLETE = "complete"
    }
}
