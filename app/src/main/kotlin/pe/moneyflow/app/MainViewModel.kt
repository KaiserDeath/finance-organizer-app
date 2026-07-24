package pe.moneyflow.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import pe.moneyflow.core.domain.repository.SettingsRepository
import pe.moneyflow.core.model.UserPreferences
import javax.inject.Inject

data class MainUiState(
    val preferences: UserPreferences = UserPreferences(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<MainUiState> = settingsRepository.preferences
        .map { MainUiState(preferences = it, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MainUiState(),
        )
}
