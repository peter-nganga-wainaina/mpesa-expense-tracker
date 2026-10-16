package com.example.expensetracker.suggest

import com.example.expensetracker.sms.MpesaParser
import com.example.expensetracker.sms.ParseResult
import com.example.expensetracker.testing.FakeCategoryStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class MostUsedSuggesterTest {

    private val categories = FakeCategoryStore("Food", "Transport", "Bills", "Shopping")
    private val suggester = MostUsedSuggester(categories)
    private val transaction = (MpesaParser.parse(
        "TL1A2B3C4D Confirmed. Ksh1,100.00 sent to ALICE  MWANGI 0700000001 on 4/12/25 at 8:07 AM. " +
            "New M-PESA balance is Ksh25,000.00. Transaction cost, Ksh23.00."
    ) as ParseResult.Parsed).transaction

    private fun suggestedNames(limit: Int = 2) = runBlocking { suggester.suggest(transaction, limit).map { it.name } }

    @Test
    fun `the most-used categories come first`() {
        categories.uses = mapOf(
            categories.named("Bills").id to 2,
            categories.named("Shopping").id to 7,
            categories.named("Food").id to 1,
        )

        assertEquals(listOf("Shopping", "Bills"), suggestedNames())
    }

    @Test
    fun `with no history, categories are offered in the order they were created`() {
        assertEquals(listOf("Food", "Transport"), suggestedNames())
    }

    @Test
    fun `used categories outrank unused ones, which then follow in creation order`() {
        categories.uses = mapOf(categories.named("Shopping").id to 1)

        assertEquals(listOf("Shopping", "Food", "Transport"), suggestedNames(limit = 3))
    }

    @Test
    fun `never suggests more than asked for, or more than exist`() {
        assertEquals(1, suggestedNames(limit = 1).size)
        assertEquals(4, suggestedNames(limit = 10).size)
    }
}
