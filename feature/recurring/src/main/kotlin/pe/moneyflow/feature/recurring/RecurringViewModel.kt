package pe.moneyflow.feature.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.moneyflow.core.domain.usecase.DeleteRecurringExpenseUseCase
import pe.moneyflow.core.domain.usecase.ObserveCategoriesUseCase
import pe.moneyflow.core.domain.usecase.ObserveRecurringExpensesUseCase
import pe.moneyflow.core.domain.usecase.SaveRecurringExpenseUseCase
import pe.moneyflow.core.model.Category
import pe.moneyflow.core.model.RecurrenceFrequency
import pe.moneyflow.core.model.RecurringExpense
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class RecurringUiState(
    val isLoading: Boolean = true,
    val items: List<RecurringExpense> = emptyList(),
    val categories: List<Category> = emptyList(),
    val currencyCode: String = "PEN",
) {
    val isEmpty: Boolean get() = !isLoading && items.isEmpty()
}

@HiltViewModel
class RecurringViewModel @Inject constructor(
    observeRecurring: ObserveRecurringExpensesUseCase,
    observeCategories: ObserveCategoriesUseCase,
    private val saveRecurring: SaveRecurringExpenseUseCase,
    private val deleteRecurring: DeleteRecurringExpenseUseCase,
    private val clock: Clock,
) : ViewModel() {

    val uiState: StateFlow<RecurringUiState> =
        combine(observeRecurring(), observeCategories()) { items, categories ->
            RecurringUiState(
                isLoading = false,
                items = items,
                categories = categories,
                currencyCode = items.firstOrNull()?.currencyCode ?: "PEN",
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RecurringUiState(),
        )

    fun add(
        title: String,
        categoryId: String?,
        amountMinor: Long,
        frequency: RecurrenceFrequency,
        startDate: LocalDate,
        autoCreate: Boolean,
    ) {
        if (title.isBlank() || amountMinor <= 0) return
        viewModelScope.launch {
            saveRecurring(
                RecurringExpense(
                    id = UUID.randomUUID().toString(),
                    title = title.trim(),
                    amountMinor = amountMinor,
                    categoryId = categoryId,
                    frequency = frequency,
                    interval = 1,
                    nextRunDate = startDate,
                    autoCreate = autoCreate,
                ),
            )
        }
    }

    fun toggleAutoCreate(item: RecurringExpense) {
        viewModelScope.launch { saveRecurring(item.copy(autoCreate = !item.autoCreate)) }
    }

    fun delete(id: String) {
        viewModelScope.launch { deleteRecurring(id) }
    }

    fun today(): LocalDate = LocalDate.now(clock)
}
