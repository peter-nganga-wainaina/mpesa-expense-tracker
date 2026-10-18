package com.example.expensetracker.data

import androidx.room.Dao
import androidx.room.Embedded
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

    /** Every transaction with its category's name (null if uncategorized), newest first. */
    @Query(
        """
        SELECT transactions.*, categories.name AS categoryName FROM transactions
        LEFT JOIN categories ON categories.id = transactions.categoryId
        ORDER BY transactions.occurredAt DESC
        """
    )
    fun observeHistory(): Flow<List<TransactionWithCategory>>

    @Query("UPDATE transactions SET categoryId = :categoryId WHERE code = :code")
    suspend fun setCategory(code: String, categoryId: Long)

    @Query("SELECT categoryId, COUNT(*) AS uses FROM transactions WHERE categoryId IS NOT NULL GROUP BY categoryId")
    suspend fun categoryUses(): List<CategoryUse>
}

data class CategoryUse(val categoryId: Long, val uses: Int)

data class TransactionWithCategory(@Embedded val transaction: TransactionEntity, val categoryName: String?)

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY id")
    suspend fun getAll(): List<CategoryEntity>

    /** Category of the most recent categorized transaction with this counterparty, ignoring case. */
    @Query(
        """
        SELECT categories.* FROM transactions
        JOIN categories ON categories.id = transactions.categoryId
        WHERE transactions.counterparty = :counterparty COLLATE NOCASE
        ORDER BY transactions.occurredAt DESC
        LIMIT 1
        """
    )
    suspend fun lastUsedFor(counterparty: String): CategoryEntity?

    /** Returns the new ID, or -1 when a category with this name already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: CategoryEntity): Long
}

@Dao
interface UnparsedMessageDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(message: UnparsedMessageEntity)
}
