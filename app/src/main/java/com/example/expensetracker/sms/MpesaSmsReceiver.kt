package com.example.expensetracker.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.expensetracker.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Woken by Android for every incoming SMS; the app runs no service of its own. Hands M-Pesa messages
 * to [TransactionIngest] and ignores everything else.
 */
class MpesaSmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            .map { SmsPart(sender = it.originatingAddress, body = it.messageBody, timestampMillis = it.timestampMillis) }
        val sms = mpesaSms(parts) ?: return
        val receivedAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(sms.receivedAtMillis), ZoneId.systemDefault())

        val ingest = AppContainer(context).ingest
        // goAsync keeps the process alive past onReceive while the work finishes off the main thread.
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                ingest.ingest(sms.body, receivedAt)
            } finally {
                pending.finish()
            }
        }
    }
}
