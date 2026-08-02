package pe.moneyflow.core.ui.paymentmethod

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.moneyflow.core.designsystem.icon.iconForKey
import pe.moneyflow.core.designsystem.theme.IconSize
import pe.moneyflow.core.designsystem.theme.Spacing
import pe.moneyflow.core.model.CardKind
import pe.moneyflow.core.model.PaymentMethod
import pe.moneyflow.core.model.PaymentMethodType

/** The money-nature groups a payment method falls into, mirroring the creation sheet's top choice. */
enum class PaymentNature(val label: String) {
    EFECTIVO("Efectivo"),
    DEBITO("Débito"),
    CREDITO("Crédito"),
}

/** The [CardKind] a nature records on a movement; efectivo records none. */
fun PaymentNature.toCardKind(): CardKind? = when (this) {
    PaymentNature.DEBITO -> CardKind.DEBIT
    PaymentNature.CREDITO -> CardKind.CREDIT
    PaymentNature.EFECTIVO -> null
}

/** The nature that a recorded [CardKind] corresponds to (null = efectivo / not recorded). */
fun CardKind?.toNature(): PaymentNature? = when (this) {
    CardKind.DEBIT -> PaymentNature.DEBITO
    CardKind.CREDIT -> PaymentNature.CREDITO
    null -> null
}

/** Default classification for a [type]/[cardKind] pair, used by the method editor's top choice. */
fun paymentNatureOf(type: PaymentMethodType, cardKind: CardKind?): PaymentNature = when {
    cardKind == CardKind.CREDIT -> PaymentNature.CREDITO
    cardKind == CardKind.DEBIT -> PaymentNature.DEBITO
    type == PaymentMethodType.BANK -> PaymentNature.DEBITO
    else -> PaymentNature.EFECTIVO
}

/**
 * The groups a method can be *picked* from. A **bank** offers both a debit and a credit line, so it
 * shows under both Débito and Crédito; a fixed card shows under its own kind; cash and wallets are
 * efectivo. The specific debit/credit used is decided when picking, not baked into the method.
 */
fun PaymentMethod.natures(): List<PaymentNature> = when {
    type == PaymentMethodType.BANK -> listOf(PaymentNature.DEBITO, PaymentNature.CREDITO)
    cardKind == CardKind.CREDIT -> listOf(PaymentNature.CREDITO)
    cardKind == CardKind.DEBIT -> listOf(PaymentNature.DEBITO)
    else -> listOf(PaymentNature.EFECTIVO)
}

/**
 * Two-step payment picker: choose Efectivo / Débito / Crédito, then a method within that group.
 * Banks appear in both card groups; switching groups with a bank selected just changes whether the
 * movement is recorded as debit or credit. [onSelect] reports the method id (null = "sin método"
 * when [allowNone]) together with the chosen [PaymentNature] so callers can persist the debit/credit
 * characteristic. [selectedNature] seeds which group opens (from a movement's recorded kind).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PaymentMethodSelector(
    methods: List<PaymentMethod>,
    selectedId: String?,
    selectedNature: PaymentNature?,
    onSelect: (id: String?, nature: PaymentNature) -> Unit,
    modifier: Modifier = Modifier,
    allowNone: Boolean = false,
) {
    val selectedMethod = methods.firstOrNull { it.id == selectedId }
    var nature by remember {
        mutableStateOf(selectedNature ?: selectedMethod?.natures()?.firstOrNull() ?: PaymentNature.EFECTIVO)
    }
    // Follow the recorded characteristic/selection when it's set from outside (default or edit load).
    LaunchedEffect(selectedNature, selectedMethod?.id) {
        (selectedNature ?: selectedMethod?.natures()?.firstOrNull())?.let { nature = it }
    }

    val inNature = methods.filter { nature in it.natures() }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            PaymentNature.entries.forEachIndexed { index, value ->
                SegmentedButton(
                    selected = nature == value,
                    onClick = {
                        nature = value
                        // A dual method (bank) stays selected across groups — only its debit/credit
                        // characteristic changes; otherwise jump to the first method of the group.
                        if (selectedMethod != null && value in selectedMethod.natures()) {
                            onSelect(selectedId, value)
                        } else {
                            onSelect(methods.firstOrNull { value in it.natures() }?.id, value)
                        }
                    },
                    shape = SegmentedButtonDefaults.itemShape(index, PaymentNature.entries.size),
                ) { Text(value.label) }
            }
        }

        if (inNature.isEmpty()) {
            Text(
                text = when (nature) {
                    PaymentNature.EFECTIVO -> "No tienes efectivo ni billeteras todavía."
                    PaymentNature.DEBITO -> "No tienes débito ni bancos. Agrégalos en Métodos de pago."
                    PaymentNature.CREDITO -> "No tienes crédito ni bancos. Agrégalos en Métodos de pago."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            if (allowNone) {
                FilterChip(
                    selected = selectedId == null,
                    onClick = { onSelect(null, nature) },
                    label = { Text("Sin método") },
                )
            }
            inNature.forEach { method ->
                FilterChip(
                    selected = selectedId == method.id,
                    onClick = { onSelect(method.id, nature) },
                    label = { Text(method.name) },
                    leadingIcon = {
                        Icon(
                            imageVector = iconForKey(method.iconKey),
                            contentDescription = null,
                            modifier = Modifier.size(IconSize.chip),
                        )
                    },
                )
            }
        }
    }
}
