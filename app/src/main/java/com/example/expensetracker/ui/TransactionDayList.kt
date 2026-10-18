package com.example.expensetracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.expensetracker.data.StoredTransaction
import com.example.expensetracker.notify.describe
import java.time.LocalDate

/**
 * Transactions grouped under day headings, newest first. Tapping one opens the category picker to
 * (re)file it or read its original message. Shared by History and the category drill-down, which
 * hides each row's category tag ([showCategory] false) since every row there has the same one.
 */
@Composable
fun TransactionDayList(
    entries: List<StoredTransaction>,
    container: AppContainer,
    modifier: Modifier = Modifier,
    showCategory: Boolean = true,
) {
    val categories by remember { container.categories.observeAll() }.collectAsState(initial = emptyList())
    var pickingCode by rememberSaveable { mutableStateOf<String?>(null) }
    var editingCode by rememberSaveable { mutableStateOf<String?>(null) }
    val filing = rememberFiling(container)
    val today = LocalDate.now()

    // Stored transactions always have a time: the arrival time stands in when the SMS has none.
    val days = groupByDay(entries) { checkNotNull(it.transaction.occurredAt) }
    // Bottom padding keeps the last rows clear of the floating "Add spending" button.
    LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 96.dp)) {
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
                        // Manual entries open their form (edit or delete); M-Pesa ones can only be re-filed.
                        HistoryRow(entry, showCategory, onClick = {
                            if (entry.isManual) editingCode = entry.transaction.code else pickingCode = entry.transaction.code
                        })
                    }
                }
            }
        }
    }

    val editingEntry = entries.firstOrNull { it.transaction.code == editingCode }
    if (editingEntry != null) {
        AddSpendingSheet(container, editing = editingEntry, onSaved = { _, _ -> editingCode = null }, onDismiss = { editingCode = null })
    }

    // Looked up in the live list, so the sheet closes if the transaction leaves it (e.g. re-filed away).
    val picking = entries.firstOrNull { it.transaction.code == pickingCode }
    if (picking != null) {
        CategoryPicker(
            transaction = picking.transaction,
            originalMessage = picking.originalMessage,
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
private fun HistoryRow(entry: StoredTransaction, showCategory: Boolean, onClick: () -> Unit) {
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
                val time = timeLabel(it)
                Text(if (entry.isManual) "$time · ${entry.source.label}" else time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (showCategory) CategoryTag(entry)
    }
}

/** The category with its colour, or a maroon outline asking for one. */
@Composable
private fun CategoryTag(entry: StoredTransaction) {
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
