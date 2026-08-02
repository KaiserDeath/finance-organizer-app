package pe.moneyflow.app

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import pe.moneyflow.core.common.Money
import pe.moneyflow.core.designsystem.theme.Spacing

private const val StepCount = 5

/**
 * Five steps, one decision each: the promise, how much you want to spend, what you pay with,
 * what you buy daily, and a summary.
 *
 * This is not a welcome tour — it is the minimum setup without which the other screens have
 * nothing to say. Steps 2 to 4 can be skipped, and each skip has a defined degradation
 * (see [OnboardingViewModel]).
 */
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { StepCount })
    val scope = rememberCoroutineScope()

    var budgetText by remember { mutableStateOf("") }
    // Every method starts selected: the step is about *removing* what you don't have.
    var selectedMethodIds by remember { mutableStateOf<Set<String>?>(null) }
    val selectedShortcuts: SnapshotStateList<ShortcutOption> =
        remember { ShortcutPool.take(2).toMutableStateList() }

    val effectiveMethodIds = selectedMethodIds ?: state.methods.map { it.id }.toSet()
    val budgetMinor = Money.parseToMinor(budgetText)?.takeIf { it > 0 }

    val goNext = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } }
    val finish = {
        viewModel.finish(
            monthlyBudgetMinor = budgetMinor,
            selectedMethodIds = effectiveMethodIds,
            selectedShortcuts = selectedShortcuts.toList(),
            onDone = onFinish,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .padding(Spacing.xl),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            // Only the configuration steps are skippable; the promise and the summary are not.
            if (pagerState.currentPage in 1..3) {
                TextButton(onClick = { goNext() }) { Text("Saltar") }
            } else {
                Spacer(Modifier.height(48.dp))
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) { page ->
            when (page) {
                0 -> PromiseStep()
                1 -> BudgetStep(budgetText = budgetText, onBudgetChange = { budgetText = it })
                2 -> MethodsStep(
                    methods = state.methods,
                    selectedIds = effectiveMethodIds,
                    onToggle = { id ->
                        val current = effectiveMethodIds
                        selectedMethodIds =
                            if (id in current) current - id else current + id
                    },
                )
                3 -> ShortcutsStep(
                    selected = selectedShortcuts,
                    onToggle = { option ->
                        if (option in selectedShortcuts) {
                            selectedShortcuts.remove(option)
                        } else {
                            selectedShortcuts.add(option)
                        }
                    },
                )
                else -> SummaryStep(
                    budgetMinor = budgetMinor,
                    methodCount = effectiveMethodIds.size,
                    shortcutCount = selectedShortcuts.size,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.lg),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(StepCount) { index ->
                val selected = index == pagerState.currentPage
                val width by animateDpAsState(if (selected) 24.dp else 8.dp, label = "dot")
                Box(
                    modifier = Modifier
                        .padding(horizontal = Spacing.xxs)
                        .size(width = width, height = 8.dp)
                        .background(
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = CircleShape,
                        ),
                )
            }
        }

        val isLast = pagerState.currentPage == StepCount - 1
        Button(
            onClick = { if (isLast) finish() else goNext() },
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text(if (isLast) "Comenzar" else "Siguiente")
        }
    }
}

@Composable
private fun StepScaffold(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

@Composable
private fun PromiseStep() {
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.AccountBalanceWallet,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp),
            )
        }
        Spacer(Modifier.height(Spacing.xxl))
        Text(
            text = "Sabe siempre cuánto te queda",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = "Tres preguntas rápidas y la app queda lista para tu día a día. " +
                "Tus datos viven solo en tu teléfono.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BudgetStep(budgetText: String, onBudgetChange: (String) -> Unit) {
    StepScaffold(
        title = "¿Cuánto quieres gastar al mes?",
        subtitle = "Es el punto de referencia de tu pantalla de inicio. Puedes cambiarlo cuando quieras.",
    ) {
        OutlinedTextField(
            value = budgetText,
            onValueChange = onBudgetChange,
            label = { Text("Presupuesto mensual") },
            prefix = { Text("${Money.symbolFor("PEN")} ") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MethodsStep(
    methods: List<pe.moneyflow.core.model.PaymentMethod>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
) {
    StepScaffold(
        title = "¿Con qué pagas?",
        subtitle = "Solo estos aparecerán al registrar un gasto y al pagar. Quita los que no uses.",
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            methods.forEach { method ->
                FilterChip(
                    selected = method.id in selectedIds,
                    onClick = { onToggle(method.id) },
                    label = { Text(method.name) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShortcutsStep(
    selected: List<ShortcutOption>,
    onToggle: (ShortcutOption) -> Unit,
) {
    StepScaffold(
        title = "¿Qué compras casi a diario?",
        subtitle = "Cada uno se vuelve un atajo de un toque en tu inicio, con su monto habitual.",
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            ShortcutPool.forEach { option ->
                FilterChip(
                    selected = option in selected,
                    onClick = { onToggle(option) },
                    label = {
                        Text("${option.label} · ${Money.format(option.amountMinor)}")
                    },
                )
            }
        }
    }
}

@Composable
private fun SummaryStep(budgetMinor: Long?, methodCount: Int, shortcutCount: Int) {
    StepScaffold(
        title = "Listo",
        subtitle = "Así queda tu configuración. Todo se puede cambiar después en Ajustes.",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            SummaryLine(
                label = "Presupuesto mensual",
                value = budgetMinor?.let { Money.format(it) } ?: "Sin definir",
            )
            SummaryLine(label = "Métodos de pago", value = "$methodCount activo(s)")
            SummaryLine(
                label = "Atajos de un toque",
                value = if (shortcutCount > 0) "$shortcutCount" else "Ninguno por ahora",
            )
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
