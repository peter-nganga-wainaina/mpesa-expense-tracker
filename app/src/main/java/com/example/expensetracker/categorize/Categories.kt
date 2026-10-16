package com.example.expensetracker.categorize

data class Category(val id: Long, val name: String)

/** The user's categories and how often each has been used. */
interface CategoryStore {
    /** All categories, in the order they were created. */
    suspend fun categories(): List<Category>

    /** Number of transactions filed under each category ID; unused categories are absent. */
    suspend fun useCounts(): Map<Long, Int>

    /** Adds a category, or returns null if one with this name already exists (ignoring case). */
    suspend fun add(name: String): Category?
}

sealed interface NewCategoryName {
    data class Valid(val name: String) : NewCategoryName
    data object Blank : NewCategoryName
    data class Duplicate(val existing: Category) : NewCategoryName
}

/** Checks a typed category name: tidies its spacing and rejects blanks and case-insensitive repeats. */
fun validateNewCategoryName(input: String, existing: List<Category>): NewCategoryName {
    val name = input.trim().replace(Regex("""\s+"""), " ")
    if (name.isEmpty()) return NewCategoryName.Blank
    existing.firstOrNull { it.name.equals(name, ignoreCase = true) }?.let { return NewCategoryName.Duplicate(it) }
    return NewCategoryName.Valid(name)
}
