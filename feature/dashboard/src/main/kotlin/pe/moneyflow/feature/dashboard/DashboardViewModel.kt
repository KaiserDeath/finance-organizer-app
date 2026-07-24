package pe.moneyflow.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import pe.moneyflow.core.domain.model.DashboardData
import pe.moneyflow.core.domain.usecase.GetDashboardUseCase
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val data: DashboardData = DashboardData.Empty,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    getDashboard: GetDashboardUseCase,
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = getDashboard()
        .map { DashboardUiState(isLoading = false, data = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardUiState(isLoading = true),
        )
}
