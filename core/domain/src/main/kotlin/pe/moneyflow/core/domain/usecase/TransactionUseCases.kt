package pe.moneyflow.core.domain.usecase

import kotlinx.coroutines.flow.Flow
import pe.moneyflow.core.domain.repository.TransactionRepository
import pe.moneyflow.core.model.Transaction
import pe.moneyflow.core.model.TransactionStatus
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

/** Streams every transaction, newest activity first is applied by callers as needed. */
class ObserveTransactionsUseCase @Inject constructor(
    private val repository: TransactionRepository,
) {
    operator fun invoke(): Flow<List<Transaction>> = repository.observeAll()
}

class GetTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository,
) {
    suspend operator fun invoke(id: String): Transaction? = repository.getById(id)
}

/** Creates or updates a transaction. Used by the add/edit screen. */
class SaveTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository,
) {
    suspend operator fun invoke(transaction: Transaction) = repository.upsert(transaction)
}

class DeleteTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository,
) {
    suspend operator fun invoke(id: String) = repository.delete(id)
}

/**
 * Marks a pending payment as paid, stamping today's date. Used by the upcoming-payments screen.
 *
 * When [paymentMethodId] is given it is persisted with the payment — the only ground truth about
 * what the user actually pays each bill with, and what makes the suggested method improve with use.
 */
class MarkTransactionPaidUseCase @Inject constructor(
    private val repository: TransactionRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(id: String, paymentMethodId: String? = null) {
        val transaction = repository.getById(id) ?: return
        repository.upsert(
            transaction.copy(
                status = TransactionStatus.PAID,
                actualDate = LocalDate.now(clock),
                paymentMethodId = paymentMethodId ?: transaction.paymentMethodId,
                updatedAt = Instant.now(clock),
            ),
        )
    }
}

/**
 * Reverts a paid charge back to pending, clearing the settled date. Backs the "Deshacer" action
 * after a mistaken mark-paid in the upcoming-payments screen.
 */
class UnmarkTransactionPaidUseCase @Inject constructor(
    private val repository: TransactionRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(id: String) {
        val transaction = repository.getById(id) ?: return
        repository.upsert(
            transaction.copy(
                status = TransactionStatus.PENDING,
                actualDate = null,
                updatedAt = Instant.now(clock),
            ),
        )
    }
}
