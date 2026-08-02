package pe.moneyflow.feature.upcoming

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
import pe.moneyflow.core.domain.model.UpcomingBucket
import pe.moneyflow.core.domain.model.UpcomingPayment
import pe.moneyflow.core.model.PaymentMethod
import pe.moneyflow.core.model.Transaction
import pe.moneyflow.core.model.TransactionStatus
import pe.moneyflow.core.model.TransactionType
import java.time.LocalDate

/**
 * Composition-level guards for the pay sheet.
 *
 * These exist because the sheet's defects are structural rather than computational: an action
 * offered twice, a label that does not match what the button will do, a control shown in a state
 * where it means nothing. A ViewModel test cannot see any of that — it never composes anything —
 * and a screenshot only proves the one state that happened to be on screen.
 */
class PaySheetTest {

    @get:Rule
    val rule = createComposeRule()

    private val yape = PaymentMethod(
        id = "yape",
        name = "Yape",
        iconKey = "wallet",
        colorHex = "#7E57C2",
        deepLinkPackage = "com.bcp.innovacxion.yapeapp",
    )
    private val cash = PaymentMethod(
        id = "cash",
        name = "Efectivo",
        iconKey = "cash",
        colorHex = "#26A69A",
    )

    private val gym = Transaction(
        id = "gym",
        title = "Gimnasio",
        amountMinor = 12_000,
        categoryId = "c1",
        type = TransactionType.EXPENSE,
        status = TransactionStatus.PENDING,
        estimatedDate = LocalDate.of(2026, 8, 20),
    )

    private fun show(
        transaction: Transaction = gym,
        suggested: String?,
        projected: Boolean = false,
        onLaunchApp: (PaymentMethod) -> Unit = {},
        onSettle: (PaymentMethod?) -> Unit = {},
    ) = rule.setContent {
        MoneyFlowTheme(darkTheme = false) {
            PaySheet(
                payment = UpcomingPayment(
                    transaction = transaction,
                    bucket = UpcomingBucket.THIS_MONTH,
                    category = null,
                    dueDate = transaction.estimatedDate,
                    isProjected = projected,
                ),
                methods = listOf(cash, yape),
                suggestedMethodId = suggested,
                onLaunchApp = onLaunchApp,
                onSettle = onSettle,
                onDismiss = {},
            )
        }
    }

    // -----------------------------------------------------------------------------------------
    // The duplicate-action defect. With a method that has no app, the primary button and
    // "Ya pagué por fuera" both called onSettle — two stacked controls, one behaviour, no way for
    // the user to tell them apart. The secondary only earns its place when the primary does
    // something else.
    // -----------------------------------------------------------------------------------------

    @Test
    fun methodWithoutApp_offersOnlyOneWayToRecordThePayment() {
        show(suggested = "cash")

        rule.onNodeWithText("Registrar el pago").assertIsDisplayed()
        rule.onNodeWithText("Ya pagué por fuera").assertDoesNotExist()
    }

    @Test
    fun methodWithApp_offersOpeningTheAppAndRecordingItSeparately() {
        show(suggested = "yape")

        rule.onNodeWithText("Abrir Yape y registrar").assertIsDisplayed()
        rule.onNodeWithText("Ya pagué por fuera").assertIsDisplayed()
    }

    /** The explanation replaces a dead button; without an app there has to be a reason, not silence. */
    @Test
    fun methodWithoutApp_explainsWhyThereIsNothingToOpen() {
        show(suggested = "cash")

        rule.onNodeWithText("Efectivo no tiene app vinculada", substring = true).assertIsDisplayed()
    }

    @Test
    fun methodWithApp_showsNoSuchExplanation() {
        show(suggested = "yape")

        rule.onNodeWithText("no tiene app vinculada", substring = true).assertDoesNotExist()
    }

    // -----------------------------------------------------------------------------------------
    // Each control does what its label says.
    // -----------------------------------------------------------------------------------------

    @Test
    fun primaryLaunchesTheApp_andDoesNotSettleBehindTheUsersBack() {
        var launched: PaymentMethod? = null
        var settled = false
        show(suggested = "yape", onLaunchApp = { launched = it }, onSettle = { settled = true })

        rule.onNodeWithText("Abrir Yape y registrar").performClick()

        assert(launched?.id == "yape") { "the primary must open the selected method's app" }
        assert(!settled) { "settling happens on return from the app, not on tapping it" }
    }

    @Test
    fun paidElsewhere_settlesWithTheSelectedMethod() {
        var settled: PaymentMethod? = null
        show(suggested = "yape", onSettle = { settled = it })

        rule.onNodeWithText("Ya pagué por fuera").performClick()

        assert(settled?.id == "yape") {
            "recording the method is what makes the suggestion improve with use"
        }
    }

    // -----------------------------------------------------------------------------------------
    // Recurrence and reach.
    // -----------------------------------------------------------------------------------------

    @Test
    fun recurringOccurrence_saysSoWithoutClaimingACadence() {
        show(transaction = gym.copy(recurringId = "tpl-1"), suggested = "cash")

        rule.onNodeWithText("Pago recurrente").assertIsDisplayed()
    }

    @Test
    fun oneOffPayment_isNotMarkedRecurring() {
        show(suggested = "cash")

        rule.onNodeWithText("Pago recurrente").assertDoesNotExist()
    }

    @Test
    fun projectedOccurrence_isMarkedRecurringEvenWithoutARowBehindIt() {
        show(transaction = gym.copy(id = ""), suggested = "cash", projected = true)

        rule.onNodeWithText("Pago recurrente").assertIsDisplayed()
    }

    /** The handoff's 48dp floor, on the control that commits money. */
    @Test
    fun primaryActionMeetsTheMinimumTouchTarget() {
        show(suggested = "cash")

        rule.onNodeWithText("Registrar el pago")
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
    }
}
