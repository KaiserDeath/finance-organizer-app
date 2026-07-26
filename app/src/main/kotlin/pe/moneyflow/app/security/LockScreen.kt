package pe.moneyflow.app.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.moneyflow.core.designsystem.theme.Spacing

/**
 * Full-screen app lock. Verifies a PIN and, when enabled and available, offers biometric unlock
 * via [onBiometric] (which the hosting activity wires to the system prompt).
 */
@Composable
fun LockScreen(
    onUnlocked: () -> Unit,
    onBiometric: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LockViewModel = hiltViewModel(),
) {
    val biometricEnabled by viewModel.biometricEnabled.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    var pin by remember { mutableStateOf("") }

    // Offer biometrics immediately when enabled.
    LaunchedEffect(biometricEnabled) {
        if (biometricEnabled) onBiometric()
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.height(Spacing.lg))
            Text(
                text = "Ingresa tu PIN",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(Spacing.xl))
            OutlinedTextField(
                value = pin,
                onValueChange = {
                    if (it.length <= 8 && it.all(Char::isDigit)) {
                        pin = it
                        viewModel.clearError()
                    }
                },
                label = { Text("PIN") },
                singleLine = true,
                isError = error,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(),
            )
            if (error) {
                Text(
                    text = "PIN incorrecto",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs),
                )
            }
            Spacer(Modifier.height(Spacing.lg))
            Button(
                onClick = { viewModel.verify(pin, onUnlocked) },
                enabled = pin.length >= 4,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text("Desbloquear")
            }
            if (biometricEnabled) {
                Spacer(Modifier.height(Spacing.md))
                OutlinedButton(
                    onClick = onBiometric,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Icon(Icons.Rounded.Fingerprint, contentDescription = null)
                    Spacer(Modifier.size(Spacing.sm))
                    Text("Usar biometría")
                }
            }
        }
    }
}
