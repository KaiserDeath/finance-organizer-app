package pe.moneyflow.core.domain.usecase

import kotlinx.coroutines.flow.first
import pe.moneyflow.core.domain.repository.SettingsRepository
import java.security.MessageDigest
import javax.inject.Inject

/** Hashes app-lock PINs with SHA-256 so the raw PIN is never stored. */
object PinHasher {
    fun hash(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(pin.trim().toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}

/** Stores a PIN (as its hash), enabling the app lock. */
class SetPinUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(pin: String) {
        settingsRepository.setPinHash(PinHasher.hash(pin))
    }
}

/** Removes the PIN and turns off biometrics, disabling the app lock. */
class ClearPinUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke() {
        settingsRepository.setPinHash(null)
        settingsRepository.setBiometricEnabled(false)
    }
}

/** True when [pin] matches the stored PIN hash. */
class VerifyPinUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(pin: String): Boolean {
        val stored = settingsRepository.preferences.first().pinHash ?: return false
        return stored == PinHasher.hash(pin)
    }
}

class SetBiometricEnabledUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(enabled: Boolean) {
        settingsRepository.setBiometricEnabled(enabled)
    }
}
