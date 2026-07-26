package pe.moneyflow.core.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import pe.moneyflow.core.domain.model.CategoryDelta
import pe.moneyflow.core.domain.model.MonthlyReport
import pe.moneyflow.core.domain.repository.CategoryRepository
import pe.moneyflow.core.domain.repository.SettingsRepository
import pe.moneyflow.core.domain.repository.TransactionRepository
import pe.moneyflow.core.model.Transaction
import pe.moneyflow.core.model.TransactionType
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * Builds a [MonthlyReport] for the current calendar month, comparing its spend against the
 * previous month both in total and per category.
 */
class GetMonthlyReportUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<MonthlyReport> {
        val today = LocalDate.now(clock)
        val month = YearMonth.from(today)
        val previous = month.minusMonths(1)
        val rangeStart = previous.atDay(1)
        val rangeEnd = month.atEndOfMonth()

        return combine(
            transactionRepository.observeBetween(rangeStart, rangeEnd),
            categoryRepository.observeAll(),
            settingsRepository.preferences,
        ) { transactions, categories, prefs ->
            val current = transactions.filter { YearMonth.from(it.effectiveDate) == month }
            val prior = transactions.filter { YearMonth.from(it.effectiveDate) == previous }

            val currentExpenses = current.filter { it.type == TransactionType.EXPENSE }
            val priorExpenses = prior.filter { it.type == TransactionType.EXPENSE }

            val currentByCategory = currentExpenses.sumByCategory()
            val priorByCategory = priorExpenses.sumByCategory()
            val deltas = (currentByCategory.keys + priorByCategory.keys)
                .mapNotNull { categoryId ->
                    val category = categories.firstOrNull { it.id == categoryId }
                        ?: return@mapNotNull null
                    CategoryDelta(
                        category = category,
                        currentMinor = currentByCategory[categoryId] ?: 0L,
                        previousMinor = priorByCategory[categoryId] ?: 0L,
                    )
                }
                .sortedByDescending { it.currentMinor }

            MonthlyReport(
                month = month,
                currentExpenseMinor = currentExpenses.sumOf { it.amountMinor },
                previousExpenseMinor = priorExpenses.sumOf { it.amountMinor },
                currentIncomeMinor = current.filter { it.type == TransactionType.INCOME }.sumOf { it.amountMinor },
                previousIncomeMinor = prior.filter { it.type == TransactionType.INCOME }.sumOf { it.amountMinor },
                transactionCount = current.size,
                categoryDeltas = deltas,
                currencyCode = prefs.currencyCode,
            )
        }
    }

    private fun List<Transaction>.sumByCategory(): Map<String, Long> =
        filter { it.categoryId != null }
            .groupBy { it.categoryId!! }
            .mapValues { (_, list) -> list.sumOf { it.amountMinor } }
}
