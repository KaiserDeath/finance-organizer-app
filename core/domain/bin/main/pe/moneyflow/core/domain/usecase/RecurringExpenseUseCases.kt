package pe.moneyflow.core.domain.usecase

import kotlinx.coroutines.flow.Flow
import pe.moneyflow.core.domain.repository.RecurringExpenseRepository
import pe.moneyflow.core.domain.repository.TransactionRepository
import pe.moneyflow.core.model.RecurringExpense
import pe.moneyflow.core.model.Transaction
import pe.moneyflow.core.model.TransactionStatus
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/** Streams every recurring template, soonest next-run first. */
class ObserveRecurringExpensesUseCase @Inject constructor(
    private val repository: RecurringExpenseRepository,
) {
    operator fun invoke(): Flow<List<RecurringExpense>> = repository.observeAll()
}

/** Creates or updates a recurring template. */
class SaveRecurringExpenseUseCase @Inject constructor(
    private val repository: RecurringExpenseRepository,
) {
    suspend operator fun invoke(recurring: RecurringExpense) = repository.upsert(recurring)
}

class DeleteRecurringExpenseUseCase @Inject constructor(
    private val repository: RecurringExpenseRepository,
) {
    suspend operator fun invoke(id: String) = repository.delete(id)
}

/**
 * Materializes every due occurrence of each auto-generating template into a PENDING transaction,
 * advancing the template's [RecurringExpense.nextRunDate] past today (or its end date). Idempotent:
 * running twice on the same day produces no extra transactions because next-run is always advanced
 * beyond today. Returns the number of transactions created — the worker uses it for notifications.
 */
class GenerateDueRecurringUseCase @Inject constructor(
    private val recurringRepository: RecurringExpenseRepository,
    private val transactionRepository: TransactionRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(): Int {
        val today = LocalDate.now(clock)
        val now = Instant.now(clock)
        var created = 0

        for (template in recurringRepository.getDue(today)) {
            var runDate = template.nextRunDate
            // Guard against pathological configs (interval <= 0 would never advance).
            var guard = 0
            while (!runDate.isAfter(today) &&
                (template.endDate == null || !runDate.isAfter(template.endDate)) &&
                guard < MAX_OCCURRENCES_PER_RUN
            ) {
                transactionRepository.upsert(
                    Transaction(
                        id = UUID.randomUUID().toString(),
                        title = template.title,
                        amountMinor = template.amountMinor,
                        currencyCode = template.currencyCode,
                        categoryId = template.categoryId,
                        paymentMethodId = template.paymentMethodId,
                        accountId = template.accountId,
                        type = template.type,
                        status = TransactionStatus.PENDING,
                        estimatedDate = runDate,
                        recurringId = template.id,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
                created++
                runDate = template.advance(runDate)
                guard++
            }

            recurringRepository.upsert(
                template.copy(
                    nextRunDate = runDate,
                    lastGeneratedDate = today,
                ),
            )
        }
        return created
    }

    private companion object {
        /** Safety cap so a long-dormant template can't flood the ledger in a single pass. */
        const val MAX_OCCURRENCES_PER_RUN = 60
    }
}
