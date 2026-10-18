package com.example.expensetracker.manual

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ManualEntriesTest {

    private val now = LocalDateTime.of(2026, 10, 18, 13, 30)

    @Test
    fun `typed amounts are read as cents`() {
        assertEquals(45000L, parseKshAmount("450"))
        assertEquals(125050L, parseKshAmount("1,250.50"))
        assertEquals(30000L, parseKshAmount("Ksh 300"))
        assertEquals(1050L, parseKshAmount(" 10.5 "))
    }

    @Test
    fun `anything that isn't a positive amount is rejected`() {
        for (text in listOf("", "   ", "0", "0.00", "-5", "12.345", "abc", "4 5 0x", "1234567890")) {
            assertNull("\"$text\"", parseKshAmount(text))
        }
    }

    @Test
    fun `an amount shows for editing the way it would be typed, and reads back unchanged`() {
        assertEquals("450", editableAmount(45000))
        assertEquals("1250.50", editableAmount(125050))
        assertEquals("10.05", editableAmount(1005))
        for (cents in listOf(45000L, 125050L, 1005L, 1L, 99_999_999_999L)) {
            assertEquals(cents, parseKshAmount(editableAmount(cents)))
        }
    }

    @Test
    fun `manual codes can never look like an M-Pesa code`() {
        val mpesaCode = Regex("""[A-Z0-9]{10}""")

        repeat(50) { assertFalse(mpesaCode.matches(newManualCode())) }
    }

    @Test
    fun `each new manual entry gets its own code`() {
        assertNotEquals(newManualCode(), newManualCode())
    }

    @Test
    fun `a complete form becomes an entry`() {
        val form = ManualEntryForm("450", "  Mama   Mboga ", PaymentSource.CASH, now, categoryId = 1)

        val entry = (form.check(newCode = { "MANUAL-1" }) as FormCheck.Ready).entry

        assertEquals(ManualEntry("MANUAL-1", 45000, "Mama Mboga", PaymentSource.CASH, now, 1), entry)
    }

    @Test
    fun `a blank note is saved as no note`() {
        val entry = (ManualEntryForm("450", "   ", occurredAt = now, categoryId = 1).check() as FormCheck.Ready).entry

        assertNull(entry.note)
    }

    @Test
    fun `a form without an amount or category says what's missing`() {
        assertEquals(
            FormCheck.Incomplete(missingAmount = true, missingCategory = true),
            ManualEntryForm(occurredAt = now).check(),
        )
        assertEquals(
            FormCheck.Incomplete(missingAmount = false, missingCategory = true),
            ManualEntryForm("450", occurredAt = now).check(),
        )
    }

    @Test
    fun `editing keeps the entry's existing code`() {
        val form = ManualEntryForm("500", occurredAt = now, categoryId = 2)

        val entry = (form.check(existingCode = "MANUAL-abc") as FormCheck.Ready).entry

        assertEquals("MANUAL-abc", entry.code)
    }

    @Test
    fun `only cash, card and other are offered as manual sources`() {
        assertEquals(listOf(PaymentSource.CASH, PaymentSource.CARD, PaymentSource.OTHER), PaymentSource.MANUAL)
    }
}
