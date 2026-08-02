package pe.moneyflow.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pe.moneyflow.core.domain.repository.SettingsRepository
import pe.moneyflow.core.model.QuickShortcut
import pe.moneyflow.core.model.ThemeMode
import pe.moneyflow.core.model.UserPreferences
import java.io.IOException
import javax.inject.Inject

/** Local DTO so `core:model` stays free of serialization annotations. */
@Serializable
private data class ShortcutDto(
    val label: String,
    val amountMinor: Long,
    val categoryId: String? = null,
    val paymentMethodId: String? = null,
)

private fun QuickShortcut.toDto() = ShortcutDto(label, amountMinor, categoryId, paymentMethodId)
private fun ShortcutDto.toModel() = QuickShortcut(label, amountMinor, categoryId, paymentMethodId)

class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        // "dynamic_color" was removed with the Material You option. Installs that had it set keep an
        // orphaned key; it is never read, so it costs nothing and clears on the next data wipe.
        val CURRENCY = stringPreferencesKey("currency_code")
        val ONBOARDING = booleanPreferencesKey("onboarding_complete")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val BIOMETRIC = booleanPreferencesKey("biometric_enabled")
        val MONTHLY_BUDGET = longPreferencesKey("monthly_budget_minor")
        val ACTIVE_METHOD_IDS = stringPreferencesKey("active_method_ids")
        val SHORTCUTS = stringPreferencesKey("shortcuts_json")
    }

    private val json = Json { ignoreUnknownKeys = true }

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
                currencyCode = prefs[Keys.CURRENCY] ?: "PEN",
                onboardingComplete = prefs[Keys.ONBOARDING] ?: false,
                pinHash = prefs[Keys.PIN_HASH],
                biometricEnabled = prefs[Keys.BIOMETRIC] ?: false,
                monthlyBudgetMinor = prefs[Keys.MONTHLY_BUDGET],
                // A corrupt/unparseable value degrades to unset instead of crashing collectors.
                activeMethodIds = prefs[Keys.ACTIVE_METHOD_IDS]?.let { raw ->
                    runCatching { json.decodeFromString<List<String>>(raw).toSet() }.getOrNull()
                },
                shortcuts = prefs[Keys.SHORTCUTS]?.let { raw ->
                    runCatching {
                        json.decodeFromString<List<ShortcutDto>>(raw).map { it.toModel() }
                    }.getOrNull()
                } ?: emptyList(),
            )
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME] = mode.name }
    }

    override suspend fun setCurrency(code: String) {
        dataStore.edit { it[Keys.CURRENCY] = code }
    }

    override suspend fun setOnboardingComplete(complete: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING] = complete }
    }

    override suspend fun setPinHash(hash: String?) {
        dataStore.edit { prefs ->
            if (hash == null) prefs.remove(Keys.PIN_HASH) else prefs[Keys.PIN_HASH] = hash
        }
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.BIOMETRIC] = enabled }
    }

    override suspend fun setMonthlyBudget(minor: Long?) {
        dataStore.edit { prefs ->
            if (minor == null) prefs.remove(Keys.MONTHLY_BUDGET) else prefs[Keys.MONTHLY_BUDGET] = minor
        }
    }

    override suspend fun setActiveMethodIds(ids: Set<String>?) {
        dataStore.edit { prefs ->
            if (ids == null) {
                prefs.remove(Keys.ACTIVE_METHOD_IDS)
            } else {
                prefs[Keys.ACTIVE_METHOD_IDS] = json.encodeToString(ids.toList())
            }
        }
    }

    override suspend fun setShortcuts(shortcuts: List<QuickShortcut>) {
        dataStore.edit { prefs ->
            prefs[Keys.SHORTCUTS] = json.encodeToString(shortcuts.map { it.toDto() })
        }
    }
}
