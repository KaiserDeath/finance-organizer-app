package pe.moneyflow.core.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import pe.moneyflow.core.domain.model.AnalyticsData
import pe.moneyflow.core.domain.model.CategorySpend
import pe.moneyflow.core.domain.model.MonthlyPoint
import pe.moneyflow.core.domain.model.WeekdaySpend
import pe.moneyflow.core.domain.repository.CategoryRepository
import pe.moneyflow.core.domain.repository.SettingsRepository
import pe.moneyflow.core.domain.repository.TransactionRepository
import pe.moneyflow.core.model.TransactionType
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Aggregates the last [DEFAULT_MONTHS] calendar months of transactions into the trends,
 * category and weekday breakdowns the Analytics screen renders. Only PAID/actual money
 * (transactions with an [pe.moneyflow.core.model.Transaction.effectiveDate]) is counted.
 */
class GetAnalyticsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock,
) {
    operator fun invoke(monthsBack: Int = DEFAULT_MONTHS): Flow<AnalyticsData> {
        val today = LocalDate.now(clock)
        val currentMonth = YearMonth.from(today)
        val windowMonths = (monthsBack - 1 downTo 0).map { currentMonth.minusMonths(it.toLong()) }
        val rangeStart = windowMonths.first().atDay(1)

        return combine(
            transactionRepository.observeBetween(rangeStart, today),
            categoryRepository.observeAll(),
            settingsRepository.preferences,
        ) { transactions, categories, prefs ->
            val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
            val incomes = transactions.filter { it.type == TransactionType.INCOME }

            val expenseByMonth = expenses.groupBy { YearMonth.from(it.effectiveDate) }
            val incomeByMonth = incomes.groupBy { YearMonth.from(it.effectiveDate) }
            val months = windowMonths.map { month ->
                MonthlyPoint(
                    month = month,
                    expenseMinor = expenseByMonth[month].orEmptySum(),
                    incomeMinor = incomeByMonth[month].orEmptySum(),
                )
            }

            val totalExpense = expenses.sumOf { it.amountMinor }
            val breakdown = expenses
                .groupBy { it.categoryId }
                .mapNotNull { (categoryId, list) ->
                    val category = categories.firstOrNull { it.id == categoryId }
                        ?: return@mapNotNull null
                    val amount = list.sumOf { it.amountMinor }
                    CategorySpend(
                        category = category,
                        amountMinor = amount,
                        fraction = if (totalExpense > 0) amount.toFloat() / totalExpense else 0f,
                    )
                }
                .sortedByDescending { it.amountMinor }

            val weekdaysByDay = expenses.groupBy { it.effectiveDate?.dayOfWeek }
            val weekdays = DayOfWeek.entries.map { day ->
                val list = weekdaysByDay[day].orEmpty()
                WeekdaySpend(
                    dayOfWeek = day,
                    amountMinor = list.sumOf { it.amountMinor },
                    count = list.size,
                )
            }

            // Days covered: from the first day of the window to today, inclusive.
            val daysCovered = ChronoUnit.DAYS.between(rangeStart, today) + 1
            val avgDaily = if (daysCovered > 0) totalExpense / daysCovered else 0L

            AnalyticsData(
                months = months,
                categoryBreakdown = breakdown,
                weekdays = weekdays,
                totalExpenseMinor = totalExpense,
                totalIncomeMinor = incomes.sumOf { it.amountMinor },
                avgDailyExpenseMinor = avgDaily,
                categoriesById = categories.associateBy { it.id },
                currencyCode = prefs.currencyCode,
            )
        }
    }

    private fun List<pe.moneyflow.core.model.Transaction>?.orEmptySum(): Long =
        this?.sumOf { it.amountMinor } ?: 0L

    companion object {
        const val DEFAULT_MONTHS = 6
    }
}
