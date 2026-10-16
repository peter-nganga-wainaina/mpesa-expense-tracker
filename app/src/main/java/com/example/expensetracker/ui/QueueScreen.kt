package com.example.expensetracker.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.expensetracker.AppContainer
import com.example.expensetracker.categorize.Category
import com.example.expensetracker.categorize.NewCategoryName
import com.example.expensetracker.categorize.validateNewCategoryName
import com.example.expensetracker.notify.describe
import com.example.expensetracker.notify.formatOccurredAt
import com.example.expensetracker.sms.MpesaTransaction
import kotlinx.coroutines.launch

/**
 * The home screen: every transaction still waiting for a category, with a picker for each.
 * [requestedCode] opens the picker for one transaction, when arriving from its notification.
 */
@Composable
fun QueueScreen(container: AppContainer, requestedCode: String?, onRequestHandled: () -> Unit) {
    // Null until the database first answers, so an empty queue isn't mistaken for "still loading".
    val queue by remember { container.transactions.observeUncategorized() }.collectAsState(initial = null)
    val categories by remember { container.categories.observeAll() }.collectAsState(initial = emptyList())
    var pickingCode by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(requestedCode, queue) {
        val loaded = queue ?: return@LaunchedEffect
        if (requestedCode == null) return@LaunchedEffect
        // Already filed (e.g. from a notification button): nothing to pick.
        if (loaded.any { it.code == requestedCode }) pickingCode = requestedCode
        onRequestHandled()
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 16.dp)) {
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
                else -> LazyColumn {
                    items(waiting, key = { it.code }) { transaction ->
                        QueueRow(transaction, onClick = { pickingCode = transaction.code })
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    // The sheet closes by itself once its transaction leaves the queue, wherever it was filed.
    val picking = queue?.firstOrNull { it.code == pickingCode }
    if (picking != null) {
        CategoryPicker(
            transaction = picking,
            categories = categories,
            onPick = { category ->
                scope.launch { container.categorizer.categorize(picking.code, category.id) }
                pickingCode = null
            },
            onAddCategory = { name ->
                scope.launch {
                    val category = container.categories.add(name) ?: return@launch
                    container.categorizer.categorize(picking.code, category.id)
                }
                pickingCode = null
            },
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
            Text(formatOccurredAt(it), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun AllCaughtUp() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("All caught up ✓", style = MaterialTheme.typography.titleMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CategoryPicker(
    transaction: MpesaTransaction,
    categories: List<Category>,
    onPick: (Category) -> Unit,
    onAddCategory: (name: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var addingCategory by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
            Text(describe(transaction), style = MaterialTheme.typography.titleMedium)
            transaction.occurredAt?.let { Text(formatOccurredAt(it), style = MaterialTheme.typography.bodySmall) }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 16.dp),
            ) {
                categories.forEach { category ->
                    FilledTonalButton(onClick = { onPick(category) }) { Text(category.name) }
                }
                OutlinedButton(onClick = { addingCategory = true }) { Text("+ New category") }
            }
        }
    }

    if (addingCategory) {
        NewCategoryDialog(
            existing = categories,
            onAdd = { addingCategory = false; onAddCategory(it) },
            onDismiss = { addingCategory = false },
        )
    }
}

@Composable
private fun NewCategoryDialog(existing: List<Category>, onAdd: (name: String) -> Unit, onDismiss: () -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }
    val validation = validateNewCategoryName(input, existing)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New category") },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                singleLine = true,
                isError = validation is NewCategoryName.Duplicate,
                supportingText = {
                    if (validation is NewCategoryName.Duplicate) Text("\"${validation.existing.name}\" already exists")
                },
            )
        },
        confirmButton = {
            TextButton(
                enabled = validation is NewCategoryName.Valid,
                onClick = { (validation as? NewCategoryName.Valid)?.let { onAdd(it.name) } },
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Shown until the app may read SMS and post notifications; without them nothing reaches the queue. */
@Composable
private fun PermissionBanner() {
    val context = LocalContext.current
    fun allGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
    var granted by remember { mutableStateOf(allGranted()) }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        granted = allGranted()
    }
    if (granted) return

    Card(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Allow SMS and notifications so M-Pesa transactions show up here and prompt you to categorize them.")
            Button(onClick = { request.launch(REQUIRED_PERMISSIONS) }) { Text("Allow") }
        }
    }
}

private val REQUIRED_PERMISSIONS = buildList {
    add(Manifest.permission.RECEIVE_SMS)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
}.toTypedArray()
