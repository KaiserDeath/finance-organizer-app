package pe.moneyflow.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import pe.moneyflow.core.database.entity.PaymentMethodEntity

@Dao
interface PaymentMethodDao {

    @Query("SELECT * FROM payment_methods WHERE archived = 0 ORDER BY sortOrder ASC, name ASC")
    fun observeAll(): Flow<List<PaymentMethodEntity>>

    @Query("SELECT * FROM payment_methods WHERE id = :id")
    suspend fun getById(id: String): PaymentMethodEntity?

    @Upsert
    suspend fun upsert(paymentMethod: PaymentMethodEntity)

    @Query("DELETE FROM payment_methods WHERE id = :id")
    suspend fun deleteById(id: String)
}
