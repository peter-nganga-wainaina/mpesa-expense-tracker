package com.example.expensetracker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.graphics.Color
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.example.expensetracker.sms.DailySweepWorker
import com.example.expensetracker.ui.AppShell
import com.example.expensetracker.ui.SetupScreen
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme
import com.example.expensetracker.ui.theme.ThemeMode
import com.example.expensetracker.update.DailyUpdateCheckWorker
import com.example.expensetracker.update.ReleaseInfo
import com.example.expensetracker.update.UpdateCheck
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val container by lazy { AppContainer(this) }
    private var setupComplete by mutableStateOf(false)
    private var themeMode by mutableStateOf(ThemeMode.SYSTEM)
    private var availableUpdate by mutableStateOf<ReleaseInfo?>(null)

    /** A transaction whose picker should open, set when the user arrives from its notification. */
    private var requestedCode by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Only on a fresh start: after rotation the picker's own state already says what's open.
        if (savedInstanceState == null) requestedCode = intent.getStringExtra(EXTRA_TRANSACTION_CODE)
        setupComplete = container.setup.isComplete
        themeMode = container.themePreference.mode
        setContent {
            val showingDark = themeMode.isDark(systemIsDark = isSystemInDarkTheme())
            // Status and navigation bar icons follow the app's theme, not the phone's, so they stay
            // readable when the user picks the opposite of the phone's setting.
            LaunchedEffect(showingDark) {
                val bars = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { showingDark }
                enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
            }
            ExpenseTrackerTheme(darkTheme = showingDark) {
                if (setupComplete) {
                    AppShell(
                        container,
                        requestedCode,
                        onRequestHandled = { requestedCode = null },
                        showingDark = showingDark,
                        onToggleTheme = {
                            themeMode = ThemeMode.toggledFrom(showingDark)
                            container.themePreference.mode = themeMode
                        },
                        availableUpdate = availableUpdate,
                    )
                } else {
                    SetupScreen(container, onFinished = {
                        container.setup.markComplete()
                        setupComplete = true
                        DailySweepWorker.schedule(this)
                        DailyUpdateCheckWorker.schedule(this)
                    })
                }
            }
        }
    }

    // Each time the app comes to the front, catch any M-Pesa messages the receiver missed, look for a
    // newer release, and make sure both daily jobs are scheduled for the days the app isn't opened.
    override fun onStart() {
        super.onStart()
        if (!setupComplete) return
        lifecycleScope.launch { container.backfill.sweep() }
        lifecycleScope.launch { checkForUpdate() }
        DailySweepWorker.schedule(this)
        DailyUpdateCheckWorker.schedule(this)
    }

    private suspend fun checkForUpdate() {
        when (val check = container.updateChecker.check()) {
            is UpdateCheck.Available -> availableUpdate = check.release
            UpdateCheck.UpToDate -> availableUpdate = null
            // Keeps whatever was showing: being offline for a moment shouldn't hide a known update.
            is UpdateCheck.Failed -> Log.w("MainActivity", "Update check failed", check.cause)
        }
    }

    // singleTop: a notification tap while the app is open arrives here instead of stacking a new screen.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        requestedCode = intent.getStringExtra(EXTRA_TRANSACTION_CODE)
    }

    companion object {
        private const val EXTRA_TRANSACTION_CODE = "transactionCode"

        /** Opens the app with the category picker showing for one transaction. */
        fun pickerIntent(context: Context, transactionCode: String): Intent =
            Intent(context, MainActivity::class.java).putExtra(EXTRA_TRANSACTION_CODE, transactionCode)
    }
}
