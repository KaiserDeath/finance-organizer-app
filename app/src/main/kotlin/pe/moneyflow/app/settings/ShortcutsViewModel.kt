package pe.moneyflow.app.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.moneyflow.app.ShortcutOption
import pe.moneyflow.app.ShortcutPool
import pe.moneyflow.core.domain.repository.CategoryRepository
import pe.moneyflow.core.domain.repository.PaymentMethodRepository
import pe.moneyflow.core.domain.repository.SettingsRepository
import pe.moneyflow.core.model.Category
import pe.moneyflow.core.model.PaymentMethod
import pe.moneyflow.core.model.QuickShortcut
import javax.inject.Inject

data class ShortcutsUiState(
    val selected: List<ShortcutOption> = emptyList(),
    val categories: List<Category> = emptyList(),
    val methods: List<PaymentMethod> = emptyList(),
)

/**
 * The onboarding shortcut picker, as a destination you can return to.
 *
 * Onboarding offered this once and never again, so skipping step 4 meant waiting 30 days for
 * `GetFrequentShortcutsUseCase` to infer shortcuts from history — a documented degradation with no
 * way out of it. Same pool, same persistence, reachable from Ajustes.
 *
 * Selection is matched back from the stored [QuickShortcut]s by label: the pool is a fixed list of
 * presets, and a stored shortcut is only editable here if it came from that list.
 */
@HiltViewModel
class ShortcutsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    categoryRepository: CategoryRepository,
    paymentMethodRepository: PaymentMethodRepository,
) : ViewModel() {

    val uiState: StateFlow<ShortcutsUiState> = combine(
        settingsRepository.preferences,
        categoryRepository.observeAll(),
        paymentMethodRepository.observeAll(),
    ) { prefs, categories, methods ->
        val storedLabels = prefs.shortcuts.map { it.label }.toSet()
        ShortcutsUiState(
            selected = ShortcutPool.filter { it.label in storedLabels },
            categories = categories,
            methods = methods,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ShortcutsUiState(),
    )

    /** Writes immediately: there is no Guardar button, so a toggle is the commit. */
    fun toggle(option: ShortcutOption) {
        val state = uiState.value
        val next = if (option in state.selected) {
            state.selected - option
        } else {
            state.selected + option
        }
        viewModelScope.launch {
            settingsRepository.setShortcuts(next.map { it.toQuickShortcut(state) })
        }
    }

    /**
     * Names resolve to ids here rather than at write time in the pool, matching
     * `OnboardingViewModel.finish` — a missing match leaves the field unset and the shortcut still
     * logs, just without the category or method prefilled.
     */
    private fun ShortcutOption.toQuickShortcut(state: ShortcutsUiState) = QuickShortcut(
        label = label,
        amountMinor = amountMinor,
        categoryId = state.categories
            .firstOrNull { it.name.equals(categoryName, ignoreCase = true) }?.id,
        paymentMethodId = state.methods
            .firstOrNull { it.name.equals(methodName, ignoreCase = true) }?.id,
    )
}
