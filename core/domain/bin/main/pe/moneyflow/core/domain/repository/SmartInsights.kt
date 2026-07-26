package pe.moneyflow.core.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.moneyflow.core.domain.model.Insight

/**
 * Source of smart suggestions. The current implementation is rule-based; a future LLM-backed
 * variant can implement this same interface without touching callers.
 */
interface SmartInsights {
    fun observe(): Flow<List<Insight>>
}
