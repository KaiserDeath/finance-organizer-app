package pe.moneyflow.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import pe.moneyflow.core.database.dao.AccountDao
import pe.moneyflow.core.database.dao.BudgetDao
import pe.moneyflow.core.database.dao.CategoryDao
import pe.moneyflow.core.database.dao.ExchangeRateDao
import pe.moneyflow.core.database.dao.PaymentMethodDao
import pe.moneyflow.core.database.dao.RecurringExpenseDao
import pe.moneyflow.core.database.dao.SavingsGoalDao
import pe.moneyflow.core.database.dao.TransactionDao
import pe.moneyflow.core.database.entity.AccountEntity
import pe.moneyflow.core.database.entity.AttachmentEntity
import pe.moneyflow.core.database.entity.BudgetEntity
import pe.moneyflow.core.database.entity.CategoryEntity
import pe.moneyflow.core.database.entity.ExchangeRateEntity
import pe.moneyflow.core.database.entity.InstallmentPlanEntity
import pe.moneyflow.core.database.entity.MonthlySummaryEntity
import pe.moneyflow.core.database.entity.PaymentMethodEntity
import pe.moneyflow.core.database.entity.RecurringExpenseEntity
import pe.moneyflow.core.database.entity.ReminderEntity
import pe.moneyflow.core.database.entity.SavingsGoalEntity
import pe.moneyflow.core.database.entity.TagEntity
import pe.moneyflow.core.database.entity.TransactionEntity
import pe.moneyflow.core.database.entity.TransactionTagCrossRef

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        PaymentMethodEntity::class,
        AccountEntity::class,
        BudgetEntity::class,
        TagEntity::class,
        TransactionTagCrossRef::class,
        RecurringExpenseEntity::class,
        InstallmentPlanEntity::class,
        ReminderEntity::class,
        SavingsGoalEntity::class,
        MonthlySummaryEntity::class,
        ExchangeRateEntity::class,
        AttachmentEntity::class,
    ],
    version = 7,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class MoneyFlowDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun paymentMethodDao(): PaymentMethodDao
    abstract fun accountDao(): AccountDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringExpenseDao(): RecurringExpenseDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun exchangeRateDao(): ExchangeRateDao

    companion object {
        const val NAME = "moneyflow.db"
    }
}
