package pe.moneyflow.core.domain.usecase

import kotlinx.coroutines.flow.Flow
import pe.moneyflow.core.domain.model.Insight
import pe.moneyflow.core.domain.repository.SmartInsights
import javax.inject.Inject

class GetInsightsUseCase @Inject constructor(
    private val smartInsights: SmartInsights,
) {
    operator fun invoke(): Flow<List<Insight>> = smartInsights.observe()
}
