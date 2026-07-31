package pe.moneyflow.core.designsystem.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pe.moneyflow.core.designsystem.component.BarChart
import pe.moneyflow.core.designsystem.component.CumulativeLineChart
import pe.moneyflow.core.designsystem.component.BarChartEntry
import pe.moneyflow.core.designsystem.component.DonutChart
import pe.moneyflow.core.designsystem.component.DonutSlice
import pe.moneyflow.core.designsystem.component.EmptyState
import pe.moneyflow.core.designsystem.component.ErrorState
import pe.moneyflow.core.designsystem.component.GlassCard
import pe.moneyflow.core.designsystem.component.MoneyCard
import pe.moneyflow.core.designsystem.component.SectionHeader
import pe.moneyflow.core.designsystem.component.ShimmerBox
import pe.moneyflow.core.designsystem.component.SkeletonBlocks
import pe.moneyflow.core.designsystem.component.SkeletonRows
import pe.moneyflow.core.designsystem.component.StatTile
import pe.moneyflow.core.designsystem.illustration.Illustration
import pe.moneyflow.core.designsystem.illustration.MoneyFlowIllustration
import pe.moneyflow.core.designsystem.theme.CategoryPalette
import pe.moneyflow.core.designsystem.theme.Spacing
import pe.moneyflow.core.designsystem.theme.moneyColors

// ---------------------------------------------------------------------------------------------
// Foundation specimens — these are the previews that verify the token layer itself.
// ---------------------------------------------------------------------------------------------

/**
 * Every `ColorScheme` role, side by side.
 *
 * This is the check that the palette is actually complete: any role still falling back to Material's
 * baseline shows up here as a warm purple-grey or pink among the cool indigo family — which is
 * exactly how the nav bar, bottom sheets, and status pills were drifting off-palette.
 */
@ThemePreviews
@Composable
private fun ColorSchemePreview() {
    MoneyFlowPreviewTheme {
        val scheme = MaterialTheme.colorScheme
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text("Brand", style = MaterialTheme.typography.labelLarge)
            SwatchPair("primary", scheme.primary, scheme.onPrimary)
            SwatchPair("primaryContainer", scheme.primaryContainer, scheme.onPrimaryContainer)
            SwatchPair("secondary", scheme.secondary, scheme.onSecondary)
            SwatchPair("secondaryContainer", scheme.secondaryContainer, scheme.onSecondaryContainer)
            SwatchPair("tertiary", scheme.tertiary, scheme.onTertiary)
            SwatchPair("tertiaryContainer", scheme.tertiaryContainer, scheme.onTertiaryContainer)
            SwatchPair("error", scheme.error, scheme.onError)
            SwatchPair("errorContainer", scheme.errorContainer, scheme.onErrorContainer)

            Spacer(Modifier.height(Spacing.sm))
            Text("Surfaces", style = MaterialTheme.typography.labelLarge)
            SwatchPair("background", scheme.background, scheme.onBackground)
            SwatchPair("surface", scheme.surface, scheme.onSurface)
            SwatchPair("surfaceVariant", scheme.surfaceVariant, scheme.onSurfaceVariant)
            SwatchPair("containerLowest", scheme.surfaceContainerLowest, scheme.onSurface)
            SwatchPair("containerLow", scheme.surfaceContainerLow, scheme.onSurface)
            SwatchPair("container", scheme.surfaceContainer, scheme.onSurface)
            SwatchPair("containerHigh", scheme.surfaceContainerHigh, scheme.onSurface)
            SwatchPair("containerHighest", scheme.surfaceContainerHighest, scheme.onSurface)
            SwatchPair("inverseSurface", scheme.inverseSurface, scheme.inverseOnSurface)

            Spacer(Modifier.height(Spacing.sm))
            Text("Money direction", style = MaterialTheme.typography.labelLarge)
            SwatchPair("positive", MaterialTheme.moneyColors.positive, scheme.surface)
            SwatchPair("negative", MaterialTheme.moneyColors.negative, scheme.surface)

            Spacer(Modifier.height(Spacing.sm))
            Text("Category palette", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                CategoryPalette.forEach { color ->
                    Box(
                        Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(color),
                    )
                }
            }
        }
    }
}

@Composable
private fun SwatchPair(name: String, container: Color, on: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(width = 108.dp, height = 28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(container),
            contentAlignment = Alignment.Center,
        ) {
            Text("Aa 1,234", style = MaterialTheme.typography.labelSmall, color = on)
        }
        Spacer(Modifier.width(Spacing.sm))
        Text(name, style = MaterialTheme.typography.labelMedium)
    }
}

/**
 * The type ramp as a specimen.
 *
 * Read it top to bottom: weight must never get lighter as size grows. This is the preview that
 * catches a role falling back to a Material baseline weight — which is how `headlineSmall` (11 screen
 * titles) ended up at weight 400 next to a 600-weight `titleLarge`.
 */
@Preview(name = "Type ramp", group = "foundation", heightDp = 900)
@Composable
private fun TypographyPreview() {
    MoneyFlowPreviewTheme {
        val t = MaterialTheme.typography
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Specimen("displayLarge", t.displayLarge)
            Specimen("displayMedium", t.displayMedium)
            Specimen("displaySmall", t.displaySmall)
            Specimen("headlineLarge", t.headlineLarge)
            Specimen("headlineMedium", t.headlineMedium)
            Specimen("headlineSmall", t.headlineSmall)
            Specimen("titleLarge", t.titleLarge)
            Specimen("titleMedium", t.titleMedium)
            Specimen("titleSmall", t.titleSmall)
            Specimen("bodyLarge", t.bodyLarge)
            Specimen("bodyMedium", t.bodyMedium)
            Specimen("bodySmall", t.bodySmall)
            Specimen("labelLarge", t.labelLarge)
            Specimen("labelMedium", t.labelMedium)
            Specimen("labelSmall", t.labelSmall)
        }
    }
}

@Composable
private fun Specimen(name: String, style: TextStyle) {
    Column {
        Text(
            text = "$name · ${style.fontSize.value.toInt()}sp · w${style.fontWeight?.weight}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("S/ 1,111.00 · S/ 8,888.00", style = style)
    }
}

/**
 * Tabular figures.
 *
 * Every row must be exactly the same width and the decimal points must form a straight vertical
 * line. If `1` renders narrower than `8`, `fontFeatureSettings = "tnum"` isn't reaching this style.
 */
@Preview(name = "Tabular figures", group = "foundation")
@Composable
private fun TabularFiguresPreview() {
    MoneyFlowPreviewTheme {
        Column {
            listOf(
                "S/ 1,111.11",
                "S/ 8,888.88",
                "S/ 1,888.11",
                "S/ 8,111.88",
                "S/ 4,567.89",
            ).forEach { Text(it, style = MaterialTheme.typography.titleMedium) }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Components
// ---------------------------------------------------------------------------------------------

/**
 * The regression guard for the amount-truncation defect. At 200% font scale both values must be
 * fully readable — no ellipsis, no clipping.
 */
@ThemeAndScalePreviews
@Composable
private fun StatTilePreview() {
    MoneyFlowPreviewTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            StatTile(
                label = "Hoy",
                value = "S/ 1,234.56",
                icon = Icons.Rounded.CalendarToday,
                accent = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
                shadowElevation = 0.dp,
            )
            StatTile(
                label = "Movimientos",
                value = "128",
                icon = Icons.AutoMirrored.Rounded.TrendingUp,
                accent = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f),
                shadowElevation = 0.dp,
            )
        }
    }
}

@ThemePreviews
@Composable
private fun CardsPreview() {
    MoneyFlowPreviewTheme {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Gastado este mes",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                Text("S/ 1,240.50", style = MaterialTheme.typography.displayMedium)
            }
            MoneyCard(modifier = Modifier.fillMaxWidth(), shadowElevation = 0.dp) {
                Text("MoneyCard", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Content padding is Spacing.xl against the 24dp radius.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** "Ver todo" must render as a real, tappable button here — it used to be an inert label. */
@ThemePreviews
@Composable
private fun SectionHeaderPreview() {
    MoneyFlowPreviewTheme {
        SectionHeader(
            title = "Movimientos recientes",
            actionLabel = "Ver todo",
            onActionClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@ThemePreviews
@Composable
private fun EmptyStatePreview() {
    MoneyFlowPreviewTheme {
        MoneyCard(modifier = Modifier.fillMaxWidth(), shadowElevation = 0.dp) {
            EmptyState(
                icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                title = "Aún no hay gastos",
                subtitle = "Toca el botón + para registrar tu primer gasto.",
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@ThemePreviews
@Composable
private fun ChartsPreview() {
    MoneyFlowPreviewTheme {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xl)) {
            DonutChart(
                slices = listOf(
                    DonutSlice(0.42f, CategoryPalette[0]),
                    DonutSlice(0.25f, CategoryPalette[1]),
                    DonutSlice(0.18f, CategoryPalette[2]),
                    DonutSlice(0.15f, CategoryPalette[3]),
                ),
                diameter = 132.dp,
                contentDescription = "Gasto por categoría, total S/ 1,240.50. " +
                    "Mayor: Comida, 42%.",
                centerContent = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("S/ 1,240.50", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "4 categorías",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
            BarChart(
                entries = listOf(
                    BarChartEntry("Feb", 120_00),
                    BarChartEntry("Mar", 185_00),
                    BarChartEntry("Abr", 90_00),
                    BarChartEntry("May", 240_00),
                    BarChartEntry("Jun", 160_00),
                    BarChartEntry("Jul", 210_00, highlighted = true),
                ),
                contentDescription = "Gasto por mes.",
                valueLabel = { "S/ ${it.value / 100}" },
            )
        }
    }
}

@ThemePreviews
@Composable
private fun SkeletonPreview() {
    MoneyFlowPreviewTheme {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            ShimmerBox(modifier = Modifier.fillMaxWidth().height(140.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                ShimmerBox(modifier = Modifier.weight(1f).height(96.dp))
                ShimmerBox(modifier = Modifier.weight(1f).height(96.dp))
            }
        }
    }
}

/** The two shared skeleton shapes. Previews are static — run these interactively to see the sweep. */
@Preview(name = "Skeleton · blocks", group = "states")
@Composable
private fun SkeletonBlocksPreview() {
    MoneyFlowPreviewTheme {
        SkeletonBlocks(heroHeight = 116.dp, count = 2, blockHeight = 148.dp)
    }
}

@Preview(name = "Skeleton · rows", group = "states")
@Composable
private fun SkeletonRowsPreview() {
    MoneyFlowPreviewTheme { SkeletonRows(count = 4) }
}

/**
 * The illustration family, all together.
 *
 * Reviewing them as a set is the point: they must read as one system. Same 2.5dp rounded stroke
 * weight, same two-tone construction, same optical weight — a single drawing that's noticeably heavier
 * or busier than the others breaks the family even if it looks fine alone.
 */
@ThemePreviews
@Composable
private fun IllustrationsPreview() {
    MoneyFlowPreviewTheme {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            Illustration.entries.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                    row.forEach { art ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            MoneyFlowIllustration(illustration = art, size = 88.dp)
                            Text(
                                text = art.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** The cash-flow curve: solid current month, dashed previous, shared y-ceiling. */
@ThemePreviews
@Composable
private fun CumulativeLineChartPreview() {
    MoneyFlowPreviewTheme {
        // 18 days in, running ahead of a full 30-day previous month.
        val previous = (1..30).runningFold(0L) { acc, d -> acc + 40_00 + (d % 5) * 9_00 }.drop(1)
        val current = (1..18).runningFold(0L) { acc, d -> acc + 55_00 + (d % 4) * 11_00 }.drop(1)
        CumulativeLineChart(
            current = current,
            previous = previous,
            peakMinor = maxOf(current.max(), previous.max()),
            contentDescription = "Ritmo de gasto acumulado.",
        )
    }
}

/**
 * Empty vs. error, side by side — the pair worth reviewing together. The error variant must read as
 * "something went wrong, try again", never as "you have no data".
 */
@ThemePreviews
@Composable
private fun EmptyVsErrorPreview() {
    MoneyFlowPreviewTheme {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            MoneyCard(modifier = Modifier.fillMaxWidth(), shadowElevation = 0.dp) {
                EmptyState(
                    icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                    title = "Aún no hay gastos",
                    subtitle = "Toca el botón + para registrar tu primer gasto.",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            MoneyCard(modifier = Modifier.fillMaxWidth(), shadowElevation = 0.dp) {
                ErrorState(
                    title = "No pudimos cargar tus gastos",
                    subtitle = "Tus datos están a salvo. Revisa tu conexión e inténtalo de nuevo.",
                    onRetry = {},
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
