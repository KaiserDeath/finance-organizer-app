package pe.moneyflow.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import pe.moneyflow.core.designsystem.icon.iconForKey
import pe.moneyflow.core.designsystem.theme.IconSize
import pe.moneyflow.core.designsystem.util.colorFromHex

/**
 * Shared swatch palette for user-assignable colors on categories, payment methods, and accounts.
 * Previously duplicated verbatim in CategoriesScreen and PaymentMethodsScreen.
 */
val SwatchPalette = listOf(
    "#4F46E5", "#0EA5A5", "#F59E0B", "#EC4899", "#8B5CF6", "#10B981",
    "#EF4444", "#3B82F6", "#F97316", "#14B8A6", "#A855F7", "#84CC16",
)

/**
 * The minimum touch target these pickers guarantee.
 *
 * Both pickers keep their compact *visual* size (a 30dp dot, a 40dp icon circle) but center it in a
 * 48dp tappable cell. The originals made the visual size the tappable size, leaving 30dp and 40dp
 * targets against the 48dp accessibility minimum — with only 8dp of spacing between them, so the
 * gaps didn't rescue the target either. Cell spacing is 0 here because the cells now supply their
 * own breathing room.
 */
private val TouchTarget = 48.dp

/** A wrapping grid of color swatches. Selection is shown with a ring, not by color alone. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorSwatchPicker(
    selectedHex: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    palette: List<String> = SwatchPalette,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        palette.forEachIndexed { index, hex ->
            val selected = selectedHex == hex
            val color = colorFromHex(hex, MaterialTheme.colorScheme.primary)
            Box(
                modifier = Modifier
                    .size(TouchTarget)
                    .clip(CircleShape)
                    .clickable { onSelect(hex) }
                    .semantics {
                        role = Role.RadioButton
                        this.selected = selected
                        contentDescription = "Color ${index + 1}"
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (selected) 3.dp else 0.dp,
                            color = MaterialTheme.colorScheme.onSurface,
                            shape = CircleShape,
                        ),
                )
            }
        }
    }
}

/**
 * A wrapping grid of icon choices. [choices] are `iconForKey` keys — the sets differ by screen
 * (category icons vs. payment-method icons), so the list is a parameter rather than a constant.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IconChoicePicker(
    choices: List<String>,
    selectedKey: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        choices.forEach { key ->
            val selected = selectedKey == key
            Box(
                modifier = Modifier
                    .size(TouchTarget)
                    .clip(CircleShape)
                    .clickable { onSelect(key) }
                    .semantics {
                        role = Role.RadioButton
                        this.selected = selected
                        contentDescription = "Ícono $key"
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = iconForKey(key),
                        contentDescription = null,
                        tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(IconSize.md),
                    )
                }
            }
        }
    }
}
