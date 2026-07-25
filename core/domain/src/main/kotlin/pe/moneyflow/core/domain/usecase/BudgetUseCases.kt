package pe.moneyflow.core.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import pe.moneyflow.core.domain.model.BudgetProgress
import pe.moneyflow.core.domain.repository.BudgetRepository
import pe.moneyflow.core.domain.repository.CategoryRepository
import pe.moneyflow.core.domain.repository.SettingsRepository
import pe.moneyflow.core.domain.repository.TransactionRepository
import pe.moneyflow.core.model.Budget
import pe.moneyflow.core.model.BudgetPeriod
import pe.moneyflow.core.model.TransactionType
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

class SaveBudgetUseCase @Inject constructor(
    private val repository: BudgetRepository,
) {
    suspend operator fun invoke(budget: Budget) = repository.upsert(budget)
}

class DeleteBudgetUseCase @Inject constructor(
    private val repository: BudgetRepository,
) {
    suspend operator fun invoke(id: String) = repository.delete(id)
}

/**
 * Streams every budget with its spend for the current period computed from transactions,
 * so the UI can show progress bars, remaining amounts and over-limit warnings.
 */
class GetBudgetsProgressUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<List<BudgetProgress>> = combine(
        budgetRepository.observeAll(),
        transactionRepository.observeAll(),
        categoryRepository.observeAll(),
        settingsRepository.preferences,
    ) { budgets, transactions, categories, prefs ->
        val today = LocalDate.now(clock)
        budgets.map { budget ->
            val (start, end) = periodWindow(budget.period, today)
            val spent = transactions
                .filter { tx ->
                    tx.type == TransactionType.EXPENSE &&
                        (budget.categoryId == null || tx.categoryId == budget.categoryId) &&
                        tx.effectiveDate?.let { it in start..end } == true
                }
                .sumOf { it.amountMinor }
            BudgetProgress(
                budget = budget,
                category = categories.firstOrNull { it.id == budget.categoryId },
                spentMinor = spent,
                currencyCode = prefs.currencyCode,
            )
        }
    }

    private fun periodWindow(period: BudgetPeriod, today: LocalDate): Pair<LocalDate, LocalDate> =
        when (period) {
            BudgetPeriod.WEEKLY -> {
                val start = today.minusDays((today.dayOfWeek.value - 1).toLong())
                start to start.plusDays(6)
            }

            BudgetPeriod.MONTHLY ->
                today.withDayOfMonth(1) to today.withDayOfMonth(today.lengthOfMonth())

            BudgetPeriod.YEARLY ->
                today.withDayOfYear(1) to today.withDayOfYear(today.lengthOfYear())
        }
}
