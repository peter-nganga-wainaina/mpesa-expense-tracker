package com.example.expensetracker.suggest

import com.example.expensetracker.categorize.Category
import com.example.expensetracker.sms.MpesaParser
import com.example.expensetracker.sms.ParseResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MerchantMemorySuggesterTest {

    private val groceries = Category(7, "Groceries")
    private val suggester = MerchantMemorySuggester { counterparty ->
        if (counterparty == "CARREFOUR") groceries else null
    }

    private fun suggest(message: String, limit: Int = 2) = runBlocking {
        suggester.suggest((MpesaParser.parse(message) as ParseResult.Parsed).transaction, limit)
    }

    @Test
    fun `suggests the category last chosen for this merchant`() {
        assertEquals(listOf(groceries), suggest(paidTo("CARREFOUR")))
    }

    @Test
    fun `suggests nothing for a merchant with no history`() {
        assertTrue(suggest(paidTo("NAIVAS")).isEmpty())
    }

    @Test
    fun `suggests nothing when the counterparty could not be read`() {
        assertTrue(suggest("TL1A2B3C4D Confirmed. Ksh50.00 something unusual on 4/12/25 at 8:07 AM.").isEmpty())
    }

    @Test
    fun `suggests nothing when asked for no suggestions`() {
        assertTrue(suggest(paidTo("CARREFOUR"), limit = 0).isEmpty())
    }

    private fun paidTo(merchant: String) =
        "TP2C3D4E5F Confirmed. Ksh780.00 paid to $merchant. on 13/10/26 at 10:40 PM. " +
            "New M-PESA balance is Ksh23,250.00. Transaction cost, Ksh0.00."
}
