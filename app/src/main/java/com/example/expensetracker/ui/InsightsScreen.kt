package com.example.expensetracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.AppContainer
import com.example.expensetracker.categorize.Category
import com.example.expensetracker.insights.CategoryTotal
import com.example.expensetracker.insights.MonthSummary
import com.example.expensetracker.insights.canGoBack
import com.example.expensetracker.insights.canGoForward
import com.example.expensetracker.insights.earliestMonth
import com.example.expensetracker.insights.summarize
import com.example.expensetracker.notify.formatKsh
import com.example.expensetracker.ui.theme.categoryColor
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * A month's categorized totals: a donut, then each category largest first. Tapping a category shows
 * the transactions behind its total. [onOpenQueue] goes to the queue.
 */
@Composable
fun InsightsScreen(container: AppContainer, onOpenQueue: () -> Unit, modifier: Modifier = Modifier) {
    val history by remember { container.transactions.observeHistory() }.collectAsState(initial = null)
    val categories by remember { container.categories.observeAll() }.collectAsState(initial = emptyList())
    val current = YearMonth.now()
    // YearMonth isn't saveable as-is; its ISO text ("2026-10") is.
    var monthText by rememberSaveable { mutableStateOf(current.toString()) }
    var openCategoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    val month = YearMonth.parse(monthText)
    val transactions = history ?: return
    val summary = remember(transactions, month) { summarize(month, transactions) }
    val earliest = remember(transactions) { earliestMonth(transactions) }

    // Looked up by id so the detail stays open even after its last transaction is re-filed elsewhere.
    val openCategory = openCategoryId?.let { id -> categories.firstOrNull { it.id == id } }
    if (openCategory != null) {
        CategoryDetailScreen(container, month, openCategory, transactions, onBack = { openCategoryId = null }, modifier = modifier)
        return
    }

    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 96.dp), // clear of the floating "Add spending" button
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Insights", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 16.dp))
        MonthSwitcher(
            month = month,
            canGoBack = canGoBack(month, earliest),
            canGoForward = canGoForward(month, current),
            onBack = { monthText = month.minusMonths(1).toString() },
            onForward = { monthText = month.plusMonths(1).toString() },
        )
        TotalCard(summary)
        if (summary.categories.isNotEmpty()) CategoryList(summary.categories, onOpen = { openCategoryId = it.id })
        if (summary.uncategorizedCount > 0) UncategorizedNote(summary.uncategorizedCount, onOpenQueue)
    }
}

@Composable
private fun MonthSwitcher(month: YearMonth, canGoBack: Boolean, canGoForward: Boolean, onBack: () -> Unit, onForward: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, enabled = canGoBack, modifier = Modifier.semantics { contentDescription = "Previous month" }) { Text("‹", style = MaterialTheme.typography.headlineSmall) }
        Text(
            MONTH_LABEL.format(month),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onForward, enabled = canGoForward, modifier = Modifier.semantics { contentDescription = "Next month" }) { Text("›", style = MaterialTheme.typography.headlineSmall) }
    }
}

/** The maroon card: donut with the month's total in the middle. */
@Composable
private fun TotalCard(summary: MonthSummary) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.primaryContainer, RoundedCornerShape(28.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val description = if (summary.categories.isEmpty()) {
            "No categorized transactions this month"
        } else {
            summary.categories.joinToString(prefix = "Breakdown: ") { "${it.category.name} ${it.percent}%" }
        }
        Donut(
            slices = summary.categories.map { DonutSlice(it.totalCents, categoryColor(it.category.id)) },
            trackColor = colors.onPrimaryContainer.copy(alpha = 0.15f),
            description = description,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Total", style = MaterialTheme.typography.bodyMedium, color = colors.onPrimaryContainer)
                // Shrinks to fit inside the ring, so a big month never spills over it.
                BasicText(
                    formatKsh(summary.totalCents),
                    style = MaterialTheme.typography.titleLarge.copy(color = colors.onPrimaryContainer, textAlign = TextAlign.Center),
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = MaterialTheme.typography.titleLarge.fontSize),
                    modifier = Modifier.width(116.dp),
                )
            }
        }
        if (summary.categories.isEmpty()) {
            Text(
                "Nothing categorized this month yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onPrimaryContainer,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

@Composable
private fun CategoryList(totals: List<CategoryTotal>, onOpen: (Category) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        totals.forEachIndexed { index, total ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClickLabel = "Show ${total.category.name} transactions") { onOpen(total.category) }
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CategoryDot(total.category)
                // The name gives way (ellipsis) so the amount never breaks across lines.
                Text(
                    total.category.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${total.percent}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    formatKsh(total.totalCents),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

/** Totals leave these out, so say how many and offer a way to file them. */
@Composable
private fun UncategorizedNote(count: Int, onOpenQueue: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenQueue)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (count == 1) "1 transaction isn't categorized yet" else "$count transactions aren't categorized yet",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text("Categorize ›", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}

private val MONTH_LABEL = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
