package pe.moneyflow.core.database.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import pe.moneyflow.core.database.MoneyFlowDatabase
import pe.moneyflow.core.database.SeedData
import pe.moneyflow.core.database.dao.AccountDao
import pe.moneyflow.core.database.dao.BudgetDao
import pe.moneyflow.core.database.dao.CategoryDao
import pe.moneyflow.core.database.dao.ExchangeRateDao
import pe.moneyflow.core.database.dao.PaymentMethodDao
import pe.moneyflow.core.database.dao.RecurringExpenseDao
import pe.moneyflow.core.database.dao.SavingsGoalDao
import pe.moneyflow.core.database.dao.TransactionDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): MoneyFlowDatabase = Room.databaseBuilder(
        context,
        MoneyFlowDatabase::class.java,
        MoneyFlowDatabase.NAME,
    )
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                SeedData.seed(db)
            }
        })
        // Pre-release schema is still evolving; drop and reseed rather than ship migrations yet.
        .fallbackToDestructiveMigration()
        .build()

    @Provides
    fun provideTransactionDao(db: MoneyFlowDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideCategoryDao(db: MoneyFlowDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun providePaymentMethodDao(db: MoneyFlowDatabase): PaymentMethodDao = db.paymentMethodDao()

    @Provides
    fun provideAccountDao(db: MoneyFlowDatabase): AccountDao = db.accountDao()

    @Provides
    fun provideBudgetDao(db: MoneyFlowDatabase): BudgetDao = db.budgetDao()

    @Provides
    fun provideRecurringExpenseDao(db: MoneyFlowDatabase): RecurringExpenseDao =
        db.recurringExpenseDao()

    @Provides
    fun provideSavingsGoalDao(db: MoneyFlowDatabase): SavingsGoalDao = db.savingsGoalDao()

    @Provides
    fun provideExchangeRateDao(db: MoneyFlowDatabase): ExchangeRateDao = db.exchangeRateDao()
}
