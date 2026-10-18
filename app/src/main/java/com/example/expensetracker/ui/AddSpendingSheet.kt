package com.example.expensetracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.expensetracker.AppContainer
import com.example.expensetracker.categorize.Category
import com.example.expensetracker.data.StoredTransaction
import com.example.expensetracker.manual.FormCheck
import com.example.expensetracker.manual.ManualEntry
import com.example.expensetracker.manual.ManualEntryForm
import com.example.expensetracker.manual.PaymentSource
import com.example.expensetracker.manual.check
import com.example.expensetracker.manual.editableAmount
import com.example.expensetracker.unparsed.UnparsedMessage
import com.example.expensetracker.unparsed.entryCodeFor
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The add form: amount, what for, how it was paid, when, and category. With [editing] set it edits
 * that manual entry instead, and offers Delete. With [fromMessage] set it types in an unreadable
 * M-Pesa message: the entry takes the message's code (so it can only ever exist once), was paid with
 * M-Pesa, and starts at the message's time. [onSaved] receives the saved entry and its category.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSpendingSheet(
    container: AppContainer,
    editing: StoredTransaction?,
    onSaved: (ManualEntry, Category) -> Unit,
    fromMessage: UnparsedMessage? = null,
    onDismiss: () -> Unit,
) {
    val categories by remember { container.categories.observeAll() }.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Plain strings and enums so the half-filled form survives rotation.
    var amountText by rememberSaveable { mutableStateOf(editing?.let { editableAmount(it.transaction.amountCents) } ?: "") }
    var note by rememberSaveable { mutableStateOf(editing?.transaction?.counterparty ?: "") }
    var source by rememberSaveable {
        mutableStateOf(editing?.source ?: if (fromMessage != null) PaymentSource.MPESA else PaymentSource.CASH)
    }
    var occurredAtText by rememberSaveable {
        mutableStateOf((editing?.transaction?.occurredAt ?: fromMessage?.receivedAt ?: LocalDateTime.now().withSecond(0).withNano(0)).toString())
    }
    var categoryId by rememberSaveable { mutableStateOf(editing?.category?.id) }
    var pickedCategoryByHand by rememberSaveable { mutableStateOf(editing != null) }
    var choosingDate by rememberSaveable { mutableStateOf(false) }
    var choosingTime by rememberSaveable { mutableStateOf(false) }
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }
    var addingCategory by rememberSaveable { mutableStateOf(false) }
    val occurredAt = LocalDateTime.parse(occurredAtText)

    // Merchant memory: "Mama Mboga" pre-selects what she filed it under last time, until she picks herself.
    LaunchedEffect(note) {
        if (pickedCategoryByHand || note.isBlank()) return@LaunchedEffect
        delay(300) // wait for a pause in typing
        container.categories.lastCategoryFor(note.trim())?.let { categoryId = it.id }
    }

    val form = ManualEntryForm(amountText, note, source, occurredAt, categoryId)
    val checked = form.check(existingCode = editing?.transaction?.code ?: fromMessage?.let(::entryCodeFor))

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                when {
                    editing != null -> "Edit spending"
                    fromMessage != null -> "Add from message"
                    else -> "Add spending"
                },
                style = MaterialTheme.typography.headlineSmall,
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount") },
                prefix = { Text("Ksh ") },
                singleLine = true,
                isError = amountText.isNotBlank() && checked is FormCheck.Incomplete && checked.missingAmount,
                supportingText = {
                    if (amountText.isNotBlank() && checked is FormCheck.Incomplete && checked.missingAmount) {
                        Text("Enter an amount like 450 or 1,250.50")
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("What for (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Paid with", style = MaterialTheme.typography.labelLarge)
                // Typed in from an M-Pesa message, so it was paid with M-Pesa: not a choice.
                if (source == PaymentSource.MPESA) {
                    Text("M-Pesa (from the message)", style = MaterialTheme.typography.bodyLarge)
                } else SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    PaymentSource.MANUAL.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = source == option,
                            onClick = { source = option },
                            shape = SegmentedButtonDefaults.itemShape(index, PaymentSource.MANUAL.size),
                        ) { Text(option.label) }
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("When", style = MaterialTheme.typography.labelLarge)
                    Text(whenLabel(occurredAt), style = MaterialTheme.typography.bodyLarge)
                }
                TextButton(onClick = { choosingDate = true }) { Text("Change") }
            }

            CategoryChoice(
                categories,
                categoryId,
                onPick = { categoryId = it.id; pickedCategoryByHand = true },
                onAddNew = { addingCategory = true },
            )

            Button(
                onClick = {
                    val entry = (checked as FormCheck.Ready).entry
                    val category = categories.first { it.id == entry.categoryId }
                    scope.launch {
                        container.transactions.save(entry)
                        onSaved(entry, category)
                    }
                },
                enabled = checked is FormCheck.Ready,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save") }

            if (editing != null) {
                TextButton(
                    onClick = { confirmingDelete = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) { Text("Delete") }
            }
        }
    }

    if (choosingDate) {
        val dateState = rememberDatePickerState(
            // The picker works in UTC midnights; converting through UTC keeps the chosen day exact.
            initialSelectedDateMillis = occurredAt.toLocalDate().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { choosingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { millis ->
                        val day = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        occurredAtText = LocalDateTime.of(day, occurredAt.toLocalTime()).toString()
                    }
                    choosingDate = false
                    choosingTime = true
                }) { Text("Next") }
            },
            dismissButton = { TextButton(onClick = { choosingDate = false }) { Text("Cancel") } },
        ) { DatePicker(dateState) }
    }

    if (choosingTime) {
        val timeState = rememberTimePickerState(occurredAt.hour, occurredAt.minute, is24Hour = false)
        AlertDialog(
            onDismissRequest = { choosingTime = false },
            title = { Text("Time") },
            text = { TimePicker(timeState) },
            confirmButton = {
                TextButton(onClick = {
                    occurredAtText = LocalDateTime.of(occurredAt.toLocalDate(), LocalTime.of(timeState.hour, timeState.minute)).toString()
                    choosingTime = false
                }) { Text("Done") }
            },
            dismissButton = { TextButton(onClick = { choosingTime = false }) { Text("Keep time") } },
        )
    }

    if (addingCategory) {
        NewCategoryDialog(
            existing = categories,
            onAdd = { name ->
                addingCategory = false
                // Created, then chosen: making a category from here means she wants to file under it.
                scope.launch {
                    container.categories.add(name)?.let { categoryId = it.id; pickedCategoryByHand = true }
                }
            },
            onDismiss = { addingCategory = false },
        )
    }

    if (confirmingDelete && editing != null) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text("Delete this spending?") },
            text = { Text("It will be removed from History and Insights.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmingDelete = false
                        scope.launch {
                            container.transactions.delete(editing.transaction.code)
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text("Cancel") } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryChoice(categories: List<Category>, selectedId: Long?, onPick: (Category) -> Unit, onAddNew: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Category", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.forEach { category ->
                val isSelected = category.id == selectedId
                FilledTonalButton(
                    onClick = { onPick(category) },
                    colors = if (isSelected) {
                        ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    } else {
                        ButtonDefaults.filledTonalButtonColors()
                    },
                    modifier = Modifier.semantics { selected = isSelected },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CategoryDot(category)
                        Text(category.name)
                    }
                }
            }
            OutlinedButton(onClick = onAddNew) { Text("+ New category") }
        }
    }
}

/** "Today, 1:30 PM", "Yesterday, 9:00 AM", or "Mon 13 Oct, 6:12 PM". */
private fun whenLabel(time: LocalDateTime): String = "${dayLabel(time.toLocalDate(), LocalDate.now())}, ${timeLabel(time)}"
