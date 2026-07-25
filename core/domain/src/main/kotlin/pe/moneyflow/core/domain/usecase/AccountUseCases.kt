package pe.moneyflow.core.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import pe.moneyflow.core.domain.model.AccountBalance
import pe.moneyflow.core.domain.model.NetWorth
import pe.moneyflow.core.domain.repository.AccountRepository
import pe.moneyflow.core.domain.repository.ExchangeRateRepository
import pe.moneyflow.core.domain.repository.SettingsRepository
import pe.moneyflow.core.domain.repository.TransactionRepository
import pe.moneyflow.core.model.Account
import pe.moneyflow.core.model.ExchangeRate
import pe.moneyflow.core.model.Transaction
import pe.moneyflow.core.model.TransactionStatus
import pe.moneyflow.core.model.TransactionType
import javax.inject.Inject

class ObserveAccountsUseCase @Inject constructor(
    private val repository: AccountRepository,
) {
    operator fun invoke(): Flow<List<Account>> = repository.observeAll()
}

class SaveAccountUseCase @Inject constructor(
    private val repository: AccountRepository,
) {
    suspend operator fun invoke(account: Account) = repository.upsert(account)
}

/** Archiving hides an account from balances and pickers without deleting its history. */
class ArchiveAccountUseCase @Inject constructor(
    private val repository: AccountRepository,
) {
    suspend operator fun invoke(account: Account) =
        repository.upsert(account.copy(archived = true))
}

/**
 * Streams each (non-archived) account with its current balance computed from paid transactions,
 * for the accounts list. Balances are in each account's own currency; conversion is left to
 * [GetNetWorthUseCase].
 */
class GetAccountBalancesUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
) {
    operator fun invoke(): Flow<List<AccountBalance>> = combine(
        accountRepository.observeAll(),
        transactionRepository.observeAll(),
    ) { accounts, transactions ->
        accounts.map { account ->
            AccountBalance(
                account = account,
                currentBalanceMinor = balanceOf(account, transactions),
                convertedMinor = null,
            )
        }
    }
}

/**
 * Streams overall net worth: every account balance converted into the user's base currency and
 * summed, split into assets (positive) and liabilities (negative).
 */
class GetNetWorthUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val settingsRepository: SettingsRepository,
) {
    operator fun invoke(): Flow<NetWorth> = combine(
        accountRepository.observeAll(),
        transactionRepository.observeAll(),
        exchangeRateRepository.observeAll(),
        settingsRepository.preferences,
    ) { accounts, transactions, rates, prefs ->
        netWorth(accounts, transactions, rates, prefs.currencyCode)
    }

    private fun netWorth(
        accounts: List<Account>,
        transactions: List<Transaction>,
        rates: List<ExchangeRate>,
        baseCurrency: String,
    ): NetWorth {
        var assets = 0L
        var liabilities = 0L
        var hasUnconvertible = false

        val balances = accounts.map { account ->
            val raw = balanceOf(account, transactions)
            val converted = CurrencyConverter.convert(raw, account.currencyCode, baseCurrency, rates)
            if (converted == null) {
                hasUnconvertible = true
            } else if (converted >= 0) {
                assets += converted
            } else {
                liabilities += converted
            }
            AccountBalance(account = account, currentBalanceMinor = raw, convertedMinor = converted)
        }

        return NetWorth(
            currencyCode = baseCurrency,
            totalMinor = assets + liabilities,
            assetsMinor = assets,
            liabilitiesMinor = liabilities,
            balances = balances,
            hasUnconvertible = hasUnconvertible,
        )
    }
}

/**
 * Current balance of [account]: opening balance adjusted by every *paid* movement touching it —
 * income in, expenses out, transfers out of it and transfers into it.
 */
internal fun balanceOf(account: Account, transactions: List<Transaction>): Long {
    var balance = account.openingBalanceMinor
    for (tx in transactions) {
        if (tx.status != TransactionStatus.PAID) continue
        when (tx.type) {
            TransactionType.INCOME -> if (tx.accountId == account.id) balance += tx.amountMinor
            TransactionType.EXPENSE -> if (tx.accountId == account.id) balance -= tx.amountMinor
            TransactionType.TRANSFER -> {
                if (tx.accountId == account.id) balance -= tx.amountMinor
                if (tx.transferAccountId == account.id) balance += tx.amountMinor
            }
        }
    }
    return balance
}
