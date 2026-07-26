package pe.moneyflow.core.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import pe.moneyflow.core.domain.model.Insight
import pe.moneyflow.core.domain.repository.CategoryRepository
import pe.moneyflow.core.domain.repository.SettingsRepository
import pe.moneyflow.core.domain.repository.SmartInsights
import pe.moneyflow.core.domain.repository.TransactionRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** Feeds live data into [InsightEngine] and streams the resulting suggestions. */
class RuleBasedSmartInsights @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock,
) : SmartInsights {

    override fun observe(): Flow<List<Insight>> = combine(
        transactionRepository.observeAll(),
        categoryRepository.observeAll(),
        settingsRepository.preferences,
    ) { transactions, categories, prefs ->
        InsightEngine.generate(
            transactions = transactions,
            categories = categories,
            today = LocalDate.now(clock),
            currencyCode = prefs.currencyCode,
        )
    }
}
