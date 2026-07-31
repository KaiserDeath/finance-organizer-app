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
import java.time.YearMonth
import javax.inject.Inject

/**
 * Computes the dashboard snapshot for [month] by combining that month's transactions, the full recent
 * list, categories and user preferences.
 *
 * Takes an explicit [month] rather than always reading "now". The dashboard previously hardcoded the
 * current month, so "how did last month go?" — a top-three question for an expense app — had no answer
 * anywhere in the UI. It also fixes a subtler issue: `today` used to be captured once when the flow was
 * built, so a session left open across midnight kept reporting the old day's spend.
 */
class GetDashboardUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock,
) {
    operator fun invoke(month: YearMonth = YearMonth.now(clock)): Flow<DashboardData> {
        val monthStart = month.atDay(1)
        val monthEnd = month.atEndOfMonth()
        val previous = month.minusMonths(1)

        return combine(
            transactionRepository.observeBetween(monthStart, monthEnd),
            transactionRepository.observeBetween(previous.atDay(1), previous.atEndOfMonth()),
            transactionRepository.observeAll(),
            categoryRepository.observeAll(),
            settingsRepository.preferences,
        ) { monthTx, previousTx, allTx, categories, prefs ->
            val today = LocalDate.now(clock)
            val expenses = monthTx.filter { it.type == TransactionType.EXPENSE }
            val monthSpent = expenses.sumOf { it.amountMinor }
            val monthIncome = monthTx
                .filter { it.type == TransactionType.INCOME }
                .sumOf { it.amountMinor }
            // "Today" only exists inside the month being viewed; a past month has no today.
            val todaySpent = if (YearMonth.from(today) == month) {
                expenses.filter { it.effectiveDate == today }.sumOf { it.amountMinor }
            } else {
                0L
            }

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
                month = month,
                monthSpentMinor = monthSpent,
                todaySpentMinor = todaySpent,
                monthIncomeMinor = monthIncome,
                monthTransactionCount = monthTx.size,
                previousMonthSpentMinor = previousTx
                    .filter { it.type == TransactionType.EXPENSE }
                    .sumOf { it.amountMinor },
                largestExpense = expenses.maxByOrNull { it.amountMinor },
                recent = allTx.sortedByDescending { it.createdAt }.take(6),
                categoryBreakdown = breakdown,
                categoriesById = categories.associateBy { it.id },
                currencyCode = prefs.currencyCode,
            )
        }
    }
}
