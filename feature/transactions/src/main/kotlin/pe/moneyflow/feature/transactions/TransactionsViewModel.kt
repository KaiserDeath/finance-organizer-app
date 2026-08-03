package pe.moneyflow.feature.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.moneyflow.core.domain.model.TransactionFilter
import pe.moneyflow.core.domain.usecase.DeleteTransactionUseCase
import pe.moneyflow.core.domain.usecase.FilterTransactionsUseCase
import pe.moneyflow.core.domain.usecase.GetTransactionUseCase
import pe.moneyflow.core.domain.usecase.ObserveCategoriesUseCase
import pe.moneyflow.core.domain.usecase.ObserveTransactionsUseCase
import pe.moneyflow.core.domain.usecase.SaveTransactionUseCase
import pe.moneyflow.core.model.Category
import pe.moneyflow.core.model.Transaction
import pe.moneyflow.core.model.TransactionType
import pe.moneyflow.core.ui.util.toRelativeLabel
import java.time.LocalDate
import javax.inject.Inject

data class TransactionSection(
    val dateLabel: String,
    val items: List<Transaction>,
    val expenseTotalMinor: Long,
)

data class TransactionsUiState(
    val isLoading: Boolean = true,
    val sections: List<TransactionSection> = emptyList(),
    val categoriesById: Map<String, Category> = emptyMap(),
    val currencyCode: String = "PEN",
    val filter: TransactionFilter = TransactionFilter(),
    val expenseCategories: List<Category> = emptyList(),
) {
    val isEmpty: Boolean get() = !isLoading && sections.isEmpty()
    val isFilterActive: Boolean get() = filter.isActive
}

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeTransactions: ObserveTransactionsUseCase,
    observeCategories: ObserveCategoriesUseCase,
    private val filterTransactions: FilterTransactionsUseCase,
    private val getTransaction: GetTransactionUseCase,
    private val deleteTransaction: DeleteTransactionUseCase,
    private val saveTransaction: SaveTransactionUseCase,
) : ViewModel() {

    private var recentlyDeleted: Transaction? = null

    private val filterState = MutableStateFlow(
        TransactionFilter(
            categoryIds = setOfNotNull(savedStateHandle.toRoute<TransactionsRoute>().categoryId),
        ),
    )

    val uiState: StateFlow<TransactionsUiState> =
        combine(
            observeTransactions(),
            observeCategories(),
            filterState,
        ) { transactions, categories, filter ->
            val filtered = filterTransactions(transactions, filter)
            TransactionsUiState(
                isLoading = false,
                sections = buildSections(filtered),
                categoriesById = categories.associateBy { it.id },
                currencyCode = transactions.firstOrNull()?.currencyCode ?: "PEN",
                filter = filter,
                expenseCategories = categories,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TransactionsUiState(),
        )

    fun onQueryChange(query: String) = filterState.update { it.copy(query = query) }

    fun toggleType(type: TransactionType) = filterState.update { current ->
        current.copy(
            types = if (type in current.types) current.types - type else current.types + type,
        )
    }

    fun toggleCategory(categoryId: String) = filterState.update { current ->
        current.copy(
            categoryIds = if (categoryId in current.categoryIds) {
                current.categoryIds - categoryId
            } else {
                current.categoryIds + categoryId
            },
        )
    }

    /**
     * Drops every category constraint — what the "Todas" chip means.
     *
     * The screen used to express this by iterating the current selection and calling
     * [toggleCategory] on each id. That happened to work, because the set it iterated was an
     * immutable snapshot from the last emission rather than the live one, so nothing was being
     * mutated underneath the loop. But it read as though it were, and it made a single intent —
     * "no category filter" — depend on that detail holding. One update, once.
     *
     * Distinct from [clearFilters], which also drops the query and the type filters; the chip row
     * has no business resetting the search box above it.
     */
    fun clearCategories() = filterState.update { it.copy(categoryIds = emptySet()) }

    fun clearFilters() = filterState.update { TransactionFilter() }

    fun delete(id: String) {
        viewModelScope.launch {
            recentlyDeleted = getTransaction(id)
            deleteTransaction(id)
        }
    }

    fun undoDelete() {
        val toRestore = recentlyDeleted ?: return
        recentlyDeleted = null
        viewModelScope.launch { saveTransaction(toRestore) }
    }

    private fun buildSections(transactions: List<Transaction>): List<TransactionSection> =
        transactions
            .sortedByDescending { it.effectiveDate ?: LocalDate.MIN }
            .groupBy { it.effectiveDate }
            .map { (date, items) ->
                TransactionSection(
                    dateLabel = date?.toRelativeLabel() ?: "Sin fecha",
                    items = items,
                    expenseTotalMinor = items
                        .filter { it.type == TransactionType.EXPENSE }
                        .sumOf { it.amountMinor },
                )
            }
}
