package com.example.expensetracker.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.expensetracker.notify.CategoryPrompt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Woken by Android for every incoming SMS; the app runs no service of its own. Hands M-Pesa messages
 * to [TransactionIngest] and ignores everything else.
 */
class MpesaSmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            .map { SmsPart(sender = it.originatingAddress, body = it.messageBody) }
        val body = mpesaBody(parts) ?: return

        val ingest = TransactionIngest(
            notifier = CategoryPrompt(context.applicationContext),
            // Temporary until the UnparsedMessage table exists.
            recordUnparsed = { Log.w(TAG, "Unrecognized M-Pesa message: $it") },
        )
        // goAsync keeps the process alive past onReceive while the work finishes off the main thread.
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                ingest.ingest(body)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "MpesaSmsReceiver"
    }
}
