package pe.moneyflow.feature.upcoming

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.moneyflow.core.common.Money
import pe.moneyflow.core.designsystem.theme.Spacing
import pe.moneyflow.core.domain.model.UpcomingPayment
import pe.moneyflow.core.model.PaymentMethod
import pe.moneyflow.core.ui.util.toShortLabel

/**
 * The pay flow: amount, due date, and the method — then the user actually pays, instead of the
 * app only recording that they did.
 *
 * Three states by selected method:
 *  - **With app** (Yape, BCP…): the primary action opens the app; on return the caller settles
 *    the charge and offers deshacer.
 *  - **Without app** (efectivo, tarjetas, Plin): the primary action settles directly, with one
 *    line explaining why there is no app to open — an explained button, never a dead one.
 *  - **"Ya pagué por fuera"**: always available; settles with the selected method *and records
 *    it*, which is what makes the suggestion improve with use.
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
                    ) { Text("Pagar con ${selected.name}") }
                } else {
                    Button(
                        onClick = { onSettle(selected) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) { Text("Marcar pagado") }
                    // The reason there is no app to open, instead of a dead button.
                    Text(
                        text = when {
                            selected == null -> "Este pago no tiene un método asignado."
                            else -> "${selected.name} no abre una app propia; se paga directo o desde tu app de banco."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // Always available: the payment happened outside the app, record it — with the
                // method, so the suggestion learns.
                TextButton(
                    onClick = { onSettle(selected) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) { Text("Ya pagué por fuera") }
            }
        }
    }
}
