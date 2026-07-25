package pe.moneyflow.core.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import pe.moneyflow.core.domain.model.UpcomingBucket
import pe.moneyflow.core.domain.model.UpcomingPayment
import pe.moneyflow.core.domain.repository.CategoryRepository
import pe.moneyflow.core.domain.repository.TransactionRepository
import pe.moneyflow.core.model.TransactionStatus
import pe.moneyflow.core.model.TransactionType
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** Streams pending expenses classified into due-date buckets, earliest first. */
class GetUpcomingPaymentsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<List<UpcomingPayment>> = combine(
        transactionRepository.observeAll(),
        categoryRepository.observeAll(),
    ) { transactions, categories ->
        val today = LocalDate.now(clock)
        transactions
            .filter { it.type == TransactionType.EXPENSE && it.status == TransactionStatus.PENDING }
            .sortedBy { it.estimatedDate ?: LocalDate.MAX }
            .map { tx ->
                UpcomingPayment(
                    transaction = tx,
                    bucket = bucketFor(tx.estimatedDate, today),
                    category = categories.firstOrNull { it.id == tx.categoryId },
                )
            }
    }

    private fun bucketFor(date: LocalDate?, today: LocalDate): UpcomingBucket = when {
        date == null -> UpcomingBucket.LATER
        date.isBefore(today) -> UpcomingBucket.OVERDUE
        date == today -> UpcomingBucket.TODAY
        date == today.plusDays(1) -> UpcomingBucket.TOMORROW
        date.isBefore(today.plusDays(8)) -> UpcomingBucket.THIS_WEEK
        date.month == today.month && date.year == today.year -> UpcomingBucket.THIS_MONTH
        else -> UpcomingBucket.LATER
    }
}
