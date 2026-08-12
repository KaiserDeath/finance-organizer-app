package pe.moneyflow.feature.paymentmethods

import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import pe.moneyflow.core.domain.usecase.DeletePaymentMethodUseCase
import pe.moneyflow.core.domain.usecase.ObservePaymentMethodsUseCase
import pe.moneyflow.core.domain.usecase.SaveAccountUseCase
import pe.moneyflow.core.domain.usecase.SavePaymentMethodUseCase
import pe.moneyflow.core.domain.usecase.SetDefaultPaymentMethodUseCase
import pe.moneyflow.core.model.PaymentMethod
import pe.moneyflow.core.testing.FakeAccountRepository
import pe.moneyflow.core.testing.FakePaymentMethodRepository
import pe.moneyflow.core.testing.FakeSettingsRepository
import pe.moneyflow.core.testing.MainDispatcherRule

/**
 * That the ViewModel's write paths keep exactly one default.
 *
 * [SetDefaultPaymentMethodUseCaseTest] pins the rule itself; these pin that every route into a
 * write actually goes through it. The one that mattered is [PaymentMethodsViewModel.saveWithAccount]
 * — the path the sheet takes when creating a method — which upserted directly, so creating a method
 * with the default toggle on left the previous default flagged as well.
 */
class PaymentMethodsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun method(id: String, isDefault: Boolean = false) = PaymentMethod(
        id = id,
        name = id,
        iconKey = "wallet",
        colorHex = "#7E57C2",
        isDefault = isDefault,
    )

    private fun viewModel(seed: List<PaymentMethod>): Pair<PaymentMethodsViewModel, FakePaymentMethodRepository> {
        val repo = FakePaymentMethodRepository(seed)
        val vm = PaymentMethodsViewModel(
            observePaymentMethods = ObservePaymentMethodsUseCase(repo),
            accountRepository = FakeAccountRepository(),
            settingsRepository = FakeSettingsRepository(),
            savePaymentMethod = SavePaymentMethodUseCase(repo),
            saveAccount = SaveAccountUseCase(FakeAccountRepository()),
            deletePaymentMethod = DeletePaymentMethodUseCase(repo),
            setDefaultPaymentMethod = SetDefaultPaymentMethodUseCase(repo),
        )
        return vm to repo
    }

    private fun defaults(repo: FakePaymentMethodRepository) =
        repo.all().filter { it.isDefault }.map { it.id }

    @Test
    fun `setDefault promotes one and demotes the rest`() = runTest {
        val (vm, repo) = viewModel(listOf(method("cash", isDefault = true), method("yape")))

        vm.setDefault("yape")
        advanceUntilIdle()

        assertEquals(listOf("yape"), defaults(repo))
    }

    @Test
    fun `saving an existing method as default demotes the previous one`() = runTest {
        val (vm, repo) = viewModel(listOf(method("cash", isDefault = true), method("yape")))

        vm.save(method("yape", isDefault = true))
        advanceUntilIdle()

        assertEquals(listOf("yape"), defaults(repo))
    }

    /** The create path. It upserted directly, so this is the case that produced two defaults. */
    @Test
    fun `creating a method as default demotes the previous one`() = runTest {
        val (vm, repo) = viewModel(listOf(method("cash", isDefault = true)))

        vm.saveWithAccount(
            method("plin", isDefault = true),
            alsoCreateAccount = false,
            currencyCode = "PEN",
        )
        advanceUntilIdle()

        assertEquals(listOf("plin"), defaults(repo))
    }

    /** Saving an ordinary edit must not disturb whoever the default is. */
    @Test
    fun `saving a non-default method leaves the default alone`() = runTest {
        val (vm, repo) = viewModel(listOf(method("cash", isDefault = true), method("yape")))

        vm.save(method("yape").copy(name = "Yape renombrado"))
        advanceUntilIdle()

        assertEquals(listOf("cash"), defaults(repo))
    }
}
