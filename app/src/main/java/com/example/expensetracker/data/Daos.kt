package com.example.expensetracker.data

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
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

    /** Adds or replaces a manual entry. M-Pesa rows only ever go through [insert]. */
    @Upsert
    suspend fun upsert(transaction: TransactionEntity)

    /** Deletes a manual entry; the type check makes it impossible to delete a parsed M-Pesa transaction. */
    @Query("DELETE FROM transactions WHERE code = :code AND type = 'MANUAL'")
    suspend fun deleteManual(code: String): Int

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
    /** IGNORE keeps an existing row as it is, including whether it was dismissed. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(message: UnparsedMessageEntity)

    @Query("SELECT * FROM unparsed_messages WHERE dismissed = 0 ORDER BY receivedAt DESC")
    fun observeWaiting(): Flow<List<UnparsedMessageEntity>>

    @Query("UPDATE unparsed_messages SET dismissed = 1 WHERE code = :code")
    suspend fun dismiss(code: String)
}
