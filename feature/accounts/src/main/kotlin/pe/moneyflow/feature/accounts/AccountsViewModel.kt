package pe.moneyflow.feature.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.moneyflow.core.domain.model.NetWorth
import pe.moneyflow.core.domain.usecase.ArchiveAccountUseCase
import pe.moneyflow.core.domain.usecase.CreateTransferUseCase
import pe.moneyflow.core.domain.usecase.GetNetWorthUseCase
import pe.moneyflow.core.domain.usecase.SaveAccountUseCase
import pe.moneyflow.core.model.Account
import pe.moneyflow.core.model.AccountType
import java.util.UUID
import javax.inject.Inject

data class AccountsUiState(
    val isLoading: Boolean = true,
    val netWorth: NetWorth? = null,
) {
    val accounts: List<Account> get() = netWorth?.balances?.map { it.account }.orEmpty()
    val isEmpty: Boolean get() = !isLoading && (netWorth?.balances.isNullOrEmpty())
}

@HiltViewModel
class AccountsViewModel @Inject constructor(
    getNetWorth: GetNetWorthUseCase,
    private val saveAccount: SaveAccountUseCase,
    private val archiveAccount: ArchiveAccountUseCase,
    private val createTransfer: CreateTransferUseCase,
) : ViewModel() {

    val uiState: StateFlow<AccountsUiState> =
        getNetWorth().map { AccountsUiState(isLoading = false, netWorth = it) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = AccountsUiState(),
            )

    fun add(
        name: String,
        type: AccountType,
        currencyCode: String,
        openingBalanceMinor: Long,
    ) {
        if (name.isBlank()) return
        val preset = AccountPresets.of(type)
        viewModelScope.launch {
            saveAccount(
                Account(
                    id = UUID.randomUUID().toString(),
                    name = name.trim(),
                    type = type,
                    currencyCode = currencyCode,
                    openingBalanceMinor = openingBalanceMinor,
                    colorHex = preset.colorHex,
                    iconKey = preset.iconKey,
                ),
            )
        }
    }

    fun archive(account: Account) {
        viewModelScope.launch { archiveAccount(account) }
    }

    fun transfer(fromAccountId: String, toAccountId: String, amountMinor: Long, currencyCode: String) {
        viewModelScope.launch {
            createTransfer(fromAccountId, toAccountId, amountMinor, currencyCode)
        }
    }
}
