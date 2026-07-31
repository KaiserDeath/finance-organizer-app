package pe.moneyflow.core.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import pe.moneyflow.core.domain.model.CumulativeSpend
import pe.moneyflow.core.domain.repository.SettingsRepository
import pe.moneyflow.core.domain.repository.TransactionRepository
import pe.moneyflow.core.model.TransactionType
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * Streams the running expense total for [month] and the month before it, for the cash-flow curve.
 *
 * The current month's series stops at today. Extending it to month end with a flat line would draw
 * "spending stopped on the 10th", which misrepresents an in-progress month as a finished one.
 */
class GetCumulativeSpendUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock,
) {
    operator fun invoke(month: YearMonth = YearMonth.now(clock)): Flow<CumulativeSpend> {
        val previous = month.minusMonths(1)

        return combine(
            transactionRepository.observeBetween(month.atDay(1), month.atEndOfMonth()),
            transactionRepository.observeBetween(previous.atDay(1), previous.atEndOfMonth()),
            settingsRepository.preferences,
        ) { monthTx, previousTx, prefs ->
            val today = LocalDate.now(clock)
            val isCurrentMonth = YearMonth.from(today) == month
            // A past month runs to its end; the live one stops at today.
            val currentDayCount = if (isCurrentMonth) today.dayOfMonth else month.lengthOfMonth()

            CumulativeSpend(
                month = month,
                current = CumulativeSpend.runningTotal(
                    dayCount = currentDayCount,
                    amountsByDay = monthTx.expenseAmountsByDay(),
                ),
                previous = CumulativeSpend.runningTotal(
                    dayCount = previous.lengthOfMonth(),
                    amountsByDay = previousTx.expenseAmountsByDay(),
                ),
                currencyCode = prefs.currencyCode,
            )
        }
    }

    private fun List<pe.moneyflow.core.model.Transaction>.expenseAmountsByDay(): Map<Int, Long> =
        filter { it.type == TransactionType.EXPENSE }
            .mapNotNull { tx -> tx.effectiveDate?.let { it.dayOfMonth to tx.amountMinor } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, amounts) -> amounts.sum() }
}
