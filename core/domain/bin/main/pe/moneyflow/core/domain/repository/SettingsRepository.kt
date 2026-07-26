package pe.moneyflow.core.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.moneyflow.core.model.ThemeMode
import pe.moneyflow.core.model.UserPreferences

interface SettingsRepository {
    val preferences: Flow<UserPreferences>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setDynamicColor(enabled: Boolean)

    suspend fun setCurrency(code: String)

    suspend fun setOnboardingComplete(complete: Boolean)

    /** Sets or clears (null) the app-lock PIN hash. */
    suspend fun setPinHash(hash: String?)

    suspend fun setBiometricEnabled(enabled: Boolean)
}
