package pe.moneyflow.core.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.moneyflow.core.domain.repository.TransactionRepository
import pe.moneyflow.core.model.QuickShortcut
import pe.moneyflow.core.model.Transaction
import pe.moneyflow.core.model.TransactionStatus
import pe.moneyflow.core.model.TransactionType
import java.time.Clock
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject

/**
 * Infers one-tap shortcuts from history when onboarding didn't collect any: the four most
 * frequent description+category+method combinations among the last 30 days of paid expenses.
 * Amount is the most recent occurrence's, since prices drift.
 *
 * Emits an empty list until the ledger is at least 30 days old — a fresh install shouldn't
 * suggest shortcuts off three data points.
 */
class GetFrequentShortcutsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<List<QuickShortcut>> =
        transactionRepository.observeAll().map { transactions ->
            infer(transactions, LocalDate.now(clock))
        }

    internal fun infer(transactions: List<Transaction>, today: LocalDate): List<QuickShortcut> {
        val oldest = transactions.mapNotNull { it.effectiveDate }.minOrNull() ?: return emptyList()
        if (oldest.isAfter(today.minusDays(HISTORY_MIN_DAYS))) return emptyList()

        val cutoff = today.minusDays(WINDOW_DAYS)
        return transactions
            .filter {
                it.type == TransactionType.EXPENSE &&
                    it.status == TransactionStatus.PAID &&
                    it.title.isNotBlank() &&
                    it.effectiveDate?.isAfter(cutoff) == true
            }
            .groupBy {
                Triple(it.title.trim().lowercase(Locale.ROOT), it.categoryId, it.paymentMethodId)
            }
            .entries
            .sortedByDescending { it.value.size }
            .take(SHORTCUT_COUNT)
            .map { (_, occurrences) ->
                val latest = occurrences.maxBy { it.effectiveDate ?: LocalDate.MIN }
                QuickShortcut(
                    label = latest.title.trim(),
                    amountMinor = latest.amountMinor,
                    categoryId = latest.categoryId,
                    paymentMethodId = latest.paymentMethodId,
                )
            }
    }

    private companion object {
        const val WINDOW_DAYS = 30L
        const val HISTORY_MIN_DAYS = 30L
        const val SHORTCUT_COUNT = 4
    }
}
