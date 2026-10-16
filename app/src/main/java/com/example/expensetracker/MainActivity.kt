package com.example.expensetracker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.example.expensetracker.sms.DailySweepWorker
import com.example.expensetracker.ui.QueueScreen
import com.example.expensetracker.ui.SetupScreen
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val container by lazy { AppContainer(this) }
    private var setupComplete by mutableStateOf(false)

    /** A transaction whose picker should open, set when the user arrives from its notification. */
    private var requestedCode by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Only on a fresh start: after rotation the picker's own state already says what's open.
        if (savedInstanceState == null) requestedCode = intent.getStringExtra(EXTRA_TRANSACTION_CODE)
        setupComplete = container.setup.isComplete
        enableEdgeToEdge()
        setContent {
            ExpenseTrackerTheme {
                if (setupComplete) {
                    QueueScreen(container, requestedCode, onRequestHandled = { requestedCode = null })
                } else {
                    SetupScreen(container, onFinished = {
                        container.setup.markComplete()
                        setupComplete = true
                        DailySweepWorker.schedule(this)
                    })
                }
            }
        }
    }

    // Each time the app comes to the front, catch any M-Pesa messages the receiver missed, and make
    // sure the daily sweep is scheduled for the days the app isn't opened.
    override fun onStart() {
        super.onStart()
        if (!setupComplete) return
        lifecycleScope.launch { container.backfill.sweep() }
        DailySweepWorker.schedule(this)
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
