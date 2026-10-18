package com.example.expensetracker.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.expensetracker.AppContainer

/** Every transaction, newest first and grouped by day. Tapping one opens the picker to (re)file it. */
@Composable
fun HistoryScreen(container: AppContainer, modifier: Modifier = Modifier) {
    val history by remember { container.transactions.observeHistory() }.collectAsState(initial = null)

    Column(modifier.padding(horizontal = 16.dp)) {
        Text("History", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(vertical = 16.dp))
        val items = history ?: return@Column
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nothing yet. M-Pesa transactions will appear here.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            TransactionDayList(items, container)
        }
    }
}
