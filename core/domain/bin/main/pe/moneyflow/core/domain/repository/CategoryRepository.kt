package pe.moneyflow.core.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.moneyflow.core.model.Category

interface CategoryRepository {
    fun observeAll(): Flow<List<Category>>

    suspend fun getById(id: String): Category?

    suspend fun upsert(category: Category)

    suspend fun delete(id: String)
}
