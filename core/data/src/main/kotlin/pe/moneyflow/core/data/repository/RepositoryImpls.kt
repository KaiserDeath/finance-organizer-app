package pe.moneyflow.core.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import pe.moneyflow.core.common.IoDispatcher
import pe.moneyflow.core.data.mapper.toDomain
import pe.moneyflow.core.data.mapper.toEntity
import pe.moneyflow.core.database.dao.AccountDao
import pe.moneyflow.core.database.dao.CategoryDao
import pe.moneyflow.core.database.dao.PaymentMethodDao
import pe.moneyflow.core.database.dao.TransactionDao
import pe.moneyflow.core.domain.repository.AccountRepository
import pe.moneyflow.core.domain.repository.CategoryRepository
import pe.moneyflow.core.domain.repository.PaymentMethodRepository
import pe.moneyflow.core.domain.repository.TransactionRepository
import pe.moneyflow.core.model.Account
import pe.moneyflow.core.model.Category
import pe.moneyflow.core.model.PaymentMethod
import pe.moneyflow.core.model.Transaction
import java.time.LocalDate
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val dao: TransactionDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : TransactionRepository {

    override fun observeAll(): Flow<List<Transaction>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }.flowOn(ioDispatcher)

    override fun observeBetween(start: LocalDate, end: LocalDate): Flow<List<Transaction>> =
        dao.observeBetween(start, end).map { list -> list.map { it.toDomain() } }.flowOn(ioDispatcher)

    override suspend fun getById(id: String): Transaction? =
        withContext(ioDispatcher) { dao.getById(id)?.toDomain() }

    override suspend fun upsert(transaction: Transaction) =
        withContext(ioDispatcher) { dao.upsert(transaction.toEntity()) }

    override suspend fun delete(id: String) =
        withContext(ioDispatcher) { dao.deleteById(id) }
}

class CategoryRepositoryImpl @Inject constructor(
    private val dao: CategoryDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CategoryRepository {

    override fun observeAll(): Flow<List<Category>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }.flowOn(ioDispatcher)

    override suspend fun getById(id: String): Category? =
        withContext(ioDispatcher) { dao.getById(id)?.toDomain() }

    override suspend fun upsert(category: Category) =
        withContext(ioDispatcher) { dao.upsert(category.toEntity()) }

    override suspend fun delete(id: String) =
        withContext(ioDispatcher) { dao.deleteById(id) }
}

class PaymentMethodRepositoryImpl @Inject constructor(
    private val dao: PaymentMethodDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : PaymentMethodRepository {

    override fun observeAll(): Flow<List<PaymentMethod>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }.flowOn(ioDispatcher)

    override suspend fun getById(id: String): PaymentMethod? =
        withContext(ioDispatcher) { dao.getById(id)?.toDomain() }

    override suspend fun upsert(paymentMethod: PaymentMethod) =
        withContext(ioDispatcher) { dao.upsert(paymentMethod.toEntity()) }

    override suspend fun delete(id: String) =
        withContext(ioDispatcher) { dao.deleteById(id) }
}

class AccountRepositoryImpl @Inject constructor(
    private val dao: AccountDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AccountRepository {

    override fun observeAll(): Flow<List<Account>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }.flowOn(ioDispatcher)

    override suspend fun getById(id: String): Account? =
        withContext(ioDispatcher) { dao.getById(id)?.toDomain() }

    override suspend fun upsert(account: Account) =
        withContext(ioDispatcher) { dao.upsert(account.toEntity()) }
}
