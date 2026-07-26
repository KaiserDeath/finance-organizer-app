package pe.moneyflow.core.domain.usecase

import pe.moneyflow.core.domain.model.TransactionFilter
import pe.moneyflow.core.model.Transaction
import javax.inject.Inject

/**
 * Applies a [TransactionFilter] to a list of transactions. Pure and synchronous so it can be
 * driven from a ViewModel's combined state and unit-tested directly. Text search matches the
 * title, description and notes case-insensitively; date bounds use the effective date.
 */
class FilterTransactionsUseCase @Inject constructor() {
    operator fun invoke(
        transactions: List<Transaction>,
        filter: TransactionFilter,
    ): List<Transaction> {
        if (!filter.isActive) return transactions
        val query = filter.query.trim().lowercase()
        return transactions.filter { tx ->
            matchesQuery(tx, query) &&
                (filter.types.isEmpty() || tx.type in filter.types) &&
                (filter.categoryIds.isEmpty() || tx.categoryId in filter.categoryIds) &&
                withinBounds(tx, filter)
        }
    }

    private fun matchesQuery(tx: Transaction, query: String): Boolean {
        if (query.isEmpty()) return true
        return tx.title.lowercase().contains(query) ||
            tx.description?.lowercase()?.contains(query) == true ||
            tx.notes?.lowercase()?.contains(query) == true
    }

    private fun withinBounds(tx: Transaction, filter: TransactionFilter): Boolean {
        if (filter.start == null && filter.end == null) return true
        val date = tx.effectiveDate ?: return false
        if (filter.start != null && date < filter.start) return false
        if (filter.end != null && date > filter.end) return false
        return true
    }
}
