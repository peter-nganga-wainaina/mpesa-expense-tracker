package com.example.expensetracker.data

import android.content.Context
import com.example.expensetracker.ui.theme.ThemeMode

/** Remembers the light/dark choice across launches. */
class ThemePreference(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var mode: ThemeMode
        // An unknown stored value (e.g. from a future version) falls back to following the phone.
        get() = prefs.getString(KEY_THEME, null)?.let { stored -> ThemeMode.entries.find { it.name == stored } }
            ?: ThemeMode.SYSTEM
        set(value) = prefs.edit().putString(KEY_THEME, value.name).apply()

    private companion object {
        const val KEY_THEME = "theme_mode"
    }
}
