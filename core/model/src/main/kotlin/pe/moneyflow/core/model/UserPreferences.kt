package pe.moneyflow.core.model

/** User-tunable app preferences, persisted in DataStore. */
data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val currencyCode: String = "PEN",
    val onboardingComplete: Boolean = false,
    /** SHA-256 hash of the app-lock PIN, or null when no PIN is set. */
    val pinHash: String? = null,
    /** Whether biometric unlock is allowed in addition to the PIN. */
    val biometricEnabled: Boolean = false,
    /** Monthly spending target from onboarding; null when the step was skipped (hero degrades). */
    val monthlyBudgetMinor: Long? = null,
    /** Payment-method ids the user declared having; null means all methods are active. */
    val activeMethodIds: Set<String>? = null,
    /** One-tap expense presets for the dashboard; empty until onboarding or 30 days of history. */
    val shortcuts: List<QuickShortcut> = emptyList(),
) {
    /** The app is locked behind a PIN (and optionally biometrics) when a PIN hash exists. */
    val appLockEnabled: Boolean get() = pinHash != null
}
