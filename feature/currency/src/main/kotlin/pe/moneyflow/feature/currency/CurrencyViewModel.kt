package pe.moneyflow.feature.currency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.moneyflow.core.domain.repository.SettingsRepository
import pe.moneyflow.core.domain.usecase.DeleteExchangeRateUseCase
import pe.moneyflow.core.domain.usecase.ObserveExchangeRatesUseCase
import pe.moneyflow.core.domain.usecase.SaveExchangeRateUseCase
import pe.moneyflow.core.model.ExchangeRate
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class CurrencyUiState(
    val isLoading: Boolean = true,
    val baseCurrency: String = "PEN",
    val rates: List<ExchangeRate> = emptyList(),
)

@HiltViewModel
class CurrencyViewModel @Inject constructor(
    observeRates: ObserveExchangeRatesUseCase,
    private val settingsRepository: SettingsRepository,
    private val saveRate: SaveExchangeRateUseCase,
    private val deleteRate: DeleteExchangeRateUseCase,
    private val clock: Clock,
) : ViewModel() {

    val uiState: StateFlow<CurrencyUiState> =
        combine(observeRates(), settingsRepository.preferences) { rates, prefs ->
            CurrencyUiState(
                isLoading = false,
                baseCurrency = prefs.currencyCode,
                rates = rates,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CurrencyUiState(),
        )

    fun setBaseCurrency(code: String) {
        viewModelScope.launch { settingsRepository.setCurrency(code) }
    }

    fun addRate(base: String, quote: String, rate: Double) {
        if (base.equals(quote, ignoreCase = true) || rate <= 0.0) return
        viewModelScope.launch {
            saveRate(
                ExchangeRate(
                    id = UUID.randomUUID().toString(),
                    base = base.uppercase(),
                    quote = quote.uppercase(),
                    rate = rate,
                    asOf = LocalDate.now(clock),
                ),
            )
        }
    }

    fun delete(id: String) {
        viewModelScope.launch { deleteRate(id) }
    }
}
