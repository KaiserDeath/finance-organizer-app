package pe.moneyflow.feature.budgets

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import pe.moneyflow.core.designsystem.theme.MoneyFlowTheme
import pe.moneyflow.core.model.Category
import pe.moneyflow.core.model.CategoryType

/**
 * Which control the budget sheets use to enter an amount.
 *
 * Both sheets took a plain text field with a decimal IME while add/edit and onboarding had already
 * moved to an in-app keypad — so the same job, typing an amount, looked like two different jobs
 * depending on the screen you were standing on. That drift arrived silently and would return the
 * same way: nothing computational changes when a keypad reverts to a text field, so no ViewModel
 * test can see it. These compose the sheets and look.
 */
class BudgetSheetsKeypadTest {

    @get:Rule
    val rule = createComposeRule()

    private val comida = Category(
        id = "comida",
        name = "Comida",
        iconKey = "food",
        colorHex = "#FF7043",
        type = CategoryType.EXPENSE,
    )

    private fun showMonthSheet(current: Long? = null) = rule.setContent {
        MoneyFlowTheme(darkTheme = false) {
            MonthBudgetSheet(
                currentMinor = current,
                currencyCode = "PEN",
                onDismiss = {},
                onConfirm = {},
            )
        }
    }

    private fun showEditor() = rule.setContent {
        MoneyFlowTheme(darkTheme = false) {
            BudgetEditorSheet(
                existing = null,
                categories = listOf(comida),
                onDismiss = {},
                currencyCode = "PEN",
                onConfirm = { _, _, _, _ -> },
            )
        }
    }

    @Test
    fun monthBudgetSheet_entersTheAmountOnTheKeypad() {
        showMonthSheet()

        // A digit key and the backspace, which only the keypad has.
        rule.onNodeWithContentDescription("7").assertHeightIsAtLeast(48.dp)
        rule.onNodeWithContentDescription("Borrar").assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun monthBudgetSheet_keypadDrivesTheReadout() {
        showMonthSheet()

        rule.onNodeWithContentDescription("2").performClick()
        rule.onNodeWithContentDescription("5").performClick()
        rule.onNodeWithContentDescription("0").performClick()

        rule.onNodeWithText("S/ 250").assertExists()
    }

    /** Backspace has to reach the read-out too, or the display and the value can disagree. */
    @Test
    fun monthBudgetSheet_backspaceReachesTheReadout() {
        showMonthSheet()

        rule.onNodeWithContentDescription("9").performClick()
        rule.onNodeWithContentDescription("9").performClick()
        rule.onNodeWithContentDescription("Borrar").performClick()

        rule.onNodeWithText("S/ 9").assertExists()
    }

    @Test
    fun budgetEditor_opensOnTheKeypadForANewBudget() {
        showEditor()

        rule.onNodeWithContentDescription("7").assertHeightIsAtLeast(48.dp)
        rule.onNodeWithContentDescription("Borrar").assertExists()
    }

    /**
     * The name field is the one place an IME still belongs, and two keyboards stacked on one sheet
     * is the state worth guarding against — so focusing the name puts the keypad away.
     */
    @Test
    fun budgetEditor_typingTheNamePutsTheKeypadAway() {
        showEditor()

        rule.onNodeWithText("Nombre").performClick()

        rule.onNodeWithContentDescription("7").assertDoesNotExist()
    }

    /** And the read-out brings it back, which is the only way back once the name took focus. */
    @Test
    fun budgetEditor_tappingTheAmountBringsTheKeypadBack() {
        showEditor()

        rule.onNodeWithText("Nombre").performClick()
        rule.onNodeWithText("Monto").performClick()

        rule.onNodeWithContentDescription("7").assertExists()
    }
}
