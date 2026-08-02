package pe.moneyflow.feature.budgets

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.moneyflow.core.domain.model.BudgetProgress
import pe.moneyflow.core.domain.repository.SettingsRepository
import pe.moneyflow.core.domain.usecase.DeleteBudgetUseCase
import pe.moneyflow.core.domain.usecase.GetBudgetsProgressUseCase
import pe.moneyflow.core.domain.usecase.ObserveCategoriesUseCase
import pe.moneyflow.core.domain.usecase.ObserveTransactionsUseCase
import pe.moneyflow.core.domain.usecase.SaveBudgetUseCase
import pe.moneyflow.core.model.Budget
import pe.moneyflow.core.model.BudgetPeriod
import pe.moneyflow.core.model.Category
import pe.moneyflow.core.model.CategoryType
import pe.moneyflow.core.model.TransactionStatus
import pe.moneyflow.core.model.TransactionType
import java.time.Clock
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject

data class BudgetsUiState(
    val isLoading: Boolean = true,
    val items: List<BudgetProgress> = emptyList(),
    val expenseCategories: List<Category> = emptyList(),
    val currencyCode: String = "PEN",
    /** The month-level budget set during onboarding; null when it was skipped. */
    val monthlyBudgetMinor: Long? = null,
    /** Everything paid this month, whether or not a budget covers it. */
    val monthSpentMinor: Long = 0,
    /** Of [monthSpentMinor], the part no category budget accounts for. */
    val unbudgetedSpentMinor: Long = 0,
    val month: YearMonth = YearMonth.now(),
) {
    /** Sum of the category limits — how much of the month has actually been allocated. */
    val totalLimitMinor: Long get() = items.sumOf { it.budget.amountMinor }
    val totalSpentMinor: Long get() = items.sumOf { it.spentMinor }
    val isEmpty: Boolean get() = !isLoading && items.isEmpty()

    /**
     * Month budget not yet handed to any category. Floored at zero: over-allocating is a legitimate
     * choice (the limits are per-category caps, not slices of a fixed pie), and rendering it as a
     * negative "unassigned" would read as an error rather than a decision.
     */
    val unassignedMinor: Long?
        get() = monthlyBudgetMinor?.let { (it - totalLimitMinor).coerceAtLeast(0) }

    /** The roll-up only says something once there is a month budget to divide up. */
    val hasRollup: Boolean get() = !isLoading && monthlyBudgetMinor != null
}

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getBudgetsProgress: GetBudgetsProgressUseCase,
    observeCategories: ObserveCategoriesUseCase,
    observeTransactions: ObserveTransactionsUseCase,
    private val settingsRepository: SettingsRepository,
    private val saveBudget: SaveBudgetUseCase,
    private val deleteBudget: DeleteBudgetUseCase,
    private val clock: Clock,
) : ViewModel() {

    /** Budget to open in the editor on arrival ("Ajustar el límite" from Análisis). */
    val initialEditBudgetId: String? =
        savedStateHandle.toRoute<BudgetsRoute>().editBudgetId

    /** The budget as it was before the last edit, for the snackbar's deshacer. */
    private var editedPrevious: Budget? = null

    /** Same idea for the month budget, whose "clear" is destructive enough to need a way back. */
    private var previousMonthlyBudget: Long? = null

    val uiState: StateFlow<BudgetsUiState> =
        combine(
            getBudgetsProgress(),
            observeCategories(),
            observeTransactions(),
            settingsRepository.preferences,
        ) { progress, categories, transactions, prefs ->
            val month = YearMonth.now(clock)
            val paidThisMonth = transactions.filter {
                it.type == TransactionType.EXPENSE &&
                    it.status == TransactionStatus.PAID &&
                    it.effectiveDate?.let { d -> YearMonth.from(d) == month } == true
            }
            // "Covered" is by category, not by amount: a category with a budget is accounted for
            // even when it blew past its limit. What this figure answers is "which spending has no
            // limit watching it at all", which is a different question from "who overspent".
            val budgetedCategoryIds = progress.mapNotNull { it.budget.categoryId }.toSet()
            val monthSpent = paidThisMonth.sumOf { it.amountMinor }
            val unbudgeted = paidThisMonth
                .filter { it.categoryId == null || it.categoryId !in budgetedCategoryIds }
                .sumOf { it.amountMinor }

            BudgetsUiState(
                isLoading = false,
                items = progress,
                expenseCategories = categories.filter { it.type == CategoryType.EXPENSE },
                currencyCode = progress.firstOrNull()?.currencyCode ?: prefs.currencyCode,
                monthlyBudgetMinor = prefs.monthlyBudgetMinor,
                monthSpentMinor = monthSpent,
                unbudgetedSpentMinor = unbudgeted,
                month = month,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BudgetsUiState(),
        )

    /** Creates when [existing] is null, updates otherwise (stashing the previous for [undoEdit]). */
    fun save(
        existing: Budget?,
        name: String,
        categoryId: String?,
        amountMinor: Long,
        period: BudgetPeriod,
    ) {
        if (name.isBlank() || amountMinor <= 0) return
        editedPrevious = existing
        viewModelScope.launch {
            saveBudget(
                Budget(
                    id = existing?.id ?: UUID.randomUUID().toString(),
                    name = name.trim(),
                    categoryId = categoryId,
                    amountMinor = amountMinor,
                    period = period,
                ),
            )
        }
    }

    fun undoEdit() {
        val previous = editedPrevious ?: return
        editedPrevious = null
        viewModelScope.launch { saveBudget(previous) }
    }

    fun delete(id: String) {
        viewModelScope.launch { deleteBudget(id) }
    }

    /**
     * Sets the month budget, or clears it with a null [minor].
     *
     * This is the only way to change it after onboarding. It used to be the *only* way full stop —
     * `setMonthlyBudget` had a single caller in `OnboardingViewModel`, so skipping that step left
     * the value null permanently, and with it the hero's denominator, the daily allowance and this
     * screen's roll-up. All three simply never appeared, with nothing explaining why.
     */
    fun setMonthlyBudget(minor: Long?) {
        previousMonthlyBudget = uiState.value.monthlyBudgetMinor
        viewModelScope.launch { settingsRepository.setMonthlyBudget(minor) }
    }

    fun undoMonthlyBudget() {
        val previous = previousMonthlyBudget
        previousMonthlyBudget = null
        viewModelScope.launch { settingsRepository.setMonthlyBudget(previous) }
    }
}
