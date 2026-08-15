package pe.moneyflow.feature.pet

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toPixelMap
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlinx.coroutines.runBlocking
import pe.moneyflow.core.testing.FakeSettingsRepository
import pe.moneyflow.core.ui.safearea.LocalSafeAreaRegistry
import pe.moneyflow.core.ui.safearea.SafeAreaRegistry
import pe.moneyflow.core.designsystem.theme.MoneyFlowTheme
import org.junit.Assert.assertNotEquals

class PetOverlayHostTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun draggingCharacter_reportsMovementAndCompletes() {
        val dragDeltas = mutableListOf<Offset>()
        var dragStarted = false
        var dragEnded = false

        composeRule.setContent {
            MaterialTheme {
                BeaverPrototype(
                    rendererInput = PetVisualState.IDLE.toRendererInput(
                        reducedMotion = true,
                        playbackEnabled = true,
                    ),
                    onTap = {},
                    onDragStart = { dragStarted = true },
                    onDrag = dragDeltas::add,
                    onDragEnd = { dragEnded = true },
                )
            }
        }

        composeRule.onNodeWithTag(PET_CHARACTER_TAG).performTouchInput {
            swipe(center, center + Offset(160f, -220f), durationMillis = 700)
        }

        composeRule.runOnIdle {
            assertTrue("Drag start was not reported", dragStarted)
            assertTrue("No drag movement was reported", dragDeltas.isNotEmpty())
            assertTrue("Drag end was not reported", dragEnded)
        }
    }

    @Test
    fun speechDismissButton_remainsVisibleAtTwoHundredPercentFontScale() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 2f),
            ) {
                MaterialTheme {
                    PetSpeechBubble(
                        message = "Hola, soy Castor. Puedes tocarme o moverme.",
                        onSizeChanged = {},
                        onDismiss = {},
                    )
                }
            }
        }

        composeRule.onNodeWithTag(PET_SPEECH_DISMISS_TAG).assertIsDisplayed()
    }

    @Test
    fun speechBubble_exposesOrderedActionsAndDismisses() {
        var dismissed = false
        composeRule.setContent {
            MaterialTheme {
                PetSpeechBubble(
                    message = "Mensaje accesible",
                    onSizeChanged = {},
                    onDismiss = { dismissed = true },
                )
            }
        }

        composeRule.onNodeWithTag(PET_SPEECH_BUBBLE_TAG)
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.TraversalIndex, 0f))
        composeRule.onNodeWithTag(PET_SPEECH_DISMISS_TAG)
            .assertIsDisplayed()
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.TraversalIndex, 0.5f))
            .performClick()

        composeRule.runOnIdle { assertTrue("Dismiss action was not delivered", dismissed) }
    }

    @Test
    fun overlayState_survivesSiblingContentNavigation() = runBlocking<Unit> {
        val repository = FakeSettingsRepository().also {
            it.setPetEnabled(true)
            it.setPetIntroductionComplete(true)
        }
        val viewModel = PetViewModel(repository)
        val destination = mutableStateOf("Inicio")

        composeRule.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalSafeAreaRegistry provides SafeAreaRegistry()) {
                    Text(destination.value)
                    PetOverlayHost(eventBus = PetEventBus(), viewModel = viewModel)
                }
            }
        }

        composeRule.onNodeWithTag(PET_CHARACTER_TAG).assertIsDisplayed().performClick()
        composeRule.onNodeWithTag(PET_SPEECH_BUBBLE_TAG).assertIsDisplayed()
        composeRule.runOnIdle { destination.value = "Movimientos" }

        composeRule.onNodeWithText("Movimientos").assertIsDisplayed()
        composeRule.onNodeWithTag(PET_CHARACTER_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(PET_SPEECH_BUBBLE_TAG).assertIsDisplayed()
    }

    @Test
    fun enabledSwitch_persistsAcrossViewModelRecreation() = runBlocking<Unit> {
        val repository = FakeSettingsRepository()
        val viewModel = mutableStateOf(PetViewModel(repository))

        composeRule.setContent {
            MaterialTheme { PetSettingsScreen(onBack = {}, viewModel = viewModel.value) }
        }
        composeRule.onNodeWithTag(PET_ENABLED_SWITCH_TAG).assertIsOff().performClick()
        composeRule.waitUntil { repository.current.petEnabled }

        composeRule.runOnIdle { viewModel.value = PetViewModel(repository) }
        composeRule.onNodeWithTag(PET_ENABLED_SWITCH_TAG).assertIsOn()
    }

    @Test
    fun speechBubble_rendersWithDarkThemeSurface() {
        composeRule.setContent {
            MoneyFlowTheme(darkTheme = true) {
                PetSpeechBubble(
                    message = "Mensaje en tema oscuro",
                    onSizeChanged = {},
                    onDismiss = {},
                )
            }
        }

        val pixels = composeRule.onNodeWithTag(PET_SPEECH_BUBBLE_TAG)
            .assertIsDisplayed()
            .captureToImage()
            .toPixelMap()
        var luminanceTotal = 0f
        var opaquePixels = 0
        for (y in 0 until pixels.height) {
            for (x in 0 until pixels.width) {
                val color = pixels[x, y]
                if (color.alpha > 0.9f) {
                    luminanceTotal += color.red * 0.2126f + color.green * 0.7152f + color.blue * 0.0722f
                    opaquePixels++
                }
            }
        }
        assertTrue("Dark bubble rendered no opaque pixels", opaquePixels > 0)
        assertTrue("Dark bubble surface is unexpectedly bright", luminanceTotal / opaquePixels < 0.5f)
    }

    @Test
    fun normalizedPlacement_restoresAcrossWindowSizeChange() = runBlocking<Unit> {
        val repository = FakeSettingsRepository().also {
            it.setPetEnabled(true)
            it.setPetIntroductionComplete(true)
            it.setPetPlacement(0.25f, 0.25f)
        }
        val viewModel = PetViewModel(repository)
        val landscape = mutableStateOf(false)

        composeRule.setContent {
            MoneyFlowTheme {
                CompositionLocalProvider(LocalSafeAreaRegistry provides SafeAreaRegistry()) {
                    Box(
                        Modifier
                            .size(
                                width = if (landscape.value) 640.dp else 360.dp,
                                height = if (landscape.value) 360.dp else 640.dp,
                            )
                            .testTag("pet_test_window"),
                    ) {
                        PetOverlayHost(eventBus = PetEventBus(), viewModel = viewModel)
                    }
                }
            }
        }

        val portrait = composeRule.onNodeWithTag(PET_CHARACTER_TAG).getUnclippedBoundsInRoot()
        composeRule.runOnIdle { landscape.value = true }
        composeRule.waitForIdle()
        val landscapeBounds = composeRule.onNodeWithTag(PET_CHARACTER_TAG).getUnclippedBoundsInRoot()

        assertNotEquals(portrait.left, landscapeBounds.left)
        assertNotEquals(portrait.top, landscapeBounds.top)
        assertTrue((landscapeBounds.left + landscapeBounds.right) / 2 < 320.dp)
        assertTrue((landscapeBounds.top + landscapeBounds.bottom) / 2 < 180.dp)
    }
}
