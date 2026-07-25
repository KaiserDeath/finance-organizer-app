package pe.moneyflow.core.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.moneyflow.core.model.SavingsGoal

interface SavingsGoalRepository {
    fun observeAll(): Flow<List<SavingsGoal>>

    suspend fun getById(id: String): SavingsGoal?

    suspend fun upsert(goal: SavingsGoal)

    suspend fun delete(id: String)
}
