package pe.moneyflow.core.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.moneyflow.core.model.ExchangeRate

interface ExchangeRateRepository {
    fun observeAll(): Flow<List<ExchangeRate>>

    suspend fun upsert(rate: ExchangeRate)

    suspend fun delete(id: String)
}
