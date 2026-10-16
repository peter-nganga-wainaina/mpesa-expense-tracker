package com.example.expensetracker.data

import com.example.expensetracker.categorize.Category
import com.example.expensetracker.categorize.CategoryStore
import com.example.expensetracker.suggest.MerchantHistory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomCategoryStore(private val db: AppDatabase) : CategoryStore, MerchantHistory {

    override suspend fun categories(): List<Category> = db.categories().getAll().map { it.toCategory() }

    override suspend fun useCounts(): Map<Long, Int> =
        db.transactions().categoryUses().associate { it.categoryId to it.uses }

    override suspend fun add(name: String): Category? =
        db.categories().insert(CategoryEntity(name = name)).takeIf { it != -1L }?.let { Category(it, name) }

    override suspend fun lastCategoryFor(counterparty: String): Category? =
        db.categories().lastUsedFor(counterparty)?.toCategory()

    /** All categories by name, updating as new ones are added. */
    fun observeAll(): Flow<List<Category>> = db.categories().observeAll().map { rows -> rows.map { it.toCategory() } }

    private fun CategoryEntity.toCategory() = Category(id, name)
}
