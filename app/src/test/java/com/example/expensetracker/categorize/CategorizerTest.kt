package com.example.expensetracker.categorize

import com.example.expensetracker.sms.MpesaParser
import com.example.expensetracker.sms.ParseResult
import com.example.expensetracker.testing.FakeNotifier
import com.example.expensetracker.testing.FakeTransactionStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CategorizerTest {

    private val store = FakeTransactionStore()
    private val notifier = FakeNotifier()
    private val categorizer = Categorizer(store, notifier)

    @Test
    fun `categorizing files the transaction under the chosen category`() = runBlocking {
        categorizer.categorize("TL1A2B3C4D", categoryId = 3)

        assertEquals(3L, store.categoryOf["TL1A2B3C4D"])
    }

    @Test
    fun `categorizing removes the transaction's prompt from the shade`() = runBlocking {
        val transaction = (MpesaParser.parse(SENT_MESSAGE) as ParseResult.Parsed).transaction
        notifier.promptForCategory(transaction, quickPicks = emptyList())

        categorizer.categorize(transaction.code, categoryId = 3)

        assertFalse(transaction.code in notifier.showing)
    }

    @Test
    fun `re-categorizing replaces the earlier choice`() = runBlocking {
        categorizer.categorize("TL1A2B3C4D", categoryId = 3)
        categorizer.categorize("TL1A2B3C4D", categoryId = 5)

        assertEquals(5L, store.categoryOf["TL1A2B3C4D"])
    }

    private companion object {
        const val SENT_MESSAGE =
            "TL1A2B3C4D Confirmed. Ksh1,100.00 sent to ALICE  MWANGI 0700000001 on 4/12/25 at 8:07 AM. " +
                "New M-PESA balance is Ksh25,000.00. Transaction cost, Ksh23.00."
    }
}
