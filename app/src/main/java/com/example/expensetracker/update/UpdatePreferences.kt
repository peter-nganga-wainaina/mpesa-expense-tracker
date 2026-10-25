package com.example.expensetracker.update

import android.content.Context

/** Which release the daily check last notified about, so each version is announced once. */
class UpdatePreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("updates", Context.MODE_PRIVATE)

    var lastAnnouncedVersionCode: Long?
        get() = if (prefs.contains(KEY_LAST_ANNOUNCED)) prefs.getLong(KEY_LAST_ANNOUNCED, 0) else null
        set(value) = prefs.edit().apply { if (value == null) remove(KEY_LAST_ANNOUNCED) else putLong(KEY_LAST_ANNOUNCED, value) }.apply()

    private companion object {
        const val KEY_LAST_ANNOUNCED = "last_announced_version_code"
    }
}
