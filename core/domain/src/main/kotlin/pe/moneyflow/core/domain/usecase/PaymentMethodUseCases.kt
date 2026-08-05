package pe.moneyflow.core.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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

/**
 * Makes [id] the one default payment method, clearing the flag from every other.
 *
 * Exists because "default" is a property of the *set*, not of a row, and nothing enforced that.
 * [SavePaymentMethodUseCase] upserts whatever it is handed, so saving a second method with
 * `isDefault = true` left two of them flagged — and every consumer asks for the default with
 * `firstOrNull { it.isDefault }`, which then answers by table order rather than by the user's
 * choice. Setting a default has to be one operation over the whole list, which is this.
 *
 * Only rows whose flag actually changes are written, so the common case touches two.
 */
class SetDefaultPaymentMethodUseCase @Inject constructor(
    private val repository: PaymentMethodRepository,
) {
    suspend operator fun invoke(id: String) {
        val methods = repository.observeAll().first()
        // An id nobody holds is a no-op, not a reason to clear the flag from everyone. Without this
        // the loop below demotes the current default and leaves the set with none — a worse state
        // than the one it was asked to change, reached by a request that should have done nothing.
        if (methods.none { it.id == id }) return
        methods
            .filter { it.isDefault != (it.id == id) }
            .forEach { repository.upsert(it.copy(isDefault = it.id == id)) }
    }
}
