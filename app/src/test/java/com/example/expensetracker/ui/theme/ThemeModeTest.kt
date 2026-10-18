package com.example.expensetracker.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeModeTest {

    @Test
    fun `following the system shows whatever the phone shows`() {
        assertTrue(ThemeMode.SYSTEM.isDark(systemIsDark = true))
        assertFalse(ThemeMode.SYSTEM.isDark(systemIsDark = false))
    }

    @Test
    fun `an explicit choice ignores the phone's setting`() {
        assertTrue(ThemeMode.DARK.isDark(systemIsDark = false))
        assertFalse(ThemeMode.LIGHT.isDark(systemIsDark = true))
    }

    @Test
    fun `toggling always flips what is showing`() {
        for (systemIsDark in listOf(true, false)) {
            for (mode in ThemeMode.entries) {
                val showingDark = mode.isDark(systemIsDark)
                assertEquals(!showingDark, ThemeMode.toggledFrom(showingDark).isDark(systemIsDark))
            }
        }
    }

    @Test
    fun `a category keeps the same colour every time`() {
        assertEquals(categoryColorIndex(3, paletteSize = 8), categoryColorIndex(3, paletteSize = 8))
    }

    @Test
    fun `categories created one after another get different colours`() {
        for (id in 1L..20L) {
            assertNotEquals(categoryColorIndex(id, 8), categoryColorIndex(id + 1, 8))
        }
    }

    @Test
    fun `colours wrap around once there are more categories than colours`() {
        val indexes = (1L..20L).map { categoryColorIndex(it, 8) }

        assertTrue(indexes.all { it in 0 until 8 })
        assertEquals(8, indexes.toSet().size)
    }
}
