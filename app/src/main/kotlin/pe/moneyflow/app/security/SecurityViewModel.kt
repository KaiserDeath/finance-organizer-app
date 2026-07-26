package pe.moneyflow.app.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.moneyflow.core.domain.usecase.ClearPinUseCase
import pe.moneyflow.core.domain.usecase.SetBiometricEnabledUseCase
import pe.moneyflow.core.domain.usecase.SetPinUseCase
import pe.moneyflow.core.domain.repository.SettingsRepository
import javax.inject.Inject

data class SecurityUiState(
    val lockEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,
)

@HiltViewModel
class SecurityViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    private val setPinUseCase: SetPinUseCase,
    private val clearPinUseCase: ClearPinUseCase,
    private val setBiometricUseCase: SetBiometricEnabledUseCase,
) : ViewModel() {

    val uiState: StateFlow<SecurityUiState> =
        settingsRepository.preferences
            .map { SecurityUiState(lockEnabled = it.appLockEnabled, biometricEnabled = it.biometricEnabled) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SecurityUiState())

    fun setPin(pin: String) {
        if (pin.length < 4) return
        viewModelScope.launch { setPinUseCase(pin) }
    }

    fun removePin() {
        viewModelScope.launch { clearPinUseCase() }
    }

    fun setBiometric(enabled: Boolean) {
        viewModelScope.launch { setBiometricUseCase(enabled) }
    }
}
