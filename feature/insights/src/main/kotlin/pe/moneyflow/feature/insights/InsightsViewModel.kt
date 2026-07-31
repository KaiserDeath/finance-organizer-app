package pe.moneyflow.feature.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import pe.moneyflow.core.domain.model.Insight
import pe.moneyflow.core.domain.usecase.GetInsightsUseCase
import javax.inject.Inject

data class InsightsUiState(
    val isLoading: Boolean = true,
    val insights: List<Insight> = emptyList(),
) {
    /** Genuinely nothing to suggest, as opposed to not having loaded yet. */
    val isEmpty: Boolean get() = !isLoading && insights.isEmpty()
}

@HiltViewModel
class InsightsViewModel @Inject constructor(
    getInsights: GetInsightsUseCase,
) : ViewModel() {

    val uiState: StateFlow<InsightsUiState> =
        getInsights().map { InsightsUiState(isLoading = false, insights = it) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = InsightsUiState(),
            )
}
