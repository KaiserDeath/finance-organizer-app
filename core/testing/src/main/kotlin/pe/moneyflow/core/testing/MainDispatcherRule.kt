package pe.moneyflow.core.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.TestDispatcher
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Swaps `Dispatchers.Main` for a test dispatcher around each test.
 *
 * Required by anything touching `viewModelScope`, which dispatches on Main and would otherwise throw
 * "Module with the Main dispatcher had failed to initialize" on the JVM — there is no Android
 * looper under a unit test.
 *
 * Defaults to [StandardTestDispatcher], so coroutines queue rather than running eagerly: a test has
 * to `advanceUntilIdle()` (or `runTest`'s implicit drain) before asserting, which makes the moment
 * work completes explicit instead of accidental. Pass `UnconfinedTestDispatcher()` where a test
 * genuinely needs eager execution.
 */
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)

    override fun finished(description: Description) = Dispatchers.resetMain()
}
