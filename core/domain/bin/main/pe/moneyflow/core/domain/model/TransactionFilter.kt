package pe.moneyflow.core.domain.model

import pe.moneyflow.core.model.TransactionType
import java.time.LocalDate

/**
 * Criteria for narrowing the transaction list. An empty set / null field means "no constraint"
 * on that dimension, so the default [TransactionFilter] matches everything.
 */
data class TransactionFilter(
    val query: String = "",
    val types: Set<TransactionType> = emptySet(),
    val categoryIds: Set<String> = emptySet(),
    val start: LocalDate? = null,
    val end: LocalDate? = null,
) {
    val isActive: Boolean
        get() = query.isNotBlank() ||
            types.isNotEmpty() ||
            categoryIds.isNotEmpty() ||
            start != null ||
            end != null
}
