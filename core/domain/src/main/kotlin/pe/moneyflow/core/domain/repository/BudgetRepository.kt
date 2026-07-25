package pe.moneyflow.core.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.moneyflow.core.model.Budget

interface BudgetRepository {
    fun observeAll(): Flow<List<Budget>>

    suspend fun getById(id: String): Budget?

    suspend fun upsert(budget: Budget)

    suspend fun delete(id: String)
}
