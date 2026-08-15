package pe.moneyflow.feature.pet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.moneyflow.core.designsystem.component.MoneyCard

@Composable
fun PetSettingsCard(
    onOpenSettings: () -> Unit,
    viewModel: PetViewModel = hiltViewModel(),
) {
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    MoneyCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenSettings)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Compañero Castor", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (preferences.enabled) "Activado · Configurar comportamiento"
                    else "Opcional · Toca para configurar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = preferences.enabled, onCheckedChange = viewModel::setEnabled)
            Icon(Icons.Rounded.ChevronRight, contentDescription = "Configurar Castor")
        }
    }
}
