package com.example.expensetracker.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MpesaSmsTest {

    @Test
    fun `a single-part message from MPESA is returned as is`() {
        assertEquals("TL1A2B3C4D Confirmed.", mpesaSms(listOf(part("MPESA", "TL1A2B3C4D Confirmed.")))?.body)
    }

    @Test
    fun `a multipart message is joined in order into one body`() {
        val parts = listOf(
            part("MPESA", "TL1A2B3C4D Confirmed. Ksh1,100.00 sent to ALICE"),
            part("MPESA", "  MWANGI 0700000001 on 4/12/25 at 8:07 AM."),
        )

        assertEquals(
            "TL1A2B3C4D Confirmed. Ksh1,100.00 sent to ALICE  MWANGI 0700000001 on 4/12/25 at 8:07 AM.",
            mpesaSms(parts)?.body,
        )
    }

    @Test
    fun `the message is stamped with when its first part arrived`() {
        val parts = listOf(part("MPESA", "first", timestampMillis = 1_000), part("MPESA", "second", timestampMillis = 2_000))

        assertEquals(1_000L, mpesaSms(parts)?.receivedAtMillis)
    }

    @Test
    fun `a message from another sender is not an M-Pesa message`() {
        assertNull(mpesaSms(listOf(part("+254700000001", "Confirmed. Ksh1,000.00 sent to you"))))
    }

    @Test
    fun `a look-alike sender is not an M-Pesa message`() {
        assertNull(mpesaSms(listOf(part("MPESA-PROMO", "Win big!"))))
        assertNull(mpesaSms(listOf(part("mpesa", "Win big!"))))
    }

    @Test
    fun `a message with no sender is not an M-Pesa message`() {
        assertNull(mpesaSms(listOf(part(null, "TL1A2B3C4D Confirmed."))))
    }

    private fun part(sender: String?, body: String, timestampMillis: Long = 0) = SmsPart(sender, body, timestampMillis)
}
