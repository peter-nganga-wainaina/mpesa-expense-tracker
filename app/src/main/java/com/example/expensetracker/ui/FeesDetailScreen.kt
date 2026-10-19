package com.example.expensetracker.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.expensetracker.R
import com.example.expensetracker.data.StoredTransaction
import com.example.expensetracker.insights.feeCents
import com.example.expensetracker.insights.feesIn
import com.example.expensetracker.insights.fulizaAccessFeesCents
import com.example.expensetracker.notify.describe
import com.example.expensetracker.notify.formatKsh
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Where the month's "M-Pesa fees" number comes from: each fee with the transaction it was charged on.
 * Read-only: a fee is a fact from the SMS, with nothing to file.
 */
@Composable
fun FeesDetailScreen(month: YearMonth, allTransactions: List<StoredTransaction>, onBack: () -> Unit, modifier: Modifier = Modifier) {
    BackHandler(onBack = onBack)
    val entries = remember(allTransactions, month) { feesIn(month, allTransactions) }
    val fulizaFees = remember(allTransactions, month) { fulizaAccessFeesCents(month, allTransactions) }
    val today = LocalDate.now()
    // Stored transactions always have a time: the arrival time stands in when the SMS has none.
    val days = groupByDay(entries) { checkNotNull(it.transaction.occurredAt) }

    Column(modifier.padding(horizontal = 16.dp)) {
        Row(Modifier.padding(top = 8.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back to insights")
            }
            Column(Modifier.padding(start = 4.dp)) {
                Text("M-Pesa fees", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "${MONTH.format(month)} · ${formatKsh(entries.sumOf { it.feeCents })}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // Bottom padding keeps the last rows clear of the floating "Add spending" button.
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 96.dp)) {
            days.forEach { day ->
                item(key = day.date.toString()) {
                    Text(
                        dayLabel(day.date, today),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
                item(key = "${day.date}-fees") {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        day.entries.forEach { FeeRow(it) }
                    }
                }
            }
            if (fulizaFees > 0) {
                item(key = "fuliza-note") {
                    Text(
                        "Fuliza access fees (${formatKsh(fulizaFees)}) aren't listed here: they're part of what you repaid, " +
                            "which already counts in money out.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
        }
    }
}

/** The fee first, since it's what this screen is about; then the transaction it was charged on. */
@Composable
private fun FeeRow(entry: StoredTransaction) {
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(formatKsh(entry.feeCents), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        val time = entry.transaction.occurredAt?.let { " · ${timeLabel(it)}" } ?: ""
        Text(
            "on ${describe(entry.transaction)}$time",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
