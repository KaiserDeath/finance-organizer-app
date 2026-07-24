package pe.moneyflow.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

/*
 * Schema-complete tables for later phases. They are registered in the database now so future
 * features (budgets, recurring, installments, reminders, savings, reports, multi-currency,
 * attachments) don't require a disruptive migration. DAOs/repositories are added per phase.
 */

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("categoryId")],
)
data class BudgetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val categoryId: String?,
    val amountMinor: Long,
    val period: String,
    val startDate: LocalDate,
    val rollover: Boolean,
    val currencyCode: String,
)

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey val id: String,
    val name: String,
    val colorHex: String,
)

@Entity(
    tableName = "transaction_tag_cross_ref",
    primaryKeys = ["transactionId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("tagId")],
)
data class TransactionTagCrossRef(
    val transactionId: String,
    val tagId: String,
)

@Entity(tableName = "recurring_expenses")
data class RecurringExpenseEntity(
    @PrimaryKey val id: String,
    val title: String,
    val amountMinor: Long,
    val currencyCode: String,
    val categoryId: String?,
    val paymentMethodId: String?,
    val accountId: String?,
    val frequency: String,
    val interval: Int,
    val nextRunDate: LocalDate,
    val endDate: LocalDate?,
    val autoCreate: Boolean,
    val lastGeneratedDate: LocalDate?,
)

@Entity(tableName = "installment_plans")
data class InstallmentPlanEntity(
    @PrimaryKey val id: String,
    val title: String,
    val totalAmountMinor: Long,
    val installmentCount: Int,
    val installmentAmountMinor: Long,
    val startDate: LocalDate,
    val frequency: String,
    val paymentMethodId: String?,
    val categoryId: String?,
)

@Entity(
    tableName = "reminders",
    indices = [Index("transactionId")],
)
data class ReminderEntity(
    @PrimaryKey val id: String,
    val transactionId: String?,
    val title: String,
    val triggerAt: Instant,
    val repeat: String?,
    val enabled: Boolean,
    val workRequestId: String?,
)

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey val id: String,
    val name: String,
    val targetAmountMinor: Long,
    val currentAmountMinor: Long,
    val targetDate: LocalDate?,
    val accountId: String?,
    val colorHex: String,
    val iconKey: String,
)

@Entity(
    tableName = "monthly_summaries",
    indices = [Index(value = ["year", "month"], unique = true)],
)
data class MonthlySummaryEntity(
    @PrimaryKey val id: String,
    val year: Int,
    val month: Int,
    val totalSpentMinor: Long,
    val totalIncomeMinor: Long,
    val topCategoryId: String?,
    val topPaymentMethodId: String?,
    val avgDailyMinor: Long,
    val computedAt: Instant,
)

@Entity(
    tableName = "exchange_rates",
    indices = [Index(value = ["base", "quote", "asOf"], unique = true)],
)
data class ExchangeRateEntity(
    @PrimaryKey val id: String,
    val base: String,
    val quote: String,
    val rate: Double,
    val asOf: LocalDate,
)

@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("transactionId")],
)
data class AttachmentEntity(
    @PrimaryKey val id: String,
    val transactionId: String,
    val uri: String,
    val type: String,
)
