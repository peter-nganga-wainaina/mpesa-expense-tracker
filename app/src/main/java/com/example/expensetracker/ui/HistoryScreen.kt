package com.example.expensetracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.expensetracker.AppContainer
import com.example.expensetracker.data.HistoryItem
import com.example.expensetracker.notify.describe
import java.time.LocalDate

/** Every transaction, newest first and grouped by day. Tapping one opens the picker to (re)file it. */
@Composable
fun HistoryScreen(container: AppContainer, modifier: Modifier = Modifier) {
    val history by remember { container.transactions.observeHistory() }.collectAsState(initial = null)
    val categories by remember { container.categories.observeAll() }.collectAsState(initial = emptyList())
    var pickingCode by rememberSaveable { mutableStateOf<String?>(null) }
    val filing = rememberFiling(container)
    val today = LocalDate.now()

    Column(modifier.padding(horizontal = 16.dp)) {
        Text("History", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(vertical = 16.dp))
        val items = history ?: return@Column
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nothing yet. M-Pesa transactions will appear here.", style = MaterialTheme.typography.bodyLarge)
            }
            return@Column
        }
        // Stored transactions always have a time: the arrival time stands in when the SMS has none.
        val days = groupByDay(items) { checkNotNull(it.transaction.occurredAt) }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            days.forEach { day ->
                item(key = day.date.toString()) { DayHeading(dayLabel(day.date, today)) }
                item(key = "${day.date}-entries") {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        day.entries.forEach { entry ->
                            HistoryRow(entry, onClick = { pickingCode = entry.transaction.code })
                        }
                    }
                }
            }
        }
    }

    val picking = history?.firstOrNull { it.transaction.code == pickingCode }
    if (picking != null) {
        CategoryPicker(
            transaction = picking.transaction,
            categories = categories,
            currentCategoryId = picking.category?.id,
            onPick = { filing.fileUnder(picking.transaction.code, it); pickingCode = null },
            onAddCategory = { filing.fileUnderNew(picking.transaction.code, it); pickingCode = null },
            onDismiss = { pickingCode = null },
        )
    }
}

@Composable
private fun DayHeading(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp),
    )
}

@Composable
private fun HistoryRow(entry: HistoryItem, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(describe(entry.transaction), style = MaterialTheme.typography.bodyLarge)
            entry.transaction.occurredAt?.let {
                Text(timeLabel(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        CategoryTag(entry)
    }
}

/** The category with its colour, or a maroon outline asking for one. */
@Composable
private fun CategoryTag(entry: HistoryItem) {
    val shape = RoundedCornerShape(12.dp)
    val category = entry.category
    if (category == null) {
        Text(
            "Categorize",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.primary, shape).padding(horizontal = 10.dp, vertical = 6.dp),
        )
    } else {
        Row(
            Modifier.background(MaterialTheme.colorScheme.surfaceContainer, shape).padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CategoryDot(category)
            Text(category.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
        }
    }
}
