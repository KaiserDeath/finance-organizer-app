package pe.moneyflow.feature.upcoming

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import org.junit.Rule
import org.junit.Test
import pe.moneyflow.core.common.Money
import pe.moneyflow.core.designsystem.theme.LocalAmountsHidden
import pe.moneyflow.core.designsystem.theme.MoneyFlowTheme
import pe.moneyflow.core.domain.model.UpcomingBucket
import pe.moneyflow.core.domain.model.UpcomingPayment
import pe.moneyflow.core.model.Transaction
import pe.moneyflow.core.model.TransactionStatus
import pe.moneyflow.core.model.TransactionType
import java.time.LocalDate

/**
 * Discreet mode, where it actually matters: on a rendered row.
 *
 * The store test proves the flag persists; it says nothing about whether any screen honours it, and
 * that is the half that fails silently. A composable that kept calling `Money.format` directly
 * would still compile, still pass every ViewModel test, and quietly print the figure — the exact
 * hole the feature cannot have.
 *
 * `UpcomingRow` is the case worth pinning: it is an ordinary list row, not the hero, so it is the
 * kind of place a mask gets forgotten.
 */
class DiscreetModeTest {

    @get:Rule
    val rule = createComposeRule()

    private val internet = Transaction(
        id = "internet",
        title = "Internet",
        amountMinor = 12_999,
        categoryId = "servicios",
        type = TransactionType.EXPENSE,
        status = TransactionStatus.PENDING,
        estimatedDate = LocalDate.of(2026, 8, 20),
    )

    private fun showRow(hidden: Boolean) = rule.setContent {
        MoneyFlowTheme(darkTheme = false, amountsHidden = hidden) {
            UpcomingRow(
                payment = UpcomingPayment(
                    transaction = internet,
                    bucket = UpcomingBucket.THIS_MONTH,
                    category = null,
                    dueDate = internet.estimatedDate,
                ),
                onClick = {},
                onPay = {},
            )
        }
    }

    @Test
    fun visibleByDefault_theAmountIsPrinted() {
        showRow(hidden = false)

        rule.onNodeWithText("S/ 129.99").assertIsDisplayed()
    }

    @Test
    fun hidden_theAmountIsReplacedByTheMask() {
        showRow(hidden = true)

        rule.onNodeWithText(Money.mask("PEN")).assertIsDisplayed()
        // The figure must be gone, not merely covered.
        rule.onAllNodesWithText("S/ 129.99").assertCountEquals(0)
    }

    /** The row still has to be usable while masked: the title and the action are not amounts. */
    @Test
    fun hidden_theRowRemainsUsable() {
        showRow(hidden = true)

        rule.onNodeWithText("Internet").assertIsDisplayed()
        rule.onNodeWithText("Pagar").assertIsDisplayed()
    }

    /**
     * The theme is the only thing that should be able to turn this on. If a screen could be composed
     * outside `MoneyFlowTheme` and default to masked, every preview and test would render dots.
     */
    @Test
    fun theDefaultIsVisible() {
        rule.setContent {
            CompositionLocalProvider(LocalAmountsHidden provides LocalAmountsHidden.current) {
                MoneyFlowTheme(darkTheme = false) {
                    UpcomingRow(
                        payment = UpcomingPayment(
                            transaction = internet,
                            bucket = UpcomingBucket.THIS_MONTH,
                            category = null,
                            dueDate = internet.estimatedDate,
                        ),
                        onClick = {},
                        onPay = {},
                    )
                }
            }
        }

        rule.onNodeWithText("S/ 129.99").assertIsDisplayed()
    }
}
