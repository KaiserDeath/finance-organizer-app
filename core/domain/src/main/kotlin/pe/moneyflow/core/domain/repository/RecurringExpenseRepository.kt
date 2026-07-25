package pe.moneyflow.core.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.moneyflow.core.model.RecurringExpense
import java.time.LocalDate

interface RecurringExpenseRepository {
    fun observeAll(): Flow<List<RecurringExpense>>

    /** Auto-generating templates whose next run date is on or before [date]. */
    suspend fun getDue(date: LocalDate): List<RecurringExpense>

    suspend fun getById(id: String): RecurringExpense?

    suspend fun upsert(recurring: RecurringExpense)

    suspend fun delete(id: String)
}
