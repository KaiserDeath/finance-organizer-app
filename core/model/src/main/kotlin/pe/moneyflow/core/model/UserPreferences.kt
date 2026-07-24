package pe.moneyflow.core.model

/** User-tunable app preferences, persisted in DataStore. */
data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val currencyCode: String = "PEN",
    val onboardingComplete: Boolean = false,
)
