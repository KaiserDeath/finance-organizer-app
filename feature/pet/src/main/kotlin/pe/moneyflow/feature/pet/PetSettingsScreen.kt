package pe.moneyflow.feature.pet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.moneyflow.core.designsystem.component.MoneyCard
import pe.moneyflow.core.model.PetSpeechFrequency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetSettingsScreen(onBack: () -> Unit, viewModel: PetViewModel = hiltViewModel()) {
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compañero Castor") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
        ) {
            item {
                MoneyCard(modifier = Modifier.fillMaxWidth()) {
                    ToggleRow(
                        "Activar compañero",
                        preferences.enabled,
                        viewModel::setEnabled,
                        testTag = PET_ENABLED_SWITCH_TAG,
                    )
                    ToggleRow(
                        "Movimiento reducido",
                        preferences.reducedMotion,
                        viewModel::setReducedMotion,
                        preferences.enabled,
                    )
                }
            }
            item {
                MoneyCard(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    Text("Frecuencia de mensajes", style = MaterialTheme.typography.titleMedium)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                        val options = listOf(
                            PetSpeechFrequency.NORMAL to "Normal",
                            PetSpeechFrequency.LOW to "Baja",
                            PetSpeechFrequency.SILENT to "Silencio",
                        )
                        options.forEachIndexed { index, (frequency, label) ->
                            SegmentedButton(
                                selected = preferences.speechFrequency == frequency,
                                onClick = { viewModel.setSpeechFrequency(frequency) },
                                enabled = preferences.enabled,
                                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                            ) { Text(label) }
                        }
                    }
                    Text(
                        "El modo bajo limita reacciones contextuales a una cada cinco minutos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            item {
                MoneyCard(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    Text("Posición y tutorial", style = MaterialTheme.typography.titleMedium)
                    Button(
                        onClick = viewModel::resetPosition,
                        enabled = preferences.enabled,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    ) { Text("Restablecer posición") }
                    Button(
                        onClick = viewModel::replayGestureOnboarding,
                        enabled = preferences.enabled && preferences.speechFrequency != PetSpeechFrequency.SILENT,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    ) { Text("Practicar toque y arrastre") }
                    Text(
                        "Sonido estará disponible cuando el rig final incluya audio.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    testTag: String? = null,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        Switch(
            checked,
            onCheckedChange,
            enabled = enabled,
            modifier = if (testTag == null) Modifier else Modifier.testTag(testTag),
        )
    }
}

internal const val PET_ENABLED_SWITCH_TAG = "pet_enabled_switch"
