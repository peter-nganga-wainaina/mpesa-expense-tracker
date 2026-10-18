package com.example.expensetracker.manual

/** Where manual entries are kept: alongside M-Pesa transactions, so History and Insights include them. */
interface ManualEntryStore {
    /** Adds a new entry, or replaces the one with the same code (an edit). */
    suspend fun save(entry: ManualEntry)

    /** Removes a manual entry. M-Pesa transactions can't be deleted. */
    suspend fun delete(code: String)
}
