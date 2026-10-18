package com.example.expensetracker.ui

import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryDaysTest {

    private val today = LocalDate.of(2026, 10, 17)

    private fun at(day: Int, hour: Int) = LocalDateTime.of(2026, 10, day, hour, 0)

    @Test
    fun `transactions are grouped under their day, newest day first`() {
        val newestFirst = listOf(at(17, 9), at(16, 20), at(16, 8), at(13, 12))

        val days = groupByDay(newestFirst) { it }

        assertEquals(listOf(17, 16, 13), days.map { it.date.dayOfMonth })
        assertEquals(listOf(at(16, 20), at(16, 8)), days[1].entries)
    }

    @Test
    fun `an empty history has no days`() {
        assertTrue(groupByDay(emptyList<LocalDateTime>()) { it }.isEmpty())
    }

    @Test
    fun `recent days read as today and yesterday`() {
        assertEquals("Today", dayLabel(today, today))
        assertEquals("Yesterday", dayLabel(today.minusDays(1), today))
    }

    @Test
    fun `older days this year show weekday, day and month`() {
        assertEquals("Tue 13 Oct", dayLabel(LocalDate.of(2026, 10, 13), today))
    }

    @Test
    fun `days from another year include the year`() {
        assertEquals("Thu 4 Dec 2025", dayLabel(LocalDate.of(2025, 12, 4), today))
    }

    @Test
    fun `rows show a 12-hour time`() {
        assertEquals("10:40 PM", timeLabel(LocalDateTime.of(2026, 10, 13, 22, 40)))
    }
}
