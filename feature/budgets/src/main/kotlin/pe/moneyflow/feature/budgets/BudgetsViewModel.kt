package pe.moneyflow.feature.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.moneyflow.core.domain.model.BudgetProgress
import pe.moneyflow.core.domain.usecase.DeleteBudgetUseCase
import pe.moneyflow.core.domain.usecase.GetBudgetsProgressUseCase
import pe.moneyflow.core.domain.usecase.ObserveCategoriesUseCase
import pe.moneyflow.core.domain.usecase.SaveBudgetUseCase
import pe.moneyflow.core.model.Budget
import pe.moneyflow.core.model.BudgetPeriod
import pe.moneyflow.core.model.Category
import pe.moneyflow.core.model.CategoryType
import java.util.UUID
import javax.inject.Inject

data class BudgetsUiState(
    val isLoading: Boolean = true,
    val items: List<BudgetProgress> = emptyList(),
    val expenseCategories: List<Category> = emptyList(),
    val currencyCode: String = "PEN",
) {
    val totalLimitMinor: Long get() = items.sumOf { it.budget.amountMinor }
    val totalSpentMinor: Long get() = items.sumOf { it.spentMinor }
    val isEmpty: Boolean get() = !isLoading && items.isEmpty()
}

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    getBudgetsProgress: GetBudgetsProgressUseCase,
    observeCategories: ObserveCategoriesUseCase,
    private val saveBudget: SaveBudgetUseCase,
    private val deleteBudget: DeleteBudgetUseCase,
) : ViewModel() {

    val uiState: StateFlow<BudgetsUiState> =
        combine(getBudgetsProgress(), observeCategories()) { progress, categories ->
            BudgetsUiState(
                isLoading = false,
                items = progress,
                expenseCategories = categories.filter { it.type == CategoryType.EXPENSE },
                currencyCode = progress.firstOrNull()?.currencyCode ?: "PEN",
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BudgetsUiState(),
        )

    fun add(name: String, categoryId: String?, amountMinor: Long, period: BudgetPeriod) {
        if (name.isBlank() || amountMinor <= 0) return
        viewModelScope.launch {
            saveBudget(
                Budget(
                    id = UUID.randomUUID().toString(),
                    name = name.trim(),
                    categoryId = categoryId,
                    amountMinor = amountMinor,
                    period = period,
                ),
            )
        }
    }

    fun delete(id: String) {
        viewModelScope.launch { deleteBudget(id) }
    }
}
