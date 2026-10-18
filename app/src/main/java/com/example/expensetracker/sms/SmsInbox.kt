package com.example.expensetracker.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads M-Pesa messages from the system SMS inbox. The SMS app stores multipart messages joined. */
class SmsInbox(private val context: Context) : MpesaInbox {

    override suspend fun messagesSince(since: LocalDateTime): List<ReceivedSms> = withContext(Dispatchers.IO) {
        // Without READ_SMS there is nothing to read; live SMS still arrive through the receiver.
        if (!canRead()) return@withContext emptyList()
        val zone = ZoneId.systemDefault()
        val sinceMillis = since.atZone(zone).toInstant().toEpochMilli()
        val cursor = context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(Telephony.Sms.BODY, Telephony.Sms.DATE),
            "${Telephony.Sms.ADDRESS} = ? AND ${Telephony.Sms.DATE} >= ?",
            arrayOf(MPESA_SENDER_ID, sinceMillis.toString()),
            "${Telephony.Sms.DATE} ASC",
        ) ?: return@withContext emptyList()

        cursor.use {
            val body = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val date = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
            buildList {
                while (it.moveToNext()) {
                    val receivedAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(it.getLong(date)), zone)
                    add(ReceivedSms(it.getString(body).orEmpty(), receivedAt))
                }
            }
        }
    }

    private fun canRead() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
}
