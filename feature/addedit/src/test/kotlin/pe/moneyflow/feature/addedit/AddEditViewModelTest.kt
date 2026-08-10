package pe.moneyflow.feature.addedit

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pe.moneyflow.core.domain.usecase.GenerateDueRecurringUseCase
import pe.moneyflow.core.domain.usecase.GetFrequentShortcutsUseCase
import pe.moneyflow.core.domain.usecase.GetTransactionUseCase
import pe.moneyflow.core.domain.usecase.MarkTransactionPaidUseCase
import pe.moneyflow.core.domain.usecase.SaveRecurringExpenseUseCase
import pe.moneyflow.core.domain.usecase.SaveTransactionUseCase
import pe.moneyflow.core.domain.usecase.UnmarkTransactionPaidUseCase
import pe.moneyflow.core.model.Category
import pe.moneyflow.core.model.CategoryType
import pe.moneyflow.core.testing.FakeAccountRepository
import pe.moneyflow.core.testing.FakeCategoryRepository
import pe.moneyflow.core.testing.FakePaymentMethodRepository
import pe.moneyflow.core.testing.FakeRecurringExpenseRepository
import pe.moneyflow.core.testing.FakeSettingsRepository
import pe.moneyflow.core.testing.FakeTransactionRepository
import pe.moneyflow.core.testing.MainDispatcherRule
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * Covers the behaviour the redesign changed: the title is now optional and falls back to the
 * category's name, the amount buffer stays plain (no thousands separators) whether typed or
 * loaded from an existing transaction, and unsaved changes are tracked so the screen can guard
 * against a silent back-press discard.
 */
class AddEditViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-08-10T12:00:00Z"), ZoneOffset.UTC)

    private val comida = Category(
        id = "comida",
        name = "Comida",
        iconKey = "food",
        colorHex = "#FF7043",
        type = CategoryType.EXPENSE,
    )

    private fun viewModel(
        transactions: FakeTransactionRepository = FakeTransactionRepository(),
        categories: List<Category> = listOf(comida),
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ): AddEditViewModel = AddEditViewModel(
        saveTransaction = SaveTransactionUseCase(transactions),
        saveRecurring = SaveRecurringExpenseUseCase(FakeRecurringExpenseRepository()),
        generateDueRecurring = GenerateDueRecurringUseCase(FakeRecurringExpenseRepository(), transactions, clock),
        getTransaction = GetTransactionUseCase(transactions),
        markTransactionPaid = MarkTransactionPaidUseCase(transactions, clock),
        unmarkTransactionPaid = UnmarkTransactionPaidUseCase(transactions, clock),
        categoryRepository = FakeCategoryRepository(categories),
        paymentMethodRepository = FakePaymentMethodRepository(),
        accountRepository = FakeAccountRepository(),
        settingsRepository = FakeSettingsRepository(),
        getFrequentShortcuts = GetFrequentShortcutsUseCase(transactions, clock),
        savedStateHandle = savedStateHandle,
    )

    @Test
    fun `blank title defaults to the category name on save`() = runTest {
        val transactions = FakeTransactionRepository()
        val vm = viewModel(transactions)
        advanceUntilIdle()

        vm.onAmountChange("18")
        vm.onCategorySelect(comida.id)
        vm.save()
        advanceUntilIdle()

        val saved = transactions.all().single()
        assertEquals("Comida", saved.title)
    }

    @Test
    fun `typed title is kept as-is`() = runTest {
        val transactions = FakeTransactionRepository()
        val vm = viewModel(transactions)
        advanceUntilIdle()

        vm.onAmountChange("18")
        vm.onCategorySelect(comida.id)
        vm.onTitleChange("Almuerzo")
        vm.save()
        advanceUntilIdle()

        assertEquals("Almuerzo", transactions.all().single().title)
    }

    @Test
    fun `canSave requires only a positive amount, not a title`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.canSave)
        vm.onAmountChange("18")
        assertTrue(vm.uiState.value.canSave)
    }

    @Test
    fun `amount display groups thousands without touching the editable buffer`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onAmountChange("1234567.5")

        assertEquals("1234567.5", vm.uiState.value.amountText)
        assertEquals("1,234,567.5", vm.uiState.value.groupedAmountText)
    }

    // Loading an existing transaction (transactionId present) goes through
    // `SavedStateHandle.toRoute<AddEditRoute>()`, which needs a real Android `Bundle` and isn't
    // mockable on the plain JVM unit-test classpath this module uses (no Robolectric here — see
    // core:testing's fakes, none of which stub navigation). That path — the plain, comma-free
    // amount buffer on edit-load — is covered by the manual emulator pass instead
    // (docs/manual-test-pay-roundtrip.md-style checklist), not by this suite.

    @Test
    fun `is not dirty right after loading, dirty once a field changes`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        assertFalse(vm.isDirty)
        vm.onAmountChange("18")
        assertTrue(vm.isDirty)
    }

    @Test
    fun `applying a prediction fills fields without saving`() = runTest {
        val transactions = FakeTransactionRepository()
        val vm = viewModel(transactions)
        advanceUntilIdle()

        vm.applyPrediction(
            pe.moneyflow.core.model.QuickShortcut(
                label = "Almuerzo",
                amountMinor = 1800,
                categoryId = comida.id,
            ),
        )

        assertEquals("Almuerzo", vm.uiState.value.title)
        assertEquals("18", vm.uiState.value.amountText)
        assertTrue(transactions.all().isEmpty())
    }

    @Test
    fun `first prediction round trip resolves without throwing when history is empty`() = runTest {
        // Guards the init-block wiring of GetFrequentShortcutsUseCase — a fresh ledger must yield
        // no predictions rather than fail the collection.
        val vm = viewModel()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.predictions.isEmpty())
        assertEquals(comida.id, vm.uiState.value.categoryId)
    }
}
