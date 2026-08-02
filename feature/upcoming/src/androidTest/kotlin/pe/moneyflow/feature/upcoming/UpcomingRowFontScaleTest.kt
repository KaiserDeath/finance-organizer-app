package pe.moneyflow.feature.upcoming

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import pe.moneyflow.core.designsystem.theme.MoneyFlowTheme
import pe.moneyflow.core.domain.model.UpcomingBucket
import pe.moneyflow.core.domain.model.UpcomingPayment
import pe.moneyflow.core.model.Transaction
import pe.moneyflow.core.model.TransactionStatus
import pe.moneyflow.core.model.TransactionType
import java.time.LocalDate

/**
 * The upcoming row at the accessibility ceiling.
 *
 * This is the densest row in the app — avatar, title, due label, amount and an action — and the
 * amount has no `maxLines`, so a five-figure sum at 200% is exactly where it would fight the button
 * for width. It lives here rather than in `core:ui`'s `AccessibilityTest` because `UpcomingRow` is
 * this module's, and `core:ui` cannot depend on a feature module to reach it.
 *
 * Width is pinned to a narrow phone: the failure being guarded against is a squeeze, so testing at
 * the emulator's natural width would hide it.
 */
class UpcomingRowFontScaleTest {

    @get:Rule
    val rule = createComposeRule()

    private val internet = Transaction(
        id = "internet",
        // Long enough to need eliding, which is the row's documented behaviour for the title.
        title = "Internet — Movistar Fibra 200 Mbps",
        amountMinor = 1_299_900,
        categoryId = "servicios",
        type = TransactionType.EXPENSE,
        status = TransactionStatus.PENDING,
        estimatedDate = LocalDate.of(2026, 8, 20),
    )

    private fun showRow(fontScale: Float, transaction: Transaction = internet) = rule.setContent {
        val base = LocalDensity.current
        CompositionLocalProvider(
            LocalDensity provides Density(density = base.density, fontScale = fontScale),
        ) {
            MoneyFlowTheme(darkTheme = false) {
                UpcomingRow(
                    payment = UpcomingPayment(
                        transaction = transaction,
                        bucket = UpcomingBucket.THIS_MONTH,
                        category = null,
                        dueDate = transaction.estimatedDate,
                    ),
                    onClick = {},
                    onPay = {},
                    modifier = Modifier.width(320.dp),
                )
            }
        }
    }

    @Test
    fun payAction_keepsItsLabelAndTouchTargetAtDoubleFontScale() {
        showRow(fontScale = 2f)

        // The pill draws at 32dp; the hit area must stay 48dp however large the text gets.
        rule.onNodeWithText("Pagar").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun amountStaysVisible_atDoubleFontScale() {
        showRow(fontScale = 2f)

        // A five-figure sum is the case that used to fight two icon buttons for trailing width.
        rule.onNodeWithText("S/ 12,999.00").assertIsDisplayed().assertWidthIsAtLeast(1.dp)
    }

    @Test
    fun dueLabelSurvives_atDoubleFontScale() {
        showRow(fontScale = 2f)

        rule.onNodeWithText("20 ago", substring = true).assertIsDisplayed()
    }
}
