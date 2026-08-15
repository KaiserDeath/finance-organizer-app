package pe.moneyflow.feature.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import pe.moneyflow.core.ui.safearea.safeArea
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.moneyflow.core.common.Money
import pe.moneyflow.core.designsystem.component.animatedItem
import pe.moneyflow.core.designsystem.component.EmptyState
import pe.moneyflow.core.designsystem.component.MoneyCard
import pe.moneyflow.core.designsystem.component.SkeletonBlocks
import pe.moneyflow.core.designsystem.icon.iconForKey
import pe.moneyflow.core.designsystem.theme.moneyColors
import pe.moneyflow.core.designsystem.theme.IconSize
import pe.moneyflow.core.designsystem.theme.Spacing
import pe.moneyflow.core.designsystem.util.colorFromHex
import pe.moneyflow.core.domain.model.AccountBalance
import pe.moneyflow.core.domain.model.NetWorth
import pe.moneyflow.core.model.Account
import pe.moneyflow.core.model.AccountType
import pe.moneyflow.core.model.PaymentMethodType
import pe.moneyflow.core.ui.component.CategoryAvatar
import pe.moneyflow.core.ui.preset.FinancePresets
import pe.moneyflow.core.ui.util.money

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Cuentas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Rounded.Add, contentDescription = "Nueva cuenta")
            }
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
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            uiState.netWorth?.let { netWorth ->
                item { NetWorthCard(netWorth) }
            }

            if (uiState.accounts.size >= 2) {
                item {
                    OutlinedButton(
                        onClick = { showTransferDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.SwapHoriz, contentDescription = null)
                        Spacer(Modifier.padding(horizontal = Spacing.xs))
                        Text("Transferir entre cuentas")
                    }
                }
            }

            if (uiState.isLoading) {
                item { SkeletonBlocks(heroHeight = 150.dp, count = 4, blockHeight = 88.dp) }
            } else if (uiState.isEmpty) {
                item {
                    MoneyCard(modifier = Modifier.fillMaxWidth()) {
                        EmptyState(
                            icon = Icons.Rounded.AccountBalanceWallet,
                            title = "Sin cuentas",
                            subtitle = "Agrega tus cuentas (efectivo, banco, tarjeta) para ver saldos y patrimonio.",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            } else {
                items(uiState.netWorth?.balances.orEmpty(), key = { it.account.id }) { balance ->
                    AccountCard(
                        balance = balance,
                        onArchive = { viewModel.archive(balance.account) },
                        modifier = animatedItem(),
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddAccountSheet(
            onDismiss = { showAddDialog = false },
            onConfirm = { form ->
                viewModel.add(
                    name = form.name,
                    type = form.type,
                    currencyCode = form.currency,
                    openingBalanceMinor = form.openingMinor,
                    colorHex = form.colorHex,
                    iconKey = form.iconKey,
                    alsoCreatePaymentMethod = form.alsoCreatePaymentMethod,
                    paymentMethodType = form.paymentMethodType,
                    deepLinkPackage = form.deepLinkPackage,
                )
                showAddDialog = false
            },
        )
    }

    if (showTransferDialog && uiState.accounts.size >= 2) {
        TransferSheet(
            accounts = uiState.accounts,
            onDismiss = { showTransferDialog = false },
            onConfirm = { fromId, toId, amount, currency ->
                viewModel.transfer(fromId, toId, amount, currency)
                showTransferDialog = false
            },
        )
    }
}

@Composable
private fun NetWorthCard(netWorth: NetWorth) {
    MoneyCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Patrimonio neto",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = money(netWorth.totalMinor, netWorth.currencyCode),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(Spacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xl),
        ) {
            AmountPill("Activos", netWorth.assetsMinor, netWorth.currencyCode, MaterialTheme.moneyColors.positive)
            AmountPill("Pasivos", netWorth.liabilitiesMinor, netWorth.currencyCode, MaterialTheme.moneyColors.negative)
        }
        if (netWorth.hasUnconvertible) {
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = "Algunas cuentas usan una moneda sin tipo de cambio y no se incluyen en el total.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun AmountPill(label: String, amountMinor: Long, currency: String, accent: androidx.compose.ui.graphics.Color) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = money(amountMinor, currency),
            style = MaterialTheme.typography.titleMedium,
            color = accent,
        )
    }
}

@Composable
private fun AccountCard(
    balance: AccountBalance,
    onArchive: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val account = balance.account
    val accent = colorFromHex(account.colorHex, MaterialTheme.colorScheme.primary)
    val balanceColor =
        if (balance.currentBalanceMinor < 0) MaterialTheme.moneyColors.negative
        else MaterialTheme.colorScheme.onSurface

    MoneyCard(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryAvatar(icon = iconForKey(account.iconKey), accent = accent)
            Column(modifier = Modifier.weight(1f).padding(horizontal = Spacing.md)) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${accountTypeLabel(account.type)} · ${account.currencyCode}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = money(balance.currentBalanceMinor, account.currencyCode),
                    style = MaterialTheme.typography.titleMedium,
                    color = balanceColor,
                )
                IconButton(onClick = onArchive) {
                    Icon(
                        Icons.Rounded.Archive,
                        contentDescription = "Archivar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Collected result of the new-account sheet. */
private data class NewAccountForm(
    val name: String,
    val type: AccountType,
    val currency: String,
    val openingMinor: Long,
    val colorHex: String?,
    val iconKey: String?,
    val alsoCreatePaymentMethod: Boolean,
    val paymentMethodType: PaymentMethodType?,
    val deepLinkPackage: String?,
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun AddAccountSheet(
    onDismiss: () -> Unit,
    onConfirm: (NewAccountForm) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(AccountType.CASH) }
    var currency by remember { mutableStateOf("PEN") }
    var openingText by remember { mutableStateOf("") }
    // Set when a preset chip is chosen, so the created account keeps that brand's look and can be
    // paired with a matching payment method. Cleared when the user picks a type manually.
    var presetColorHex by remember { mutableStateOf<String?>(null) }
    var presetIconKey by remember { mutableStateOf<String?>(null) }
    var presetPmType by remember { mutableStateOf<PaymentMethodType?>(null) }
    var presetDeepLink by remember { mutableStateOf<String?>(null) }
    var alsoCreatePaymentMethod by remember { mutableStateOf(true) }

    val openingMinor = Money.parseToMinor(openingText) ?: 0L
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        modifier = Modifier.safeArea("sheet_account_editor"),
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Text(
                text = "Nueva cuenta",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )

            // Quick presets — tap a bank/wallet to fill everything, or type your own below.
            Text("Elige uno o personaliza abajo", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                FinancePresets.all.forEach { preset ->
                    FilterChip(
                        selected = name == preset.name && type == preset.accountType,
                        onClick = {
                            name = preset.name
                            type = preset.accountType
                            presetColorHex = preset.colorHex
                            presetIconKey = preset.iconKey
                            presetPmType = preset.paymentMethodType
                            presetDeepLink = preset.deepLinkPackage
                        },
                        label = { Text(preset.name) },
                        leadingIcon = {
                            Icon(
                                imageVector = iconForKey(preset.iconKey),
                                contentDescription = null,
                                modifier = Modifier.size(IconSize.chip),
                            )
                        },
                    )
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Tipo", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                AccountPresets.ordered.forEach { option ->
                    FilterChip(
                        selected = type == option,
                        onClick = {
                            type = option
                            // Manual type choice drops the preset's brand look/pairing.
                            presetColorHex = null
                            presetIconKey = null
                            presetPmType = null
                            presetDeepLink = null
                        },
                        label = { Text(AccountPresets.of(option).label) },
                    )
                }
            }

            Text("Moneda", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                supportedCurrencies.forEach { code ->
                    FilterChip(
                        selected = currency == code,
                        onClick = { currency = code },
                        label = { Text(code) },
                    )
                }
            }

            OutlinedTextField(
                value = openingText,
                onValueChange = { openingText = it },
                label = { Text("Saldo inicial") },
                prefix = { Text("${Money.symbolFor(currency)} ") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            // Link the two: also make it selectable as a payment method when logging an expense.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "También crear método de pago",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Para poder elegirla al registrar un movimiento",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = alsoCreatePaymentMethod,
                    onCheckedChange = { alsoCreatePaymentMethod = it },
                )
            }

            Button(
                onClick = {
                    onConfirm(
                        NewAccountForm(
                            name = name,
                            type = type,
                            currency = currency,
                            openingMinor = openingMinor,
                            colorHex = presetColorHex,
                            iconKey = presetIconKey,
                            alsoCreatePaymentMethod = alsoCreatePaymentMethod,
                            paymentMethodType = presetPmType,
                            deepLinkPackage = presetDeepLink,
                        ),
                    )
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Crear cuenta") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun TransferSheet(
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirm: (fromId: String, toId: String, amountMinor: Long, currency: String) -> Unit,
) {
    var fromId by remember { mutableStateOf(accounts.first().id) }
    var toId by remember { mutableStateOf(accounts.first { it.id != accounts.first().id }.id) }
    var amountText by remember { mutableStateOf("") }

    val fromAccount = accounts.firstOrNull { it.id == fromId }
    val currency = fromAccount?.currencyCode ?: "PEN"
    val amountMinor = Money.parseToMinor(amountText) ?: 0L
    val valid = fromId != toId && amountMinor > 0
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        modifier = Modifier.safeArea("sheet_account_transfer"),
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Text(
                text = "Transferir",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text("Desde", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                accounts.forEach { account ->
                    FilterChip(
                        selected = fromId == account.id,
                        onClick = {
                            fromId = account.id
                            if (toId == account.id) {
                                toId = accounts.first { it.id != account.id }.id
                            }
                        },
                        label = { Text(account.name) },
                    )
                }
            }

            Text("Hacia", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                accounts.filter { it.id != fromId }.forEach { account ->
                    FilterChip(
                        selected = toId == account.id,
                        onClick = { toId = account.id },
                        label = { Text(account.name) },
                    )
                }
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Monto") },
                prefix = { Text("${Money.symbolFor(currency)} ") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = { onConfirm(fromId, toId, amountMinor, currency) },
                enabled = valid,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Transferir") }
        }
    }
}

private fun accountTypeLabel(type: AccountType): String = AccountPresets.of(type).label
