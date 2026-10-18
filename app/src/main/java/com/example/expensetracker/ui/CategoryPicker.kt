package com.example.expensetracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.expensetracker.categorize.Category
import com.example.expensetracker.categorize.NewCategoryName
import com.example.expensetracker.categorize.validateNewCategoryName
import com.example.expensetracker.notify.describe
import com.example.expensetracker.notify.formatOccurredAt
import com.example.expensetracker.sms.MpesaTransaction
import com.example.expensetracker.ui.theme.categoryColor

/**
 * Bottom sheet for filing one transaction: every category, plus "New category". Shared by the
 * queue (first-time filing) and History (re-filing), where [currentCategoryId] is highlighted.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CategoryPicker(
    transaction: MpesaTransaction,
    categories: List<Category>,
    currentCategoryId: Long?,
    onPick: (Category) -> Unit,
    onAddCategory: (name: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var addingCategory by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
            Text(describe(transaction), style = MaterialTheme.typography.titleLarge)
            transaction.occurredAt?.let { Text(formatOccurredAt(it), style = MaterialTheme.typography.bodySmall) }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 16.dp),
            ) {
                categories.forEach { category ->
                    CategoryButton(category, isCurrent = category.id == currentCategoryId, onClick = { onPick(category) })
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
private fun CategoryButton(category: Category, isCurrent: Boolean, onClick: () -> Unit) {
    val colors = if (isCurrent) {
        ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    } else {
        ButtonDefaults.filledTonalButtonColors()
    }
    FilledTonalButton(onClick = onClick, colors = colors, modifier = Modifier.semantics { selected = isCurrent }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CategoryDot(category)
            Text(category.name)
        }
    }
}

/** The category's colour as a small dot, so it reads the same in lists, pickers and charts. */
@Composable
fun CategoryDot(category: Category, modifier: Modifier = Modifier) {
    Box(modifier.size(10.dp).background(categoryColor(category.id), CircleShape))
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
