package pe.moneyflow.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import pe.moneyflow.core.domain.repository.SettingsRepository
import pe.moneyflow.core.model.ThemeMode
import pe.moneyflow.core.model.UserPreferences
import java.io.IOException
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val CURRENCY = stringPreferencesKey("currency_code")
        val ONBOARDING = booleanPreferencesKey("onboarding_complete")
    }

    override val preferences: Flow<UserPreferences> = dataStore.data
        .catch { throwable ->
            // A read error should not crash collectors — fall back to defaults.
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { prefs ->
            UserPreferences(
                themeMode = prefs[Keys.THEME]
                    ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                    ?: ThemeMode.SYSTEM,
                useDynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: true,
                currencyCode = prefs[Keys.CURRENCY] ?: "PEN",
                onboardingComplete = prefs[Keys.ONBOARDING] ?: false,
            )
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME] = mode.name }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    override suspend fun setCurrency(code: String) {
        dataStore.edit { it[Keys.CURRENCY] = code }
    }

    override suspend fun setOnboardingComplete(complete: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING] = complete }
    }
}
