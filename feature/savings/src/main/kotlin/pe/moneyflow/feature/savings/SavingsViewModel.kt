package pe.moneyflow.feature.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.moneyflow.core.domain.repository.SettingsRepository
import pe.moneyflow.core.domain.usecase.ContributeToSavingsGoalUseCase
import pe.moneyflow.core.domain.usecase.DeleteSavingsGoalUseCase
import pe.moneyflow.core.domain.usecase.ObserveSavingsGoalsUseCase
import pe.moneyflow.core.domain.usecase.SaveSavingsGoalUseCase
import pe.moneyflow.core.model.SavingsGoal
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class SavingsUiState(
    val isLoading: Boolean = true,
    val goals: List<SavingsGoal> = emptyList(),
    val currencyCode: String = "PEN",
) {
    val totalSavedMinor: Long get() = goals.sumOf { it.currentAmountMinor }
    val totalTargetMinor: Long get() = goals.sumOf { it.targetAmountMinor }
    val isEmpty: Boolean get() = !isLoading && goals.isEmpty()
}

@HiltViewModel
class SavingsViewModel @Inject constructor(
    observeGoals: ObserveSavingsGoalsUseCase,
    settingsRepository: SettingsRepository,
    private val saveGoal: SaveSavingsGoalUseCase,
    private val contributeUseCase: ContributeToSavingsGoalUseCase,
    private val deleteGoal: DeleteSavingsGoalUseCase,
) : ViewModel() {

    private var recentlyDeleted: SavingsGoal? = null

    val uiState: StateFlow<SavingsUiState> =
        combine(observeGoals(), settingsRepository.preferences) { goals, prefs ->
            SavingsUiState(
                isLoading = false,
                goals = goals,
                currencyCode = prefs.currencyCode,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SavingsUiState(),
        )

    fun add(name: String, targetMinor: Long, targetDate: LocalDate?) {
        if (name.isBlank() || targetMinor <= 0) return
        viewModelScope.launch {
            saveGoal(
                SavingsGoal(
                    id = UUID.randomUUID().toString(),
                    name = name.trim(),
                    targetAmountMinor = targetMinor,
                    targetDate = targetDate,
                ),
            )
        }
    }

    fun contribute(goalId: String, deltaMinor: Long) {
        if (deltaMinor == 0L) return
        viewModelScope.launch { contributeUseCase(goalId, deltaMinor) }
    }

    fun delete(id: String) {
        recentlyDeleted = uiState.value.goals.firstOrNull { it.id == id }
        viewModelScope.launch { deleteGoal(id) }
    }

    fun undoDelete() {
        val toRestore = recentlyDeleted ?: return
        recentlyDeleted = null
        viewModelScope.launch { saveGoal(toRestore) }
    }
}
