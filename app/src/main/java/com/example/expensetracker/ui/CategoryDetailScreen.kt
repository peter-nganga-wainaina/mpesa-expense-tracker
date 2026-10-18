package com.example.expensetracker.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.expensetracker.AppContainer
import com.example.expensetracker.R
import com.example.expensetracker.categorize.Category
import com.example.expensetracker.data.StoredTransaction
import com.example.expensetracker.insights.transactionsIn
import com.example.expensetracker.notify.formatKsh
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * What makes up one category's total for a month, e.g. October › Savings. Updates live: re-filing a
 * transaction elsewhere takes it off this list.
 */
@Composable
fun CategoryDetailScreen(
    container: AppContainer,
    month: YearMonth,
    category: Category,
    allTransactions: List<StoredTransaction>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    val entries = remember(allTransactions, month, category.id) { transactionsIn(month, category.id, allTransactions) }
    val total = entries.sumOf { it.transaction.amountCents }

    Column(modifier.padding(horizontal = 16.dp)) {
        Row(Modifier.padding(top = 8.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back to insights")
            }
            Column(Modifier.padding(start = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CategoryDot(category)
                    Text(category.name, style = MaterialTheme.typography.headlineSmall)
                }
                Text(
                    "${MONTH.format(month)} · ${formatKsh(total)} · ${countLabel(entries.size)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nothing in ${category.name} for ${MONTH.format(month)}.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            TransactionDayList(entries, container, showCategory = false)
        }
    }
}

private fun countLabel(count: Int) = if (count == 1) "1 transaction" else "$count transactions"

private val MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
