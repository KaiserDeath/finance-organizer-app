package pe.moneyflow.core.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import pe.moneyflow.core.domain.model.CategorySpend
import pe.moneyflow.core.domain.model.DashboardData
import pe.moneyflow.core.domain.repository.CategoryRepository
import pe.moneyflow.core.domain.repository.SettingsRepository
import pe.moneyflow.core.domain.repository.TransactionRepository
import pe.moneyflow.core.model.TransactionType
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Computes the dashboard snapshot for the current calendar month by combining the month's
 * transactions, the full recent list, categories and user preferences.
 */
class GetDashboardUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<DashboardData> {
        val today = LocalDate.now(clock)
        val monthStart = today.withDayOfMonth(1)
        val monthEnd = today.withDayOfMonth(today.lengthOfMonth())

        return combine(
            transactionRepository.observeBetween(monthStart, monthEnd),
            transactionRepository.observeAll(),
            categoryRepository.observeAll(),
            settingsRepository.preferences,
        ) { monthTx, allTx, categories, prefs ->
            val expenses = monthTx.filter { it.type == TransactionType.EXPENSE }
            val monthSpent = expenses.sumOf { it.amountMinor }
            val monthIncome = monthTx
                .filter { it.type == TransactionType.INCOME }
                .sumOf { it.amountMinor }
            val todaySpent = expenses
                .filter { it.effectiveDate == today }
                .sumOf { it.amountMinor }

            val breakdown = expenses
                .groupBy { it.categoryId }
                .mapNotNull { (categoryId, list) ->
                    val category = categories.firstOrNull { it.id == categoryId }
                        ?: return@mapNotNull null
                    val amount = list.sumOf { it.amountMinor }
                    CategorySpend(
                        category = category,
                        amountMinor = amount,
                        fraction = if (monthSpent > 0) amount.toFloat() / monthSpent else 0f,
                    )
                }
                .sortedByDescending { it.amountMinor }

            DashboardData(
                monthSpentMinor = monthSpent,
                todaySpentMinor = todaySpent,
                monthIncomeMinor = monthIncome,
                monthTransactionCount = monthTx.size,
                remainingBudgetMinor = null,
                recent = allTx.sortedByDescending { it.createdAt }.take(6),
                categoryBreakdown = breakdown,
                categoriesById = categories.associateBy { it.id },
                currencyCode = prefs.currencyCode,
            )
        }
    }
}
