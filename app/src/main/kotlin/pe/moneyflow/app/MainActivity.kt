package pe.moneyflow.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import pe.moneyflow.core.designsystem.theme.MoneyFlowTheme
import pe.moneyflow.core.model.ThemeMode

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* result ignored */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        maybeRequestNotificationPermission()
        enableEdgeToEdge()
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val darkTheme = when (state.preferences.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            MoneyFlowTheme(
                darkTheme = darkTheme,
                dynamicColor = state.preferences.useDynamicColor,
            ) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    MoneyFlowApp()
                }
            }
        }
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
