package pe.moneyflow.feature.paymentmethods

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.moneyflow.core.domain.repository.AccountRepository
import pe.moneyflow.core.domain.usecase.DeletePaymentMethodUseCase
import pe.moneyflow.core.domain.usecase.ObservePaymentMethodsUseCase
import pe.moneyflow.core.domain.usecase.SaveAccountUseCase
import pe.moneyflow.core.domain.usecase.SavePaymentMethodUseCase
import pe.moneyflow.core.model.Account
import pe.moneyflow.core.model.PaymentMethod
import pe.moneyflow.core.ui.preset.toAccountType
import java.util.UUID
import javax.inject.Inject

data class PaymentMethodsUiState(
    val isLoading: Boolean = true,
    val methods: List<PaymentMethod> = emptyList(),
    val accounts: List<Account> = emptyList(),
)

@HiltViewModel
class PaymentMethodsViewModel @Inject constructor(
    observePaymentMethods: ObservePaymentMethodsUseCase,
    accountRepository: AccountRepository,
    private val savePaymentMethod: SavePaymentMethodUseCase,
    private val saveAccount: SaveAccountUseCase,
    private val deletePaymentMethod: DeletePaymentMethodUseCase,
) : ViewModel() {

    private var recentlyDeleted: PaymentMethod? = null

    val uiState: StateFlow<PaymentMethodsUiState> =
        combine(observePaymentMethods(), accountRepository.observeAll()) { methods, accounts ->
            PaymentMethodsUiState(isLoading = false, methods = methods, accounts = accounts)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PaymentMethodsUiState(),
        )

    fun save(method: PaymentMethod) {
        viewModelScope.launch { savePaymentMethod(method) }
    }

    /**
     * Save a new method and, when [alsoCreateAccount] is on, create a matching account (same
     * name/look) and link the method to it — so a "BCP" set up here also shows up under Cuentas.
     */
    fun saveWithAccount(method: PaymentMethod, alsoCreateAccount: Boolean, currencyCode: String) {
        viewModelScope.launch {
            var linkedAccountId = method.accountId
            if (alsoCreateAccount) {
                val accountId = UUID.randomUUID().toString()
                saveAccount(
                    Account(
                        id = accountId,
                        name = method.name,
                        type = method.type.toAccountType(),
                        currencyCode = currencyCode,
                        openingBalanceMinor = 0,
                        colorHex = method.colorHex,
                        iconKey = method.iconKey,
                    ),
                )
                linkedAccountId = accountId
            }
            savePaymentMethod(method.copy(accountId = linkedAccountId))
        }
    }

    fun delete(id: String) {
        recentlyDeleted = uiState.value.methods.firstOrNull { it.id == id }
        viewModelScope.launch { deletePaymentMethod(id) }
    }

    fun undoDelete() {
        val method = recentlyDeleted ?: return
        recentlyDeleted = null
        viewModelScope.launch { savePaymentMethod(method) }
    }
}
