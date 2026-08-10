package pe.moneyflow.core.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pe.moneyflow.core.designsystem.theme.MoneyFlowTheme

/**
 * Isolates the settle/undo snackbar pattern shared by Inicio, Próximos, and AddEdit's "Pagar con X":
 * show a snackbar with a "Deshacer" action, and treat `SnackbarResult.ActionPerformed` as the signal
 * to undo. A manual device pass repeatedly saw the action time out instead of resolving as performed
 * when driven by `adb shell input tap`; this test drives the exact same click through Compose's own
 * test framework instead of raw screen coordinates, to tell a tooling quirk apart from a real bug.
 */
class SnackbarUndoTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun tappingTheSnackbarActionReportsActionPerformed() {
        var result: SnackbarResult? = null

        rule.setContent {
            MoneyFlowTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                LaunchedEffect(Unit) {
                    result = snackbarHostState.showSnackbar(
                        message = "Pagado con Yape",
                        actionLabel = "Deshacer",
                        duration = SnackbarDuration.Indefinite,
                    )
                }
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                ) { }
            }
        }

        rule.onNodeWithText("Deshacer").performClick()
        rule.waitForIdle()

        assertTrue(
            "tapping the snackbar action should resolve as ActionPerformed, was $result",
            result == SnackbarResult.ActionPerformed,
        )
    }
}
