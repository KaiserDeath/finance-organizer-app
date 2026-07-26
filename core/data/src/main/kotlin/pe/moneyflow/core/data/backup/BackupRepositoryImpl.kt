package pe.moneyflow.core.data.backup

import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import pe.moneyflow.core.domain.model.BackupSummary
import pe.moneyflow.core.domain.repository.AccountRepository
import pe.moneyflow.core.domain.repository.BackupRepository
import pe.moneyflow.core.domain.repository.BudgetRepository
import pe.moneyflow.core.domain.repository.CategoryRepository
import pe.moneyflow.core.domain.repository.ExchangeRateRepository
import pe.moneyflow.core.domain.repository.PaymentMethodRepository
import pe.moneyflow.core.domain.repository.RecurringExpenseRepository
import pe.moneyflow.core.domain.repository.SavingsGoalRepository
import pe.moneyflow.core.domain.repository.TransactionRepository
import javax.inject.Inject

/**
 * Reads every repository into a [BackupData] document and serializes it, and restores by upserting
 * each record back. On import, parent records (accounts, categories, payment methods) are written
 * before transactions so foreign-key references resolve.
 */
class BackupRepositoryImpl @Inject constructor(
    private val transactions: TransactionRepository,
    private val categories: CategoryRepository,
    private val paymentMethods: PaymentMethodRepository,
    private val accounts: AccountRepository,
    private val budgets: BudgetRepository,
    private val recurring: RecurringExpenseRepository,
    private val savingsGoals: SavingsGoalRepository,
    private val exchangeRates: ExchangeRateRepository,
) : BackupRepository {

    override suspend fun exportJson(): String {
        val data = BackupData(
            transactions = transactions.observeAll().first().map { it.toDto() },
            categories = categories.observeAll().first().map { it.toDto() },
            paymentMethods = paymentMethods.observeAll().first().map { it.toDto() },
            accounts = accounts.observeAll().first().map { it.toDto() },
            budgets = budgets.observeAll().first().map { it.toDto() },
            recurring = recurring.observeAll().first().map { it.toDto() },
            savingsGoals = savingsGoals.observeAll().first().map { it.toDto() },
            exchangeRates = exchangeRates.observeAll().first().map { it.toDto() },
        )
        return backupJson.encodeToString(data)
    }

    override suspend fun importJson(json: String): Result<BackupSummary> = runCatching {
        val data = backupJson.decodeFromString<BackupData>(json)

        // Parents first so transaction foreign keys resolve on insert.
        data.accounts.forEach { accounts.upsert(it.toDomain()) }
        data.categories.forEach { categories.upsert(it.toDomain()) }
        data.paymentMethods.forEach { paymentMethods.upsert(it.toDomain()) }
        data.budgets.forEach { budgets.upsert(it.toDomain()) }
        data.recurring.forEach { recurring.upsert(it.toDomain()) }
        data.savingsGoals.forEach { savingsGoals.upsert(it.toDomain()) }
        data.exchangeRates.forEach { exchangeRates.upsert(it.toDomain()) }
        data.transactions.forEach { transactions.upsert(it.toDomain()) }

        BackupSummary(
            transactions = data.transactions.size,
            categories = data.categories.size,
            paymentMethods = data.paymentMethods.size,
            accounts = data.accounts.size,
            budgets = data.budgets.size,
            recurring = data.recurring.size,
            savingsGoals = data.savingsGoals.size,
            exchangeRates = data.exchangeRates.size,
        )
    }
}
