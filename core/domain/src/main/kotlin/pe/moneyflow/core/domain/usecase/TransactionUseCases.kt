package pe.moneyflow.core.domain.usecase

import kotlinx.coroutines.flow.Flow
import pe.moneyflow.core.domain.repository.TransactionRepository
import pe.moneyflow.core.model.Transaction
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
