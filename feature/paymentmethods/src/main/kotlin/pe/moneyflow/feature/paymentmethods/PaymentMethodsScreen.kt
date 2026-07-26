package pe.moneyflow.feature.paymentmethods

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import pe.moneyflow.core.designsystem.component.MoneyCard
import pe.moneyflow.core.designsystem.icon.iconForKey
import pe.moneyflow.core.designsystem.theme.Spacing
import pe.moneyflow.core.designsystem.util.colorFromHex
import pe.moneyflow.core.model.Account
import pe.moneyflow.core.model.PaymentMethod
import pe.moneyflow.core.model.PaymentMethodType
import pe.moneyflow.core.ui.component.CategoryAvatar
import pe.moneyflow.core.ui.util.launchPaymentApp
import java.util.UUID

private val PaletteHex = listOf(
    "#4F46E5", "#0EA5A5", "#F59E0B", "#EC4899", "#8B5CF6", "#10B981",
    "#EF4444", "#3B82F6", "#F97316", "#14B8A6", "#A855F7", "#84CC16",
)
private val IconChoices = listOf(
    "payments", "card", "wallet", "account_balance", "attach_money", "savings", "phone", "category",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PaymentMethodsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // null = sheet closed; non-null = editing that method; NewMethod sentinel = creating.
    var editing by remember { mutableStateOf<PaymentMethod?>(null) }
    var showSheet by remember { mutableStateOf(false) }

    val onDelete: (PaymentMethod) -> Unit = { method ->
        viewModel.delete(method.id)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Método eliminado",
                actionLabel = "Deshacer",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Métodos de pago") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editing = null
                showSheet = true
            }) {
                Icon(Icons.Rounded.Add, contentDescription = "Nuevo método de pago")
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
            item {
                Text(
                    text = "Toca un método para editarlo, o el ícono ↗ para abrir su app.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            items(items = uiState.methods, key = { it.id }) { method ->
                SwipeablePaymentMethod(
                    method = method,
                    onClick = {
                        editing = method
                        showSheet = true
                    },
                    onLaunch = {
                        launchPaymentApp(
                            context = context,
                            packageName = method.deepLinkPackage,
                            appName = method.name,
                            playStoreId = method.playStoreId,
                        )
                    },
                    onDelete = onDelete,
                )
            }
        }
    }

    if (showSheet) {
        AddEditPaymentMethodSheet(
            existing = editing,
            accounts = uiState.accounts,
            onDismiss = { showSheet = false },
            onConfirm = { method ->
                viewModel.save(method)
                showSheet = false
            },
        )
    }
}

@Composable
private fun SwipeablePaymentMethod(
    method: PaymentMethod,
    onClick: () -> Unit,
    onLaunch: () -> Unit,
    onDelete: (PaymentMethod) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onDelete(method)
                true
            } else {
                false
            }
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = { DeleteBackground() },
    ) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            MoneyCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = Spacing.xs),
            ) {
                PaymentMethodRow(method = method, onClick = onClick, onLaunch = onLaunch)
            }
        }
    }
}

@Composable
private fun DeleteBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.xl),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Icon(
            imageVector = Icons.Rounded.Delete,
            contentDescription = "Eliminar",
            tint = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun PaymentMethodRow(method: PaymentMethod, onClick: () -> Unit, onLaunch: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryAvatar(
            icon = iconForKey(method.iconKey),
            accent = colorFromHex(method.colorHex, MaterialTheme.colorScheme.primary),
        )
        Column(modifier = Modifier.weight(1f).padding(horizontal = Spacing.md)) {
            Text(
                text = method.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = method.type.label() + if (method.isDefault) " · Predeterminado" else "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!method.deepLinkPackage.isNullOrBlank()) {
            IconButton(onClick = onLaunch) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = "Abrir app",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun AddEditPaymentMethodSheet(
    existing: PaymentMethod?,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirm: (PaymentMethod) -> Unit,
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var type by remember { mutableStateOf(existing?.type ?: PaymentMethodType.CASH) }
    var colorHex by remember { mutableStateOf(existing?.colorHex ?: PaletteHex.first()) }
    var iconKey by remember { mutableStateOf(existing?.iconKey ?: IconChoices.first()) }
    var accountId by remember { mutableStateOf(existing?.accountId) }
    var isDefault by remember { mutableStateOf(existing?.isDefault ?: false) }
    var deepLink by remember { mutableStateOf(existing?.deepLinkPackage ?: "") }
    var playStore by remember { mutableStateOf(existing?.playStoreId ?: "") }
    var showAdvanced by remember { mutableStateOf(!existing?.deepLinkPackage.isNullOrBlank()) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
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
                text = if (existing == null) "Nuevo método" else "Editar método",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre") },
                placeholder = { Text("Ej. Yape, BCP, Efectivo") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Tipo", style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val types = listOf(
                    PaymentMethodType.CASH to "Efectivo",
                    PaymentMethodType.CARD to "Tarjeta",
                    PaymentMethodType.EWALLET to "Billetera",
                    PaymentMethodType.BANK to "Banco",
                )
                types.forEachIndexed { index, (value, label) ->
                    SegmentedButton(
                        selected = type == value,
                        onClick = { type = value },
                        shape = SegmentedButtonDefaults.itemShape(index, types.size),
                    ) { Text(label) }
                }
            }

            Text("Color", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                PaletteHex.forEach { hex ->
                    val color = colorFromHex(hex, MaterialTheme.colorScheme.primary)
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (colorHex == hex) 3.dp else 0.dp,
                                color = MaterialTheme.colorScheme.onSurface,
                                shape = CircleShape,
                            )
                            .clickable { colorHex = hex },
                    )
                }
            }

            Text("Ícono", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                IconChoices.forEach { key ->
                    val selected = iconKey == key
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                            )
                            .clickable { iconKey = key },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = iconForKey(key),
                            contentDescription = null,
                            tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            if (accounts.isNotEmpty()) {
                Text("Cuenta (opcional)", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    FilterChip(
                        selected = accountId == null,
                        onClick = { accountId = null },
                        label = { Text("Ninguna") },
                    )
                    accounts.forEach { account ->
                        FilterChip(
                            selected = accountId == account.id,
                            onClick = { accountId = account.id },
                            label = { Text(account.name) },
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Predeterminado",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Se selecciona solo al registrar un movimiento",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = isDefault, onCheckedChange = { isDefault = it })
            }

            // Advanced: the app link (technical, hidden by default).
            val chevron by animateFloatAsState(if (showAdvanced) 180f else 0f, label = "adv-chevron")
            TextButton(
                onClick = { showAdvanced = !showAdvanced },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Opciones avanzadas (enlace a la app)")
                Spacer(Modifier.width(Spacing.xs))
                Icon(Icons.Rounded.ExpandMore, contentDescription = null, modifier = Modifier.rotate(chevron))
            }
            AnimatedVisibility(visible = showAdvanced) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    OutlinedTextField(
                        value = deepLink,
                        onValueChange = { deepLink = it },
                        label = { Text("Paquete de la app") },
                        placeholder = { Text("com.bcp.innovacxion.yapeapp") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = playStore,
                        onValueChange = { playStore = it },
                        label = { Text("ID en Play Store (opcional)") },
                        placeholder = { Text("Por defecto usa el paquete de arriba") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Button(
                onClick = {
                    val base = existing ?: PaymentMethod(
                        id = UUID.randomUUID().toString(),
                        name = "",
                        iconKey = iconKey,
                        colorHex = colorHex,
                    )
                    onConfirm(
                        base.copy(
                            name = name.trim(),
                            type = type,
                            iconKey = iconKey,
                            colorHex = colorHex,
                            accountId = accountId,
                            isDefault = isDefault,
                            deepLinkPackage = deepLink.trim().ifBlank { null },
                            playStoreId = playStore.trim().ifBlank { null },
                        ),
                    )
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text(if (existing == null) "Agregar método" else "Guardar cambios") }
        }
    }
}

private fun PaymentMethodType.label(): String = when (this) {
    PaymentMethodType.CASH -> "Efectivo"
    PaymentMethodType.CARD -> "Tarjeta"
    PaymentMethodType.EWALLET -> "Billetera digital"
    PaymentMethodType.BANK -> "Banco"
}
