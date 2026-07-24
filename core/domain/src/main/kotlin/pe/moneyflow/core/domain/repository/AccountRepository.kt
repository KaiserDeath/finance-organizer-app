package pe.moneyflow.core.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.moneyflow.core.model.Account

interface AccountRepository {
    fun observeAll(): Flow<List<Account>>

    suspend fun getById(id: String): Account?

    suspend fun upsert(account: Account)
}
