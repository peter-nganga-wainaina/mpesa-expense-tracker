package com.example.expensetracker.sms

import com.example.expensetracker.sms.MpesaTransactionType.AIRTIME
import com.example.expensetracker.sms.MpesaTransactionType.FULIZA_DRAWDOWN
import com.example.expensetracker.sms.MpesaTransactionType.FULIZA_REPAYMENT
import com.example.expensetracker.sms.MpesaTransactionType.PAID
import com.example.expensetracker.sms.MpesaTransactionType.PAYBILL
import com.example.expensetracker.sms.MpesaTransactionType.RECEIVED
import com.example.expensetracker.sms.MpesaTransactionType.SENT
import com.example.expensetracker.sms.MpesaTransactionType.WITHDRAW
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MpesaParserTest {

    // Sample files: every message in a file parses as that file's type, with well-formed fields.

    @Test
    fun `every sent sample parses as SENT`() = assertAllParseAs("sent.txt", SENT)

    @Test
    fun `every paybill sample parses as PAYBILL`() = assertAllParseAs("paybill.txt", PAYBILL)

    @Test
    fun `every till sample parses as PAID`() = assertAllParseAs("paid.txt", PAID)

    @Test
    fun `every received sample parses as RECEIVED`() = assertAllParseAs("received.txt", RECEIVED)

    @Test
    fun `every withdrawal sample parses as WITHDRAW`() = assertAllParseAs("withdraw.txt", WITHDRAW)

    @Test
    fun `every airtime sample parses as AIRTIME`() = assertAllParseAs("airtime.txt", AIRTIME)

    @Test
    fun `every fuliza sample parses as a fuliza transaction`() {
        samples("fuliza.txt").forEach { message ->
            val transaction = parsed(message)
            assertTrue(message, transaction.type in setOf(FULIZA_DRAWDOWN, FULIZA_REPAYMENT))
            assertWellFormed(transaction, message)
        }
    }

    @Test
    fun `every notice sample is ignored`() {
        samples("notices.txt").forEach { message ->
            assertEquals(message, ParseResult.Ignored, MpesaParser.parse(message))
        }
    }

    @Test
    fun `a message without a transaction code is ignored`() {
        assertEquals(ParseResult.Ignored, MpesaParser.parse("Your request is being processed. Please wait."))
    }

    @Test
    fun `a code without a readable amount is unrecognized`() {
        assertEquals(ParseResult.Unrecognized("XY12AB34CD"), MpesaParser.parse("XY12AB34CD Confirmed. Your request was completed."))
    }

    // Resilience to wording changes Safaricom might make.

    @Test
    fun `an unknown message type with a code and amount is kept as UNKNOWN`() {
        val t = parsed("XY12AB34CD Confirmed. Ksh250.00 transferred to NEW SERVICE on 1/1/27 at 9:00 AM.")
        assertEquals(MpesaTransactionType.UNKNOWN, t.type)
        assertEquals(25_000L, t.amountCents)
        assertEquals(LocalDateTime.of(2027, 1, 1, 9, 0), t.occurredAt)
        assertNull(t.counterparty)
    }

    @Test
    fun `an inserted word does not break extraction`() {
        val t = parsed("TF9J1K2L3M Confirmed. Ksh500.00 has been paid to SUNRISE CAFE. on 27/6/25 at 11:00 PM.")
        assertEquals(PAID, t.type)
        assertEquals("SUNRISE CAFE", t.counterparty)
        assertEquals(50_000L, t.amountCents)
    }

    // Exact values for the variants most likely to go wrong.

    @Test
    fun `send to a person extracts amount, fee, name and phone`() {
        val t = parsed(
            "TL1A2B3C4D Confirmed. Ksh1,100.00 sent to ALICE  MWANGI 0700000001 on 4/12/25 at 8:07 AM. " +
                "New M-PESA balance is Ksh25,000.00. Transaction cost, Ksh23.00.  Amount you can transact " +
                "within the day is 498,900.00. Earn interest daily on Ziidi MMF,Dial *334#"
        )
        assertEquals("TL1A2B3C4D", t.code)
        assertEquals(110_000L, t.amountCents)
        assertEquals(2_300L, t.costCents)
        assertEquals("ALICE MWANGI", t.counterparty)
        assertEquals("0700000001", t.phone)
        assertEquals(LocalDateTime.of(2025, 12, 4, 8, 7), t.occurredAt)
    }

    @Test
    fun `send to Pochi la Biashara has no phone`() {
        val t = parsed(
            "TH4D5E6F7G Confirmed. Ksh500.00 sent to DAVID  KAMAU on 9/8/25 at 7:27 PM. New M-PESA balance " +
                "is Ksh22,000.00. Transaction cost, Ksh7.00. Amount you can transact within the day is 496,500.00."
        )
        assertEquals(SENT, t.type)
        assertEquals("DAVID KAMAU", t.counterparty)
        assertNull(t.phone)
        assertEquals(LocalDateTime.of(2025, 8, 9, 19, 27), t.occurredAt)
    }

    @Test
    fun `Ratiba standing order payment parses as SENT`() {
        val t = parsed(
            "TL6A7B8C9D Confirmed M-PESA Ratiba. Ksh2,000.00 sent to HELEN  A***** 0700***007 on 1/12/25 at " +
                "5:45 PM. New M-PESA balance is Ksh20,000.00. Transaction cost, Ksh38.00."
        )
        assertEquals(SENT, t.type)
        assertEquals(200_000L, t.amountCents)
        assertEquals(3_800L, t.costCents)
        assertEquals("HELEN A*****", t.counterparty)
        assertEquals("0700***007", t.phone)
    }

    @Test
    fun `paybill account can contain spaces`() {
        val t = parsed(
            "TH8C9D1E2F Confirmed. Ksh1,500.00 sent to Sample Health Clinic for account Jane Doe on 11/8/25 at " +
                "11:09 AM New M-PESA balance is Ksh9,000.00. Transaction cost, Ksh20.00."
        )
        assertEquals(PAYBILL, t.type)
        assertEquals("Sample Health Clinic", t.counterparty)
        assertEquals("Jane Doe", t.reference)
    }

    @Test
    fun `paybill extracts business and account number`() {
        val t = parsed(
            "TH6F7G8H9J Confirmed. Ksh200.00 sent to KPLC PREPAID for account 10000000001 on 4/8/25 at 7:30 PM " +
                "New M-PESA balance is Ksh20,000.00. Transaction cost, Ksh5.00.Amount you can transact within " +
                "the day is 499,240.00."
        )
        assertEquals(PAYBILL, t.type)
        assertEquals("KPLC PREPAID", t.counterparty)
        assertEquals("10000000001", t.reference)
        assertEquals(500L, t.costCents)
    }

    @Test
    fun `till payment strips the trailing period from the merchant`() {
        val t = parsed(
            "TF9J1K2L3M Confirmed. Ksh500.00 paid to SUNRISE CAFE WESTGATE. on 27/6/25 at 11:00 PM.New M-PESA " +
                "balance is Ksh9,000.00. Transaction cost, Ksh0.00."
        )
        assertEquals("SUNRISE CAFE WESTGATE", t.counterparty)
        assertEquals(50_000L, t.amountCents)
        assertEquals(0L, t.costCents)
        assertEquals(LocalDateTime.of(2025, 6, 27, 23, 0), t.occurredAt)
    }

    @Test
    fun `received from a person keeps the masked phone`() {
        val t = parsed(
            "UJ5P6Q7R8S Confirmed.You have received Ksh3,000.00 from CAROL  KIPROP 0700***003 on 6/10/26 at " +
                "3:58 PM  New M-PESA balance is Ksh57,000.00."
        )
        assertEquals(RECEIVED, t.type)
        assertEquals("CAROL KIPROP", t.counterparty)
        assertEquals("0700***003", t.phone)
        assertEquals(LocalDateTime.of(2026, 10, 6, 15, 58), t.occurredAt)
    }

    @Test
    fun `received from a business parses a time without at`() {
        val t = parsed(
            "TH6Q7R8S9T Confirmed.You have received Ksh8,000.00 from ZIIDI on 8/8/25 5:06 PM. New M-PESA " +
                "balance is Ksh14,000.00."
        )
        assertEquals("ZIIDI", t.counterparty)
        assertNull(t.phone)
        assertEquals(LocalDateTime.of(2025, 8, 8, 17, 6), t.occurredAt)
    }

    @Test
    fun `withdrawal extracts agent number, agent name and fee`() {
        val t = parsed(
            "TH8S9T1U2V Confirmed.on 5/8/25 at 8:37 AMWithdraw Ksh400.00 from 100001 - SAMPLE AGENT SHOP MAIN RD " +
                "New M-PESA balance is Ksh19,000.00. Transaction cost, Ksh29.00."
        )
        assertEquals(WITHDRAW, t.type)
        assertEquals("100001", t.reference)
        assertEquals("SAMPLE AGENT SHOP MAIN RD", t.counterparty)
        assertEquals(40_000L, t.amountCents)
        assertEquals(2_900L, t.costCents)
        assertEquals(LocalDateTime.of(2025, 8, 5, 8, 37), t.occurredAt)
    }

    @Test
    fun `airtime for another number keeps that number`() {
        val t = parsed(
            "TH1U2V3W4X confirmed.You bought Ksh500.00 of airtime for 0700000005 on 3/8/25 at 10:49 AM.New  " +
                "balance is Ksh29,000.00. Transaction cost, Ksh0.00."
        )
        assertEquals(AIRTIME, t.type)
        assertEquals("0700000005", t.phone)
        assertEquals(50_000L, t.amountCents)
    }

    @Test
    fun `fuliza drawdown records the access fee as the cost`() {
        val t = parsed(
            "TG2V3W4X5Y Confirmed. Fuliza M-PESA amount is Ksh 948.44. Access Fee charged Ksh 9.49. Total Fuliza " +
                "M-PESA outstanding amount is Ksh 957.93 due on 29/09/25."
        )
        assertEquals(FULIZA_DRAWDOWN, t.type)
        assertEquals(94_844L, t.amountCents)
        assertEquals(949L, t.costCents)
        assertNull(t.occurredAt)
    }

    @Test
    fun `fuliza repayment has no date`() {
        val t = parsed(
            "TG3W4X5Y6Z  Confirmed. Ksh 957.93 from your M-PESA has been used to fully pay your outstanding " +
                "Fuliza M-PESA. Available Fuliza M-PESA limit is Ksh 2000.00. M-PESA balance is Ksh50.00."
        )
        assertEquals(FULIZA_REPAYMENT, t.type)
        assertEquals("TG3W4X5Y6Z", t.code)
        assertEquals(95_793L, t.amountCents)
        assertNull(t.occurredAt)
    }

    @Test
    fun `five-figure amounts keep every digit`() {
        val t = parsed(
            "TI7G8H9J1K Confirmed. Ksh50,000.00 sent to ACME CAPITAL LTD for account 123456AB on 9/9/25 at " +
                "8:43 AM New M-PESA balance is Ksh13,000.00. Transaction cost, Ksh108.00."
        )
        assertEquals(5_000_000L, t.amountCents)
        assertEquals(10_800L, t.costCents)
    }

    private fun assertAllParseAs(file: String, type: MpesaTransactionType) {
        samples(file).forEach { message ->
            val transaction = parsed(message)
            assertEquals(message, type, transaction.type)
            assertWellFormed(transaction, message)
            assertTrue("missing date: $message", transaction.occurredAt != null)
        }
    }

    private fun assertWellFormed(t: MpesaTransaction, message: String) {
        assertTrue("bad code: $message", t.code.matches(Regex("[A-Z0-9]{10}")))
        assertTrue("non-positive amount: $message", t.amountCents > 0)
        assertTrue("negative cost: $message", t.costCents >= 0)
        val name = t.counterparty
        assertTrue("missing counterparty: $message", !name.isNullOrBlank())
        assertTrue("trailing period: $message", !name!!.endsWith("."))
        assertTrue("double space: $message", "  " !in name)
    }

    private fun parsed(message: String): MpesaTransaction =
        when (val result = MpesaParser.parse(message)) {
            is ParseResult.Parsed -> result.transaction
            else -> throw AssertionError("Expected a transaction but got $result for: $message")
        }

    private fun samples(file: String): List<String> {
        val resource = javaClass.getResource("/mpesa_samples/$file") ?: error("Missing sample file $file")
        return resource.readText()
            .lines()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .also { check(it.isNotEmpty()) { "No samples found in $file" } }
    }
}
