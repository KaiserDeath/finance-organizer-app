package pe.moneyflow.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import pe.moneyflow.core.common.Money
import pe.moneyflow.core.designsystem.component.MoneyCard
import pe.moneyflow.core.designsystem.theme.IconSize
import pe.moneyflow.core.designsystem.theme.Spacing
import pe.moneyflow.core.designsystem.theme.brandSurface
import pe.moneyflow.core.model.CardKind
import pe.moneyflow.core.model.PaymentMethodType
import java.time.YearMonth

private const val StepCount = 5

/** Digits entered on the budget keypad are whole soles, not cents — nobody budgets to the céntimo. */
private const val MaxBudgetDigits = 7

/**
 * Five steps, one decision each: the promise, how much you want to spend, what you pay with,
 * what you buy daily, and a summary.
 *
 * This is not a welcome tour — it is the minimum setup without which the other screens have
 * nothing to say. Steps 2 to 4 can be skipped, and each skip has a defined degradation
 * (see [OnboardingViewModel]).
 *
 * The chrome follows the prototype rather than Material's pager conventions: a segmented bar and a
 * written "2 de 5" instead of dots, because a dot row says "there are more" without saying how many
 * are left, and a back arrow because swiping a pager backwards is not an affordance anyone sees.
 *
 * @param onFinish called with whether to open the first-expense form; the summary offers both exits.
 */
@Composable
fun OnboardingScreen(
    onFinish: (openAddTransaction: Boolean) -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { StepCount })
    val scope = rememberCoroutineScope()

    var budgetDigits by remember { mutableStateOf("") }
    // Every method starts selected: the step is about *removing* what you don't have.
    var selectedMethodIds by remember { mutableStateOf<Set<String>?>(null) }
    val selectedShortcuts: SnapshotStateList<ShortcutOption> =
        remember { ShortcutPool.take(2).toMutableStateList() }

    val effectiveMethodIds = selectedMethodIds ?: state.methods.map { it.id }.toSet()
    val budgetMinor = budgetDigits.toLongOrNull()?.takeIf { it > 0 }?.times(100)

    val goNext = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } }
    val goBack = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } }
    val finish = { openAdd: Boolean ->
        viewModel.finish(
            monthlyBudgetMinor = budgetMinor,
            selectedMethodIds = effectiveMethodIds,
            selectedShortcuts = selectedShortcuts.toList(),
            onDone = { onFinish(openAdd) },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .padding(Spacing.xl),
    ) {
        OnboardingHeader(
            step = pagerState.currentPage,
            onBack = { goBack() },
            // Only the configuration steps are skippable; the promise and the summary are not.
            onSkip = if (pagerState.currentPage in 1..3) ({ goNext(); Unit }) else null,
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) { page ->
            when (page) {
                0 -> PromiseStep()
                1 -> BudgetStep(
                    digits = budgetDigits,
                    budgetMinor = budgetMinor,
                    onDigit = { d ->
                        if (budgetDigits.length < MaxBudgetDigits) budgetDigits += d
                    },
                    onBackspace = { budgetDigits = budgetDigits.dropLast(1) },
                )
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

        Spacer(Modifier.height(Spacing.lg))

        if (pagerState.currentPage == StepCount - 1) {
            // Two exits, because "listo" is ambiguous: the point of the setup is the first expense,
            // but forcing the form on someone who just wanted to look around is a worse first move.
            Button(
                onClick = { finish(true) },
                modifier = Modifier.fillMaxWidth().height(58.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(IconSize.chip))
                Spacer(Modifier.width(Spacing.sm))
                Text("Registrar mi primer gasto")
            }
            TextButton(
                onClick = { finish(false) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { Text("Ir al inicio") }
        } else {
            Button(
                onClick = { goNext() },
                // The budget step is the one that cannot be half-answered: a zero budget is not a
                // budget, and every downstream screen that needs it would render its empty state.
                // "Saltar" is still there for skipping it outright.
                enabled = pagerState.currentPage != 1 || budgetMinor != null,
                modifier = Modifier.fillMaxWidth().height(58.dp),
            ) {
                Text("Siguiente")
            }
        }
    }
}

@Composable
private fun OnboardingHeader(step: Int, onBack: () -> Unit, onSkip: (() -> Unit)?) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (step > 0) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás")
                }
            } else {
                Spacer(Modifier.size(48.dp))
            }
            Text(
                text = "${step + 1} de $StepCount",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (onSkip != null) {
                TextButton(onClick = onSkip, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Saltar")
                }
            } else {
                Spacer(Modifier.size(48.dp))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            repeat(StepCount) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(
                            color = if (index <= step) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = CircleShape,
                        ),
                )
            }
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
            .padding(vertical = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
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

/**
 * The promise is the hero band, shown before it has any data in it.
 *
 * A generic wallet icon in a circle promised nothing; this shows the actual thing being offered, in
 * the actual colors it will appear in, so "sabrás cuánto te queda" is a claim the user can check
 * against the screen they land on.
 */
@Composable
private fun PromiseStep() {
    val brand = MaterialTheme.brandSurface
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Cada día vas a saber cuánto puedes gastar.",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = "Tres preguntas y listo. Todo se puede cambiar después.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(Spacing.xxl))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            color = brand.container,
            contentColor = brand.content,
        ) {
            Column(modifier = Modifier.padding(Spacing.xxl)) {
                Text(
                    text = "Gastado este mes",
                    style = MaterialTheme.typography.labelLarge,
                    color = brand.mutedContent,
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = Money.format(124_730),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(Spacing.md))
                Surface(shape = CircleShape, color = brand.track, contentColor = brand.content) {
                    Text(
                        text = "Te quedan ${Money.format(75_270)}",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(
                            horizontal = Spacing.md,
                            vertical = Spacing.sm,
                        ),
                    )
                }
            }
        }

        Spacer(Modifier.height(Spacing.lg))
        Text(
            text = "Dos números, no nueve. El resto está en Análisis cuando lo necesites.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun BudgetStep(
    digits: String,
    budgetMinor: Long?,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
) {
    // Days come straight from the calendar rather than from SpendingPace: that model is about
    // spending already recorded, and here there is none — only a month length to divide by.
    val perDay = budgetMinor?.let { it / YearMonth.now().lengthOfMonth() }
    StepScaffold(
        title = "¿Cuánto quieres gastar este mes?",
        subtitle = perDay
            ?.let { "Son ${Money.format(it)} al día." }
            ?: "Es el punto de referencia de tu pantalla de inicio. Puedes cambiarlo cuando quieras.",
    ) {
        MoneyCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = Money.symbolFor("PEN"),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text = digits.ifEmpty { "0" },
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Keypad(onDigit = onDigit, onBackspace = onBackspace)
    }
}

/**
 * A dedicated keypad instead of a text field with a decimal keyboard.
 *
 * The system keyboard covers the figure it is editing, offers a decimal point nobody wants in a
 * monthly budget, and accepts input this screen then has to reject. Three columns of large keys
 * accept exactly what is valid.
 */
@Composable
private fun Keypad(onDigit: (Char) -> Unit, onBackspace: () -> Unit) {
    val rows = listOf("123", "456", "789", "0")
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                row.forEach { digit ->
                    KeypadKey(modifier = Modifier.weight(1f), onClick = { onDigit(digit) }) {
                        Text(
                            text = digit.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                if (row.length == 1) {
                    // The zero row keeps the grid: an empty cell, then backspace under the 9.
                    Spacer(Modifier.weight(1f))
                    KeypadKey(modifier = Modifier.weight(1f), onClick = onBackspace) {
                        Icon(Icons.Rounded.Backspace, contentDescription = "Borrar")
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadKey(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.heightIn(min = 52.dp),
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
private fun MethodsStep(
    methods: List<pe.moneyflow.core.model.PaymentMethod>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
) {
    StepScaffold(
        title = "¿Con qué pagas?",
        subtitle = "Los que tienen app se pueden abrir desde el gasto, sin salir a buscarlos.",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            methods.forEach { method ->
                SelectableRow(
                    icon = Icons.Rounded.CreditCard,
                    title = method.name,
                    subtitle = method.natureLabel(),
                    selected = method.id in selectedIds,
                    onClick = { onToggle(method.id) },
                )
            }
        }
    }
}

@Composable
private fun ShortcutsStep(
    selected: List<ShortcutOption>,
    onToggle: (ShortcutOption) -> Unit,
) {
    StepScaffold(
        title = "¿Qué compras casi todos los días?",
        subtitle = "Cada uno se convierte en un botón de un toque. El monto se corrige al guardar.",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            ShortcutPool.forEach { option ->
                SelectableRow(
                    icon = Icons.Rounded.Bolt,
                    title = option.label,
                    subtitle = "${option.categoryName} · ${option.methodName}",
                    trailingText = Money.format(option.amountMinor),
                    selected = option in selected,
                    onClick = { onToggle(option) },
                )
            }
        }
        Text(
            text = when (selected.size) {
                0 -> "Sin atajos por ahora."
                1 -> "1 atajo en tu inicio."
                else -> "${selected.size} atajos en tu inicio."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The selection control for steps 3 and 4.
 *
 * Both used to be `FilterChip`s, which put a ~32 dp target under the 48 dp floor and left no room
 * for the second line that says what a method or a shortcut actually is.
 */
@Composable
private fun SelectableRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    trailingText: String? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 68.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(IconSize.md))
            Column(modifier = Modifier.weight(1f).padding(horizontal = Spacing.md)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (trailingText != null) {
                Text(
                    text = trailingText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(Spacing.md))
            }
            if (selected) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = "Seleccionado",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun SummaryStep(budgetMinor: Long?, methodCount: Int, shortcutCount: Int) {
    StepScaffold(
        title = "Listo.",
        subtitle = "Así queda tu mes. Todo esto vive en Tu dinero si quieres moverlo.",
    ) {
        MoneyCard(modifier = Modifier.fillMaxWidth()) {
            SummaryLine(
                icon = Icons.Rounded.Savings,
                label = "Presupuesto mensual",
                value = budgetMinor?.let { Money.format(it) } ?: "Sin definir",
            )
            SummaryLine(
                icon = Icons.Rounded.CreditCard,
                label = "Métodos de pago",
                value = "$methodCount activo(s)",
            )
            SummaryLine(
                icon = Icons.Rounded.Bolt,
                label = "Atajos de un toque",
                value = if (shortcutCount > 0) "$shortcutCount" else "Ninguno por ahora",
            )
        }
    }
}

@Composable
private fun SummaryLine(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 62.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(IconSize.md),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f).padding(start = Spacing.md),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Short "what kind of money is this" line, mirroring the one on Métodos de pago. */
private fun pe.moneyflow.core.model.PaymentMethod.natureLabel(): String = when (cardKind) {
    CardKind.DEBIT -> "Tarjeta de débito"
    CardKind.CREDIT -> "Tarjeta de crédito"
    null -> when (type) {
        PaymentMethodType.CASH -> "Efectivo"
        PaymentMethodType.CARD -> "Tarjeta"
        PaymentMethodType.EWALLET -> "Billetera"
        PaymentMethodType.BANK -> "Cuenta bancaria"
    }
}
