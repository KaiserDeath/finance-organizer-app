package pe.moneyflow.core.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import pe.moneyflow.core.domain.repository.AccountRepository
import pe.moneyflow.core.domain.repository.ExchangeRateRepository
import pe.moneyflow.core.domain.repository.SavingsGoalRepository
import pe.moneyflow.core.domain.repository.TransactionRepository
import pe.moneyflow.core.model.Account
import pe.moneyflow.core.model.ExchangeRate
import pe.moneyflow.core.model.SavingsGoal
import pe.moneyflow.core.model.Transaction
import java.time.LocalDate

/** Shared in-memory fakes for the Phase 4 (accounts/transfers/savings/currency) use-case tests. */

internal class FakeAccountRepo(private val items: List<Account>) : AccountRepository {
    override fun observeAll(): Flow<List<Account>> = flowOf(items)
    override suspend fun getById(id: String): Account? = items.firstOrNull { it.id == id }
    override suspend fun upsert(account: Account) = Unit
}

internal class FakeExchangeRateRepo(private val items: List<ExchangeRate>) : ExchangeRateRepository {
    override fun observeAll(): Flow<List<ExchangeRate>> = flowOf(items)
    override suspend fun upsert(rate: ExchangeRate) = Unit
    override suspend fun delete(id: String) = Unit
}

/** Captures the transactions written through [upsert] so tests can assert on transfers. */
internal class RecordingTxRepo : TransactionRepository {
    val saved = mutableListOf<Transaction>()
    override fun observeAll(): Flow<List<Transaction>> = flowOf(saved.toList())
    override fun observeBetween(start: LocalDate, end: LocalDate): Flow<List<Transaction>> =
        flowOf(saved.toList())
    override suspend fun getById(id: String): Transaction? = saved.firstOrNull { it.id == id }
    override suspend fun upsert(transaction: Transaction) { saved += transaction }
    override suspend fun delete(id: String) { saved.removeAll { it.id == id } }
}

/** Mutable savings store so contribution logic (read-modify-write) can be exercised. */
internal class FakeSavingsRepo(initial: List<SavingsGoal> = emptyList()) : SavingsGoalRepository {
    private val state = MutableStateFlow(initial)
    override fun observeAll(): Flow<List<SavingsGoal>> = state
    override suspend fun getById(id: String): SavingsGoal? = state.value.firstOrNull { it.id == id }
    override suspend fun upsert(goal: SavingsGoal) {
        state.value = state.value.filterNot { it.id == goal.id } + goal
    }
    override suspend fun delete(id: String) {
        state.value = state.value.filterNot { it.id == id }
    }
}
