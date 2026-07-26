package pe.moneyflow.app.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/** True when the device has an enrolled biometric we can prompt for (weak class or stronger). */
fun FragmentActivity.canUseBiometrics(): Boolean =
    BiometricManager.from(this)
        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
        BiometricManager.BIOMETRIC_SUCCESS

/** Shows the system biometric prompt; [onSuccess] fires on a successful authentication. */
fun FragmentActivity.showBiometricPrompt(
    onSuccess: () -> Unit,
    onError: (String) -> Unit = {},
) {
    if (!canUseBiometrics()) {
        onError("Biometría no disponible")
        return
    }
    val prompt = BiometricPrompt(
        this,
        ContextCompat.getMainExecutor(this),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onError(errString.toString())
            }
        },
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Desbloquear MoneyFlow")
        .setSubtitle("Usa tu huella o rostro para continuar")
        .setNegativeButtonText("Usar PIN")
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        .build()
    prompt.authenticate(info)
}
