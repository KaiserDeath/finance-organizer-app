package pe.moneyflow.feature.transactions

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

/**
 * The filter state and the day grouping — the whole of what this ViewModel owns.
 *
 * The module reported `NO-SOURCE` until now, which left the query, the two independent filter
 * dimensions, delete-with-undo and the daily subtotals all uncovered.
 */
class TransactionsViewModelTest {

    // Unconfined rather than the default Standard: these assertions are about state that only
    // exists while `uiState` is being collected, so the collector below and the ViewModel's own
    // sharing coroutine both have to run eagerly for a read after an action to mean anything.
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

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
        iconKey = "bus",
        colorHex = "#42A5F5",
        type = CategoryType.EXPENSE,
    )

    private val today = LocalDate.of(2026, 8, 15)

    private fun tx(
        id: String,
        title: String = id,
        amountMinor: Long = 1_000,
        categoryId: String? = "comida",
        type: TransactionType = TransactionType.EXPENSE,
        date: LocalDate = today,
    ) = Transaction(
        id = id,
        title = title,
        amountMinor = amountMinor,
        categoryId = categoryId,
        type = type,
        actualDate = date,
    )

    /**
     * Builds the ViewModel **and keeps its state flow hot for the whole test**.
     *
     * `uiState` is `stateIn(WhileSubscribed(5_000))`, so it only recomputes while something is
     * collecting. Reading it with a fresh `first { }` per assertion subscribes, takes the cached
     * value and unsubscribes — which returns the state from *before* the call under test, and the
     * filter assertions silently pass against stale data. The same trap `FakeSettingsRepository`
     * documents for writes; the filter lives only in the ViewModel, so there is no store to read
     * instead and the subscription has to be held open.
     *
     * The handle is empty on purpose — see the note on route arguments at the bottom of this file.
     */
    private fun TestScope.viewModel(
        transactions: List<Transaction> = emptyList(),
    ): Pair<TransactionsViewModel, FakeTransactionRepository> {
        val repo = FakeTransactionRepository(transactions)
        val vm = TransactionsViewModel(
            savedStateHandle = SavedStateHandle(),
            observeTransactions = ObserveTransactionsUseCase(repo),
            observeCategories = ObserveCategoriesUseCase(
                FakeCategoryRepository(listOf(comida, transporte)),
            ),
            filterTransactions = FilterTransactionsUseCase(),
            getTransaction = GetTransactionUseCase(repo),
            deleteTransaction = DeleteTransactionUseCase(repo),
            saveTransaction = SaveTransactionUseCase(repo),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        return vm to repo
    }

    private fun TransactionsViewModel.loaded(): TransactionsUiState = uiState.value

    private fun TransactionsViewModel.visibleIds(): List<String> =
        loaded().sections.flatMap { section -> section.items.map { it.id } }

    @Test
    fun `with no route argument nothing is filtered`() = runTest {
        val (vm, _) = viewModel(transactions = listOf(tx("a")))

        assertTrue(vm.loaded().filter.categoryIds.isEmpty())
        assertFalse(vm.loaded().isFilterActive)
    }

    // ---------------------------------------------------------------------------------------
    // The "Todas" chip. It used to be expressed as a loop calling toggleCategory over the
    // current selection; clearCategories makes it one intent.
    // ---------------------------------------------------------------------------------------

    @Test
    fun `clearCategories drops every category constraint at once`() = runTest {
        val (vm, _) = viewModel(transactions = listOf(tx("a"), tx("b", categoryId = "transporte")))
        vm.toggleCategory("comida")
        vm.toggleCategory("transporte")
        assertEquals(setOf("comida", "transporte"), vm.loaded().filter.categoryIds)

        vm.clearCategories()

        assertTrue(vm.loaded().filter.categoryIds.isEmpty())
        assertEquals(listOf("a", "b"), vm.visibleIds().sorted())
    }

    /** The chip row must not reach up and reset the search box above it. */
    @Test
    fun `clearCategories leaves the query and the type filters alone`() = runTest {
        val (vm, _) = viewModel(transactions = listOf(tx("a", title = "Almuerzo")))
        vm.onQueryChange("Almuerzo")
        vm.toggleType(TransactionType.EXPENSE)
        vm.toggleCategory("comida")

        vm.clearCategories()

        val state = vm.loaded()
        assertEquals("Almuerzo", state.filter.query)
        assertEquals(setOf(TransactionType.EXPENSE), state.filter.types)
        assertTrue(state.filter.categoryIds.isEmpty())
    }

    /** clearFilters is the broader one, and still is. */
    @Test
    fun `clearFilters drops everything`() = runTest {
        val (vm, _) = viewModel(transactions = listOf(tx("a")))
        vm.onQueryChange("algo")
        vm.toggleType(TransactionType.EXPENSE)
        vm.toggleCategory("comida")

        vm.clearFilters()

        assertFalse(vm.loaded().isFilterActive)
    }

    // ---------------------------------------------------------------------------------------
    // Filtering
    // ---------------------------------------------------------------------------------------

    @Test
    fun `toggling a category twice returns to showing everything`() = runTest {
        val (vm, _) = viewModel(transactions = listOf(tx("a"), tx("b", categoryId = "transporte")))

        vm.toggleCategory("comida")
        assertEquals(listOf("a"), vm.visibleIds())

        vm.toggleCategory("comida")
        assertEquals(listOf("a", "b"), vm.visibleIds().sorted())
    }

    @Test
    fun `the query matches on title`() = runTest {
        val (vm, _) = viewModel(
            transactions = listOf(tx("a", title = "Almuerzo"), tx("b", title = "Uber")),
        )

        vm.onQueryChange("uber")

        assertEquals(listOf("b"), vm.visibleIds())
    }

    /** Query and category are independent dimensions: both apply, not either. */
    @Test
    fun `query and category narrow together`() = runTest {
        val (vm, _) = viewModel(
            transactions = listOf(
                tx("a", title = "Almuerzo", categoryId = "comida"),
                tx("b", title = "Almuerzo", categoryId = "transporte"),
            ),
        )

        vm.onQueryChange("Almuerzo")
        vm.toggleCategory("transporte")

        assertEquals(listOf("b"), vm.visibleIds())
    }

    // ---------------------------------------------------------------------------------------
    // Sections
    // ---------------------------------------------------------------------------------------

    @Test
    fun `rows are grouped by day, newest first`() = runTest {
        val (vm, _) = viewModel(
            transactions = listOf(
                tx("old", date = today.minusDays(2)),
                tx("new", date = today),
            ),
        )

        assertEquals(listOf("new", "old"), vm.visibleIds())
        assertEquals(2, vm.loaded().sections.size)
    }

    /** The per-day subtotal is expenses only — income in the same day must not net against it. */
    @Test
    fun `the day subtotal counts expenses only`() = runTest {
        val (vm, _) = viewModel(
            transactions = listOf(
                tx("gasto", amountMinor = 3_000),
                tx("sueldo", amountMinor = 500_000, type = TransactionType.INCOME, categoryId = null),
            ),
        )

        assertEquals(3_000L, vm.loaded().sections.single().expenseTotalMinor)
    }

    // ---------------------------------------------------------------------------------------
    // Delete and undo
    // ---------------------------------------------------------------------------------------

    @Test
    fun `undo restores the deleted row verbatim`() = runTest {
        val original = tx("a", title = "Almuerzo", amountMinor = 1_850)
        val (vm, repo) = viewModel(transactions = listOf(original))

        vm.delete("a")
        advanceUntilIdle()
        assertTrue(repo.all().isEmpty())

        vm.undoDelete()
        advanceUntilIdle()

        assertEquals(listOf(original), repo.all())
    }

    /** Undo is once. A second press must not resurrect the row after a real delete. */
    @Test
    fun `undo does not fire twice`() = runTest {
        val (vm, repo) = viewModel(transactions = listOf(tx("a")))

        vm.delete("a")
        advanceUntilIdle()
        vm.undoDelete()
        advanceUntilIdle()
        vm.delete("a")
        advanceUntilIdle()
        vm.undoDelete()
        advanceUntilIdle()
        vm.undoDelete()
        advanceUntilIdle()

        assertEquals(1, repo.all().size)
    }
}

// ---------------------------------------------------------------------------------------------
// Not covered here: the route argument.
//
// `TransactionsViewModel` reads its initial category with `savedStateHandle.toRoute<TransactionsRoute>()`,
// which is how "Ver esos gastos" on Análisis lands here pre-filtered. That path cannot be reached
// from a JVM unit test: `toRoute` builds an `android.os.Bundle`, and a non-empty `SavedStateHandle`
// throws "Method putString in android.os.BaseBundle not mocked". `BudgetsViewModelTest` sidesteps
// the same problem by passing an empty handle, which is what happens above.
//
// Two ways out, neither taken here. Robolectric would cover it, at the cost of a test dependency
// this project does not otherwise have. Reading the raw key instead of decoding the route would
// make it testable but risks a real bug: navigation encodes a null `String?` argument as the
// literal string "null", which `toRoute` decodes back and a raw read would not — turning "no
// filter" into a filter on a category id nobody has.
//
// So this is a deliberate gap, not an oversight. Worth revisiting if Robolectric arrives for
// another reason.
// ---------------------------------------------------------------------------------------------
