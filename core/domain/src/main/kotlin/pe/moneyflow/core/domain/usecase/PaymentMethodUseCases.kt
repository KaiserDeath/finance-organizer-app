package pe.moneyflow.core.domain.usecase

import kotlinx.coroutines.flow.Flow
import pe.moneyflow.core.domain.repository.PaymentMethodRepository
import pe.moneyflow.core.model.PaymentMethod
import javax.inject.Inject

class ObservePaymentMethodsUseCase @Inject constructor(
    private val repository: PaymentMethodRepository,
) {
    operator fun invoke(): Flow<List<PaymentMethod>> = repository.observeAll()
}

class SavePaymentMethodUseCase @Inject constructor(
    private val repository: PaymentMethodRepository,
) {
    suspend operator fun invoke(paymentMethod: PaymentMethod) = repository.upsert(paymentMethod)
}

class DeletePaymentMethodUseCase @Inject constructor(
    private val repository: PaymentMethodRepository,
) {
    suspend operator fun invoke(id: String) = repository.delete(id)
}
