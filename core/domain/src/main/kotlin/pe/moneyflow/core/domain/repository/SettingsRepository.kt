package pe.moneyflow.core.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.moneyflow.core.model.QuickShortcut
import pe.moneyflow.core.model.ThemeMode
import pe.moneyflow.core.model.PetSpeechFrequency
import pe.moneyflow.core.model.UserPreferences

interface SettingsRepository {
    val preferences: Flow<UserPreferences>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setCurrency(code: String)

    suspend fun setOnboardingComplete(complete: Boolean)

    /** Sets or clears (null) the app-lock PIN hash. */
    suspend fun setPinHash(hash: String?)

    suspend fun setBiometricEnabled(enabled: Boolean)

    /** Sets or clears (null) the monthly spending target chosen in onboarding. */
    suspend fun setMonthlyBudget(minor: Long?)

    /** Restricts pickers to these method ids; null re-activates every method. */
    suspend fun setActiveMethodIds(ids: Set<String>?)

    suspend fun setShortcuts(shortcuts: List<QuickShortcut>)

    /** Discreet mode. Persists, so a masked screen stays masked across launches. */
    suspend fun setAmountsHidden(hidden: Boolean)

    suspend fun setPetEnabled(enabled: Boolean)

    suspend fun setPetSpeechEnabled(enabled: Boolean) = Unit

    suspend fun setPetSpeechFrequency(frequency: PetSpeechFrequency) = Unit

    suspend fun setPetIntroductionComplete(complete: Boolean) = Unit

    suspend fun setPetGestureOnboardingComplete(complete: Boolean) = Unit

    suspend fun setPetReducedMotion(reduced: Boolean)

    suspend fun setPetPlacement(normalizedX: Float, normalizedY: Float)

    suspend fun setPetLastTransactionReactionAt(timestampMillis: Long?) = Unit
}
