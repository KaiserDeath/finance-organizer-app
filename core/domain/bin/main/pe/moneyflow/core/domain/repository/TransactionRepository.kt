package pe.moneyflow.core.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.moneyflow.core.model.Transaction
import java.time.LocalDate

interface TransactionRepository {
    fun observeAll(): Flow<List<Transaction>>

    /** Transactions whose effective date (actual, else estimated) falls within [start]..[end]. */
    fun observeBetween(start: LocalDate, end: LocalDate): Flow<List<Transaction>>

    suspend fun getById(id: String): Transaction?

    suspend fun upsert(transaction: Transaction)

    suspend fun delete(id: String)
}
