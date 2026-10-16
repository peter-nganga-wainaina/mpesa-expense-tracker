package com.example.expensetracker.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.expensetracker.AppContainer
import com.example.expensetracker.sms.InboxBackfill
import kotlinx.coroutines.launch

private enum class SetupStep { Permissions, Import, Battery }

/** First run: permissions, then how much past M-Pesa history to import, then battery settings. */
@Composable
fun SetupScreen(container: AppContainer, onFinished: () -> Unit) {
    var step by rememberSaveable { mutableStateOf(SetupStep.Permissions) }

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        Column(
            Modifier.padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Step ${step.ordinal + 1} of ${SetupStep.entries.size}", style = MaterialTheme.typography.labelLarge)
            when (step) {
                SetupStep.Permissions -> PermissionsStep(onDone = { step = SetupStep.Import })
                SetupStep.Import -> ImportStep(container, onDone = { step = SetupStep.Battery })
                SetupStep.Battery -> BatteryStep(onDone = onFinished)
            }
        }
    }
}

@Composable
private fun PermissionsStep(onDone: () -> Unit) {
    val context = LocalContext.current
    // Denying is allowed: the queue keeps a banner asking again, so setup never traps the user.
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { onDone() }

    Text("Welcome", style = MaterialTheme.typography.headlineMedium)
    Text(
        "Expense Tracker reads your M-Pesa confirmation messages, saves each transaction, and " +
            "notifies you to pick a category. Messages stay on this phone."
    )
    if (context.hasRequiredPermissions()) {
        Button(onClick = onDone) { Text("Continue") }
    } else {
        Button(onClick = { request.launch(REQUIRED_PERMISSIONS) }) { Text("Allow SMS and notifications") }
    }
}

@Composable
private fun ImportStep(container: AppContainer, onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var days by rememberSaveable { mutableIntStateOf(InboxBackfill.DEFAULT_FIRST_IMPORT_DAYS) }
    var importing by remember { mutableStateOf(false) }
    var imported by rememberSaveable { mutableStateOf<Int?>(null) }

    Text("Import recent transactions", style = MaterialTheme.typography.headlineMedium)
    when {
        !context.isGranted(Manifest.permission.READ_SMS) -> {
            Text("Without SMS access, past messages can't be imported. New ones will still appear.")
            Button(onClick = onDone) { Text("Continue") }
        }
        imported != null -> {
            Text("Imported $imported transactions. They're waiting for you to categorize.")
            Button(onClick = onDone) { Text("Continue") }
        }
        else -> {
            Text("Bring in M-Pesa transactions you've already made. You'll categorize them in the app.")
            Column {
                InboxBackfill.FIRST_IMPORT_OPTIONS.forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = days == option, onClick = { days = option })
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = days == option, onClick = null)
                        Text(if (option == 0) "Start fresh" else "Last $option days", Modifier.padding(start = 8.dp))
                    }
                }
            }
            Button(
                enabled = !importing,
                onClick = {
                    importing = true
                    scope.launch {
                        imported = container.backfill.importLast(days)
                        importing = false
                        if (days == 0) onDone()
                    }
                },
            ) { Text(if (importing) "Importing…" else "Continue") }
        }
    }
}

@SuppressLint("BatteryLife") // Allowed for sideloaded apps; the SMS receiver must not be put to sleep.
@Composable
private fun BatteryStep(onDone: () -> Unit) {
    val context = LocalContext.current
    val power = remember { context.getSystemService(PowerManager::class.java) }
    var unrestricted by remember { mutableStateOf(power.isIgnoringBatteryOptimizations(context.packageName)) }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        unrestricted = power.isIgnoringBatteryOptimizations(context.packageName)
    }

    Text("Keep it running", style = MaterialTheme.typography.headlineMedium)
    Text("Phones put unused apps to sleep to save battery. A sleeping app can miss M-Pesa messages.")
    if (unrestricted) {
        Text("✓ Battery optimization is off for Expense Tracker.")
    } else {
        OutlinedButton(onClick = {
            request.launch(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
            )
        }) { Text("Allow running in the background") }
    }
    // Samsung adds its own sleeping-apps list on top of Android's setting.
    if (Build.MANUFACTURER.equals("samsung", ignoreCase = true)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("On Samsung phones, also:", style = MaterialTheme.typography.titleSmall)
                Text("Settings → Battery → Background usage limits → Never sleeping apps → add Expense Tracker")
                TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }) { Text("Open Settings") }
            }
        }
    }
    Button(onClick = onDone) { Text("Finish") }
}
