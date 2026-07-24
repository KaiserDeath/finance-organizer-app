package pe.moneyflow.feature.paymentmethods

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import pe.moneyflow.core.domain.usecase.ObservePaymentMethodsUseCase
import pe.moneyflow.core.model.PaymentMethod
import javax.inject.Inject

data class PaymentMethodsUiState(
    val isLoading: Boolean = true,
    val methods: List<PaymentMethod> = emptyList(),
)

@HiltViewModel
class PaymentMethodsViewModel @Inject constructor(
    observePaymentMethods: ObservePaymentMethodsUseCase,
) : ViewModel() {

    val uiState: StateFlow<PaymentMethodsUiState> = observePaymentMethods()
        .map { PaymentMethodsUiState(isLoading = false, methods = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PaymentMethodsUiState(),
        )
}
