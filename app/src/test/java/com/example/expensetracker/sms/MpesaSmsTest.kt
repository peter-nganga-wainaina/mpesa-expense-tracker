package com.example.expensetracker.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MpesaSmsTest {

    @Test
    fun `a single-part message from MPESA is returned as is`() {
        assertEquals("TL1A2B3C4D Confirmed.", mpesaBody(listOf(SmsPart("MPESA", "TL1A2B3C4D Confirmed."))))
    }

    @Test
    fun `a multipart message is joined in order into one body`() {
        val parts = listOf(
            SmsPart("MPESA", "TL1A2B3C4D Confirmed. Ksh1,100.00 sent to ALICE"),
            SmsPart("MPESA", "  MWANGI 0700000001 on 4/12/25 at 8:07 AM."),
        )

        assertEquals(
            "TL1A2B3C4D Confirmed. Ksh1,100.00 sent to ALICE  MWANGI 0700000001 on 4/12/25 at 8:07 AM.",
            mpesaBody(parts),
        )
    }

    @Test
    fun `a message from another sender is not an M-Pesa message`() {
        assertNull(mpesaBody(listOf(SmsPart("+254700000001", "Confirmed. Ksh1,000.00 sent to you"))))
    }

    @Test
    fun `a look-alike sender is not an M-Pesa message`() {
        assertNull(mpesaBody(listOf(SmsPart("MPESA-PROMO", "Win big!"))))
        assertNull(mpesaBody(listOf(SmsPart("mpesa", "Win big!"))))
    }

    @Test
    fun `a message with no sender is not an M-Pesa message`() {
        assertNull(mpesaBody(listOf(SmsPart(null, "TL1A2B3C4D Confirmed."))))
    }
}
