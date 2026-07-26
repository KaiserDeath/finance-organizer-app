package pe.moneyflow.core.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityUseCasesTest {

    @Test
    fun `hash is deterministic and hides the pin`() {
        val hash = PinHasher.hash("1234")
        assertEquals(hash, PinHasher.hash("1234"))
        assertNotEquals("1234", hash)
        assertEquals(64, hash.length) // SHA-256 hex
    }

    @Test
    fun `different pins hash differently`() {
        assertNotEquals(PinHasher.hash("1234"), PinHasher.hash("4321"))
    }

    @Test
    fun `verify accepts the correct pin`() = runTest {
        val settings = FakeSettings(pinHash = PinHasher.hash("2468"))
        assertTrue(VerifyPinUseCase(settings)("2468"))
    }

    @Test
    fun `verify rejects the wrong pin`() = runTest {
        val settings = FakeSettings(pinHash = PinHasher.hash("2468"))
        assertFalse(VerifyPinUseCase(settings)("0000"))
    }

    @Test
    fun `verify rejects when no pin is set`() = runTest {
        assertFalse(VerifyPinUseCase(FakeSettings())("2468"))
    }
}
