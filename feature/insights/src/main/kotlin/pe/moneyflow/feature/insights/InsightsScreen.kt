package pe.moneyflow.feature.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.moneyflow.core.designsystem.component.MoneyCard
import pe.moneyflow.core.designsystem.theme.NegativeRed
import pe.moneyflow.core.designsystem.theme.PositiveGreen
import pe.moneyflow.core.designsystem.theme.Spacing
import pe.moneyflow.core.domain.model.Insight
import pe.moneyflow.core.domain.model.InsightKind
import pe.moneyflow.core.domain.model.InsightSeverity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InsightsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Sugerencias") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(
                start = Spacing.lg,
                end = Spacing.lg,
                top = Spacing.md,
                bottom = 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item {
                Text(
                    text = "Basado en tus movimientos de este mes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Spacing.xs),
                )
            }
            items(uiState.insights, key = { it.id }) { insight ->
                InsightCard(insight)
            }
        }
    }
}

@Composable
private fun InsightCard(insight: Insight) {
    val accent = insight.severity.accent()
    MoneyCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = insight.kind.icon(),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = Spacing.md)) {
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = insight.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun InsightSeverity.accent(): Color = when (this) {
    InsightSeverity.WARNING -> NegativeRed
    InsightSeverity.POSITIVE -> PositiveGreen
    InsightSeverity.INFO -> MaterialTheme.colorScheme.primary
}

private fun InsightKind.icon(): ImageVector = when (this) {
    InsightKind.GETTING_STARTED -> Icons.Rounded.Lightbulb
    InsightKind.CASHFLOW -> Icons.AutoMirrored.Rounded.TrendingUp
    InsightKind.SPENDING_SPIKE -> Icons.Rounded.Warning
    InsightKind.TOP_CATEGORY -> Icons.Rounded.Category
    InsightKind.UPCOMING_BILLS -> Icons.Rounded.CalendarMonth
    InsightKind.OVERDUE_BILLS -> Icons.Rounded.Savings
}
