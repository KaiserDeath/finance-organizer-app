package pe.moneyflow.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import pe.moneyflow.core.database.entity.RecurringExpenseEntity
import java.time.LocalDate

@Dao
interface RecurringExpenseDao {

    @Query("SELECT * FROM recurring_expenses ORDER BY nextRunDate ASC")
    fun observeAll(): Flow<List<RecurringExpenseEntity>>

    /** Templates that should auto-generate on or before [date]. Used by the generation worker. */
    @Query(
        """
        SELECT * FROM recurring_expenses
        WHERE autoCreate = 1 AND nextRunDate <= :date
        ORDER BY nextRunDate ASC
        """,
    )
    suspend fun getDue(date: LocalDate): List<RecurringExpenseEntity>

    @Query("SELECT * FROM recurring_expenses WHERE id = :id")
    suspend fun getById(id: String): RecurringExpenseEntity?

    @Upsert
    suspend fun upsert(recurring: RecurringExpenseEntity)

    @Query("DELETE FROM recurring_expenses WHERE id = :id")
    suspend fun deleteById(id: String)
}
