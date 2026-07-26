package pe.moneyflow.core.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import pe.moneyflow.core.model.SavingsGoal

class ContributeToSavingsGoalUseCaseTest {

    private fun goal() = SavingsGoal(id = "g1", name = "Viaje", targetAmountMinor = 5_000, currentAmountMinor = 1_000)

    @Test
    fun `contribution increases the saved amount`() = runTest {
        val repo = FakeSavingsRepo(listOf(goal()))
        ContributeToSavingsGoalUseCase(repo)("g1", 2_000)
        assertEquals(3_000L, repo.getById("g1")!!.currentAmountMinor)
    }

    @Test
    fun `withdrawal below zero is clamped to empty`() = runTest {
        val repo = FakeSavingsRepo(listOf(goal()))
        ContributeToSavingsGoalUseCase(repo)("g1", -5_000)
        assertEquals(0L, repo.getById("g1")!!.currentAmountMinor)
    }

    @Test
    fun `missing goal is a no-op`() = runTest {
        val repo = FakeSavingsRepo(listOf(goal()))
        ContributeToSavingsGoalUseCase(repo)("does-not-exist", 1_000)
        assertEquals(1_000L, repo.getById("g1")!!.currentAmountMinor)
    }
}
