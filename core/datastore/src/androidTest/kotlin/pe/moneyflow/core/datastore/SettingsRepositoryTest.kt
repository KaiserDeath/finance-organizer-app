package pe.moneyflow.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pe.moneyflow.core.model.QuickShortcut
import pe.moneyflow.core.model.ThemeMode
import java.io.File

/**
 * The settings store, against a real file.
 *
 * Every value here is written once and read back on some later launch, so the failures worth
 * guarding are the ones that only appear across that boundary: a key that does not survive the
 * round trip, a null that comes back as a default, or a value written by an older build that the
 * current parser chokes on. A fake `DataStore` substitutes exactly that boundary away, which is why
 * these are instrumented.
 *
 * The corruption cases are not hypothetical. `SettingsRepositoryImpl` degrades unparseable values
 * to defaults on purpose, and if that ever regressed the symptom would be the app failing to start
 * for the one user whose file is bad — invisible in development, where the file is always well
 * formed because this build wrote it.
 */
@RunWith(AndroidJUnit4::class)
class SettingsRepositoryTest {

    private lateinit var scope: CoroutineScope
    private lateinit var file: File
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: SettingsRepositoryImpl

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // A per-run file, so one test's writes cannot become another's starting state.
        file = File(context.filesDir, "settings-test-${System.nanoTime()}.preferences_pb")
        scope = CoroutineScope(UnconfinedTestDispatcher())
        dataStore = PreferenceDataStoreFactory.create(scope = scope) { file }
        repository = SettingsRepositoryImpl(dataStore)
    }

    @After
    fun tearDown() {
        scope.cancel()
        file.delete()
    }

    /** Reads the store through a second repository over the same file, as a fresh launch would. */
    private suspend fun reread() = SettingsRepositoryImpl(dataStore).preferences.first()

    /** Writes a raw value the current code would never write — an older build, or a corrupt file. */
    private suspend fun putRaw(key: String, value: String) {
        dataStore.edit { it[stringPreferencesKey(key)] = value }
    }

    @Test
    fun anUntouchedStore_readsDocumentedDefaults() = runTest {
        val prefs = repository.preferences.first()

        assertEquals(ThemeMode.SYSTEM, prefs.themeMode)
        assertEquals("PEN", prefs.currencyCode)
        assertEquals(false, prefs.onboardingComplete)
        assertEquals(false, prefs.biometricEnabled)
        // Discreet mode is off until asked for; a store that came up masked would look broken.
        assertEquals(false, prefs.amountsHidden)
        assertEquals(false, prefs.petEnabled)
        assertEquals(true, prefs.petSpeechEnabled)
        assertEquals(pe.moneyflow.core.model.PetSpeechFrequency.NORMAL, prefs.petSpeechFrequency)
        assertEquals(false, prefs.petIntroductionComplete)
        assertEquals(false, prefs.petGestureOnboardingComplete)
        assertEquals(false, prefs.petReducedMotion)
        assertEquals(1f, prefs.petPositionX)
        assertEquals(1f, prefs.petPositionY)
        assertNull(prefs.pinHash)
        assertNull(prefs.monthlyBudgetMinor)
        // null and empty mean different things here: null is "no restriction", so it must not
        // arrive as an empty set that would read as "no methods active".
        assertNull(prefs.activeMethodIds)
        assertTrue(prefs.shortcuts.isEmpty())
    }

    @Test
    fun everyScalarSurvivesAReread() = runTest {
        repository.setThemeMode(ThemeMode.DARK)
        repository.setCurrency("USD")
        repository.setOnboardingComplete(true)
        repository.setPinHash("hash-abc")
        repository.setBiometricEnabled(true)
        repository.setMonthlyBudget(400_000)
        repository.setAmountsHidden(true)
        repository.setPetEnabled(false)
        repository.setPetSpeechEnabled(false)
        repository.setPetIntroductionComplete(true)
        repository.setPetSpeechFrequency(pe.moneyflow.core.model.PetSpeechFrequency.LOW)
        repository.setPetGestureOnboardingComplete(true)
        repository.setPetReducedMotion(true)
        repository.setPetPlacement(normalizedX = 0.42f, normalizedY = 0.35f)

        val prefs = reread()

        assertEquals(ThemeMode.DARK, prefs.themeMode)
        assertEquals("USD", prefs.currencyCode)
        assertEquals(true, prefs.onboardingComplete)
        assertEquals("hash-abc", prefs.pinHash)
        assertEquals(true, prefs.biometricEnabled)
        assertEquals(400_000L, prefs.monthlyBudgetMinor)
        // The whole point of persisting the mask: a masked screen stays masked across launches.
        assertEquals(true, prefs.amountsHidden)
        assertEquals(false, prefs.petEnabled)
        assertEquals(true, prefs.petSpeechEnabled)
        assertEquals(pe.moneyflow.core.model.PetSpeechFrequency.LOW, prefs.petSpeechFrequency)
        assertEquals(true, prefs.petIntroductionComplete)
        assertEquals(true, prefs.petGestureOnboardingComplete)
        assertEquals(true, prefs.petReducedMotion)
        assertEquals(0.42f, prefs.petPositionX)
        assertEquals(0.35f, prefs.petPositionY)
    }

    @Test
    fun `discreetModeTogglesBackOff`() = runTest {
        repository.setAmountsHidden(true)
        repository.setAmountsHidden(false)

        assertEquals(false, reread().amountsHidden)
    }

    /**
     * Clearing has to remove the key, not write a falsy value. Onboarding's budget step is
     * skippable and the PIN is removable, and both are "unset" rather than "zero"/"empty".
     */
    @Test
    fun clearingANullableValueRemovesIt() = runTest {
        repository.setPinHash("hash-abc")
        repository.setMonthlyBudget(400_000)
        repository.setActiveMethodIds(setOf("yape"))

        repository.setPinHash(null)
        repository.setMonthlyBudget(null)
        repository.setActiveMethodIds(null)

        val prefs = reread()
        assertNull(prefs.pinHash)
        assertNull(prefs.monthlyBudgetMinor)
        assertNull(prefs.activeMethodIds)
    }

    @Test
    fun activeMethodIds_roundTrip_andKeepEmptyDistinctFromNull() = runTest {
        repository.setActiveMethodIds(setOf("yape", "cash"))
        assertEquals(setOf("yape", "cash"), reread().activeMethodIds)

        // Selecting nothing is a real state and is not the same as not restricting.
        repository.setActiveMethodIds(emptySet())
        assertEquals(emptySet<String>(), reread().activeMethodIds)
    }

    /** Shortcuts cross the file as JSON, so every field has to make the trip — including the nulls. */
    @Test
    fun shortcuts_roundTripWithAllFields() = runTest {
        repository.setShortcuts(
            listOf(
                QuickShortcut("Almuerzo", 1_800, categoryId = "comida", paymentMethodId = "yape"),
                QuickShortcut("Pasaje", 500, categoryId = null, paymentMethodId = null),
            ),
        )

        val shortcuts = reread().shortcuts

        assertEquals(2, shortcuts.size)
        assertEquals(QuickShortcut("Almuerzo", 1_800, "comida", "yape"), shortcuts[0])
        assertEquals(QuickShortcut("Pasaje", 500, null, null), shortcuts[1])
    }

    @Test
    fun shortcuts_canBeClearedToEmpty() = runTest {
        repository.setShortcuts(listOf(QuickShortcut("Almuerzo", 1_800)))
        repository.setShortcuts(emptyList())

        assertTrue(reread().shortcuts.isEmpty())
    }

    @Test
    fun petTransactionCooldownTimestamp_persistsAndClears() = runTest {
        repository.setPetLastTransactionReactionAt(123_456L)
        assertEquals(123_456L, reread().petLastTransactionReactionAt)

        repository.setPetLastTransactionReactionAt(null)
        assertNull(reread().petLastTransactionReactionAt)
    }

    // ---------------------------------------------------------------------------------------
    // Degradation. Each of these would otherwise throw while mapping, on a Flow the whole app
    // collects — so the failure is not a wrong setting, it is a launch that never completes.
    // ---------------------------------------------------------------------------------------

    @Test
    fun anUnknownThemeName_degradesToSystem() = runTest {
        putRaw("theme_mode", "SOLARIZED")

        assertEquals(ThemeMode.SYSTEM, reread().themeMode)
    }

    @Test
    fun unparseableActiveMethodIds_degradeToUnset() = runTest {
        putRaw("active_method_ids", "{not json")

        assertNull(reread().activeMethodIds)
    }

    @Test
    fun unparseableShortcuts_degradeToEmpty() = runTest {
        putRaw("shortcuts_json", "[[[")

        assertTrue(reread().shortcuts.isEmpty())
    }

    /**
     * An install that predates the Material You removal still has `dynamic_color` on disk. The key
     * is intentionally never read, so what is being pinned is that its presence is harmless —
     * a stray key must not break the mapping around it.
     */
    @Test
    fun anOrphanedDynamicColorKey_isIgnored() = runTest {
        putRaw("dynamic_color", "true")
        repository.setThemeMode(ThemeMode.LIGHT)

        val prefs = reread()

        assertEquals(ThemeMode.LIGHT, prefs.themeMode)
        assertEquals("PEN", prefs.currencyCode)
    }

    /**
     * Shortcuts are written by one build and read by the next. `Json { ignoreUnknownKeys = true }`
     * is what lets a field added later be read by a build that predates it; without it the whole
     * list decodes to null and the user silently loses their shortcuts.
     */
    @Test
    fun shortcutsWithAnUnknownField_stillDecode() = runTest {
        putRaw(
            "shortcuts_json",
            """[{"label":"Almuerzo","amountMinor":1800,"iconKey":"food"}]""",
        )

        val shortcuts = reread().shortcuts

        assertEquals(1, shortcuts.size)
        assertEquals("Almuerzo", shortcuts[0].label)
        assertEquals(1_800L, shortcuts[0].amountMinor)
    }
}
