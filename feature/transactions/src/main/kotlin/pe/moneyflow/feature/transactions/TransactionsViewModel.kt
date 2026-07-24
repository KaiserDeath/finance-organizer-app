package pe.moneyflow.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.moneyflow.core.domain.usecase.DeleteTransactionUseCase
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
) {
    val isEmpty: Boolean get() = !isLoading && sections.isEmpty()
}

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    observeTransactions: ObserveTransactionsUseCase,
    observeCategories: ObserveCategoriesUseCase,
    private val getTransaction: GetTransactionUseCase,
    private val deleteTransaction: DeleteTransactionUseCase,
    private val saveTransaction: SaveTransactionUseCase,
) : ViewModel() {

    private var recentlyDeleted: Transaction? = null

    val uiState: StateFlow<TransactionsUiState> =
        combine(observeTransactions(), observeCategories()) { transactions, categories ->
            TransactionsUiState(
                isLoading = false,
                sections = buildSections(transactions),
                categoriesById = categories.associateBy { it.id },
                currencyCode = transactions.firstOrNull()?.currencyCode ?: "PEN",
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TransactionsUiState(),
        )

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
