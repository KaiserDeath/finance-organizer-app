package pe.moneyflow.feature.dashboard

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import pe.moneyflow.core.designsystem.theme.MoneyFlowTheme
import pe.moneyflow.core.domain.model.DashboardData
import pe.moneyflow.core.domain.model.SpendingPace
import pe.moneyflow.core.domain.model.StreakDay
import pe.moneyflow.core.model.QuickShortcut
import java.time.LocalDate
import java.time.YearMonth

/**
 * Composition guards for the dashboard's brand band and the one-tap row.
 *
 * The band is conditional in four places — projection, denominator, bar, streak — and each condition
 * is a decision rather than a detail: a projection on a finished month would be nonsense, and budget
 * figures on a past month would compare that month's spend against this month's limits. Those are
 * exactly the rules that rot silently.
 *
 * The shortcuts grid is here for a different reason: it has never rendered on the development
 * device, because the frequency heuristic needs a 30-day-old ledger and that install is younger.
 * A test is the only place it has ever been drawn.
 */
class HeroAndShortcutsTest {

    @get:Rule
    val rule = createComposeRule()

    private val july = YearMonth.of(2026, 7)
    private val july10 = LocalDate.of(2026, 7, 10)

    private fun data(spent: Long = 120_000, income: Long = 320_000) =
        DashboardData.empty(july).copy(monthSpentMinor = spent, monthIncomeMinor = income)

    private fun pace(budget: Long? = 200_000, spent: Long = 120_000) =
        SpendingPace.of(july, july10, spent, budget, 0)

    private fun showHero(
        data: DashboardData = data(),
        pace: SpendingPace? = pace(),
        streak: List<StreakDay> = emptyList(),
    ) = rule.setContent {
        MoneyFlowTheme(darkTheme = false) {
            HeroBalanceCard(
                data = data,
                pace = pace,
                canGoForward = false,
                onPreviousMonth = {},
                onNextMonth = {},
                streak = streak,
            )
        }
    }

    // -----------------------------------------------------------------------------------------
    // The band's conditional sections.
    // -----------------------------------------------------------------------------------------

    @Test
    fun withABudget_theFigureGetsItsDenominatorAndRemaining() {
        showHero()

        rule.onNodeWithText("de S/ 2,000.00 presupuestado").assertIsDisplayed()
        rule.onNodeWithText("Quedan", substring = true).assertIsDisplayed()
    }

    /** Skipping the onboarding budget must degrade the band, not blank or break it. */
    @Test
    fun withoutABudget_theDenominatorIsAbsentButTheFigureRemains() {
        showHero(pace = pace(budget = null))

        rule.onNodeWithText("presupuestado", substring = true).assertDoesNotExist()
        rule.onNodeWithText("Gastado este mes").assertIsDisplayed()
        rule.onNodeWithText("A este ritmo", substring = true).assertIsDisplayed()
    }

    /**
     * A past month has no pace at all. Nothing about a finished month is forward-looking, and its
     * budget figures would be measured against the *current* period's limits.
     */
    @Test
    fun aPastMonth_showsNoProjectionAndNoBudget() {
        showHero(data = data().copy(month = YearMonth.of(2026, 6)), pace = null)

        rule.onNodeWithText("A este ritmo", substring = true).assertDoesNotExist()
        rule.onNodeWithText("presupuestado", substring = true).assertDoesNotExist()
        // ignoreCase deliberately: `toMonthNameOnly()` title-cases, so this reads "Gastado en
        // Junio" while Spanish orthography — and the prototype's "Presupuesto de julio" — want it
        // lowercase mid-sentence. Not this test's call to settle, and it must not cement either.
        rule.onNodeWithText("Gastado en junio", ignoreCase = true).assertIsDisplayed()
    }

    /** The month-over-month delta the user sessions rejected. Its absence is a decision. */
    @Test
    fun theRejectedMonthOverMonthComparisonIsGone() {
        showHero()

        rule.onNodeWithText("vs mes pasado").assertDoesNotExist()
    }

    @Test
    fun withoutStreakData_theStreakRowIsAbsent() {
        showHero(streak = emptyList())

        rule.onNodeWithText("/7", substring = true).assertDoesNotExist()
    }

    @Test
    fun withStreakData_theRowCountsTheDaysLogged() {
        val days = (0..6).map {
            StreakDay(
                date = july10.minusDays((6 - it).toLong()),
                logged = it % 2 == 0,
                withinAllowance = true,
            )
        }
        showHero(streak = days)

        rule.onNodeWithText("4/7").assertIsDisplayed()
    }

    // -----------------------------------------------------------------------------------------
    // "De un toque".
    // -----------------------------------------------------------------------------------------

    @Test
    fun everyShortcutIsShownAndTappable() {
        val shortcuts = listOf(
            QuickShortcut("Almuerzo", 1_500, "comida", "cash"),
            QuickShortcut("Pasaje", 250, "transporte", "cash"),
            QuickShortcut("Café", 800, "comida", "cash"),
            QuickShortcut("Mercado", 9_000, "comida", "cash"),
        )
        var tapped: QuickShortcut? = null
        rule.setContent {
            MoneyFlowTheme(darkTheme = false) {
                ShortcutsRow(
                    shortcuts = shortcuts,
                    currencyCode = "PEN",
                    onShortcut = { tapped = it },
                )
            }
        }

        // All four, not just the two a horizontal strip used to leave on screen.
        shortcuts.forEach { rule.onNodeWithText(it.label).assertIsDisplayed() }

        rule.onNodeWithText("Mercado").performClick()
        assert(tapped?.label == "Mercado") { "the fourth shortcut must be reachable, not clipped" }
    }

    @Test
    fun shortcutCardsMeetTheMinimumTouchTarget() {
        rule.setContent {
            MoneyFlowTheme(darkTheme = false) {
                ShortcutsRow(
                    shortcuts = listOf(QuickShortcut("Almuerzo", 1_500, "comida", "cash")),
                    currencyCode = "PEN",
                    onShortcut = {},
                )
            }
        }

        rule.onNodeWithText("Almuerzo")
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
    }

    /** With nothing to show, the row states the rule rather than rendering nothing. */
    @Test
    fun withNoShortcuts_theEmptyStateExplainsWhen() {
        rule.setContent {
            MoneyFlowTheme(darkTheme = false) { ShortcutsEmptyCard() }
        }

        rule.onNodeWithText("De un toque").assertIsDisplayed()
        rule.onNodeWithText("30 días", substring = true).assertIsDisplayed()
    }
}
