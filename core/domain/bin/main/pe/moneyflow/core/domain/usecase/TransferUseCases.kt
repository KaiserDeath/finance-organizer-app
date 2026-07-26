package pe.moneyflow.core.domain.usecase

import pe.moneyflow.core.domain.repository.TransactionRepository
import pe.moneyflow.core.model.Transaction
import pe.moneyflow.core.model.TransactionStatus
import pe.moneyflow.core.model.TransactionType
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/**
 * Records a money transfer between two accounts as a single [TransactionType.TRANSFER] entry:
 * [Transaction.accountId] is the source and [Transaction.transferAccountId] the destination, so
 * balance computation debits one and credits the other.
 */
class CreateTransferUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(
        fromAccountId: String,
        toAccountId: String,
        amountMinor: Long,
        currencyCode: String,
        date: LocalDate = LocalDate.now(clock),
        note: String? = null,
    ): Result<Unit> {
        if (fromAccountId == toAccountId) {
            return Result.failure(IllegalArgumentException("Source and destination must differ"))
        }
        if (amountMinor <= 0) {
            return Result.failure(IllegalArgumentException("Amount must be positive"))
        }
        val now = Instant.now(clock)
        transactionRepository.upsert(
            Transaction(
                id = UUID.randomUUID().toString(),
                title = "Transferencia",
                amountMinor = amountMinor,
                currencyCode = currencyCode,
                accountId = fromAccountId,
                transferAccountId = toAccountId,
                type = TransactionType.TRANSFER,
                status = TransactionStatus.PAID,
                estimatedDate = date,
                actualDate = date,
                notes = note,
                createdAt = now,
                updatedAt = now,
            ),
        )
        return Result.success(Unit)
    }
}
