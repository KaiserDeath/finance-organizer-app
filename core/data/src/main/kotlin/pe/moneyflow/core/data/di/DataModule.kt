package pe.moneyflow.core.data.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import pe.moneyflow.core.common.DefaultDispatcher
import pe.moneyflow.core.common.IoDispatcher
import pe.moneyflow.core.data.backup.BackupRepositoryImpl
import pe.moneyflow.core.data.repository.AccountRepositoryImpl
import pe.moneyflow.core.data.repository.BudgetRepositoryImpl
import pe.moneyflow.core.data.repository.CategoryRepositoryImpl
import pe.moneyflow.core.data.repository.ExchangeRateRepositoryImpl
import pe.moneyflow.core.data.repository.PaymentMethodRepositoryImpl
import pe.moneyflow.core.data.repository.RecurringExpenseRepositoryImpl
import pe.moneyflow.core.data.repository.SavingsGoalRepositoryImpl
import pe.moneyflow.core.data.repository.TransactionRepositoryImpl
import pe.moneyflow.core.domain.repository.AccountRepository
import pe.moneyflow.core.domain.repository.BackupRepository
import pe.moneyflow.core.domain.repository.BudgetRepository
import pe.moneyflow.core.domain.repository.CategoryRepository
import pe.moneyflow.core.domain.repository.ExchangeRateRepository
import pe.moneyflow.core.domain.repository.PaymentMethodRepository
import pe.moneyflow.core.domain.repository.RecurringExpenseRepository
import pe.moneyflow.core.domain.repository.SavingsGoalRepository
import pe.moneyflow.core.domain.repository.SmartInsights
import pe.moneyflow.core.domain.repository.TransactionRepository
import pe.moneyflow.core.domain.usecase.RuleBasedSmartInsights
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindPaymentMethodRepository(impl: PaymentMethodRepositoryImpl): PaymentMethodRepository

    @Binds
    @Singleton
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

    @Binds
    @Singleton
    abstract fun bindBudgetRepository(impl: BudgetRepositoryImpl): BudgetRepository

    @Binds
    @Singleton
    abstract fun bindRecurringExpenseRepository(
        impl: RecurringExpenseRepositoryImpl,
    ): RecurringExpenseRepository

    @Binds
    @Singleton
    abstract fun bindSavingsGoalRepository(impl: SavingsGoalRepositoryImpl): SavingsGoalRepository

    @Binds
    @Singleton
    abstract fun bindExchangeRateRepository(impl: ExchangeRateRepositoryImpl): ExchangeRateRepository

    @Binds
    @Singleton
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository

    @Binds
    @Singleton
    abstract fun bindSmartInsights(impl: RuleBasedSmartInsights): SmartInsights
}

@Module
@InstallIn(SingletonComponent::class)
object CoroutinesModule {

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}

@Module
@InstallIn(SingletonComponent::class)
object TimeModule {

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()
}
