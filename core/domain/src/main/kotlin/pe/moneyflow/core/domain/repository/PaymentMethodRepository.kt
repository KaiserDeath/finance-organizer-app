package pe.moneyflow.core.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.moneyflow.core.model.PaymentMethod

interface PaymentMethodRepository {
    fun observeAll(): Flow<List<PaymentMethod>>

    suspend fun getById(id: String): PaymentMethod?

    suspend fun upsert(paymentMethod: PaymentMethod)

    suspend fun delete(id: String)
}
