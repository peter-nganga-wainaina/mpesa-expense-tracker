package com.example.expensetracker.unparsed

import com.example.expensetracker.data.StoredTransaction
import com.example.expensetracker.data.TransactionWithCategory
import com.example.expensetracker.data.toEntity
import com.example.expensetracker.data.toStoredTransaction
import com.example.expensetracker.manual.FormCheck
import com.example.expensetracker.manual.ManualEntryForm
import com.example.expensetracker.manual.PaymentSource
import com.example.expensetracker.manual.check
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnparsedMessagesTest {

    private val arrived = LocalDateTime.of(2026, 10, 18, 13, 41)
    private val message = UnparsedMessage("TP3D4E5F6G", "TP3D4E5F6G Confirmed. Something brand new.", arrived)

    /** Saves the form like the app does: upsert by code, so the map holds what the table would. */
    private fun saveFromMessage(table: MutableMap<String, StoredTransaction>, amount: String) {
        val form = ManualEntryForm(amount, source = PaymentSource.MPESA, occurredAt = arrived, categoryId = 1)
        val entry = (form.check(existingCode = entryCodeFor(message)) as FormCheck.Ready).entry
        table[entry.code] = TransactionWithCategory(entry.toEntity(), "Food").toStoredTransaction()
    }

    @Test
    fun `no banner when nothing is waiting`() {
        assertNull(unparsedBannerText(0))
    }

    @Test
    fun `the banner counts what's waiting, in singular or plural`() {
        assertEquals("1 M-Pesa message couldn't be read", unparsedBannerText(1))
        assertEquals("3 M-Pesa messages couldn't be read", unparsedBannerText(3))
    }

    @Test
    fun `a message not yet typed in can be added`() {
        assertEquals(MessageStatus.NotAdded, statusOf(message, emptyMap()))
    }

    @Test
    fun `once typed in, the message shows the entry it became`() {
        val table = mutableMapOf<String, StoredTransaction>()

        saveFromMessage(table, "450")

        val added = statusOf(message, table) as MessageStatus.Added
        assertEquals(45000L, added.entry.transaction.amountCents)
        assertEquals(PaymentSource.MPESA, added.entry.source)
        assertTrue(added.entry.isManual)
    }

    @Test
    fun `saving from the same message twice keeps one entry, the latest`() {
        val table = mutableMapOf<String, StoredTransaction>()

        saveFromMessage(table, "450")
        saveFromMessage(table, "480")

        assertEquals(1, table.size)
        assertEquals(48000L, table.values.single().transaction.amountCents)
    }

    @Test
    fun `deleting the entry makes the message addable again`() {
        val table = mutableMapOf<String, StoredTransaction>()
        saveFromMessage(table, "450")

        table.remove(entryCodeFor(message))

        assertEquals(MessageStatus.NotAdded, statusOf(message, table))
    }

    @Test
    fun `an entry from a message uses the message's own M-Pesa code`() {
        assertEquals("TP3D4E5F6G", entryCodeFor(message))
    }

    @Test
    fun `an unrelated transaction doesn't mark the message as added`() {
        val table = mutableMapOf<String, StoredTransaction>()
        saveFromMessage(table, "450")
        val other = UnparsedMessage("TX0000000Z", "TX0000000Z Confirmed. Other.", arrived)

        assertEquals(MessageStatus.NotAdded, statusOf(other, table))
    }
}
