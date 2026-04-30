package com.ugurbuga.blockgames.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ugurbuga.blockgames.ads.GameAdController
import com.ugurbuga.blockgames.ads.NoOpGameAdController
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.game.model.GameStatus
import com.ugurbuga.blockgames.game.model.GameTextKey
import com.ugurbuga.blockgames.game.model.GridPoint
import com.ugurbuga.blockgames.game.model.Piece
import com.ugurbuga.blockgames.game.model.PlacementPreview
import com.ugurbuga.blockgames.game.model.gameText
import com.ugurbuga.blockgames.telemetry.AppTelemetry
import com.ugurbuga.blockgames.telemetry.NoOpAppTelemetry
import com.ugurbuga.blockgames.ui.theme.BlockGamesThemeTokens
import com.ugurbuga.blockgames.ui.theme.GameUiShapeTokens
import com.ugurbuga.blockgames.ui.theme.appBackgroundBrush
import com.ugurbuga.blockgames.ui.theme.blockGamesSurfaceShadow
import org.jetbrains.compose.resources.stringResource
import blockgames.composeapp.generated.resources.Res
import blockgames.composeapp.generated.resources.time_remaining

private const val FreePlacementDragLiftPx = 400f
private val FreePlacementTrayCardHeight = 104.dp
private val FreePlacementCompactTrayCardHeight = 72.dp
private val FreePlacementTrayPieceCellSize = 18.dp
private val FreePlacementCompactTrayPieceCellSize = 14.dp

@Composable
fun FreePlacementGameScreen(
    gameState: GameState,
    onRequestPreview: (Long, GridPoint) -> PlacementPreview?,
    onResolvePreviewImpact: (PlacementPreview?) -> Set<GridPoint>,
    onPlacePiece: (Long, GridPoint) -> Unit,
    onRestart: () -> Unit,
    onRewardedRevive: () -> Unit,
    onBack: () -> Unit,
    highestScore: Int,
    showNewHighScoreMessage: Boolean,
    adController: GameAdController = NoOpGameAdController,
    telemetry: AppTelemetry = NoOpAppTelemetry,
    modifier: Modifier = Modifier,
) {
    val currentGameState by rememberUpdatedState(gameState)
    val currentOnRequestPreview by rememberUpdatedState(onRequestPreview)
    val currentOnResolvePreviewImpact by rememberUpdatedState(onResolvePreviewImpact)
    val currentOnPlacePiece by rememberUpdatedState(onPlacePiece)

    telemetry.hashCode()
    val uiColors = BlockGamesThemeTokens.uiColors
    var hostRectInRoot by remember { mutableStateOf(Rect.Zero) }
    var boardRectInRoot by remember { mutableStateOf(Rect.Zero) }
    val trayPieceRectsInRoot = remember { mutableStateMapOf<Long, Rect>() }
    var draggedPieceId by remember { mutableStateOf<Long?>(null) }
    var dragPointerInHost by remember { mutableStateOf<Offset?>(null) }
    var showRestartDialog by remember { mutableStateOf(false) }
    var rewardedReviveLoading by remember { mutableStateOf(false) }
    val gameOverDialogRevealProgress = remember { Animatable(0f) }

    LaunchedEffect(gameState.status) {
        if (gameState.status == GameStatus.GameOver) {
            gameOverDialogRevealProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 260,
                    easing = FastOutSlowInEasing,
                ),
            )
        } else {
            gameOverDialogRevealProgress.snapTo(0f)
        }
    }

    val trayPieces by remember {
        derivedStateOf { currentGameState.trayPieces }
    }
    val draggedPiece by remember {
        derivedStateOf { trayPieces.firstOrNull { it.id == draggedPieceId } }
    }
    val density = LocalDensity.current
    val hasActiveChallenge = gameState.activeChallenge != null
    val trayCardHeight = if (hasActiveChallenge) {
        FreePlacementCompactTrayCardHeight
    } else {
        FreePlacementTrayCardHeight
    }
    val trayPieceCellSize = if (hasActiveChallenge) {
        FreePlacementCompactTrayPieceCellSize
    } else {
        FreePlacementTrayPieceCellSize
    }
    val boardRect by remember {
        derivedStateOf {
            val insetPx = with(density) { 1.dp.toPx() }
            boardRectInRoot
                .toLocalRect(hostRectInRoot)
                .deflate(insetPx)
        }
    }
    val cellSizePx by remember {
        derivedStateOf {
            if (boardRect == Rect.Zero) 0f else boardRect.width / currentGameState.config.columns.toFloat()
        }
    }
    val overlayTopLeft by remember {
        derivedStateOf {
            val pointer = dragPointerInHost ?: return@derivedStateOf null
            val piece = draggedPiece ?: return@derivedStateOf null
            freePlacementDragTopLeft(
                pointerInHost = pointer,
                piece = piece,
                cellSizePx = cellSizePx,
            )
        }
    }
    val placementPreview by remember {
        derivedStateOf {
            val pieceId = draggedPieceId ?: return@derivedStateOf null
            if (currentGameState.status != GameStatus.Running) return@derivedStateOf null
            resolveNearestFreePlacementPreview(
                pieceId = pieceId,
                piece = draggedPiece,
                overlayTopLeft = overlayTopLeft,
                boardRect = boardRect,
                cellSizePx = cellSizePx,
                config = currentGameState.config,
                requestPreview = currentOnRequestPreview,
            )
        }
    }
    val impactedPreviewCells by remember {
        derivedStateOf { currentOnResolvePreviewImpact(placementPreview) }
    }

    LaunchedEffect(gameState.activePiece?.id, gameState.status) {
        if (gameState.status != GameStatus.Running) {
            draggedPieceId = null
            dragPointerInHost = null
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
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .onGloballyPositioned { hostRectInRoot = it.boundsInRoot() },
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MinimalTopBar(
                    gameState = gameState,
                    scoreHighlightStrengthProvider = { if (showNewHighScoreMessage) 1f else 0f },
                    scoreHighlightScaleProvider = { if (showNewHighScoreMessage) 1.04f else 1f },
                    remainingTimeLabel = stringResource(Res.string.time_remaining),
                    onBack = onBack,
                    onRestart = { showRestartDialog = true },
                    stylePulse = 0f,
                )

                StatusCard(text = resolveGameText(gameState.message))

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    val boardAspectRatio = gameState.config.columns.toFloat() / gameState.config.rows.toFloat()
                    val boardWidth = minOf(maxWidth, maxHeight * boardAspectRatio)
                    val boardHeight = boardWidth / boardAspectRatio
                    Box(
                        modifier = Modifier
                            .size(width = boardWidth, height = boardHeight)
                            .blockGamesSurfaceShadow(
                                shape = RoundedCornerShape(GameUiShapeTokens.panelCorner),
                                elevation = 10.dp,
                            ),
                    ) {
                        BoardGrid(
                            modifier = Modifier
                                .fillMaxSize()
                                .onGloballyPositioned { boardRectInRoot = it.boundsInRoot() },
                            gameState = gameState,
                            preview = placementPreview,
                            impactedPreviewCells = impactedPreviewCells,
                            activeColumn = placementPreview?.selectedColumn,
                            activePiece = draggedPiece,
                            isDragging = draggedPiece != null,
                        )
                    }
                }

                TrayDock(
                    pieces = trayPieces,
                    challenge = gameState.activeChallenge,
                    activeDragPieceId = draggedPieceId,
                    onPieceRectChanged = { pieceId, rect -> trayPieceRectsInRoot[pieceId] = rect },
                    onStartDrag = { piece, dragStartOffset ->
                        val rect = trayPieceRectsInRoot[piece.id] ?: return@TrayDock
                        draggedPieceId = piece.id
                        dragPointerInHost = initialDragPointerPosition(
                            pieceRectInRoot = rect,
                            pointerOffsetInPieceCard = dragStartOffset,
                            hostRectInRoot = hostRectInRoot,
                        )
                    },
                    onDrag = { dragAmount ->
                        val current = dragPointerInHost ?: return@TrayDock
                        dragPointerInHost = current + dragAmount
                    },
                    onCancelDrag = {
                        draggedPieceId = null
                        dragPointerInHost = null
                    },
                    onEndDrag = {
                        val pieceId = draggedPieceId
                        val preview = placementPreview
                        if (pieceId != null && preview != null) {
                            currentOnPlacePiece(pieceId, preview.landingAnchor)
                        }
                        draggedPieceId = null
                        dragPointerInHost = null
                    },
                    pieceCardHeight = trayCardHeight,
                    pieceCellSize = trayPieceCellSize,
                )
            }

            val dragOverlayTopLeft = overlayTopLeft
            val dragOverlayPiece = draggedPiece
            if (dragOverlayPiece != null && dragOverlayTopLeft != null && cellSizePx > 0f) {
                val boardCellSize = with(density) { cellSizePx.toDp() }
                val overlayTranslationX = dragOverlayTopLeft.x
                val overlayTranslationY = dragOverlayTopLeft.y
                PieceBlocks(
                    piece = dragOverlayPiece,
                    cellSize = boardCellSize,
                    modifier = Modifier
                        .graphicsLayer(
                            translationX = overlayTranslationX,
                            translationY = overlayTranslationY,
                            transformOrigin = TransformOrigin(0f, 0f),
                        ),
                )
            }
        }
    }

    if (showRestartDialog) {
        RestartConfirmDialog(
            onDismissRequest = { showRestartDialog = false },
            title = resolveGameText(gameText(GameTextKey.RestartConfirmTitle)),
            message = resolveGameText(gameText(GameTextKey.RestartConfirmBody)),
            confirmLabel = resolveGameText(gameText(GameTextKey.RestartConfirm)),
            dismissLabel = resolveGameText(gameText(GameTextKey.RestartCancel)),
            onConfirm = {
                showRestartDialog = false
                adController.showRestartInterstitial {
                    onRestart()
                }
            },
        )
    }

    if (gameState.status == GameStatus.GameOver) {
        GameOverDialog(
            gameState = gameState,
            highestScore = highestScore,
            showNewHighScoreMessage = showNewHighScoreMessage,
            revealProgressProvider = { gameOverDialogRevealProgress.value },
            canUseExtraLife = !gameState.rewardedReviveUsed,
            isExtraLifeLoading = rewardedReviveLoading,
            showExtraLifeButton = adController !== NoOpGameAdController && gameState.activeChallenge?.isCompleted != true,
            onPlayAgain = {
                if (gameState.activeChallenge?.isCompleted == true) {
                    onBack()
                } else {
                    adController.showRestartInterstitial {
                        onRestart()
                    }
                }
            },
            onUseExtraLife = {
                if (rewardedReviveLoading || gameState.rewardedReviveUsed) return@GameOverDialog
                rewardedReviveLoading = true
                adController.showRewardedRevive { rewarded ->
                    rewardedReviveLoading = false
                    if (rewarded) {
                        onRewardedRevive()
                    }
                }
            },
        )
    }
}

@Composable
private fun StatusCard(text: String) {
    val uiColors = BlockGamesThemeTokens.uiColors
    Card(
        shape = RoundedCornerShape(GameUiShapeTokens.hintCorner),
        colors = CardDefaults.cardColors(containerColor = uiColors.metricCard.copy(alpha = 0.88f)),
        border = BorderStroke(1.dp, uiColors.panelStroke.copy(alpha = 0.66f)),
    ) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TrayDock(
    pieces: List<Piece>,
    challenge: com.ugurbuga.blockgames.game.model.DailyChallenge?,
    activeDragPieceId: Long?,
    onPieceRectChanged: (Long, Rect) -> Unit,
    onStartDrag: (Piece, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onEndDrag: () -> Unit,
    onCancelDrag: () -> Unit,
    pieceCardHeight: androidx.compose.ui.unit.Dp,
    pieceCellSize: androidx.compose.ui.unit.Dp,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    Card(
        shape = RoundedCornerShape(GameUiShapeTokens.dockCorner),
        colors = CardDefaults.cardColors(containerColor = uiColors.panel.copy(alpha = 0.92f)),
        border = BorderStroke(1.dp, uiColors.panelStroke.copy(alpha = 0.72f)),
        modifier = Modifier.blockGamesSurfaceShadow(
            shape = RoundedCornerShape(GameUiShapeTokens.dockCorner),
            elevation = 10.dp,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            uiColors.panelHighlight.copy(alpha = 0.16f),
                            uiColors.launchGlow.copy(alpha = 0.08f),
                            androidx.compose.ui.graphics.Color.Transparent,
                        ),
                    ),
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(if (challenge != null) 8.dp else 10.dp),
        ) {
            if (challenge != null) {
                ChallengeTasksDock(
                    challenge = challenge,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text(
                    text = resolveGameText(gameText(GameTextKey.LaunchDragHintBlockWise)),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                pieces.forEach { piece ->
                    TrayPieceCard(
                        piece = piece,
                        hidden = activeDragPieceId == piece.id,
                        cardHeight = pieceCardHeight,
                        pieceCellSize = pieceCellSize,
                        modifier = Modifier.weight(1f),
                        onRectChanged = { rect -> onPieceRectChanged(piece.id, rect) },
                        onStartDrag = { offset -> onStartDrag(piece, offset) },
                        onDrag = onDrag,
                        onEndDrag = onEndDrag,
                        onCancelDrag = onCancelDrag,
                    )
                }
                repeat((3 - pieces.size).coerceAtLeast(0)) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TrayPieceCard(
    piece: Piece,
    hidden: Boolean,
    cardHeight: androidx.compose.ui.unit.Dp,
    pieceCellSize: androidx.compose.ui.unit.Dp,
    onRectChanged: (Rect) -> Unit,
    onStartDrag: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onEndDrag: () -> Unit,
    onCancelDrag: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnStartDrag by rememberUpdatedState(onStartDrag)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnEndDrag by rememberUpdatedState(onEndDrag)
    val currentOnCancelDrag by rememberUpdatedState(onCancelDrag)

    val uiColors = BlockGamesThemeTokens.uiColors
    Card(
        modifier = modifier
            .height(cardHeight)
            .onGloballyPositioned { onRectChanged(it.boundsInRoot()) }
            .graphicsLayer { alpha = if (hidden) 0f else 1f }
            .pointerInput(piece.id) {
                detectDragGestures(
                    onDragStart = { offset -> currentOnStartDrag(offset) },
                    onDragEnd = { currentOnEndDrag() },
                    onDragCancel = { currentOnCancelDrag() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        currentOnDrag(dragAmount)
                    },
                )
            },
        shape = RoundedCornerShape(GameUiShapeTokens.surfaceCorner),
        colors = CardDefaults.cardColors(containerColor = uiColors.metricCard.copy(alpha = 0.88f)),
        border = BorderStroke(1.dp, uiColors.panelStroke.copy(alpha = 0.7f)),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            PieceBlocks(piece = piece, cellSize = pieceCellSize)
        }
    }
}

internal fun freePlacementDragTopLeft(
    pointerInHost: Offset,
    piece: Piece,
    cellSizePx: Float,
    liftPx: Float = FreePlacementDragLiftPx,
): Offset? {
    if (cellSizePx <= 0f) return null
    val pieceWidthPx = piece.width * cellSizePx
    return Offset(
        x = pointerInHost.x - (pieceWidthPx / 2f),
        y = pointerInHost.y - liftPx,
    )
}

internal fun resolveNearestFreePlacementPreview(
    pieceId: Long,
    piece: Piece?,
    overlayTopLeft: Offset?,
    boardRect: Rect,
    cellSizePx: Float,
    config: GameConfig,
    requestPreview: (Long, GridPoint) -> PlacementPreview?,
): PlacementPreview? {
    val resolvedPiece = piece ?: return null
    val resolvedOverlayTopLeft = overlayTopLeft ?: return null
    if (boardRect == Rect.Zero || cellSizePx <= 0f) return null
    val maxColumn = config.columns - resolvedPiece.width
    val maxRow = config.rows - resolvedPiece.height
    if (maxColumn < 0 || maxRow < 0) return null
    val overlayCellRects = resolvedPiece.cellRects(
        topLeft = resolvedOverlayTopLeft,
        cellSizePx = cellSizePx,
    )
    if (overlayCellRects.none { overlapArea(it, boardRect) > 0f }) {
        return null
    }
    val candidates = buildList {
        for (row in 0..maxRow) {
            for (column in 0..maxColumn) {
                val origin = GridPoint(column = column, row = row)
                requestPreview(pieceId, origin)?.let(::add)
            }
        }
    }.mapNotNull { preview ->
        val candidateTopLeft = preview.landingAnchor.toFreePlacementTopLeft(boardRect, cellSizePx)
        val candidateCellRects = resolvedPiece.cellRects(
            topLeft = candidateTopLeft,
            cellSizePx = cellSizePx,
        )
        val overlap = overlapArea(
            first = overlayCellRects,
            second = candidateCellRects,
        )
        if (overlap <= 0f) {
            null
        } else {
            FreePlacementPreviewCandidate(
                preview = preview,
                overlapBlockCount = overlapBlockCount(
                    overlayCellRects = overlayCellRects,
                    candidateCellRects = candidateCellRects,
                ),
                overlapArea = overlap,
                proximityScore = -squaredDistance(candidateTopLeft, resolvedOverlayTopLeft),
            )
        }
    }

    val bestCandidate = candidates.maxWithOrNull(
        compareBy<FreePlacementPreviewCandidate> { it.overlapBlockCount }
            .thenBy { it.overlapArea }
            .thenBy { it.proximityScore }
    ) ?: return null

    val totalPieceArea = resolvedPiece.cells.size * cellSizePx * cellSizePx
    return if (bestCandidate.overlapArea >= totalPieceArea * 0.5f) {
        bestCandidate.preview
    } else {
        null
    }
}

private fun initialDragPointerPosition(
    pieceRectInRoot: Rect,
    pointerOffsetInPieceCard: Offset,
    hostRectInRoot: Rect,
): Offset? {
    if (hostRectInRoot == Rect.Zero || pieceRectInRoot == Rect.Zero) {
        return null
    }
    return Offset(
        x = pieceRectInRoot.left + pointerOffsetInPieceCard.x - hostRectInRoot.left,
        y = pieceRectInRoot.top + pointerOffsetInPieceCard.y - hostRectInRoot.top,
    )
}

internal fun GridPoint.toFreePlacementTopLeft(
    boardRect: Rect,
    cellSizePx: Float,
): Offset = Offset(
    x = boardRect.left + (column * cellSizePx),
    y = boardRect.top + (row * cellSizePx),
)

private fun overlapArea(
    first: Rect,
    second: Rect,
): Float {
    val overlapWidth = (minOf(first.right, second.right) - maxOf(first.left, second.left)).coerceAtLeast(0f)
    val overlapHeight = (minOf(first.bottom, second.bottom) - maxOf(first.top, second.top)).coerceAtLeast(0f)
    return overlapWidth * overlapHeight
}

private fun overlapArea(
    first: List<Rect>,
    second: List<Rect>,
): Float = first.sumOf { firstRect ->
    second.sumOf { secondRect ->
        overlapArea(firstRect, secondRect).toDouble()
    }
}.toFloat()

private fun overlapBlockCount(
    overlayCellRects: List<Rect>,
    candidateCellRects: List<Rect>,
): Int = overlayCellRects.count { overlayRect ->
    candidateCellRects.any { candidateRect -> overlapArea(overlayRect, candidateRect) > 0f }
}

private fun squaredDistance(
    first: Offset,
    second: Offset,
): Float {
    val dx = first.x - second.x
    val dy = first.y - second.y
    return (dx * dx) + (dy * dy)
}

private fun Rect.toLocalRect(hostRect: Rect): Rect {
    if (this == Rect.Zero || hostRect == Rect.Zero) return Rect.Zero
    return Rect(
        left = left - hostRect.left,
        top = top - hostRect.top,
        right = right - hostRect.left,
        bottom = bottom - hostRect.top,
    )
}

private fun Piece.cellRects(
    topLeft: Offset,
    cellSizePx: Float,
): List<Rect> = cells.map { cell ->
    Rect(
        left = topLeft.x + (cell.column * cellSizePx),
        top = topLeft.y + (cell.row * cellSizePx),
        right = topLeft.x + ((cell.column + 1) * cellSizePx),
        bottom = topLeft.y + ((cell.row + 1) * cellSizePx),
    )
}

private data class FreePlacementPreviewCandidate(
    val preview: PlacementPreview,
    val overlapBlockCount: Int,
    val overlapArea: Float,
    val proximityScore: Float,
)



