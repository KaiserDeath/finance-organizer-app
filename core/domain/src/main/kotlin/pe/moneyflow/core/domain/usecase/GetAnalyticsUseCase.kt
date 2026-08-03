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
import pe.moneyflow.core.model.TransactionStatus
import pe.moneyflow.core.model.TransactionType
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * Aggregates transactions into everything the Analytics screen renders. Only PAID/actual money
 * (transactions with an [pe.moneyflow.core.model.Transaction.effectiveDate]) is counted.
 *
 * Reads the last [DEFAULT_MONTHS] calendar months once, then serves **two windows** from it: the
 * monthly trend and the weekday profile span the whole window, while the total, the category
 * breakdown and the uncategorized remainder describe the current month alone. See [AnalyticsData]
 * for why the month fields carry their scope in their names.
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
            // PAID only, matching the dashboard: the "total del mes" is what was spent, and the
            // donut here must agree with the hero there. Pending money is shown as committed.
            val expenses = transactions.filter {
                it.type == TransactionType.EXPENSE && it.status == TransactionStatus.PAID
            }
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

            // The month figures are scoped to the *current* month, not the window. The screen
            // labels them as the month and the hero on Inicio prints the same idea, so a
            // window-wide sum here was six months of spending wearing a one-month label — and it
            // could never match the hero no matter how the two were reconciled.
            //
            // `GetDashboardUseCase.monthSpent` applies the identical filter (EXPENSE + PAID) over
            // the identical month, so the two agree by construction rather than by coincidence.
            val monthExpenses = expenses.filter { YearMonth.from(it.effectiveDate) == currentMonth }
            val monthExpense = monthExpenses.sumOf { it.amountMinor }

            val monthBreakdown = monthExpenses
                .groupBy { it.categoryId }
                .mapNotNull { (categoryId, list) ->
                    val category = categories.firstOrNull { it.id == categoryId }
                        ?: return@mapNotNull null
                    val amount = list.sumOf { it.amountMinor }
                    CategorySpend(
                        category = category,
                        amountMinor = amount,
                        // Over the month total, not over the categorized subtotal: when some
                        // spending has no category the fractions must sum to less than 1, which
                        // is what leaves room for the remainder the screen states.
                        fraction = if (monthExpense > 0) amount.toFloat() / monthExpense else 0f,
                    )
                }
                .sortedByDescending { it.amountMinor }

            // Whatever the breakdown could not claim. The `mapNotNull` above drops two cases — no
            // category at all, and a category id whose row is gone — and both used to vanish
            // silently, which is why the donut centre disagreed with the total above it.
            val monthUncategorized = monthExpense - monthBreakdown.sumOf { it.amountMinor }

            val weekdaysByDay = expenses.groupBy { it.effectiveDate?.dayOfWeek }
            val weekdays = DayOfWeek.entries.map { day ->
                val list = weekdaysByDay[day].orEmpty()
                WeekdaySpend(
                    dayOfWeek = day,
                    amountMinor = list.sumOf { it.amountMinor },
                    count = list.size,
                )
            }

            AnalyticsData(
                months = months,
                monthCategoryBreakdown = monthBreakdown,
                weekdays = weekdays,
                monthExpenseMinor = monthExpense,
                monthIncomeMinor = incomes
                    .filter { YearMonth.from(it.effectiveDate) == currentMonth }
                    .sumOf { it.amountMinor },
                monthUncategorizedMinor = monthUncategorized,
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
