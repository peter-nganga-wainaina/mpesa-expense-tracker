package com.example.expensetracker.categorize

import org.junit.Assert.assertEquals
import org.junit.Test

class NewCategoryNameTest {

    private val existing = listOf(Category(1, "Food"), Category(2, "School fees"))

    @Test
    fun `a new name is accepted`() {
        assertEquals(NewCategoryName.Valid("Health"), validateNewCategoryName("Health", existing))
    }

    @Test
    fun `surrounding and repeated spaces are tidied`() {
        assertEquals(NewCategoryName.Valid("Eating out"), validateNewCategoryName("  Eating   out ", existing))
    }

    @Test
    fun `an empty or all-space name is rejected`() {
        assertEquals(NewCategoryName.Blank, validateNewCategoryName("", existing))
        assertEquals(NewCategoryName.Blank, validateNewCategoryName("   ", existing))
    }

    @Test
    fun `a name matching an existing category in any case is a duplicate`() {
        assertEquals(NewCategoryName.Duplicate(Category(1, "Food")), validateNewCategoryName("food", existing))
        assertEquals(
            NewCategoryName.Duplicate(Category(2, "School fees")),
            validateNewCategoryName(" SCHOOL  FEES ", existing),
        )
    }
}
