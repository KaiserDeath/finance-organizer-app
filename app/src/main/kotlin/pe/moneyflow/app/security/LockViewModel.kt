package pe.moneyflow.app.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.moneyflow.core.domain.repository.SettingsRepository
import pe.moneyflow.core.domain.usecase.VerifyPinUseCase
import javax.inject.Inject

@HiltViewModel
class LockViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    private val verifyPin: VerifyPinUseCase,
) : ViewModel() {

    val biometricEnabled: StateFlow<Boolean> =
        settingsRepository.preferences
            .map { it.biometricEnabled }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _error = MutableStateFlow(false)
    val error: StateFlow<Boolean> = _error

    fun verify(pin: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (verifyPin(pin)) {
                _error.value = false
                onSuccess()
            } else {
                _error.value = true
            }
        }
    }

    fun clearError() {
        _error.value = false
    }
}
