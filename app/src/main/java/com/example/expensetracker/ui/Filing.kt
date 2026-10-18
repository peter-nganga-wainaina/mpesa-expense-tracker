package com.example.expensetracker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.example.expensetracker.AppContainer
import com.example.expensetracker.categorize.Category
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** What a category picker does when the user chooses: the same from the queue and from History. */
class Filing(private val container: AppContainer, private val scope: CoroutineScope) {

    fun fileUnder(transactionCode: String, category: Category) {
        scope.launch { container.categorizer.categorize(transactionCode, category.id) }
    }

    /** Creates the category, then files under it. Validation already rejected duplicates in the dialog. */
    fun fileUnderNew(transactionCode: String, categoryName: String) {
        scope.launch {
            val category = container.categories.add(categoryName) ?: return@launch
            container.categorizer.categorize(transactionCode, category.id)
        }
    }
}

@Composable
fun rememberFiling(container: AppContainer): Filing {
    val scope = rememberCoroutineScope()
    return remember(container, scope) { Filing(container, scope) }
}
