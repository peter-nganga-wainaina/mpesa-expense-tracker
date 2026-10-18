package com.example.expensetracker.ui

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** One day's heading in History and the entries under it, in the order given. */
data class Day<T>(val date: LocalDate, val entries: List<T>)

/**
 * Splits a newest-first list into days, newest day first, keeping each day's entries in order.
 * Entries without a time can't be placed on a day, which never happens for stored transactions.
 */
fun <T> groupByDay(newestFirst: List<T>, timeOf: (T) -> LocalDateTime): List<Day<T>> =
    newestFirst.groupBy { timeOf(it).toLocalDate() }
        .map { (date, entries) -> Day(date, entries) }
        .sortedByDescending { it.date }

/** "Today", "Yesterday", then "Mon 13 Oct", adding the year once it isn't this year. */
fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> "Today"
    today.minusDays(1) -> "Yesterday"
    else -> (if (date.year == today.year) THIS_YEAR else OTHER_YEAR).format(date)
}

/** "10:40 PM", for a row under its day heading. */
fun timeLabel(time: LocalDateTime): String = TIME.format(time)

// English names to match the app's English UI, whatever the phone's locale.
private val THIS_YEAR = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)
private val OTHER_YEAR = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.ENGLISH)
private val TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
