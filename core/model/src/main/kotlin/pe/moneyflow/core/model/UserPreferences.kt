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
    /**
     * Discreet mode: every amount renders masked. Presentation only — nothing about what is stored
     * or computed changes, so turning it off shows the same figures again.
     */
    val amountsHidden: Boolean = false,
    /** Optional in-app companion. New installs require explicit opt-in. */
    val petEnabled: Boolean = false,
    val petSpeechEnabled: Boolean = true,
    val petSpeechFrequency: PetSpeechFrequency = PetSpeechFrequency.NORMAL,
    val petIntroductionComplete: Boolean = false,
    val petGestureOnboardingComplete: Boolean = false,
    /** Explicit override in addition to the device animator-duration setting. */
    val petReducedMotion: Boolean = false,
    /** Normalized free-screen position survives rotation and different window sizes. */
    val petPositionX: Float = 1f,
    val petPositionY: Float = 1f,
    /** Wall-clock timestamp of the last contextual reaction; persists cooldowns across process death. */
    val petLastTransactionReactionAt: Long? = null,
) {
    /** The app is locked behind a PIN (and optionally biometrics) when a PIN hash exists. */
    val appLockEnabled: Boolean get() = pinHash != null
}

enum class PetSpeechFrequency { NORMAL, LOW, SILENT }
