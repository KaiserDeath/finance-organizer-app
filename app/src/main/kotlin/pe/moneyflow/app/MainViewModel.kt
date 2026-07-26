package pe.moneyflow.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.moneyflow.core.domain.repository.SettingsRepository
import pe.moneyflow.core.model.UserPreferences
import javax.inject.Inject

data class MainUiState(
    val preferences: UserPreferences = UserPreferences(),
    val isLoading: Boolean = true,
    /** True once the user has passed the app lock this session (PIN or biometric). */
    val unlocked: Boolean = false,
) {
    val showOnboarding: Boolean get() = !isLoading && !preferences.onboardingComplete
    val showLock: Boolean
        get() = !isLoading && preferences.onboardingComplete && preferences.appLockEnabled && !unlocked
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val unlocked = MutableStateFlow(false)

    val uiState: StateFlow<MainUiState> =
        combine(settingsRepository.preferences, unlocked) { preferences, unlocked ->
            MainUiState(preferences = preferences, isLoading = false, unlocked = unlocked)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MainUiState(),
        )

    fun unlock() {
        unlocked.value = true
    }

    fun completeOnboarding() {
        viewModelScope.launch { settingsRepository.setOnboardingComplete(true) }
    }
}
