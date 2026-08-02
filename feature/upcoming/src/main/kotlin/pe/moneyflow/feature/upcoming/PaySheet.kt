package pe.moneyflow.feature.upcoming

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.moneyflow.core.common.Money
import pe.moneyflow.core.designsystem.theme.IconSize
import pe.moneyflow.core.designsystem.theme.Spacing
import pe.moneyflow.core.domain.model.UpcomingPayment
import pe.moneyflow.core.model.PaymentMethod
import pe.moneyflow.core.ui.util.toShortLabel

/**
 * The pay flow: amount, due date, and the method — then the user actually pays, instead of the
 * app only recording that they did.
 *
 * Two states by selected method:
 *  - **With app** (Yape, BCP…): the primary action opens the app ("Abrir {app} y registrar"); on
 *    return the caller settles the charge and offers deshacer. **"Ya pagué por fuera"** sits under
 *    it for the case where the bill was already settled elsewhere.
 *  - **Without app** (efectivo, tarjetas, Plin): the primary action records the payment directly
 *    ("Registrar el pago"), with one line explaining why there is no app to open — an explained
 *    button, never a dead one.
 *
 * "Ya pagué por fuera" is deliberately **not** shown in the second case. It used to be, and it fired
 * the same `onSettle` as the primary button sitting directly above it: two controls, one behaviour,
 * no way for the user to tell which was which. It only earns its place when the primary does
 * something else.
 *
 * Either path settles *with the selected method* and records it, which is what makes the suggested
 * method improve with use.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun PaySheet(
    payment: UpcomingPayment,
    methods: List<PaymentMethod>,
    suggestedMethodId: String?,
    onLaunchApp: (PaymentMethod) -> Unit,
    onSettle: (PaymentMethod?) -> Unit,
    onDismiss: () -> Unit,
) {
    val tx = payment.transaction
    var selectedId by remember { mutableStateOf(suggestedMethodId ?: methods.firstOrNull()?.id) }
    val selected = methods.firstOrNull { it.id == selectedId }
    val selectedHasApp = !selected?.deepLinkPackage.isNullOrBlank()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Column {
                Text(
                    text = tx.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = Money.format(tx.amountMinor, tx.currencyCode),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = payment.dueDate?.let { due ->
                        if (payment.isOverdue) "Venció el ${due.toShortLabel()}" else "Vence el ${due.toShortLabel()}"
                    } ?: "Sin fecha de vencimiento",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Says that settling this one won't be the last time, without claiming a cadence:
                // the schedule lives on the template, and the sheet only has the occurrence.
                if (payment.isProjected || tx.recurringId != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        modifier = Modifier.padding(top = Spacing.xs),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EventRepeat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(IconSize.sm),
                        )
                        Text(
                            text = "Pago recurrente",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (methods.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(
                        text = "Método de pago",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        methods.forEach { method ->
                            val hasApp = !method.deepLinkPackage.isNullOrBlank()
                            FilterChip(
                                selected = method.id == selectedId,
                                onClick = { selectedId = method.id },
                                label = { Text(method.name) },
                                leadingIcon = if (hasApp) {
                                    {
                                        Icon(
                                            Icons.Rounded.Smartphone,
                                            contentDescription = "Tiene app",
                                            modifier = Modifier.padding(0.dp),
                                        )
                                    }
                                } else {
                                    null
                                },
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                if (selected != null && selectedHasApp) {
                    Button(
                        onClick = { onLaunchApp(selected) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(IconSize.sm),
                        )
                        Spacer(Modifier.size(Spacing.sm))
                        Text("Abrir ${selected.name} y registrar")
                    }
                    // Only offered when there *is* an app to open. Without one the primary action
                    // already records the payment, so a second button firing the same `onSettle`
                    // would be two controls with one behaviour — the user cannot tell them apart,
                    // and picking the "wrong" one changes nothing.
                    TextButton(
                        onClick = { onSettle(selected) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) { Text("Ya pagué por fuera") }
                } else {
                    Button(
                        onClick = { onSettle(selected) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(IconSize.sm),
                        )
                        Spacer(Modifier.size(Spacing.sm))
                        Text("Registrar el pago")
                    }
                    // The reason there is no app to open, instead of a dead button.
                    Text(
                        text = when {
                            selected == null -> "Este pago no tiene un método asignado."
                            else ->
                                "${selected.name} no tiene app vinculada. Se registra el gasto, " +
                                    "pero el pago lo haces por fuera."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
