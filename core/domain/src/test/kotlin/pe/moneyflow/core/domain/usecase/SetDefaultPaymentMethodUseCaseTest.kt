package pe.moneyflow.core.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import pe.moneyflow.core.model.PaymentMethod

/**
 * "Default" is a property of the set, not of a row, and nothing used to enforce that.
 *
 * The screen's old control was a Switch inside the edit sheet writing through a plain upsert, so
 * flagging a second method left two of them default — and every consumer reads the default with
 * `firstOrNull { it.isDefault }`, which then answers by list order rather than by what the user
 * chose. These pin the invariant that replaced it.
 */
class SetDefaultPaymentMethodUseCaseTest {

    private fun method(id: String, isDefault: Boolean = false) = PaymentMethod(
        id = id,
        name = id,
        iconKey = "wallet",
        colorHex = "#7E57C2",
        isDefault = isDefault,
    )

    @Test
    fun `promoting a method demotes the previous default`() = runTest {
        val repo = FakePmRepo(
            listOf(method("cash", isDefault = true), method("yape")),
        )

        SetDefaultPaymentMethodUseCase(repo)("yape")

        assertEquals(listOf("yape"), repo.all().filter { it.isDefault }.map { it.id })
    }

    /** The bug the use case exists for: two rows flagged, and the reader picking by position. */
    @Test
    fun `an already broken set is repaired down to one default`() = runTest {
        val repo = FakePmRepo(
            listOf(
                method("cash", isDefault = true),
                method("yape", isDefault = true),
                method("bcp", isDefault = true),
            ),
        )

        SetDefaultPaymentMethodUseCase(repo)("yape")

        assertEquals(listOf("yape"), repo.all().filter { it.isDefault }.map { it.id })
    }

    @Test
    fun `promoting the method that is already default changes nothing`() = runTest {
        val repo = FakePmRepo(
            listOf(method("cash", isDefault = true), method("yape")),
        )
        val before = repo.all()

        SetDefaultPaymentMethodUseCase(repo)("cash")

        assertEquals(before, repo.all())
    }

    /** A set with no default at all is a real state — skipping onboarding reaches it. */
    @Test
    fun `a set with no default can be given one`() = runTest {
        val repo = FakePmRepo(listOf(method("cash"), method("yape")))

        SetDefaultPaymentMethodUseCase(repo)("cash")

        assertEquals(listOf("cash"), repo.all().filter { it.isDefault }.map { it.id })
    }

    /** An id nobody holds must not clear the existing default as a side effect. */
    @Test
    fun `an unknown id leaves the set alone`() = runTest {
        val repo = FakePmRepo(
            listOf(method("cash", isDefault = true), method("yape")),
        )

        SetDefaultPaymentMethodUseCase(repo)("gone")

        assertEquals(listOf("cash"), repo.all().filter { it.isDefault }.map { it.id })
    }
}
