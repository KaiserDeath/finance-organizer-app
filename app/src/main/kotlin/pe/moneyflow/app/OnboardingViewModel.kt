package pe.moneyflow.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.moneyflow.core.domain.repository.CategoryRepository
import pe.moneyflow.core.domain.repository.PaymentMethodRepository
import pe.moneyflow.core.domain.repository.SettingsRepository
import pe.moneyflow.core.model.Category
import pe.moneyflow.core.model.PaymentMethod
import pe.moneyflow.core.model.QuickShortcut
import javax.inject.Inject

/** One of the daily-purchase presets offered in step 4, before it resolves to real ids. */
data class ShortcutOption(
    val label: String,
    val amountMinor: Long,
    val categoryName: String,
    val methodName: String,
)

/** The six everyday purchases the prototype tested with; the user picks which apply. */
val ShortcutPool = listOf(
    ShortcutOption("Almuerzo", 1800, "Comida", "Yape"),
    ShortcutOption("Pasaje", 500, "Transporte", "Efectivo"),
    ShortcutOption("Mercado", 9000, "Comida", "BCP"),
    ShortcutOption("Café", 800, "Comida", "Yape"),
    ShortcutOption("Taxi", 1500, "Transporte", "Yape"),
    ShortcutOption("Delivery", 3000, "Restaurantes", "Visa crédito"),
)

data class OnboardingUiState(
    val methods: List<PaymentMethod> = emptyList(),
    val categories: List<Category> = emptyList(),
)

/**
 * Persists what onboarding collects. Every step pays for a concrete screen, so every skip has a
 * defined degradation rather than a broken one:
 *  - no budget → the hero has no denominator and renders its no-budget state;
 *  - no methods → every method stays active;
 *  - no shortcuts → the "De un toque" row stays empty until 30 days of history exist.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    paymentMethodRepository: PaymentMethodRepository,
    categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<OnboardingUiState> = combine(
        paymentMethodRepository.observeAll(),
        categoryRepository.observeAll(),
    ) { methods, categories ->
        OnboardingUiState(methods = methods, categories = categories)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = OnboardingUiState(),
    )

    /**
     * Writes the collected setup, then marks onboarding done. Names are resolved to ids against
     * the seeded data at this point and tolerate a missing match (the shortcut still works with
     * the field unset).
     */
    fun finish(
        monthlyBudgetMinor: Long?,
        selectedMethodIds: Set<String>?,
        selectedShortcuts: List<ShortcutOption>,
        onDone: () -> Unit,
    ) {
        val state = uiState.value
        val shortcuts = selectedShortcuts.map { option ->
            QuickShortcut(
                label = option.label,
                amountMinor = option.amountMinor,
                categoryId = state.categories
                    .firstOrNull { it.name.equals(option.categoryName, ignoreCase = true) }?.id,
                paymentMethodId = state.methods
                    .firstOrNull { it.name.equals(option.methodName, ignoreCase = true) }?.id,
            )
        }
        viewModelScope.launch {
            settingsRepository.setMonthlyBudget(monthlyBudgetMinor)
            // Selecting everything is the same as not restricting anything.
            settingsRepository.setActiveMethodIds(
                selectedMethodIds?.takeIf { it.size < state.methods.size },
            )
            settingsRepository.setShortcuts(shortcuts)
            onDone()
        }
    }
}
