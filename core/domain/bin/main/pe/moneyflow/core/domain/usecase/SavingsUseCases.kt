package pe.moneyflow.core.domain.usecase

import kotlinx.coroutines.flow.Flow
import pe.moneyflow.core.domain.repository.SavingsGoalRepository
import pe.moneyflow.core.model.SavingsGoal
import javax.inject.Inject

class ObserveSavingsGoalsUseCase @Inject constructor(
    private val repository: SavingsGoalRepository,
) {
    operator fun invoke(): Flow<List<SavingsGoal>> = repository.observeAll()
}

class SaveSavingsGoalUseCase @Inject constructor(
    private val repository: SavingsGoalRepository,
) {
    suspend operator fun invoke(goal: SavingsGoal) = repository.upsert(goal)
}

class DeleteSavingsGoalUseCase @Inject constructor(
    private val repository: SavingsGoalRepository,
) {
    suspend operator fun invoke(id: String) = repository.delete(id)
}

/**
 * Adds [deltaMinor] to a goal's saved amount (negative to withdraw), clamped at zero so a goal
 * never goes below empty. No-op if the goal no longer exists.
 */
class ContributeToSavingsGoalUseCase @Inject constructor(
    private val repository: SavingsGoalRepository,
) {
    suspend operator fun invoke(goalId: String, deltaMinor: Long) {
        val goal = repository.getById(goalId) ?: return
        val updated = (goal.currentAmountMinor + deltaMinor).coerceAtLeast(0)
        repository.upsert(goal.copy(currentAmountMinor = updated))
    }
}
