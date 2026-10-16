package com.example.expensetracker.data

import com.example.expensetracker.categorize.Category
import com.example.expensetracker.categorize.CategoryStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomCategoryStore(private val db: AppDatabase) : CategoryStore {

    override suspend fun categories(): List<Category> = db.categories().getAll().map { it.toCategory() }

    override suspend fun useCounts(): Map<Long, Int> =
        db.transactions().categoryUses().associate { it.categoryId to it.uses }

    override suspend fun add(name: String): Category? =
        db.categories().insert(CategoryEntity(name = name)).takeIf { it != -1L }?.let { Category(it, name) }

    /** All categories by name, updating as new ones are added. */
    fun observeAll(): Flow<List<Category>> = db.categories().observeAll().map { rows -> rows.map { it.toCategory() } }

    private fun CategoryEntity.toCategory() = Category(id, name)
}
