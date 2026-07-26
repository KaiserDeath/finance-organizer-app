package pe.moneyflow.core.model

/** User-tunable app preferences, persisted in DataStore. */
data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val currencyCode: String = "PEN",
    val onboardingComplete: Boolean = false,
    /** SHA-256 hash of the app-lock PIN, or null when no PIN is set. */
    val pinHash: String? = null,
    /** Whether biometric unlock is allowed in addition to the PIN. */
    val biometricEnabled: Boolean = false,
) {
    /** The app is locked behind a PIN (and optionally biometrics) when a PIN hash exists. */
    val appLockEnabled: Boolean get() = pinHash != null
}
