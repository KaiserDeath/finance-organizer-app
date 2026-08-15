package pe.moneyflow.feature.pet

import android.animation.ValueAnimator
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import pe.moneyflow.feature.pet.R
import pe.moneyflow.core.ui.safearea.LocalSafeAreaRegistry

/**
 * Rendered once by the app shell, after navigation, so the same pet instance survives destination
 * changes. The transparent host never consumes input outside the pet and its bubble.
 */
@Composable
fun PetOverlayHost(
    modifier: Modifier = Modifier,
    eventBus: PetEventBus,
    viewModel: PetViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val exclusions = LocalSafeAreaRegistry.current?.exclusions.orEmpty()
    val systemMotionEnabled = remember {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ValueAnimator.areAnimatorsEnabled()
    }
    val reducedMotion = preferences.reducedMotion || !systemMotionEnabled
    var playbackEnabled by remember {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }
    if (!preferences.enabled) return

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    playbackEnabled = true
                    viewModel.onAppForegrounded()
                }
                Lifecycle.Event.ON_STOP -> {
                    playbackEnabled = false
                    viewModel.onAppBackgrounded()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            playbackEnabled = true
            viewModel.onAppForegrounded()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onAppBackgrounded()
        }
    }

    var hostSize by remember { mutableStateOf(Size.Zero) }
    var petOffset by remember { mutableStateOf(Offset.Zero) }
    var bubbleHeightPx by remember { mutableStateOf(0f) }
    var bubbleWidthPx by remember { mutableStateOf(0f) }
    var lastRestoredHostSize by remember { mutableStateOf(Size.Zero) }
    var lastRestoredNormalizedPosition by remember { mutableStateOf<Offset?>(null) }
    val density = LocalDensity.current
    val petPx = with(density) { 104.dp.toPx() }
    val sidePx = with(density) { 12.dp.toPx() }
    val statusBarPx = WindowInsets.statusBars.getTop(density).toFloat()
    val navigationBarPx = WindowInsets.navigationBars.getBottom(density).toFloat()
    val imeBottomPx = WindowInsets.ime.getBottom(density).toFloat()
    val gestureLeftPx = WindowInsets.systemGestures
        .getLeft(density, androidx.compose.ui.unit.LayoutDirection.Ltr).toFloat()
    val gestureRightPx = WindowInsets.systemGestures
        .getRight(density, androidx.compose.ui.unit.LayoutDirection.Ltr).toFloat()

    fun placementBounds() = PetPlacementBounds(
        screenSize = hostSize,
        petSize = petPx,
        leftInset = gestureLeftPx,
        topInset = statusBarPx,
        rightInset = gestureRightPx,
        bottomInset = maxOf(navigationBarPx, imeBottomPx),
        margin = sidePx,
    )

    fun criticalExclusions(): List<Rect> = exclusions

    fun clampDrag(position: Offset): Offset {
        val clamped = clampToSystemBounds(position, placementBounds())
        // Speech grows above the pet. Keep it visible without turning that temporary bubble size
        // into a permanent movement boundary.
        return clamped.copy(
            y = clamped.y.coerceAtLeast(
                (statusBarPx + bubbleHeightPx + sidePx).coerceAtMost(
                    hostSize.height - navigationBarPx - petPx - sidePx,
                ),
            ),
        )
    }

    LaunchedEffect(hostSize, exclusions, preferences.normalizedX, preferences.normalizedY) {
        if (hostSize != Size.Zero) {
            val normalizedPosition = Offset(preferences.normalizedX, preferences.normalizedY)
            val windowChanged = hostSize != lastRestoredHostSize
            val persistedPositionChanged = normalizedPosition != lastRestoredNormalizedPosition
            petOffset = if (windowChanged || persistedPositionChanged) {
                lastRestoredHostSize = hostSize
                lastRestoredNormalizedPosition = normalizedPosition
                settlePet(
                    restoredPetPosition(
                        normalizedX = preferences.normalizedX,
                        normalizedY = preferences.normalizedY,
                        bounds = placementBounds(),
                    ),
                    placementBounds(),
                    criticalExclusions(),
                )
            } else {
                settlePet(petOffset, placementBounds(), criticalExclusions())
            }
        }
    }
    LaunchedEffect(eventBus) {
        eventBus.events.collect(viewModel::onProductEvent)
    }
    LaunchedEffect(state.visualState) {
        if (state.visualState == PetVisualState.IDLE) {
            delay(3_500)
            viewModel.onEvent(PetEvent.BlinkDue)
        }
    }
    LaunchedEffect(state.message) {
        if (state.message == null) {
            bubbleHeightPx = 0f
            bubbleWidthPx = 0f
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { hostSize = Size(it.width.toFloat(), it.height.toFloat()) },
    ) {
        val placeBubbleAtEnd = shouldPlaceBubbleAtEnd(
            petX = petOffset.x,
            bubbleWidth = bubbleWidthPx,
            hostWidth = hostSize.width,
            margin = sidePx,
        )
        Column(
            // [petOffset] always means the pet's top-left. Bubble growth is subtracted so large
            // accessibility fonts can never push the character into navigation or bottom actions.
            horizontalAlignment = if (placeBubbleAtEnd) Alignment.End else Alignment.Start,
            modifier = Modifier
                .offset {
                    IntOffset(
                        // When space runs out on the right, the bubble grows left while the
                        // character remains at [petOffset].
                        (petOffset.x - if (placeBubbleAtEnd) {
                            (bubbleWidthPx - petPx).coerceAtLeast(0f)
                        } else 0f).roundToInt(),
                        (petOffset.y - bubbleHeightPx).roundToInt(),
                    )
                }
                .semantics { isTraversalGroup = true },
        ) {
            state.message?.let { message ->
                PetSpeechBubble(
                    message = message,
                    onSizeChanged = {
                        bubbleWidthPx = it.width.toFloat()
                        bubbleHeightPx = it.height.toFloat()
                    },
                    onDismiss = viewModel::onSpeechDismissed,
                )
            }
            BeaverPrototype(
                rendererInput = state.visualState.toRendererInput(
                    reducedMotion = reducedMotion,
                    playbackEnabled = playbackEnabled,
                ),
                onTap = { viewModel.onEvent(PetEvent.Tapped) },
                onDragStart = { viewModel.onEvent(PetEvent.DragStarted) },
                onDrag = { delta -> petOffset = clampDrag(petOffset + delta) },
                onDragEnd = {
                    petOffset = settlePet(petOffset, placementBounds(), criticalExclusions())
                    viewModel.savePlacement(
                        normalizedX = normalizedPetX(petOffset, placementBounds()),
                        normalizedY = normalizedPetY(petOffset, placementBounds()),
                    )
                    viewModel.onEvent(PetEvent.DragEnded)
                },
            )
        }
    }
}

@Composable
internal fun PetSpeechBubble(
    message: String,
    onSizeChanged: (androidx.compose.ui.unit.IntSize) -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .padding(bottom = 4.dp)
            .widthIn(max = 280.dp)
            .onSizeChanged(onSizeChanged)
            .testTag(PET_SPEECH_BUBBLE_TAG)
            .semantics {
                contentDescription = "Castor dice: $message"
                traversalIndex = 0f
            },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 6.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = message,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp, top = 10.dp, bottom = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(48.dp)
                    .testTag(PET_SPEECH_DISMISS_TAG)
                    .semantics { traversalIndex = 0.5f },
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Cerrar mensaje")
            }
        }
    }
}

@Composable
internal fun BeaverPrototype(
    rendererInput: PetRendererInput,
    onTap: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
) {
    val targetScale = when (rendererInput.animationIntent) {
        PetAnimationIntent.TAP -> 1.08f
        PetAnimationIntent.HELD -> 0.94f
        else -> 1f
    }
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(
            if (rendererInput.reducedMotion || !rendererInput.playbackEnabled) 0 else 180,
        ),
        label = "beaverScale",
    )
    Box(
        modifier = Modifier
            .size(104.dp)
            .testTag(PET_CHARACTER_TAG)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = if (
                    !rendererInput.reducedMotion &&
                    rendererInput.playbackEnabled &&
                    rendererInput.animationIntent == PetAnimationIntent.SETTLE
                ) -4f else 0f
            }
            .clickable(onClick = onTap)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { onDragStart() },
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragEnd,
                ) { change, amount ->
                    change.consume()
                    onDrag(amount)
                }
            }
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = when (rendererInput.animationIntent) {
                    PetAnimationIntent.SLEEP -> "Castor, compañero dormido. Toca para despertarlo"
                    PetAnimationIntent.HELD -> "Castor, moviéndose"
                    else -> "Castor, compañero interactivo. Toca para saludar o arrastra para mover"
                }
                onClick(label = "Saludar a Castor") { onTap(); true }
                traversalIndex = 1f
            },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.beaver_prototype),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

internal const val PET_CHARACTER_TAG = "pet_character"
internal const val PET_SPEECH_BUBBLE_TAG = "pet_speech_bubble"
internal const val PET_SPEECH_DISMISS_TAG = "pet_speech_dismiss"
