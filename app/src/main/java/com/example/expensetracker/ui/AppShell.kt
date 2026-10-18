package com.example.expensetracker.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.expensetracker.AppContainer
import com.example.expensetracker.R
import com.example.expensetracker.notify.formatKsh
import kotlinx.coroutines.launch

private enum class Tab(val label: String, @param:DrawableRes val icon: Int) {
    Queue("To categorize", R.drawable.ic_tab_queue),
    History("History", R.drawable.ic_tab_history),
    Insights("Insights", R.drawable.ic_tab_insights),
}

/**
 * The app after setup: three tabs, with the light/dark switch in the top corner of each and an
 * "Add spending" button floating over all of them.
 */
@Composable
fun AppShell(
    container: AppContainer,
    requestedCode: String?,
    onRequestHandled: () -> Unit,
    showingDark: Boolean,
    onToggleTheme: () -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(Tab.Queue) }
    var adding by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Arriving from a notification: its picker lives on the queue tab.
    LaunchedEffect(requestedCode) { if (requestedCode != null) tab = Tab.Queue }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { adding = true },
                // Explicit label: without it the button reached accessibility services unnamed.
                modifier = Modifier.semantics { contentDescription = "Add spending" },
                icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                text = { Text("Add spending") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                Tab.entries.forEach { entry ->
                    NavigationBarItem(
                        selected = tab == entry,
                        onClick = { tab = entry },
                        icon = { Icon(painterResource(entry.icon), contentDescription = null) },
                        label = { Text(entry.label) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                Tab.Queue -> QueueScreen(container, requestedCode, onRequestHandled)
                Tab.History -> HistoryScreen(container)
                Tab.Insights -> InsightsScreen(container, onOpenQueue = { tab = Tab.Queue })
            }
            ThemeToggle(showingDark, onToggleTheme, Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 4.dp))
        }
    }

    if (adding) {
        AddSpendingSheet(
            container,
            editing = null,
            onSaved = { entry, category ->
                adding = false
                // Manual entries skip the queue, so say where it went: the current list may not change.
                scope.launch {
                    // Material's default for a snackbar with an action is to stay until tapped; this one
                    // only confirms, so it goes away by itself (about 10 seconds).
                    val result = snackbar.showSnackbar(
                        "Saved ${formatKsh(entry.amountCents)} · ${category.name}",
                        actionLabel = "View",
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) tab = Tab.History
                }
            },
            onDismiss = { adding = false },
        )
    }
}

@Composable
private fun ThemeToggle(showingDark: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onToggle, modifier = modifier) {
        Icon(
            painterResource(if (showingDark) R.drawable.ic_light_mode else R.drawable.ic_dark_mode),
            contentDescription = if (showingDark) "Switch to light theme" else "Switch to dark theme",
        )
    }
}
