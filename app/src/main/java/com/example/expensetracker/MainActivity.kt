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
import com.example.expensetracker.ui.QueueScreen
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme

class MainActivity : ComponentActivity() {

    /** A transaction whose picker should open, set when the user arrives from its notification. */
    private var requestedCode by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Only on a fresh start: after rotation the picker's own state already says what's open.
        if (savedInstanceState == null) requestedCode = intent.getStringExtra(EXTRA_TRANSACTION_CODE)
        val container = AppContainer(this)
        enableEdgeToEdge()
        setContent {
            ExpenseTrackerTheme {
                QueueScreen(container, requestedCode, onRequestHandled = { requestedCode = null })
            }
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
