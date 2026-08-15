package pe.moneyflow.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pe.moneyflow.core.domain.repository.SettingsRepository
import pe.moneyflow.core.model.QuickShortcut
import pe.moneyflow.core.model.PetSpeechFrequency
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
        val AMOUNTS_HIDDEN = booleanPreferencesKey("amounts_hidden")
        val PET_ENABLED = booleanPreferencesKey("pet_enabled")
        val PET_SPEECH_ENABLED = booleanPreferencesKey("pet_speech_enabled")
        val PET_SPEECH_FREQUENCY = stringPreferencesKey("pet_speech_frequency")
        val PET_INTRODUCTION_COMPLETE = booleanPreferencesKey("pet_introduction_complete")
        val PET_GESTURE_ONBOARDING_COMPLETE = booleanPreferencesKey("pet_gesture_onboarding_complete")
        val PET_REDUCED_MOTION = booleanPreferencesKey("pet_reduced_motion")
        val PET_ANCHOR_END = booleanPreferencesKey("pet_anchor_end")
        val PET_POSITION_X = floatPreferencesKey("pet_position_x")
        val PET_POSITION_Y = floatPreferencesKey("pet_position_y")
        val PET_LAST_TRANSACTION_REACTION_AT = longPreferencesKey("pet_last_transaction_reaction_at")
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
                amountsHidden = prefs[Keys.AMOUNTS_HIDDEN] ?: false,
                petEnabled = prefs[Keys.PET_ENABLED] ?: false,
                petSpeechEnabled = prefs[Keys.PET_SPEECH_ENABLED] ?: true,
                petSpeechFrequency = prefs[Keys.PET_SPEECH_FREQUENCY]
                    ?.let { runCatching { PetSpeechFrequency.valueOf(it) }.getOrNull() }
                    ?: if (prefs[Keys.PET_SPEECH_ENABLED] == false) PetSpeechFrequency.SILENT
                    else PetSpeechFrequency.NORMAL,
                petIntroductionComplete = prefs[Keys.PET_INTRODUCTION_COMPLETE] ?: false,
                petGestureOnboardingComplete = prefs[Keys.PET_GESTURE_ONBOARDING_COMPLETE] ?: false,
                petReducedMotion = prefs[Keys.PET_REDUCED_MOTION] ?: false,
                // Existing prototype installs stored only an edge. Use it once as the X fallback;
                // every new drag writes the continuous normalized coordinate.
                petPositionX = (prefs[Keys.PET_POSITION_X]
                    ?: if (prefs[Keys.PET_ANCHOR_END] ?: true) 1f else 0f).coerceIn(0f, 1f),
                petPositionY = (prefs[Keys.PET_POSITION_Y] ?: 1f).coerceIn(0f, 1f),
                petLastTransactionReactionAt = prefs[Keys.PET_LAST_TRANSACTION_REACTION_AT],
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

    override suspend fun setAmountsHidden(hidden: Boolean) {
        dataStore.edit { it[Keys.AMOUNTS_HIDDEN] = hidden }
    }

    override suspend fun setPetEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.PET_ENABLED] = enabled }
    }

    override suspend fun setPetSpeechEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.PET_SPEECH_ENABLED] = enabled }
    }

    override suspend fun setPetSpeechFrequency(frequency: PetSpeechFrequency) {
        dataStore.edit {
            it[Keys.PET_SPEECH_FREQUENCY] = frequency.name
            it[Keys.PET_SPEECH_ENABLED] = frequency != PetSpeechFrequency.SILENT
        }
    }

    override suspend fun setPetIntroductionComplete(complete: Boolean) {
        dataStore.edit { it[Keys.PET_INTRODUCTION_COMPLETE] = complete }
    }

    override suspend fun setPetGestureOnboardingComplete(complete: Boolean) {
        dataStore.edit { it[Keys.PET_GESTURE_ONBOARDING_COMPLETE] = complete }
    }

    override suspend fun setPetReducedMotion(reduced: Boolean) {
        dataStore.edit { it[Keys.PET_REDUCED_MOTION] = reduced }
    }

    override suspend fun setPetPlacement(normalizedX: Float, normalizedY: Float) {
        dataStore.edit {
            it[Keys.PET_POSITION_X] = normalizedX.coerceIn(0f, 1f)
            it[Keys.PET_POSITION_Y] = normalizedY.coerceIn(0f, 1f)
            it.remove(Keys.PET_ANCHOR_END)
        }
    }

    override suspend fun setPetLastTransactionReactionAt(timestampMillis: Long?) {
        dataStore.edit { prefs ->
            if (timestampMillis == null) {
                prefs.remove(Keys.PET_LAST_TRANSACTION_REACTION_AT)
            } else {
                prefs[Keys.PET_LAST_TRANSACTION_REACTION_AT] = timestampMillis
            }
        }
    }
}
