package pe.moneyflow.core.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import pe.moneyflow.core.domain.model.UpcomingBucket
import pe.moneyflow.core.domain.model.UpcomingPayment
import pe.moneyflow.core.domain.repository.CategoryRepository
import pe.moneyflow.core.domain.repository.RecurringExpenseRepository
import pe.moneyflow.core.domain.repository.TransactionRepository
import pe.moneyflow.core.model.Category
import pe.moneyflow.core.model.RecurringExpense
import pe.moneyflow.core.model.Transaction
import pe.moneyflow.core.model.TransactionStatus
import pe.moneyflow.core.model.TransactionType
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * Streams everything the user still owes through the end of next month, earliest first.
 *
 * Two sources are merged. Pending expense rows already in the ledger come through as-is. Beyond
 * them, future occurrences are **projected** in memory from recurring templates via
 * [RecurringExpense.advance] — the generator only materializes up to today, so without this the
 * forward-looking sections would always be empty. Projections are never persisted; materializing
 * them ahead of time would distort balances and analytics.
 *
 * Templates with `autoCreate = false` are projected too: they never materialize on their own, so
 * the forecast is the only place they can appear.
 */
class GetUpcomingPaymentsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val recurringExpenseRepository: RecurringExpenseRepository,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<List<UpcomingPayment>> = combine(
        transactionRepository.observeAll(),
        categoryRepository.observeAll(),
        recurringExpenseRepository.observeAll(),
    ) { transactions, categories, templates ->
        val today = LocalDate.now(clock)
        val horizon = YearMonth.from(today).plusMonths(1).atEndOfMonth()

        val materialized = transactions
            .filter { it.type == TransactionType.EXPENSE && it.status == TransactionStatus.PENDING }
            .map { tx ->
                val due = tx.estimatedDate
                UpcomingPayment(
                    transaction = tx,
                    bucket = bucketFor(due, today),
                    category = categories.categoryFor(tx.categoryId),
                    dueDate = due,
                    isOverdue = due != null && due.isBefore(today),
                )
            }

        // Every occurrence already in the ledger — paid ones included, so a bill settled early does
        // not reappear as a projection.
        val alreadyInLedger = transactions.mapNotNullTo(mutableSetOf()) { tx ->
            val recurringId = tx.recurringId ?: return@mapNotNullTo null
            val date = tx.effectiveDate ?: return@mapNotNullTo null
            recurringId to date
        }

        val projected = templates.flatMap { template ->
            projectOccurrences(template, today, horizon)
                .filterNot { date -> (template.id to date) in alreadyInLedger }
                .map { date ->
                    UpcomingPayment(
                        transaction = template.toProjectedTransaction(date),
                        bucket = bucketFor(date, today),
                        category = categories.categoryFor(template.categoryId),
                        dueDate = date,
                        isProjected = true,
                    )
                }
        }

        // Materialized before projected on a tie: the real row is the one you can act on.
        (materialized + projected).sortedWith(
            compareBy({ it.dueDate ?: LocalDate.MAX }, { it.isProjected }),
        )
    }

    /** Occurrence dates from [today] through [horizon] inclusive, honoring the template's end date. */
    private fun projectOccurrences(
        template: RecurringExpense,
        today: LocalDate,
        horizon: LocalDate,
    ): List<LocalDate> {
        if (template.type != TransactionType.EXPENSE) return emptyList()
        if (template.endDate?.isBefore(today) == true) return emptyList()

        // A stale template (worker hasn't run in a while) can sit well in the past: fast-forward to
        // today before collecting, so a DAILY schedule doesn't spend its whole budget on old dates.
        var date = template.nextRunDate
        var skipped = 0
        while (date.isBefore(today) && skipped++ < MAX_SKIPPED_PER_TEMPLATE) {
            date = template.advance(date)
        }

        val dates = mutableListOf<LocalDate>()
        var steps = 0
        while (!date.isAfter(horizon) && steps++ < MAX_PROJECTED_PER_TEMPLATE) {
            if (template.endDate != null && date.isAfter(template.endDate)) break
            if (!date.isBefore(today)) dates += date
            date = template.advance(date)
        }
        return dates
    }

    /**
     * A synthetic, unsaved [Transaction] standing in for one future occurrence. The blank id marks
     * it as having no row behind it; `recurringId` points back at the template that produced it.
     */
    private fun RecurringExpense.toProjectedTransaction(date: LocalDate): Transaction = Transaction(
        id = "",
        title = title,
        amountMinor = amountMinor,
        currencyCode = currencyCode,
        categoryId = categoryId,
        paymentMethodId = paymentMethodId,
        cardKind = cardKind,
        accountId = accountId,
        type = TransactionType.EXPENSE,
        status = TransactionStatus.PENDING,
        estimatedDate = date,
        recurringId = id,
    )

    private fun List<Category>.categoryFor(id: String?): Category? =
        id?.let { wanted -> firstOrNull { it.id == wanted } }

    private fun bucketFor(date: LocalDate?, today: LocalDate): UpcomingBucket = when {
        // No date means nothing schedules it: it needs a decision, so it belongs with "act now".
        date == null -> UpcomingBucket.DUE_NOW
        !date.isAfter(today.plusDays(1)) -> UpcomingBucket.DUE_NOW
        YearMonth.from(date) == YearMonth.from(today) -> UpcomingBucket.THIS_MONTH
        else -> UpcomingBucket.NEXT_MONTH
    }

    private companion object {
        /** Guard on the fast-forward walk for templates whose next run is long overdue. */
        const val MAX_SKIPPED_PER_TEMPLATE = 400

        /** Guard on occurrences collected per template — a DAILY schedule tops out around 60. */
        const val MAX_PROJECTED_PER_TEMPLATE = 70
    }
}
