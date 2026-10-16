package com.example.expensetracker.suggest

import com.example.expensetracker.categorize.Category
import com.example.expensetracker.sms.MpesaParser
import com.example.expensetracker.sms.ParseResult
import com.example.expensetracker.testing.FakeCategoryStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChainedSuggesterTest {

    private val food = Category(1, "Food")
    private val transport = Category(2, "Transport")
    private val bills = Category(3, "Bills")
    private val groceries = Category(7, "Groceries")

    private val transaction = (MpesaParser.parse(
        "TP2C3D4E5F Confirmed. Ksh780.00 paid to CARREFOUR. on 13/10/26 at 10:40 PM. " +
            "New M-PESA balance is Ksh23,250.00. Transaction cost, Ksh0.00."
    ) as ParseResult.Parsed).transaction

    private fun returning(vararg picks: Category) = CategorySuggester { _, limit -> picks.toList().take(limit) }

    private fun ChainedSuggester.names(limit: Int = 2) = runBlocking { suggest(transaction, limit).map { it.name } }

    @Test
    fun `earlier sources fill the first slots`() {
        val chain = ChainedSuggester(returning(groceries), returning(food, transport))

        assertEquals(listOf("Groceries", "Food"), chain.names())
    }

    @Test
    fun `later sources fill what earlier ones leave empty`() {
        val chain = ChainedSuggester(returning(), returning(food, transport))

        assertEquals(listOf("Food", "Transport"), chain.names())
    }

    @Test
    fun `a category offered by two sources appears once`() {
        val chain = ChainedSuggester(returning(food), returning(food, transport))

        assertEquals(listOf("Food", "Transport"), chain.names())
    }

    @Test
    fun `never returns more than the limit`() {
        val chain = ChainedSuggester(returning(food, transport, bills), returning(groceries))

        assertEquals(listOf("Food", "Transport"), chain.names(limit = 2))
    }

    @Test
    fun `returns nothing when no source has a suggestion`() {
        assertTrue(ChainedSuggester(returning(), returning()).names().isEmpty())
    }

    @Test
    fun `merchant memory then most-used gives the remembered pick first, then the top other category`() {
        val categories = FakeCategoryStore("Food", "Transport", "Groceries")
        categories.uses = mapOf(categories.named("Groceries").id to 5, categories.named("Food").id to 2)
        val chain = ChainedSuggester(
            MerchantMemorySuggester { if (it == "CARREFOUR") categories.named("Groceries") else null },
            MostUsedSuggester(categories),
        )

        // Groceries is also the most-used, so Food takes the second slot instead of a repeat.
        assertEquals(listOf("Groceries", "Food"), chain.names())
    }
}
