package com.example.expensetracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    /** Returns the new row ID, or -1 when a transaction with this code already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Query("SELECT * FROM transactions WHERE categoryId IS NULL ORDER BY occurredAt DESC")
    fun observeUncategorized(): Flow<List<TransactionEntity>>

    @Query("UPDATE transactions SET categoryId = :categoryId WHERE code = :code")
    suspend fun setCategory(code: String, categoryId: Long)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name")
    fun observeAll(): Flow<List<CategoryEntity>>

    /** Returns the new ID, or -1 when a category with this name already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: CategoryEntity): Long
}

@Dao
interface UnparsedMessageDao {
    @Insert
    suspend fun insert(message: UnparsedMessageEntity)
}
