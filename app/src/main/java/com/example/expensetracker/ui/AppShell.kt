package com.example.expensetracker.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.expensetracker.AppContainer
import com.example.expensetracker.R

private enum class Tab(val label: String, @param:DrawableRes val icon: Int) {
    Queue("To categorize", R.drawable.ic_tab_queue),
    History("History", R.drawable.ic_tab_history),
    Insights("Insights", R.drawable.ic_tab_insights),
}

/** The app after setup: three tabs, with the light/dark switch in the top corner of each. */
@Composable
fun AppShell(
    container: AppContainer,
    requestedCode: String?,
    onRequestHandled: () -> Unit,
    showingDark: Boolean,
    onToggleTheme: () -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(Tab.Queue) }

    // Arriving from a notification: its picker lives on the queue tab.
    LaunchedEffect(requestedCode) { if (requestedCode != null) tab = Tab.Queue }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
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
