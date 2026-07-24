package pe.moneyflow.core.model

import java.time.Instant

/**
 * A wallet/account holding money (Cash, a bank account, a credit card…). Needed to answer
 * "how much money remains" and to compute net worth. [openingBalanceMinor] is the starting
 * balance; the current balance is opening balance +/- its transactions.
 */
data class Account(
    val id: String,
    val name: String,
    val type: AccountType = AccountType.CASH,
    val currencyCode: String = "PEN",
    val openingBalanceMinor: Long = 0,
    val colorHex: String,
    val iconKey: String,
    val archived: Boolean = false,
    val createdAt: Instant = Instant.now(),
)
