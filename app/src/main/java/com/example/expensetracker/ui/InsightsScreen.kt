package com.example.expensetracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import com.example.expensetracker.insights.Direction
import com.example.expensetracker.insights.MonthSummary
import com.example.expensetracker.insights.canGoBack
import com.example.expensetracker.insights.canGoForward
import com.example.expensetracker.insights.earliestMonth
import com.example.expensetracker.insights.netCents
import com.example.expensetracker.insights.percentLabel
import com.example.expensetracker.insights.summarize
import com.example.expensetracker.notify.formatKsh
import com.example.expensetracker.notify.formatNetKsh
import com.example.expensetracker.ui.theme.categoryColor
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * A month's categorized totals, money out or money in: a donut, then each category largest first, and
 * the month's net. Tapping a category shows the transactions behind its total. [onOpenQueue] goes to
 * the queue.
 */
@Composable
fun InsightsScreen(container: AppContainer, onOpenQueue: () -> Unit, modifier: Modifier = Modifier) {
    val history by remember { container.transactions.observeHistory() }.collectAsState(initial = null)
    val categories by remember { container.categories.observeAll() }.collectAsState(initial = emptyList())
    val current = YearMonth.now()
    // YearMonth isn't saveable as-is; its ISO text ("2026-10") is.
    var monthText by rememberSaveable { mutableStateOf(current.toString()) }
    var openCategoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var directionName by rememberSaveable { mutableStateOf(Direction.OUT.name) }
    var showingFees by rememberSaveable { mutableStateOf(false) }
    val month = YearMonth.parse(monthText)
    val direction = Direction.valueOf(directionName)
    val transactions = history ?: return
    val summary = remember(transactions, month, direction) { summarize(month, direction, transactions) }
    val net = remember(transactions, month) { netCents(month, transactions) }
    val earliest = remember(transactions) { earliestMonth(transactions) }

    // Looked up by id so the detail stays open even after its last transaction is re-filed elsewhere.
    val openCategory = openCategoryId?.let { id -> categories.firstOrNull { it.id == id } }
    if (showingFees) {
        FeesDetailScreen(month, transactions, onBack = { showingFees = false }, modifier = modifier)
        return
    }
    if (openCategory != null) {
        CategoryDetailScreen(container, month, direction, openCategory, transactions, onBack = { openCategoryId = null }, modifier = modifier)
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
        DirectionSwitch(direction, onChange = { directionName = it.name })
        TotalCard(summary, direction)
        NetRow(net)
        if (summary.categories.isNotEmpty() || summary.feesCents > 0) CategoryList(summary, onOpen = { openCategoryId = it.id }, onOpenFees = { showingFees = true })
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

/** Transfers (withdrawals) have no view of their own: they count in neither. */
@Composable
private fun DirectionSwitch(direction: Direction, onChange: (Direction) -> Unit) {
    val options = listOf(Direction.OUT, Direction.IN)
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = direction == option,
                onClick = { onChange(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) { Text(option.label) }
        }
    }
}

/** The maroon card: donut with the month's total in the middle. */
@Composable
private fun TotalCard(summary: MonthSummary, direction: Direction) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.primaryContainer, RoundedCornerShape(28.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val parts = summary.categories.map { "${it.category.name} ${percentLabel(it.percent, it.totalCents)}" } +
            if (summary.feesCents > 0) listOf("$FEES_LABEL ${percentLabel(summary.feesPercent, summary.feesCents)}") else emptyList()
        val description = if (parts.isEmpty()) "No categorized ${direction.label.lowercase()} this month" else parts.joinToString(prefix = "Breakdown: ")
        Donut(
            slices = summary.categories.map { DonutSlice(it.totalCents, categoryColor(it.category.id)) } +
                if (summary.feesCents > 0) listOf(DonutSlice(summary.feesCents, feesColor())) else emptyList(),
            trackColor = colors.onPrimaryContainer.copy(alpha = 0.15f),
            description = description,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(direction.label, style = MaterialTheme.typography.bodyMedium, color = colors.onPrimaryContainer)
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
                "No ${direction.label.lowercase()} categorized this month yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onPrimaryContainer,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

/** Categories largest first, then the fees line, which isn't a category; each opens what makes it up. */
@Composable
private fun CategoryList(summary: MonthSummary, onOpen: (Category) -> Unit, onOpenFees: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        summary.categories.forEachIndexed { index, total ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            TotalRow(
                dot = { CategoryDot(total.category) },
                name = total.category.name,
                percent = total.percent,
                cents = total.totalCents,
                modifier = Modifier.clickable(onClickLabel = "Show ${total.category.name} transactions") { onOpen(total.category) },
            )
        }
        if (summary.feesCents > 0) {
            if (summary.categories.isNotEmpty()) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            TotalRow(
                dot = { Box(Modifier.size(10.dp).background(feesColor(), CircleShape)) },
                name = FEES_LABEL,
                percent = summary.feesPercent,
                cents = summary.feesCents,
                modifier = Modifier.clickable(onClickLabel = "Show the fees", onClick = onOpenFees),
            )
        }
    }
}

@Composable
private fun TotalRow(dot: @Composable () -> Unit, name: String, percent: Int, cents: Long, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        dot()
        // The name gives way (ellipsis) so the amount never breaks across lines.
        Text(
            name,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            percentLabel(percent, cents),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            formatKsh(cents),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
        )
    }
}

/** A muted brown, so the fees line reads as different from the vibrant category colours. */
@Composable
private fun feesColor() = MaterialTheme.colorScheme.outline

private const val FEES_LABEL = "M-Pesa fees"

/** Money in minus money out, the same whichever view is showing. */
@Composable
private fun NetRow(netCents: Long) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Net this month", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(formatNetKsh(netCents), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
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

/** Shown on the Insights switch and the category screen. */
internal val Direction.label: String
    get() = when (this) {
        Direction.OUT -> "Money out"
        Direction.IN -> "Money in"
        Direction.TRANSFER -> error("Transfers have no view of their own")
    }

private val MONTH_LABEL = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
