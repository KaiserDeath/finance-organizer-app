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
import pe.moneyflow.core.domain.repository.SettingsRepository
import pe.moneyflow.core.domain.usecase.DeletePaymentMethodUseCase
import pe.moneyflow.core.domain.usecase.ObservePaymentMethodsUseCase
import pe.moneyflow.core.domain.usecase.SaveAccountUseCase
import pe.moneyflow.core.domain.usecase.SavePaymentMethodUseCase
import pe.moneyflow.core.domain.usecase.SetDefaultPaymentMethodUseCase
import pe.moneyflow.core.model.Account
import pe.moneyflow.core.model.PaymentMethod
import pe.moneyflow.core.ui.preset.accountTypeFor
import java.util.UUID
import javax.inject.Inject

data class PaymentMethodsUiState(
    val isLoading: Boolean = true,
    val methods: List<PaymentMethod> = emptyList(),
    val accounts: List<Account> = emptyList(),
    /** The user's base currency, for accounts co-created from a payment method. */
    val currencyCode: String = "PEN",
) {
    /** Genuinely no methods configured, as opposed to not having loaded yet. */
    val isEmpty: Boolean get() = !isLoading && methods.isEmpty()
}

@HiltViewModel
class PaymentMethodsViewModel @Inject constructor(
    observePaymentMethods: ObservePaymentMethodsUseCase,
    accountRepository: AccountRepository,
    settingsRepository: SettingsRepository,
    private val savePaymentMethod: SavePaymentMethodUseCase,
    private val saveAccount: SaveAccountUseCase,
    private val deletePaymentMethod: DeletePaymentMethodUseCase,
    private val setDefaultPaymentMethod: SetDefaultPaymentMethodUseCase,
) : ViewModel() {

    private var recentlyDeleted: PaymentMethod? = null

    val uiState: StateFlow<PaymentMethodsUiState> =
        combine(
            observePaymentMethods(),
            accountRepository.observeAll(),
            settingsRepository.preferences,
        ) { methods, accounts, prefs ->
            PaymentMethodsUiState(
                isLoading = false,
                methods = methods,
                accounts = accounts,
                currencyCode = prefs.currencyCode,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PaymentMethodsUiState(),
        )

    fun save(method: PaymentMethod) {
        viewModelScope.launch {
            savePaymentMethod(method)
            // Saving a method *as* the default is still a change to the whole set, so it goes
            // through the same one-default rule as the explicit action below.
            if (method.isDefault) setDefaultPaymentMethod(method.id)
        }
    }

    /**
     * Makes [id] the default, immediately — no form to submit.
     *
     * The default used to be a Switch inside the edit sheet, so changing it meant opening an editor
     * on a method you did not want to edit and remembering to save. It is one fact about one
     * method; it gets one tap.
     */
    fun setDefault(id: String) {
        viewModelScope.launch { setDefaultPaymentMethod(id) }
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
                        type = accountTypeFor(method.type, method.cardKind),
                        currencyCode = currencyCode,
                        openingBalanceMinor = 0,
                        colorHex = method.colorHex,
                        iconKey = method.iconKey,
                    ),
                )
                linkedAccountId = accountId
            }
            savePaymentMethod(method.copy(accountId = linkedAccountId))
            // Same one-default rule as [save]: this is the path the sheet actually takes, so
            // leaving it out here would let creating a method as the default produce two.
            if (method.isDefault) setDefaultPaymentMethod(method.id)
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
