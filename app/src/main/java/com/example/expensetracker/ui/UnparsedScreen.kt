package com.example.expensetracker.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.expensetracker.AppContainer
import com.example.expensetracker.R
import com.example.expensetracker.categorize.Category
import com.example.expensetracker.manual.ManualEntry
import com.example.expensetracker.data.StoredTransaction
import com.example.expensetracker.notify.formatKsh
import com.example.expensetracker.unparsed.MessageStatus
import com.example.expensetracker.unparsed.UnparsedMessage
import com.example.expensetracker.unparsed.statusOf
import java.time.LocalDate
import kotlinx.coroutines.launch

/**
 * M-Pesa receipts the parser couldn't read. Each can be typed in as spending (the user enters the amount)
 * and stays listed until the user dismisses it. Once typed in, it shows the entry it became and offers to
 * edit that entry, never to add a second one.
 */
@Composable
fun UnparsedScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onManualSaved: (ManualEntry, Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    val waiting by remember { container.transactions.observeUnparsed() }.collectAsState(initial = null)
    val history by remember { container.transactions.observeHistory() }.collectAsState(initial = emptyList())
    val byCode = remember(history) { history.associateBy { it.transaction.code } }
    var addingFromCode by rememberSaveable { mutableStateOf<String?>(null) }
    var editingCode by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(modifier.padding(horizontal = 16.dp)) {
        Row(Modifier.padding(top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back")
            }
            Text("Unreadable messages", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 4.dp))
        }
        Text(
            "The app couldn't read these M-Pesa messages. Add one as spending to count it, then dismiss it.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        val messages = waiting ?: return@Column
        if (messages.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("All dealt with ✓", style = MaterialTheme.typography.titleLarge)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 96.dp)) {
                items(messages, key = { it.code }) { message ->
                    UnparsedCard(
                        message,
                        status = statusOf(message, byCode),
                        onAddAsSpending = { addingFromCode = message.code },
                        onEditEntry = { editingCode = it.transaction.code },
                        onDismiss = { scope.launch { container.transactions.dismissUnparsed(message.code) } },
                    )
                }
            }
        }
    }

    val addingFrom = waiting?.firstOrNull { it.code == addingFromCode }
    if (addingFrom != null) {
        AddSpendingSheet(
            container,
            editing = null,
            fromMessage = addingFrom,
            onSaved = { entry, category -> addingFromCode = null; onManualSaved(entry, category) },
            onDismiss = { addingFromCode = null },
        )
    }

    val editingEntry = editingCode?.let { byCode[it] }
    if (editingEntry != null) {
        AddSpendingSheet(container, editing = editingEntry, onSaved = { _, _ -> editingCode = null }, onDismiss = { editingCode = null })
    }
}

@Composable
private fun UnparsedCard(
    message: UnparsedMessage,
    status: MessageStatus,
    onAddAsSpending: () -> Unit,
    onEditEntry: (StoredTransaction) -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "${dayLabel(message.receivedAt.toLocalDate(), LocalDate.now())}, ${timeLabel(message.receivedAt)}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // Selectable, so the code or wording can be copied.
        SelectionContainer { Text(message.body, style = MaterialTheme.typography.bodyMedium) }
        if (status is MessageStatus.Added) {
            val entry = status.entry
            Text(
                "Added as ${formatKsh(entry.transaction.amountCents)}" + (entry.category?.let { " · ${it.name}" } ?: ""),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            when (status) {
                MessageStatus.NotAdded -> OutlinedButton(onClick = onAddAsSpending) { Text("Add as spending") }
                is MessageStatus.Added -> OutlinedButton(onClick = { onEditEntry(status.entry) }) { Text("Edit spending") }
            }
            TextButton(onClick = onDismiss) { Text("Dismiss") }
        }
    }
}
