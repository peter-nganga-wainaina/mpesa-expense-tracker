package com.example.expensetracker.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.expensetracker.AppContainer
import com.example.expensetracker.notify.describe
import com.example.expensetracker.notify.formatOccurredAt
import com.example.expensetracker.sms.MpesaTransaction

/**
 * Every transaction still waiting for a category, with a picker for each.
 * [requestedCode] opens the picker for one transaction, when arriving from its notification.
 */
@Composable
fun QueueScreen(
    container: AppContainer,
    requestedCode: String?,
    onRequestHandled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Null until the database first answers, so an empty queue isn't mistaken for "still loading".
    val queue by remember { container.transactions.observeUncategorized() }.collectAsState(initial = null)
    val categories by remember { container.categories.observeAll() }.collectAsState(initial = emptyList())
    var pickingCode by rememberSaveable { mutableStateOf<String?>(null) }
    val filing = rememberFiling(container)

    LaunchedEffect(requestedCode, queue) {
        val loaded = queue ?: return@LaunchedEffect
        if (requestedCode == null) return@LaunchedEffect
        // Already filed (e.g. from a notification button): nothing to pick.
        if (loaded.any { it.transaction.code == requestedCode }) pickingCode = requestedCode
        onRequestHandled()
    }

    Column(modifier.padding(horizontal = 16.dp)) {
        PermissionBanner()
        val waiting = queue.orEmpty()
        Text(
            "To categorize (${waiting.size})",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        when {
            queue == null -> Unit
            waiting.isEmpty() -> AllCaughtUp()
            else -> LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                items(waiting, key = { it.transaction.code }) { entry ->
                    QueueRow(entry.transaction, onClick = { pickingCode = entry.transaction.code })
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }

    // The sheet closes by itself once its transaction leaves the queue, wherever it was filed.
    val picking = queue?.firstOrNull { it.transaction.code == pickingCode }
    if (picking != null) {
        val code = picking.transaction.code
        CategoryPicker(
            transaction = picking.transaction,
            originalMessage = picking.originalMessage,
            categories = categories,
            currentCategoryId = null,
            onPick = { filing.fileUnder(code, it); pickingCode = null },
            onAddCategory = { filing.fileUnderNew(code, it); pickingCode = null },
            onDismiss = { pickingCode = null },
        )
    }
}

@Composable
private fun QueueRow(transaction: MpesaTransaction, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        Text(describe(transaction), style = MaterialTheme.typography.bodyLarge)
        transaction.occurredAt?.let {
            Text(
                formatOccurredAt(it),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AllCaughtUp() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("All caught up ✓", style = MaterialTheme.typography.titleLarge)
    }
}

/** Shown while a permission is missing (e.g. denied at setup, or revoked later in Settings). */
@Composable
private fun PermissionBanner() {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(context.hasRequiredPermissions()) }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        granted = context.hasRequiredPermissions()
    }
    if (granted) return

    Card(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Allow SMS and notifications so M-Pesa transactions show up here and prompt you to categorize them.")
            Button(onClick = { request.launch(REQUIRED_PERMISSIONS) }) { Text("Allow") }
        }
    }
}
