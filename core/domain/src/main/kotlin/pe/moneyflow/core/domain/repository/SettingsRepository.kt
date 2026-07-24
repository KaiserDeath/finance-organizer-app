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
}
