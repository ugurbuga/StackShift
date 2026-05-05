package com.ugurbuga.blockgames.ui.game.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import blockgames.composeapp.generated.resources.Res
import blockgames.composeapp.generated.resources.launch_label
import blockgames.composeapp.generated.resources.restart_cancel
import blockgames.composeapp.generated.resources.restart_confirm
import blockgames.composeapp.generated.resources.restart_confirm_body
import blockgames.composeapp.generated.resources.restart_confirm_title
import blockgames.composeapp.generated.resources.time_remaining
import com.ugurbuga.blockgames.BlockGamesTheme
import com.ugurbuga.blockgames.game.logic.GameEvent
import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.game.model.GameStatus
import com.ugurbuga.blockgames.game.model.GridPoint
import com.ugurbuga.blockgames.game.model.PlacementPreview
import com.ugurbuga.blockgames.game.model.formatMergeValue
import com.ugurbuga.blockgames.game.model.toTopLeft
import com.ugurbuga.blockgames.localization.LocalAppSettings
import com.ugurbuga.blockgames.platform.feedback.GameHaptics
import com.ugurbuga.blockgames.platform.feedback.NoOpGameHaptics
import com.ugurbuga.blockgames.platform.feedback.NoOpSoundEffectPlayer
import com.ugurbuga.blockgames.platform.feedback.SoundEffectPlayer
import com.ugurbuga.blockgames.presentation.game.GameDispatchResult
import com.ugurbuga.blockgames.presentation.game.InteractionFeedback
import com.ugurbuga.blockgames.settings.AppSettings
import com.ugurbuga.blockgames.settings.MergeShiftOnboardingScene
import com.ugurbuga.blockgames.settings.MergeShiftOnboardingStage
import com.ugurbuga.blockgames.settings.MergeShiftOnboardingStateFactory
import com.ugurbuga.blockgames.telemetry.AppTelemetry
import com.ugurbuga.blockgames.telemetry.NoOpAppTelemetry
import com.ugurbuga.blockgames.telemetry.TelemetryActionNames
import com.ugurbuga.blockgames.ui.game.BoardGrid
import com.ugurbuga.blockgames.ui.game.GameOverDialog
import com.ugurbuga.blockgames.ui.game.GameOverDialogRevealDurationMillis
import com.ugurbuga.blockgames.ui.game.InteractiveOnboardingCompletionDialog
import com.ugurbuga.blockgames.ui.game.MinimalTopBar
import com.ugurbuga.blockgames.ui.game.RestartConfirmDialog
import com.ugurbuga.blockgames.ui.game.boardCellCornerRadiusPx
import com.ugurbuga.blockgames.ui.game.boardFrameCornerRadiusDp
import com.ugurbuga.blockgames.ui.game.drawCellBody
import com.ugurbuga.blockgames.ui.game.onboarding.InteractiveOnboardingInfoCard
import com.ugurbuga.blockgames.ui.game.onboarding.MergeShiftInteractiveGameOnboardingUi
import com.ugurbuga.blockgames.ui.game.onboarding.MergeShiftOnboardingTargetOverlay
import com.ugurbuga.blockgames.ui.game.onboarding.rememberMergeShiftInteractiveOnboardingVisualState
import com.ugurbuga.blockgames.ui.theme.BlockGamesThemeTokens
import com.ugurbuga.blockgames.ui.theme.GameUiShapeTokens
import com.ugurbuga.blockgames.ui.theme.appBackgroundBrush
import com.ugurbuga.blockgames.ui.theme.blockGamesSurfaceShadow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

private const val LaunchAnimationMillis = 140L
private const val EntryAnimationMillis = 70L

@Composable
fun MergeShiftGameScreen(
    gameState: GameState,
    onRequestPreview: (Int) -> PlacementPreview?,
    onRequestImpactPoints: (PlacementPreview?) -> Set<GridPoint> = { emptySet() },
    onPlacePiece: (Int) -> GameDispatchResult,
    onRestart: () -> InteractionFeedback,
    onTick: () -> Unit,
    onBack: () -> Unit = {},
    telemetry: AppTelemetry = NoOpAppTelemetry,
    soundPlayer: SoundEffectPlayer,
    haptics: GameHaptics,
    highestScore: Int,
    interactiveOnboardingScene: MergeShiftOnboardingScene? = null,
    interactiveOnboardingCurrentStep: Int = 0,
    interactiveOnboardingTotalSteps: Int = 0,
    interactiveOnboardingAwaitingCommit: Boolean = false,
    interactiveOnboardingCompletionDialogVisible: Boolean = false,
    onInteractiveOnboardingStartGame: () -> Unit = {},
    onInteractiveOnboardingReturnHome: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val uiColors = BlockGamesThemeTokens.uiColors
    val settings = LocalAppSettings.current
    val textMeasurer = rememberTextMeasurer()
    
    val density = LocalDensity.current
    val resolvedStyle = com.ugurbuga.blockgames.game.model.resolveBoardBlockStyle(
        selectedStyle = settings.blockVisualStyle,
        mode = settings.boardBlockStyleMode,
    )
    
    var showRestartDialog by remember { mutableStateOf(false) }
    val screenShakeX = remember { Animatable(0f) }
    val screenShakeY = remember { Animatable(0f) }
    
    val gameOverBoardClearProgress = remember { Animatable(0f) }
    val gameOverDialogRevealProgress = remember { Animatable(0f) }
    var showGameOverDialog by remember { mutableStateOf(value = false) }

    LaunchedEffect(gameState.status) {
        if (gameState.status == GameStatus.GameOver) {
            showGameOverDialog = true
            gameOverDialogRevealProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = GameOverDialogRevealDurationMillis,
                    easing = FastOutSlowInEasing,
                ),
            )
        } else {
            showGameOverDialog = false
            gameOverDialogRevealProgress.snapTo(0f)
        }
    }

    LaunchedEffect(gameState.status, gameState.activePiece == null) {
        if (gameState.status == GameStatus.Running && gameState.activePiece == null) {
            onTick() // First tick happens immediately after piece lands
            while (gameState.status == GameStatus.Running && gameState.activePiece == null) {
                delay(400.milliseconds)
                onTick()
            }
        }
    }
    
    val stylePulseState = rememberInfiniteTransition(label = "stylePulse")
        .animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "stylePulse",
        )
    val stylePulse = stylePulseState.value

    val interactiveOnboardingEnabled = interactiveOnboardingScene != null
    val interactiveOnboardingAcceptedColumns = interactiveOnboardingScene?.acceptedColumns.orEmpty()
    val topBarControlsEnabled = !interactiveOnboardingEnabled

    val activePiece = gameState.activePiece
    var overlayHostRectInRoot by remember { mutableStateOf(Rect.Zero) }
    var boardRectInRoot by remember { mutableStateOf(Rect.Zero) }
    var trayRectInRoot by remember { mutableStateOf(Rect.Zero) }
    
    val overlayTopLeftState = remember(activePiece?.id) { mutableStateOf<Offset?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    var isLaunching by remember { mutableStateOf(false) }
    
    val boardRect by remember(boardRectInRoot, overlayHostRectInRoot) {
        derivedStateOf { boardRectInRoot.toLocalRect(overlayHostRectInRoot) }
    }
    val trayRect by remember(trayRectInRoot, overlayHostRectInRoot) {
        derivedStateOf { trayRectInRoot.toLocalRect(overlayHostRectInRoot) }
    }
    val cellSizePx by remember(boardRect, gameState.config) {
        derivedStateOf {
            if (boardRect.width == 0f) 0f else boardRect.width / gameState.config.columns
        }
    }
    
    val spawnColumn = remember(activePiece?.id, gameState.lastPlacementColumn) {
        gameState.lastPlacementColumn ?: (gameState.config.columns / 2)
    }
    
    val spawnTopLeft by remember(activePiece?.id, trayRect, boardRect, cellSizePx, spawnColumn) {
        derivedStateOf {
            if (activePiece == null || trayRect == Rect.Zero || boardRect == Rect.Zero || cellSizePx <= 0f) null
            else Offset(
                x = boardRect.left + (spawnColumn * cellSizePx),
                y = trayRect.center.y - cellSizePx / 2f
            )
        }
    }

    val overlayX = remember { Animatable(spawnTopLeft?.x ?: 0f) }
    val overlayY = remember { Animatable(spawnTopLeft?.y ?: 0f) }
    
    var hasSnappedInitially by remember(activePiece?.id) { mutableStateOf(false) }

    LaunchedEffect(activePiece?.id) {
        isLaunching = false
        isDragging = false
        hasSnappedInitially = false
        snapshotFlow { spawnTopLeft to (isLaunching || isDragging) }.collect { (spawnPos, active) ->
            if (spawnPos != null && !active) {
                overlayTopLeftState.value = spawnPos
                overlayX.snapTo(spawnPos.x)
                overlayY.snapTo(spawnPos.y)
                hasSnappedInitially = true
            }
        }
    }

    val selectedColumn by remember(activePiece?.id, boardRect, cellSizePx) {
        derivedStateOf {
            if (activePiece == null || overlayTopLeftState.value == null || boardRect == Rect.Zero || cellSizePx <= 0f) null
            else {
                val approx = ((overlayTopLeftState.value!!.x - boardRect.left) / cellSizePx).roundToInt()
                approx.coerceIn(0, gameState.config.columns - 1)
            }
        }
    }

    val placementPreview by remember(selectedColumn, activePiece?.id, gameState.status, isDragging, interactiveOnboardingAcceptedColumns) {
        derivedStateOf {
            if (gameState.status != GameStatus.Running || isLaunching) null
            else {
                if (interactiveOnboardingAcceptedColumns.isNotEmpty() && selectedColumn !in interactiveOnboardingAcceptedColumns) null
                else selectedColumn?.let(onRequestPreview)
            }
        }
    }

    val interactiveOnboardingTargetAligned by remember(
        interactiveOnboardingScene,
        interactiveOnboardingAcceptedColumns,
        selectedColumn,
        placementPreview,
        isDragging,
    ) {
        derivedStateOf {
            interactiveOnboardingScene != null &&
                    isDragging &&
                    placementPreview != null &&
                    selectedColumn != null &&
                    (interactiveOnboardingAcceptedColumns.isEmpty() || selectedColumn in interactiveOnboardingAcceptedColumns)
        }
    }

    val resolvedInteractiveOnboardingUi = interactiveOnboardingScene?.let { scene ->
        MergeShiftInteractiveGameOnboardingUi(
            scene = scene,
            currentStep = interactiveOnboardingCurrentStep,
            totalSteps = interactiveOnboardingTotalSteps,
            isTargetAligned = interactiveOnboardingTargetAligned,
            isAwaitingPlacementCommit = interactiveOnboardingAwaitingCommit,
        )
    }

    val resolvedInteractiveOnboardingVisualState = resolvedInteractiveOnboardingUi?.let {
        rememberMergeShiftInteractiveOnboardingVisualState(it)
    }

    val displayOverlayTopLeft by remember(
        activePiece?.id,
        selectedColumn,
        boardRect,
        cellSizePx,
        isLaunching,
    ) {
        derivedStateOf {
            val current = overlayTopLeftState.value ?: return@derivedStateOf null
            val snappedColumn = selectedColumn
            if (snappedColumn == null || boardRect == Rect.Zero || cellSizePx <= 0f || isLaunching) {
                return@derivedStateOf current
            }
            current.copy(x = boardRect.left + (snappedColumn * cellSizePx))
        }
    }

    val impactedPoints by remember(placementPreview, activePiece?.id) {
        derivedStateOf {
            onRequestImpactPoints(placementPreview)
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(appBackgroundBrush(uiColors))
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .onGloballyPositioned { overlayHostRectInRoot = it.boundsInRoot() },
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!interactiveOnboardingEnabled) {
                    MinimalTopBar(
                        gameState = gameState,
                        scoreHighlightStrengthProvider = { 0f },
                        scoreHighlightScaleProvider = { 1f },
                        remainingTimeLabel = stringResource(Res.string.time_remaining),
                        onBack = onBack,
                        onRestart = { showRestartDialog = true },
                        stylePulse = stylePulse,
                    )
                }

                resolvedInteractiveOnboardingUi?.let { onboardingUi ->
                    InteractiveOnboardingInfoCard(
                        currentStep = onboardingUi.currentStep,
                        totalSteps = onboardingUi.totalSteps,
                        visualState = resolvedInteractiveOnboardingVisualState
                            ?: rememberMergeShiftInteractiveOnboardingVisualState(onboardingUi),
                        onBack = onBack,
                        stylePulse = stylePulse,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    val columns = gameState.config.columns
                    val rows = gameState.config.rows
                    val widthLimitedCell = maxWidth / columns
                    val heightLimitedCell = maxHeight / (rows + 3)
                    val cellSize = minOf(widthLimitedCell, heightLimitedCell)
                    
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = cellSize * columns, height = cellSize * rows)
                                .blockGamesSurfaceShadow(
                                    shape = RoundedCornerShape(boardFrameCornerRadiusDp(resolvedStyle)),
                                    elevation = 10.dp,
                                ),
                        ) {
                            BoardGrid(
                                modifier = Modifier
                                    .matchParentSize()
                                    .onGloballyPositioned { boardRectInRoot = it.boundsInRoot() },
                                gameState = gameState,
                                preview = placementPreview,
                                impactedPreviewCells = impactedPoints,
                                activeColumn = selectedColumn,
                                activePiece = activePiece,
                                isDragging = isDragging,
                                stylePulse = stylePulse,
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Dock area
                        MergeShiftBottomDock(
                            modifier = Modifier
                                        .fillMaxWidth()
                                        .height(cellSize * 3f)
                                        .onGloballyPositioned { trayRectInRoot = it.boundsInRoot() },
                            stylePulse = stylePulse
                        )
                    }
                }
            }

            LaunchGuideLineOverlay(
                preview = placementPreview,
                activePiece = activePiece,
                pieceTopLeft = displayOverlayTopLeft,
                boardRect = boardRect,
                cellSizePx = cellSizePx,
            )

            resolvedInteractiveOnboardingUi?.let { onboardingUi ->
                MergeShiftOnboardingTargetOverlay(
                    ui = onboardingUi,
                    boardRect = boardRect,
                    cellSizePx = cellSizePx,
                )
            }

            // Active Piece Overlay
            if (activePiece != null && hasSnappedInitially && cellSizePx > 0f) {
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            translationX = overlayX.value
                            translationY = overlayY.value
                        }
                        .size(with(LocalDensity.current) { cellSizePx.toDp() })
                        .pointerInput(activePiece.id) {
                            detectDragGestures(
                                onDragStart = { if (!isLaunching) isDragging = true },
                                onDrag = { change, dragAmount ->
                                    if (!isLaunching) {
                                        change.consume()
                                        val next = overlayTopLeftState.value!! + Offset(dragAmount.x, 0f)
                                        overlayTopLeftState.value = next
                                        coroutineScope.launch {
                                            overlayX.snapTo(next.x)
                                            overlayY.snapTo(next.y)
                                        }
                                    }
                                },
                                onDragEnd = {
                                    isDragging = false
                                    val col = selectedColumn
                                    val preview = placementPreview
                                    if (col != null && preview != null) {
                                        if (interactiveOnboardingAcceptedColumns.isNotEmpty() && col !in interactiveOnboardingAcceptedColumns) {
                                            coroutineScope.launch {
                                                overlayX.animateTo(spawnTopLeft!!.x)
                                                overlayY.animateTo(spawnTopLeft!!.y)
                                            }
                                            return@detectDragGestures
                                        }
                                        isLaunching = true
                                        coroutineScope.launch {
                                            val entry = preview.entryAnchor.toTopLeft(boardRect, cellSizePx)
                                            val landing = preview.landingAnchor.toTopLeft(boardRect, cellSizePx)

                                            // Animate to entry (top of the column)
                                            launch {
                                                overlayX.animateTo(entry.x, tween(EntryAnimationMillis.toInt(), easing = FastOutSlowInEasing))
                                            }
                                            launch {
                                                overlayY.animateTo(entry.y, tween(EntryAnimationMillis.toInt(), easing = FastOutSlowInEasing))
                                            }
                                            delay(EntryAnimationMillis.milliseconds)

                                            // Fall down to landing
                                            launch {
                                                overlayX.animateTo(landing.x, tween(LaunchAnimationMillis.toInt(), easing = FastOutSlowInEasing))
                                            }
                                            launch {
                                                overlayY.animateTo(landing.y, tween(LaunchAnimationMillis.toInt(), easing = FastOutSlowInEasing))
                                            }
                                            delay(LaunchAnimationMillis.milliseconds)

                                            val result = onPlacePiece(col)
                                            dispatchFeedback(result.feedback, soundPlayer, haptics)
                                            if (GameEvent.InvalidDrop in result.events) {
                                                isLaunching = false
                                            }
                                        }
                                    } else {
                                        coroutineScope.launch {
                                            val spawnPos = spawnTopLeft
                                            overlayTopLeftState.value = spawnPos
                                            launch {
                                                overlayX.animateTo(spawnPos?.x ?: 0f, tween(180, easing = FastOutSlowInEasing))
                                            }
                                            launch {
                                                overlayY.animateTo(spawnPos?.y ?: 0f, tween(180, easing = FastOutSlowInEasing))
                                            }
                                        }
                                    }
                                },
                                onDragCancel = {
                                    isDragging = false
                                    coroutineScope.launch {
                                        val spawnPos = spawnTopLeft
                                        overlayTopLeftState.value = spawnPos
                                        launch {
                                            overlayX.animateTo(spawnPos?.x ?: 0f, tween(180, easing = FastOutSlowInEasing))
                                        }
                                        launch {
                                            overlayY.animateTo(spawnPos?.y ?: 0f, tween(180, easing = FastOutSlowInEasing))
                                        }
                                    }
                                }
                            )
                        }
                ) {
                    // Draw active piece
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCellBody(
                            tone = activePiece.tone,
                            palette = settings.blockColorPalette,
                            style = resolvedStyle,
                            topLeft = Offset.Zero,
                            size = this.size,
                            cornerRadius = CornerRadius(boardCellCornerRadiusPx(cellSizePx, resolvedStyle)),
                            pulse = stylePulse
                        )
                        // Draw value
                        val text = formatMergeValue(activePiece.value)
                        val textStyle = TextStyle(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = with(density) { (this@Canvas.size.width * 0.4f).toSp() },
                            textAlign = TextAlign.Center
                        )
                        val measuredText = textMeasurer.measure(text, textStyle)
                        drawText(
                            textMeasurer = textMeasurer,
                            text = text,
                            style = textStyle,
                            topLeft = Offset(
                                x = (this.size.width - measuredText.size.width) / 2f,
                                y = (this.size.height - measuredText.size.height) / 2f
                            )
                        )
                    }
                }
            }

            if (showGameOverDialog) {
                GameOverDialog(
                    gameState = gameState,
                    highestScore = highestScore,
                    showNewHighScoreMessage = false,
                    revealProgressProvider = { gameOverDialogRevealProgress.value },
                    canUseExtraLife = false,
                    isExtraLifeLoading = false,
                    showExtraLifeButton = false,
                    onPlayAgain = {
                        telemetry.logUserAction(TelemetryActionNames.PlayAgain)
                        dispatchFeedback(onRestart(), soundPlayer, haptics)
                    },
                    onUseExtraLife = {},
                )
            }

            if (showRestartDialog) {
                RestartConfirmDialog(
                    onDismissRequest = { showRestartDialog = false },
                    title = stringResource(Res.string.restart_confirm_title),
                    message = stringResource(Res.string.restart_confirm_body),
                    confirmLabel = stringResource(Res.string.restart_confirm),
                    dismissLabel = stringResource(Res.string.restart_cancel),
                    onConfirm = {
                        showRestartDialog = false
                        dispatchFeedback(onRestart(), soundPlayer, haptics)
                    },
                )
            }

            if (interactiveOnboardingCompletionDialogVisible) {
                InteractiveOnboardingCompletionDialog(
                    onStartGame = onInteractiveOnboardingStartGame,
                    onReturnHome = onInteractiveOnboardingReturnHome,
                )
            }
        }
    }
}

@Composable
private fun MergeShiftBottomDock(
    modifier: Modifier = Modifier,
    stylePulse: Float = 0f,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    Card(
        modifier = modifier.blockGamesSurfaceShadow(
            shape = RoundedCornerShape(GameUiShapeTokens.dockCorner),
            elevation = 10.dp,
        ),
        shape = RoundedCornerShape(GameUiShapeTokens.dockCorner),
        colors = CardDefaults.cardColors(containerColor = uiColors.panel.copy(alpha = 0.90f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, uiColors.panelStroke.copy(alpha = 0.68f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            uiColors.launchGlow.copy(alpha = 0.16f + (0.08f * stylePulse)),
                            Color.Transparent,
                        ),
                    ),
                )
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(
                text = stringResource(Res.string.launch_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Preview(name = "MergeShift - Game")
@Composable
private fun MergeShiftGameScreenPreview() {
    BlockGamesTheme(settings = AppSettings()) {
        MergeShiftGameScreen(
            gameState = MergeShiftOnboardingStateFactory.initialState(),
            onRequestPreview = { null },
            onPlacePiece = { GameDispatchResult() },
            onRestart = { InteractionFeedback.None },
            onTick = {},
            soundPlayer = NoOpSoundEffectPlayer,
            haptics = NoOpGameHaptics,
            highestScore = 1000,
        )
    }
}

@Preview(name = "MergeShift Onboarding - Launch")
@Composable
private fun MergeShiftOnboardingLaunchPreview() {
    val stage = MergeShiftOnboardingStage.Launch
    val scene = MergeShiftOnboardingStateFactory.scene(stage)
    BlockGamesTheme(settings = AppSettings()) {
        MergeShiftGameScreen(
            gameState = scene.gameState,
            onRequestPreview = { null },
            onPlacePiece = { GameDispatchResult() },
            onRestart = { InteractionFeedback.None },
            onTick = {},
            soundPlayer = NoOpSoundEffectPlayer,
            haptics = NoOpGameHaptics,
            highestScore = 1000,
            interactiveOnboardingScene = scene,
            interactiveOnboardingCurrentStep = 1,
            interactiveOnboardingTotalSteps = 4,
        )
    }
}

@Preview(name = "MergeShift Onboarding - Vertical Merge")
@Composable
private fun MergeShiftOnboardingVerticalMergePreview() {
    val stage = MergeShiftOnboardingStage.VerticalMerge
    val scene = MergeShiftOnboardingStateFactory.scene(stage)
    BlockGamesTheme(settings = AppSettings()) {
        MergeShiftGameScreen(
            gameState = scene.gameState,
            onRequestPreview = { null },
            onPlacePiece = { GameDispatchResult() },
            onRestart = { InteractionFeedback.None },
            onTick = {},
            soundPlayer = NoOpSoundEffectPlayer,
            haptics = NoOpGameHaptics,
            highestScore = 1000,
            interactiveOnboardingScene = scene,
            interactiveOnboardingCurrentStep = 2,
            interactiveOnboardingTotalSteps = 4,
        )
    }
}

@Preview(name = "MergeShift Onboarding - Horizontal Merge")
@Composable
private fun MergeShiftOnboardingHorizontalMergePreview() {
    val stage = MergeShiftOnboardingStage.HorizontalMerge
    val scene = MergeShiftOnboardingStateFactory.scene(stage)
    BlockGamesTheme(settings = AppSettings()) {
        MergeShiftGameScreen(
            gameState = scene.gameState,
            onRequestPreview = { null },
            onPlacePiece = { GameDispatchResult() },
            onRestart = { InteractionFeedback.None },
            onTick = {},
            soundPlayer = NoOpSoundEffectPlayer,
            haptics = NoOpGameHaptics,
            highestScore = 1000,
            interactiveOnboardingScene = scene,
            interactiveOnboardingCurrentStep = 3,
            interactiveOnboardingTotalSteps = 4,
        )
    }
}

@Preview(name = "MergeShift Onboarding - Multi Merge")
@Composable
private fun MergeShiftOnboardingMultiMergePreview() {
    val stage = MergeShiftOnboardingStage.MultiMerge
    val scene = MergeShiftOnboardingStateFactory.scene(stage)
    BlockGamesTheme(settings = AppSettings()) {
        MergeShiftGameScreen(
            gameState = scene.gameState,
            onRequestPreview = { null },
            onPlacePiece = { GameDispatchResult() },
            onRestart = { InteractionFeedback.None },
            onTick = {},
            soundPlayer = NoOpSoundEffectPlayer,
            haptics = NoOpGameHaptics,
            highestScore = 1000,
            interactiveOnboardingScene = scene,
            interactiveOnboardingCurrentStep = 4,
            interactiveOnboardingTotalSteps = 4,
        )
    }
}

private fun Rect.toLocalRect(hostRect: Rect): Rect {
    if (this == Rect.Zero || hostRect == Rect.Zero) return Rect.Zero
    return Rect(
        left - hostRect.left,
        top - hostRect.top,
        right - hostRect.left,
        bottom - hostRect.top,
    )
}
