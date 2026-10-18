package com.example.expensetracker.ui.theme

/** The user's light/dark choice. Until they choose, the app follows the phone's setting. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    fun isDark(systemIsDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemIsDark
        LIGHT -> false
        DARK -> true
    }

    companion object {
        /** The explicit choice that flips what is showing now; after one toggle the system setting no longer applies. */
        fun toggledFrom(showingDark: Boolean): ThemeMode = if (showingDark) LIGHT else DARK
    }
}

/** Index into a category palette: stable per category, and categories created one after another get different colours. */
fun categoryColorIndex(categoryId: Long, paletteSize: Int): Int {
    require(paletteSize > 0) { "Palette must not be empty" }
    return Math.floorMod(categoryId - 1, paletteSize.toLong()).toInt()
}
