package pe.moneyflow.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import pe.moneyflow.app.security.LockScreen
import pe.moneyflow.app.security.showBiometricPrompt
import pe.moneyflow.core.designsystem.theme.MoneyFlowTheme
import pe.moneyflow.core.model.ThemeMode

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* result ignored */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be installed before super.onCreate so the splash owns the very first frame. Held until
        // preferences resolve, which is what decides between onboarding, the lock screen, and the app
        // — previously that decision window rendered nothing at all.
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { viewModel.uiState.value.isLoading }
        maybeRequestNotificationPermission()
        enableEdgeToEdge()
        val activity = this
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            // Remembers that the user finished onboarding this launch, so the app can open the
            // first-transaction form once the graph is shown.
            var justOnboarded by rememberSaveable { mutableStateOf(false) }
            val darkTheme = when (state.preferences.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            MoneyFlowTheme(darkTheme = darkTheme) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    when {
                        // The system splash is still on screen for this branch (see
                        // setKeepOnScreenCondition above), so there is nothing to draw underneath it.
                        state.isLoading -> Unit
                        state.showOnboarding -> OnboardingScreen(
                            onFinish = {
                                justOnboarded = true
                                viewModel.completeOnboarding()
                            },
                        )

                        state.showLock -> LockScreen(
                            onUnlocked = viewModel::unlock,
                            onBiometric = {
                                activity.showBiometricPrompt(onSuccess = viewModel::unlock)
                            },
                        )

                        else -> MoneyFlowApp(startInAddTransaction = justOnboarded)
                    }
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
