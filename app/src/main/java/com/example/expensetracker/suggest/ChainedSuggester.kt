package com.example.expensetracker.suggest

import com.example.expensetracker.categorize.Category
import com.example.expensetracker.sms.MpesaTransaction

/**
 * Combines suggesters in priority order: earlier sources fill the first slots, later ones fill
 * whatever is left. A category offered by more than one source appears once, at its first position.
 */
class ChainedSuggester(private vararg val sources: CategorySuggester) : CategorySuggester {

    override suspend fun suggest(transaction: MpesaTransaction, limit: Int): List<Category> {
        val picks = linkedMapOf<Long, Category>()
        for (source in sources) {
            if (picks.size >= limit) break
            // Ask for the full limit: some of a later source's picks may repeat earlier ones.
            source.suggest(transaction, limit).forEach { picks.putIfAbsent(it.id, it) }
        }
        return picks.values.take(limit)
    }
}
