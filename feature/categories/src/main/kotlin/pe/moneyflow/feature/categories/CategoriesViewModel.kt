package pe.moneyflow.feature.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.moneyflow.core.domain.usecase.DeleteCategoryUseCase
import pe.moneyflow.core.domain.usecase.ObserveCategoriesUseCase
import pe.moneyflow.core.domain.usecase.SaveCategoryUseCase
import pe.moneyflow.core.model.Category
import pe.moneyflow.core.model.CategoryType
import java.util.UUID
import javax.inject.Inject

data class CategoriesUiState(
    val isLoading: Boolean = true,
    val expense: List<Category> = emptyList(),
    val income: List<Category> = emptyList(),
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    observeCategories: ObserveCategoriesUseCase,
    private val saveCategory: SaveCategoryUseCase,
    private val deleteCategory: DeleteCategoryUseCase,
) : ViewModel() {

    private var recentlyDeleted: Category? = null

    val uiState: StateFlow<CategoriesUiState> = observeCategories()
        .map { categories ->
            CategoriesUiState(
                isLoading = false,
                expense = categories.filter { it.type == CategoryType.EXPENSE },
                income = categories.filter { it.type == CategoryType.INCOME },
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CategoriesUiState(),
        )

    fun add(name: String, colorHex: String, iconKey: String, type: CategoryType) {
        if (name.isBlank()) return
        viewModelScope.launch {
            saveCategory(
                Category(
                    id = UUID.randomUUID().toString(),
                    name = name.trim(),
                    iconKey = iconKey,
                    colorHex = colorHex,
                    type = type,
                    isDefault = false,
                    sortOrder = 999,
                ),
            )
        }
    }

    fun delete(id: String) {
        recentlyDeleted = (uiState.value.expense + uiState.value.income).firstOrNull { it.id == id }
        viewModelScope.launch { deleteCategory(id) }
    }

    fun undoDelete() {
        val toRestore = recentlyDeleted ?: return
        recentlyDeleted = null
        viewModelScope.launch { saveCategory(toRestore) }
    }
}
