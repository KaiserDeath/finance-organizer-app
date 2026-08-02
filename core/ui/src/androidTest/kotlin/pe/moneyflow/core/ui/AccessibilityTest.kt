package pe.moneyflow.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import pe.moneyflow.core.designsystem.component.SectionHeader
import pe.moneyflow.core.designsystem.component.StatTile
import pe.moneyflow.core.designsystem.theme.MoneyFlowTheme
import pe.moneyflow.core.ui.component.ColorSwatchPicker
import pe.moneyflow.core.ui.component.IconChoicePicker

/**
 * Regression guards for the accessibility defects found in the design audit. Each test corresponds to
 * a specific fix, so a revert fails here rather than silently shipping.
 */
class AccessibilityTest {

    @get:Rule
    val rule = createComposeRule()

    private fun setThemedContent(
        fontScale: Float = 1f,
        content: @Composable () -> Unit,
    ) = rule.setContent {
        val base = LocalDensity.current
        CompositionLocalProvider(
            LocalDensity provides Density(density = base.density, fontScale = fontScale),
        ) {
            MoneyFlowTheme(darkTheme = false) { content() }
        }
    }

    // -----------------------------------------------------------------------------------------
    // Touch targets. The pickers keep a compact 30dp/40dp visual, but every cell must be tappable
    // at the 48dp minimum — previously the visual size *was* the touch target.
    // -----------------------------------------------------------------------------------------

    @Test
    fun colorSwatchPicker_cellsMeetMinimumTouchTarget() {
        setThemedContent {
            ColorSwatchPicker(selectedHex = null, onSelect = {})
        }
        rule.onNodeWithContentDescription("Color 1")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun iconChoicePicker_cellsMeetMinimumTouchTarget() {
        setThemedContent {
            IconChoicePicker(
                choices = listOf("food", "transport"),
                selectedKey = null,
                onSelect = {},
            )
        }
        rule.onNodeWithContentDescription("Ícono food")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun colorSwatchPicker_cellsAreClickable() {
        setThemedContent {
            ColorSwatchPicker(selectedHex = null, onSelect = {})
        }
        rule.onNodeWithContentDescription("Color 1").assertHasClickAction()
    }

    // -----------------------------------------------------------------------------------------
    // Text scaling. StatTile carries currency amounts, so at large font scale the figure must reflow
    // onto a second line rather than ellipsize — a half-shown amount is silently wrong.
    // -----------------------------------------------------------------------------------------

    @Test
    fun statTile_amountReflowsInsteadOfTruncating_atLargeFontScale() {
        // 160dp is roughly a half-width tile. At 2x scale the amount cannot fit on one line here, so
        // a correctly-reflowing Text occupies two lines. If `maxLines = 1` is reintroduced the node
        // collapses back to a single line and this assertion fails.
        setThemedContent(fontScale = 2f) {
            Box(modifier = Modifier.width(160.dp)) {
                StatTile(
                    label = "Hoy",
                    value = "S/ 1,234.56",
                    icon = Icons.Rounded.CalendarToday,
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        // titleLarge is 20sp/26sp line height; at 2x that is a ~52dp line, so >70dp proves two lines.
        rule.onNodeWithText("S/ 1,234.56").assertHeightIsAtLeast(70.dp)
    }

    // -----------------------------------------------------------------------------------------
    // Real affordances. SectionHeader's action rendered as a primary-colored, tappable-looking label
    // with no click handler attached at all — `onActionClick` was only a visibility guard.
    // -----------------------------------------------------------------------------------------

    @Test
    fun sectionHeaderAction_isClickableAndMeetsTouchTarget() {
        setThemedContent {
            Row {
                SectionHeader(
                    title = "Movimientos recientes",
                    actionLabel = "Ver todo",
                    onActionClick = {},
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        rule.onNodeWithText("Ver todo")
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun sectionHeaderAction_isAbsentWithoutHandler() {
        setThemedContent {
            SectionHeader(title = "Movimientos recientes", modifier = Modifier.fillMaxWidth())
        }
        rule.onNodeWithText("Ver todo").assertDoesNotExist()
    }
}
