package pe.moneyflow.core.domain.model

/** Counts of each record type in a backup, for confirming an export or import to the user. */
data class BackupSummary(
    val transactions: Int = 0,
    val categories: Int = 0,
    val paymentMethods: Int = 0,
    val accounts: Int = 0,
    val budgets: Int = 0,
    val recurring: Int = 0,
    val savingsGoals: Int = 0,
    val exchangeRates: Int = 0,
) {
    val total: Int
        get() = transactions + categories + paymentMethods + accounts +
            budgets + recurring + savingsGoals + exchangeRates
}
