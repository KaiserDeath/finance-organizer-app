package pe.moneyflow.feature.transactions

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pe.moneyflow.core.domain.usecase.DeleteTransactionUseCase
import pe.moneyflow.core.domain.usecase.FilterTransactionsUseCase
import pe.moneyflow.core.domain.usecase.GetTransactionUseCase
import pe.moneyflow.core.domain.usecase.ObserveCategoriesUseCase
import pe.moneyflow.core.domain.usecase.ObserveTransactionsUseCase
import pe.moneyflow.core.domain.usecase.SaveTransactionUseCase
import pe.moneyflow.core.model.Category
import pe.moneyflow.core.model.CategoryType
import pe.moneyflow.core.model.Transaction
import pe.moneyflow.core.model.TransactionType
import pe.moneyflow.core.testing.FakeCategoryRepository
import pe.moneyflow.core.testing.FakeTransactionRepository
import pe.moneyflow.core.testing.MainDispatcherRule
import java.time.LocalDate

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TransactionsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val comida = Category(
        id = "comida",
        name = "Comida",
        iconKey = "food",
        colorHex = "#FF7043",
        type = CategoryType.EXPENSE,
    )
    private val transporte = Category(
        id = "transporte",
        name = "Transporte",
        iconKey = "car",
        colorHex = "#42A5F5",
        type = CategoryType.EXPENSE,
    )

    private fun transaction(
        id: String,
        title: String,
        categoryId: String,
        amountMinor: Long,
        date: LocalDate,
        type: TransactionType = TransactionType.EXPENSE,
    ) = Transaction(
        id = id,
        title = title,
        amountMinor = amountMinor,
        categoryId = categoryId,
        type = type,
        actualDate = date,
    )

    private fun viewModel(
        repo: FakeTransactionRepository,
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ) = TransactionsViewModel(
        savedStateHandle = savedStateHandle,
        observeTransactions = ObserveTransactionsUseCase(repo),
        observeCategories = ObserveCategoriesUseCase(
            FakeCategoryRepository(listOf(comida, transporte)),
        ),
        filterTransactions = FilterTransactionsUseCase(),
        getTransaction = GetTransactionUseCase(repo),
        deleteTransaction = DeleteTransactionUseCase(repo),
        saveTransaction = SaveTransactionUseCase(repo),
    )

    @Test
    fun `category route starts with movements filtered to that category`() = runTest {
        val repo = FakeTransactionRepository(
            listOf(
                transaction("lunch", "Almuerzo", "comida", 12_000, LocalDate.of(2026, 8, 5)),
                transaction("bus", "Bus", "transporte", 4_000, LocalDate.of(2026, 8, 4)),
            ),
        )
        val vm = viewModel(repo, SavedStateHandle(mapOf("categoryId" to "comida")))

        val state = vm.uiState.first { !it.isLoading }

        assertEquals(setOf("comida"), state.filter.categoryIds)
        assertEquals(listOf("lunch"), state.sections.single().items.map { it.id })
    }

    @Test
    fun `query and type filters update sections without changing source data`() = runTest {
        val repo = FakeTransactionRepository(
            listOf(
                transaction("lunch", "Almuerzo", "comida", 12_000, LocalDate.of(2026, 8, 5)),
                transaction("salary", "Sueldo", "comida", 200_000, LocalDate.of(2026, 8, 1), TransactionType.INCOME),
            ),
        )
        val vm = viewModel(repo)

        vm.onQueryChange("suel")
        vm.toggleType(TransactionType.INCOME)
        advanceUntilIdle()

        val state = vm.uiState.first {
            it.filter.query == "suel" && it.filter.types == setOf(TransactionType.INCOME)
        }
        assertEquals(listOf("salary"), state.sections.single().items.map { it.id })
        assertEquals(0L, state.sections.single().expenseTotalMinor)
        assertEquals(2, repo.all().size)
    }

    @Test
    fun `sections are newest first and totals include expenses only`() = runTest {
        val repo = FakeTransactionRepository(
            listOf(
                transaction("old", "Café", "comida", 3_000, LocalDate.of(2026, 8, 1)),
                transaction("new-expense", "Taxi", "transporte", 7_000, LocalDate.of(2026, 8, 6)),
                transaction("new-income", "Sueldo", "comida", 100_000, LocalDate.of(2026, 8, 6), TransactionType.INCOME),
            ),
        )

        val state = viewModel(repo).uiState.first { !it.isLoading }

        assertEquals(
            listOf(listOf("new-expense", "new-income"), listOf("old")),
            state.sections.map { section -> section.items.map { it.id } },
        )
        assertEquals(7_000L, state.sections.first().expenseTotalMinor)
        assertEquals(3_000L, state.sections.last().expenseTotalMinor)
    }

    @Test
    fun `delete followed by undo restores the exact transaction`() = runTest {
        val original = transaction("lunch", "Almuerzo", "comida", 12_000, LocalDate.of(2026, 8, 5))
        val repo = FakeTransactionRepository(listOf(original))
        val vm = viewModel(repo)

        vm.delete(original.id)
        advanceUntilIdle()
        assertTrue(repo[original.id] == null)

        vm.undoDelete()
        advanceUntilIdle()
        assertEquals(original, repo[original.id])
    }
}
