package com.example.expensetracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TransactionEntity::class, CategoryEntity::class, UnparsedMessageEntity::class],
    version = 1,
    // Exported schemas (app/schemas/) are what future migrations are written and tested against;
    // once the app is installed for real, its data must survive upgrades.
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactions(): TransactionDao
    abstract fun categories(): CategoryDao
    abstract fun unparsedMessages(): UnparsedMessageDao

    companion object {
        /** A starting set the user can add to; nothing in the code depends on these names. */
        val DEFAULT_CATEGORIES = listOf("Food", "Transport", "Bills", "Shopping", "Savings", "Other")

        @Volatile private var instance: AppDatabase? = null

        // One instance per process: the receiver and the UI share it.
        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "expensetracker.db")
                .addCallback(SeedDefaultCategories)
                .build()
                .also { instance = it }
        }
    }

    private object SeedDefaultCategories : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            DEFAULT_CATEGORIES.forEach { db.execSQL("INSERT INTO categories (name) VALUES (?)", arrayOf(it)) }
        }
    }
}
