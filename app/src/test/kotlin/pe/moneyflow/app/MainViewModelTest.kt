package pe.moneyflow.app

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import pe.moneyflow.core.model.UserPreferences
import pe.moneyflow.core.testing.FakeSettingsRepository
import pe.moneyflow.core.testing.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `lock destination wins even when pet is enabled`() {
        val state = MainUiState(
            preferences = UserPreferences(
                onboardingComplete = true,
                pinHash = "hash",
                petEnabled = true,
            ),
            isLoading = false,
            unlocked = false,
        )

        assertEquals(MainRootDestination.LOCK, state.rootDestination)
    }

    @Test
    fun `unlock exposes app only for current ViewModel session`() = runTest {
        val repository = FakeSettingsRepository().also {
            it.setOnboardingComplete(true)
            it.setPetEnabled(true)
        }
        // The fake deliberately ignores PIN writes, so model the persisted locked preferences at
        // the root-state boundary and verify the session flag separately through the ViewModel.
        val locked = MainUiState(
            preferences = repository.current.copy(pinHash = "hash"),
            isLoading = false,
        )
        assertEquals(MainRootDestination.LOCK, locked.rootDestination)

        val viewModel = MainViewModel(repository)
        val collection = backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()
        viewModel.unlock()
        runCurrent()
        assertEquals(true, viewModel.uiState.value.unlocked)

        val recreated = MainViewModel(repository)
        val recreatedCollection = backgroundScope.launch { recreated.uiState.collect {} }
        runCurrent()
        assertEquals(false, recreated.uiState.value.unlocked)
        collection.cancel()
        recreatedCollection.cancel()
    }
}
