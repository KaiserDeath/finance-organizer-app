package pe.moneyflow.core.domain.usecase

import kotlinx.coroutines.flow.Flow
import pe.moneyflow.core.domain.repository.ExchangeRateRepository
import pe.moneyflow.core.model.ExchangeRate
import javax.inject.Inject

class ObserveExchangeRatesUseCase @Inject constructor(
    private val repository: ExchangeRateRepository,
) {
    operator fun invoke(): Flow<List<ExchangeRate>> = repository.observeAll()
}

class SaveExchangeRateUseCase @Inject constructor(
    private val repository: ExchangeRateRepository,
) {
    suspend operator fun invoke(rate: ExchangeRate) = repository.upsert(rate)
}

class DeleteExchangeRateUseCase @Inject constructor(
    private val repository: ExchangeRateRepository,
) {
    suspend operator fun invoke(id: String) = repository.delete(id)
}
