package com.ugurbuga.stackshift.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ugurbuga.stackshift.StackShiftTheme
import com.ugurbuga.stackshift.game.model.BoardMatrix
import com.ugurbuga.stackshift.game.model.CellTone
import com.ugurbuga.stackshift.game.model.ComboState
import com.ugurbuga.stackshift.game.model.FeedbackEmphasis
import com.ugurbuga.stackshift.game.model.GameConfig
import com.ugurbuga.stackshift.game.model.GameState
import com.ugurbuga.stackshift.game.model.GameStatus
import com.ugurbuga.stackshift.game.model.GameText
import com.ugurbuga.stackshift.game.model.GameTextKey
import com.ugurbuga.stackshift.game.model.GridPoint
import com.ugurbuga.stackshift.game.model.Piece
import com.ugurbuga.stackshift.game.model.PieceKind
import com.ugurbuga.stackshift.game.model.PlacementPreview
import com.ugurbuga.stackshift.game.model.PressureLevel
import com.ugurbuga.stackshift.game.model.SpecialBlockType
import com.ugurbuga.stackshift.game.model.gameText
import com.ugurbuga.stackshift.platform.feedback.GameHaptic
import com.ugurbuga.stackshift.platform.feedback.GameHaptics
import com.ugurbuga.stackshift.platform.feedback.GameSound
import com.ugurbuga.stackshift.platform.feedback.NoOpGameHaptics
import com.ugurbuga.stackshift.platform.feedback.NoOpSoundEffectPlayer
import com.ugurbuga.stackshift.platform.feedback.SoundEffectPlayer
import com.ugurbuga.stackshift.presentation.game.GameViewModel
import com.ugurbuga.stackshift.presentation.game.InteractionFeedback
import com.ugurbuga.stackshift.settings.AppSettings
import com.ugurbuga.stackshift.settings.HighScoreStorage
import com.ugurbuga.stackshift.ui.theme.StackShiftThemeTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import stackshift.composeapp.generated.resources.Res
import stackshift.composeapp.generated.resources.app_title
import stackshift.composeapp.generated.resources.block_properties_column_clearer_desc
import stackshift.composeapp.generated.resources.block_properties_ghost_desc
import stackshift.composeapp.generated.resources.block_properties_heavy_desc
import stackshift.composeapp.generated.resources.block_properties_row_clearer_desc
import stackshift.composeapp.generated.resources.block_properties_title
import stackshift.composeapp.generated.resources.boost
import stackshift.composeapp.generated.resources.continue_label
import stackshift.composeapp.generated.resources.danger
import stackshift.composeapp.generated.resources.danger_none
import stackshift.composeapp.generated.resources.feedback_chain
import stackshift.composeapp.generated.resources.feedback_clear
import stackshift.composeapp.generated.resources.feedback_hold_armed
import stackshift.composeapp.generated.resources.feedback_micro_adjust
import stackshift.composeapp.generated.resources.feedback_overflow
import stackshift.composeapp.generated.resources.feedback_perfect
import stackshift.composeapp.generated.resources.feedback_perfect_lane
import stackshift.composeapp.generated.resources.feedback_score_only
import stackshift.composeapp.generated.resources.feedback_soft_lock
import stackshift.composeapp.generated.resources.feedback_special
import stackshift.composeapp.generated.resources.feedback_special_chain
import stackshift.composeapp.generated.resources.feedback_swap
import stackshift.composeapp.generated.resources.game_message_chain_lines
import stackshift.composeapp.generated.resources.game_message_good_shot
import stackshift.composeapp.generated.resources.game_message_hold_updated
import stackshift.composeapp.generated.resources.game_message_lines_cleared
import stackshift.composeapp.generated.resources.game_message_no_opening
import stackshift.composeapp.generated.resources.game_message_overflow
import stackshift.composeapp.generated.resources.game_message_paused
import stackshift.composeapp.generated.resources.game_message_perfect_drop
import stackshift.composeapp.generated.resources.game_message_pressure_game_over
import stackshift.composeapp.generated.resources.game_message_resumed
import stackshift.composeapp.generated.resources.game_message_select_column
import stackshift.composeapp.generated.resources.game_message_soft_lock
import stackshift.composeapp.generated.resources.game_message_special_chain_board
import stackshift.composeapp.generated.resources.game_message_special_lines
import stackshift.composeapp.generated.resources.game_message_special_triggered
import stackshift.composeapp.generated.resources.game_message_tempo_critical
import stackshift.composeapp.generated.resources.game_message_tempo_up
import stackshift.composeapp.generated.resources.game_over_new_high_score
import stackshift.composeapp.generated.resources.game_over_title
import stackshift.composeapp.generated.resources.high_score
import stackshift.composeapp.generated.resources.hold
import stackshift.composeapp.generated.resources.launch
import stackshift.composeapp.generated.resources.launch_bar
import stackshift.composeapp.generated.resources.launch_boost_active
import stackshift.composeapp.generated.resources.launch_chain_message
import stackshift.composeapp.generated.resources.launch_drag_hint
import stackshift.composeapp.generated.resources.launch_game_over
import stackshift.composeapp.generated.resources.launch_paused
import stackshift.composeapp.generated.resources.launch_soft_lock_message
import stackshift.composeapp.generated.resources.launch_special_chance
import stackshift.composeapp.generated.resources.lines
import stackshift.composeapp.generated.resources.pause
import stackshift.composeapp.generated.resources.pause_title
import stackshift.composeapp.generated.resources.piece_size_format
import stackshift.composeapp.generated.resources.play_again
import stackshift.composeapp.generated.resources.queue_empty
import stackshift.composeapp.generated.resources.queue_hold
import stackshift.composeapp.generated.resources.queue_next_short
import stackshift.composeapp.generated.resources.restart
import stackshift.composeapp.generated.resources.restart_cancel
import stackshift.composeapp.generated.resources.restart_confirm
import stackshift.composeapp.generated.resources.restart_confirm_body
import stackshift.composeapp.generated.resources.restart_confirm_title
import stackshift.composeapp.generated.resources.resume
import stackshift.composeapp.generated.resources.score
import stackshift.composeapp.generated.resources.settings_title
import stackshift.composeapp.generated.resources.special_column_clearer
import stackshift.composeapp.generated.resources.special_ghost
import stackshift.composeapp.generated.resources.special_heavy
import stackshift.composeapp.generated.resources.special_row_clearer
import kotlin.math.roundToInt

private const val LaunchAnimationMillis = 140L
private const val EntryAnimationMillis = 70L
private const val NextPieceScale = 0.5f

private val BottomDockHeight = 148.dp
private val TopBarVerticalPadding = 6.dp
private val TopBarRowSpacing = 4.dp
private val TopBarIconSpacing = 4.dp
private val TopBarActionIconSize = 28.dp

@Composable
private fun resolveActivePieceProperties(
    piece: Piece?,
): String {
    if (piece == null) return resolveGameText(gameText(GameTextKey.QueueEmpty))

    val size = stringResource(Res.string.piece_size_format, piece.width, piece.height)
    return buildList {
        add(piece.kind.name)
        add(size)
        if (piece.special != SpecialBlockType.None) {
            add(resolveGameText(piece.special.shortLabel()))
        }
    }.joinToString(separator = " • ")
}

@Composable
private fun resolveBlockDetail(
    piece: Piece?,
): String {
    val special = piece?.special ?: return resolveGameText(gameText(GameTextKey.QueueEmpty))
    return when (special) {
        SpecialBlockType.None -> ""
        SpecialBlockType.ColumnClearer -> stringResource(Res.string.block_properties_column_clearer_desc)
        SpecialBlockType.RowClearer -> stringResource(Res.string.block_properties_row_clearer_desc)
        SpecialBlockType.Ghost -> stringResource(Res.string.block_properties_ghost_desc)
        SpecialBlockType.Heavy -> stringResource(Res.string.block_properties_heavy_desc)
    }
}

@Composable
fun StackShiftGameApp(
    modifier: Modifier = Modifier,
    soundPlayer: SoundEffectPlayer = NoOpSoundEffectPlayer,
    viewModel: GameViewModel = remember { GameViewModel() },
    onOpenSettings: () -> Unit = {},
) {
    val haptics = rememberGameHaptics()
    val uiState by viewModel.uiState.collectAsState()

    var highestScore by remember { mutableIntStateOf(HighScoreStorage.load()) }
    var newHighScoreReached by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.gameState.score) {
        if (uiState.gameState.score > highestScore) {
            highestScore = uiState.gameState.score
            HighScoreStorage.save(highestScore)
            newHighScoreReached = true
        }
    }
    LaunchedEffect(
        uiState.gameState.status,
        uiState.gameState.score,
        uiState.gameState.linesCleared
    ) {
        if (uiState.gameState.status == GameStatus.Running && uiState.gameState.score == 0 && uiState.gameState.linesCleared == 0) {
            newHighScoreReached = false
        }
    }

    var showBlockProperties by remember { mutableStateOf(false) }

    if (showBlockProperties) {
        BlockPropertiesScreen(
            modifier = modifier,
            onBack = { showBlockProperties = false },
        )
    } else {
        GameScreen(
            modifier = modifier,
            gameState = uiState.gameState,
            onRequestPreview = viewModel::previewPlacement,
            onResolvePreviewImpact = viewModel::previewImpactPoints,
            onPlacePiece = viewModel::placePiece,
            onHoldPiece = viewModel::holdPiece,
            onPauseToggle = viewModel::togglePause,
            onRestart = { viewModel.restart(uiState.gameState.config) },
            onOpenSettings = onOpenSettings,
            onBlockProperties = { showBlockProperties = true },
            soundPlayer = soundPlayer,
            haptics = haptics,
            highestScore = highestScore,
            showNewHighScoreMessage = newHighScoreReached,
        )
    }
}

@Composable
fun GameScreen(
    gameState: GameState,
    onRequestPreview: (Int) -> PlacementPreview?,
    onResolvePreviewImpact: (PlacementPreview?) -> Set<GridPoint>,
    onPlacePiece: (Int) -> InteractionFeedback,
    onHoldPiece: () -> InteractionFeedback,
    onPauseToggle: () -> InteractionFeedback,
    onRestart: () -> InteractionFeedback,
    onOpenSettings: () -> Unit,
    onBlockProperties: () -> Unit,
    soundPlayer: SoundEffectPlayer,
    haptics: GameHaptics,
    highestScore: Int,
    showNewHighScoreMessage: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val colorScheme = MaterialTheme.colorScheme
    val uiColors = StackShiftThemeTokens.uiColors
    val updatedPreviewProvider by rememberUpdatedState(onRequestPreview)
    val updatedPreviewImpactProvider by rememberUpdatedState(onResolvePreviewImpact)
    val updatedPlacePiece by rememberUpdatedState(onPlacePiece)
    val updatedHoldPiece by rememberUpdatedState(onHoldPiece)
    val updatedPauseToggle by rememberUpdatedState(onPauseToggle)
    val updatedRestart by rememberUpdatedState(onRestart)
    var showRestartDialog by remember { mutableStateOf(false) }
    val screenShakeX = remember { Animatable(0f) }
    val screenShakeY = remember { Animatable(0f) }
    val impactFlashAlpha = remember { Animatable(0f) }
    val comboDriftY = remember { Animatable(18f) }
    val comboAlpha = remember { Animatable(0f) }

    var overlayHostRectInRoot by remember { mutableStateOf(Rect.Zero) }
    var boardRectInRoot by remember { mutableStateOf(Rect.Zero) }
    var trayRectInRoot by remember { mutableStateOf(Rect.Zero) }
    var overlayTopLeft by remember { mutableStateOf<Offset?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    var isLaunching by remember { mutableStateOf(false) }

    val activePiece = gameState.activePiece
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
    val spawnColumn by remember(
        activePiece?.id,
        gameState.lastPlacementColumn,
        gameState.config.columns
    ) {
        derivedStateOf {
            resolveSpawnColumn(
                piece = activePiece,
                boardColumns = gameState.config.columns,
                lastPlacementColumn = gameState.lastPlacementColumn,
            )
        }
    }
    val spawnTopLeft by remember(activePiece?.id, trayRect, boardRect, cellSizePx, spawnColumn) {
        derivedStateOf {
            gameState.softLock?.preview?.landingAnchor?.toTopLeft(
                boardRect = boardRect,
                cellSizePx = cellSizePx
            )
                ?: pieceSpawnTopLeft(
                    piece = activePiece,
                    trayRect = trayRect,
                    boardRect = boardRect,
                    cellSizePx = cellSizePx,
                    column = spawnColumn,
                )
        }
    }

    LaunchedEffect(activePiece?.id, spawnTopLeft) {
        if (spawnTopLeft != null) {
            overlayTopLeft = spawnTopLeft
            isDragging = false
            isLaunching = false
        }
    }

    val selectedColumn by remember(activePiece?.id, overlayTopLeft, boardRect, cellSizePx) {
        derivedStateOf {
            resolveSelectedColumn(
                piece = activePiece,
                overlayTopLeft = overlayTopLeft,
                boardRect = boardRect,
                cellSizePx = cellSizePx,
                boardColumns = gameState.config.columns,
            )
        }
    }

    val placementPreview by remember(
        selectedColumn,
        activePiece?.id,
        gameState.status,
        gameState.board
    ) {
        derivedStateOf {
            if (gameState.status != GameStatus.Running) return@derivedStateOf null
            if (gameState.softLock != null && !isDragging) {
                gameState.softLock.preview
            } else {
                selectedColumn?.let(updatedPreviewProvider)
            }
        }
    }

    val previewImpactPoints by remember(
        placementPreview,
        activePiece?.id,
        gameState.board,
        gameState.status
    ) {
        derivedStateOf {
            if (gameState.status != GameStatus.Running || placementPreview == null) {
                emptySet()
            } else {
                updatedPreviewImpactProvider(placementPreview)
            }
        }
    }

    val displayOverlayTopLeft by remember(
        activePiece?.id,
        overlayTopLeft,
        selectedColumn,
        boardRect,
        cellSizePx,
        isLaunching,
    ) {
        derivedStateOf {
            val current = overlayTopLeft ?: return@derivedStateOf null
            val snappedColumn = selectedColumn
            if (snappedColumn == null || boardRect == Rect.Zero || cellSizePx <= 0f || isLaunching) {
                return@derivedStateOf current
            }
            current.copy(x = columnToLeft(snappedColumn, boardRect, cellSizePx))
        }
    }

    val overlayX by animateFloatAsState(
        targetValue = displayOverlayTopLeft?.x ?: 0f,
        animationSpec = if (isDragging) snap() else tween(
            durationMillis = 180,
            easing = FastOutSlowInEasing
        ),
        label = "overlayX",
    )
    val overlayY by animateFloatAsState(
        targetValue = displayOverlayTopLeft?.y ?: 0f,
        animationSpec = if (isDragging) snap() else tween(
            durationMillis = 180,
            easing = FastOutSlowInEasing
        ),
        label = "overlayY",
    )
    val overlayScale by animateFloatAsState(
        targetValue = when {
            isLaunching -> 1.06f
            gameState.isSoftLockActive -> 0.98f
            else -> 1f
        },
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "overlayScale",
    )

    LaunchedEffect(gameState.screenShakeToken) {
        if (gameState.screenShakeToken == 0L) return@LaunchedEffect
        screenShakeX.snapTo(0f)
        screenShakeY.snapTo(0f)
        listOf(14f, -10f, 8f, -6f, 3f, 0f).forEachIndexed { index, value ->
            screenShakeX.animateTo(value, animationSpec = tween(durationMillis = 34))
            screenShakeY.animateTo(
                if (index % 2 == 0) 3f else -2f,
                animationSpec = tween(durationMillis = 34)
            )
        }
        screenShakeY.animateTo(0f, animationSpec = tween(durationMillis = 40))
    }

    LaunchedEffect(gameState.impactFlashToken) {
        if (gameState.impactFlashToken == 0L) return@LaunchedEffect
        impactFlashAlpha.snapTo(0.22f)
        impactFlashAlpha.animateTo(
            0f,
            animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(gameState.comboPopupToken, gameState.floatingFeedback?.token) {
        if (gameState.floatingFeedback == null || gameState.comboPopupToken == 0L) return@LaunchedEffect
        comboDriftY.snapTo(26f)
        comboAlpha.snapTo(0f)
        comboAlpha.animateTo(1f, animationSpec = tween(durationMillis = 120))
        launch {
            comboDriftY.animateTo(
                -22f,
                animationSpec = tween(durationMillis = 760, easing = FastOutSlowInEasing)
            )
        }
        comboAlpha.animateTo(
            0f,
            animationSpec = tween(durationMillis = 760, easing = FastOutSlowInEasing)
        )
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = colorScheme.background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    translationX = screenShakeX.value,
                    translationY = screenShakeY.value,
                )
                .background(uiColors.gameSurface)
                .safeDrawingPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coordinates ->
                        overlayHostRectInRoot = coordinates.boundsInRoot()
                    },
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MinimalTopBar(
                        gameState = gameState,
                        highestScore = highestScore,
                        onHoldPiece = {
                            dispatchFeedback(
                                updatedHoldPiece(),
                                soundPlayer,
                                haptics
                            )
                        },
                        onPauseToggle = {
                            dispatchFeedback(
                                updatedPauseToggle(),
                                soundPlayer,
                                haptics
                            )
                        },
                        onRestart = { showRestartDialog = true },
                        onOpenSettings = onOpenSettings,
                        onBlockProperties = onBlockProperties,
                    )

                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        val boardRatio = gameState.config.columns / gameState.config.rows.toFloat()
                        val boardWidth = when {
                            maxWidth == 0.dp || maxHeight == 0.dp -> 0.dp
                            maxWidth / maxHeight > boardRatio -> maxHeight * boardRatio
                            else -> maxWidth
                        }
                        val boardHeight = if (boardRatio > 0f) boardWidth / boardRatio else 0.dp

                        if (boardWidth > 0.dp && boardHeight > 0.dp) {
                            BoardGrid(
                                modifier = Modifier
                                    .size(width = boardWidth, height = boardHeight)
                                    .onGloballyPositioned { coordinates ->
                                        boardRectInRoot = coordinates.boundsInRoot()
                                    },
                                gameState = gameState,
                                preview = placementPreview,
                                impactedPreviewCells = previewImpactPoints,
                                activeColumn = selectedColumn,
                                activePiece = activePiece,
                                isColumnValid = placementPreview != null,
                                isDragging = isDragging,
                            )
                        }
                    }

                    MinimalBottomDock(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(BottomDockHeight)
                            .onGloballyPositioned { coordinates ->
                                trayRectInRoot = coordinates.boundsInRoot()
                            },
                        gameState = gameState,
                        cellSizePx = cellSizePx,
                    )
                }

                gameState.floatingFeedback?.let { floatingFeedback ->
                    FloatingFeedbackBubble(
                        text = resolveGameText(floatingFeedback.text),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 96.dp),
                        isBonus = floatingFeedback.emphasis != FeedbackEmphasis.Info,
                        alpha = comboAlpha.value,
                        driftY = comboDriftY.value,
                    )
                }

                if (impactFlashAlpha.value > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = impactFlashAlpha.value)),
                    )
                }

                if (activePiece != null && overlayTopLeft != null && cellSizePx > 0f) {
                    val pieceCellDp = with(density) { cellSizePx.toDp() }
                    PieceBlocks(
                        piece = activePiece,
                        cellSize = pieceCellDp,
                        modifier = Modifier
                            .graphicsLayer(
                                translationX = overlayX,
                                translationY = overlayY,
                                scaleX = overlayScale,
                                scaleY = overlayScale,
                                transformOrigin = TransformOrigin(0f, 0f),
                            )
                            .pointerInput(
                                activePiece.id,
                                gameState.status,
                                isLaunching,
                                boardRect,
                                cellSizePx,
                            ) {
                                detectDragGestures(
                                    onDragStart = {
                                        if (gameState.status != GameStatus.Running || isLaunching) return@detectDragGestures
                                        isDragging = true
                                        dispatchFeedback(
                                            InteractionFeedback(
                                                sounds = setOf(GameSound.Grab),
                                                haptics = setOf(GameHaptic.Light),
                                            ),
                                            soundPlayer = soundPlayer,
                                            haptics = haptics,
                                        )
                                    },
                                    onDrag = { change, dragAmount ->
                                        if (gameState.status != GameStatus.Running || isLaunching) return@detectDragGestures
                                        change.consume()
                                        val current = overlayTopLeft ?: return@detectDragGestures
                                        overlayTopLeft = current.copy(x = current.x + dragAmount.x)
                                    },
                                    onDragEnd = {
                                        if (gameState.status != GameStatus.Running || isLaunching) return@detectDragGestures
                                        isDragging = false

                                        val preview = placementPreview
                                        val column = selectedColumn
                                        val currentSpawn = spawnTopLeft
                                        if (preview == null || column == null || currentSpawn == null) {
                                            overlayTopLeft = currentSpawn
                                            dispatchFeedback(
                                                InteractionFeedback(
                                                    sounds = setOf(GameSound.DropInvalid),
                                                    haptics = setOf(GameHaptic.Warning),
                                                ),
                                                soundPlayer = soundPlayer,
                                                haptics = haptics,
                                            )
                                            return@detectDragGestures
                                        }

                                        if (gameState.softLock != null) {
                                            overlayTopLeft = preview.landingAnchor.toTopLeft(
                                                boardRect = boardRect,
                                                cellSizePx = cellSizePx
                                            )
                                            val feedback = updatedPlacePiece(column)
                                            dispatchFeedback(feedback, soundPlayer, haptics)
                                        } else {
                                            isLaunching = true
                                            overlayTopLeft = preview.entryAnchor.toTopLeft(
                                                boardRect = boardRect,
                                                cellSizePx = cellSizePx
                                            )
                                            coroutineScope.launch {
                                                delay(EntryAnimationMillis)
                                                overlayTopLeft = preview.landingAnchor.toTopLeft(
                                                    boardRect = boardRect,
                                                    cellSizePx = cellSizePx
                                                )
                                                delay(LaunchAnimationMillis)
                                                val feedback = updatedPlacePiece(column)
                                                dispatchFeedback(feedback, soundPlayer, haptics)
                                                isLaunching = false
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        isDragging = false
                                        spawnTopLeft?.let { overlayTopLeft = it }
                                    },
                                )
                            },
                        alpha = when {
                            gameState.status == GameStatus.Paused -> 0.42f
                            isLaunching -> 0.92f
                            else -> 1f
                        },
                    )
                }
            }

            if (gameState.status != GameStatus.Running) {
                PauseOverlay(
                    gameState = gameState,
                    showNewHighScoreMessage = showNewHighScoreMessage,
                    onPrimaryAction = {
                        if (gameState.status == GameStatus.GameOver) {
                            dispatchFeedback(updatedRestart(), soundPlayer, haptics)
                        } else {
                            dispatchFeedback(updatedPauseToggle(), soundPlayer, haptics)
                        }
                    },
                )
            }

            if (showRestartDialog) {
                AlertDialog(
                    onDismissRequest = { showRestartDialog = false },
                    title = {
                        Text(resolveGameText(gameText(GameTextKey.RestartConfirmTitle)))
                    },
                    text = {
                        Text(resolveGameText(gameText(GameTextKey.RestartConfirmBody)))
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showRestartDialog = false
                                dispatchFeedback(updatedRestart(), soundPlayer, haptics)
                            },
                        ) {
                            Text(resolveGameText(gameText(GameTextKey.RestartConfirm)))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRestartDialog = false }) {
                            Text(resolveGameText(gameText(GameTextKey.RestartCancel)))
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun MinimalTopBar(
    gameState: GameState,
    highestScore: Int,
    onHoldPiece: () -> Unit,
    onPauseToggle: () -> Unit,
    onRestart: () -> Unit,
    onOpenSettings: () -> Unit,
    onBlockProperties: () -> Unit,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    val pressureCount =
        gameState.columnPressure.count { it.level == PressureLevel.Critical || it.level == PressureLevel.Overflow }
    val holdLabel = resolveGameText(gameText(GameTextKey.Hold))
    val pauseLabel =
        resolveGameText(gameText(if (gameState.status == GameStatus.Paused) GameTextKey.Resume else GameTextKey.Pause))
    val restartLabel = resolveGameText(gameText(GameTextKey.Restart))
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = uiColors.panel),
        border = androidx.compose.foundation.BorderStroke(1.dp, uiColors.panelStroke),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = TopBarVerticalPadding),
            verticalArrangement = Arrangement.spacedBy(TopBarRowSpacing),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    Text(
                        text = resolveGameText(gameText(GameTextKey.AppTitle)),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = resolveGameText(gameState.message),
                        style = MaterialTheme.typography.labelSmall,
                        color = uiColors.subtitle,
                        minLines = 2,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(TopBarIconSpacing)) {
                    CompactActionIconButton(
                        icon = Icons.Filled.Settings,
                        contentDescription = stringResource(Res.string.settings_title),
                        onClick = onOpenSettings,
                    )
                    CompactActionIconButton(
                        icon = Icons.Filled.ViewModule,
                        contentDescription = stringResource(Res.string.block_properties_title),
                        onClick = onBlockProperties,
                    )
                    CompactActionIconButton(
                        icon = Icons.Filled.SwapHoriz,
                        contentDescription = holdLabel,
                        onClick = onHoldPiece,
                        enabled = gameState.canHold && !gameState.isSoftLockActive,
                    )
                    CompactActionIconButton(
                        icon = if (gameState.status == GameStatus.Paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        contentDescription = pauseLabel,
                        onClick = onPauseToggle,
                    )
                    CompactActionIconButton(
                        icon = Icons.Filled.Refresh,
                        contentDescription = restartLabel,
                        onClick = onRestart,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                MetricChip(
                    title = resolveGameText(gameText(GameTextKey.Score)),
                    value = gameState.score.toString(),
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    title = resolveGameText(gameText(GameTextKey.HighScore)),
                    value = highestScore.toString(),
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    title = resolveGameText(gameText(GameTextKey.Lines)),
                    value = gameState.linesCleared.toString(),
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    title = resolveGameText(gameText(GameTextKey.Danger)),
                    value = if (pressureCount == 0) resolveGameText(gameText(GameTextKey.DangerNone)) else pressureCount.toString(),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MinimalBottomDock(
    gameState: GameState,
    cellSizePx: Float,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val uiColors = StackShiftThemeTokens.uiColors
    val queuePieces = gameState.nextQueue.take(1)
    val railWidth = with(density) {
        (((gameState.activePiece?.width ?: 3) + 1) * cellSizePx)
            .coerceAtLeast(88f)
            .toDp()
    }
    val dockMessage = resolveActivePieceProperties(piece = gameState.activePiece)
    val dockDetail = resolveBlockDetail(piece = gameState.activePiece)
    val dockMessageColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f)
    val dockDetailColor = uiColors.subtitle

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = uiColors.panel),
        border = androidx.compose.foundation.BorderStroke(1.dp, uiColors.panelStroke),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(min = railWidth)
                    .fillMaxHeight()
                    .padding(start = 10.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(uiColors.panelMuted)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = resolveGameText(gameText(GameTextKey.LaunchBar)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                )
                LaunchBarView(gameState = gameState)

                Text(
                    text = dockMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = dockMessageColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                )
                if (dockDetail.isNotBlank()) {
                    Text(
                        text = dockDetail,
                        style = MaterialTheme.typography.labelSmall,
                        color = dockDetailColor,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .weight(0.5f)
                    .fillMaxHeight()
                    .padding(end = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                }

                HoldAndQueueStrip(
                    queue = queuePieces,
                    cellSizePx = cellSizePx,
                )
            }
        }
    }
}

@Composable
private fun PauseOverlay(
    gameState: GameState,
    showNewHighScoreMessage: Boolean,
    onPrimaryAction: () -> Unit,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(uiColors.overlay),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = uiColors.panel),
            border = androidx.compose.foundation.BorderStroke(1.dp, uiColors.panelStroke),
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                uiColors.dialogStart,
                                uiColors.dialogEnd,
                            ),
                        ),
                    )
                    .padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = resolveGameText(gameText(if (gameState.status == GameStatus.GameOver) GameTextKey.GameOverTitle else GameTextKey.PauseTitle)),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = resolveGameText(gameState.message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = uiColors.subtitle,
                    textAlign = TextAlign.Center,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (gameState.status == GameStatus.GameOver && showNewHighScoreMessage) {
                    Text(
                        text = resolveGameText(gameText(GameTextKey.GameOverNewHighScore)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Button(
                    onClick = onPrimaryAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = uiColors.actionButton,
                        contentColor = uiColors.actionIcon,
                    ),
                ) {
                    Text(resolveGameText(gameText(if (gameState.status == GameStatus.GameOver) GameTextKey.PlayAgain else GameTextKey.Continue)))
                }
            }
        }
    }
}

@Composable
private fun MetricChip(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = uiColors.metricCard),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            uiColors.panelStroke.copy(alpha = 0.72f)
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = uiColors.subtitle,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun CompactActionIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = if (enabled) uiColors.actionIcon else uiColors.actionIconDisabled,
        modifier = Modifier
            .size(TopBarActionIconSize)
            .clip(RoundedCornerShape(10.dp))
            .clickable {
                if (enabled) {
                    onClick.invoke()
                }
            }
            .background(if (enabled) uiColors.actionButton else uiColors.actionButtonDisabled)
            .padding(4.dp)
    )
}

@Composable
private fun LaunchBarView(gameState: GameState) {
    val uiColors = StackShiftThemeTokens.uiColors
    val progressPercent = (gameState.launchBar.progress * 100).roundToInt()
    val isBoostActive = gameState.launchBar.boostTurnsRemaining > 0
    val animatedProgress by animateFloatAsState(
        targetValue = gameState.launchBar.progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "launchBarProgress",
    )
    val boostGlow by animateFloatAsState(
        targetValue = if (gameState.launchBar.boostTurnsRemaining > 0) 1f else 0.35f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "launchBarGlow",
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(uiColors.launchTrack),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress.coerceAtLeast(if (animatedProgress > 0f) 0.08f else 0f))
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                uiColors.launchGlow.copy(alpha = boostGlow),
                                uiColors.success.copy(alpha = 0.92f),
                                uiColors.launchAccent,
                            ),
                        ),
                    ),
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (isBoostActive) resolveGameText(gameText(GameTextKey.Boost)) else resolveGameText(
                        gameText(GameTextKey.Launch)
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (isBoostActive) "x${gameState.launchBar.boostTurnsRemaining}" else "%$progressPercent",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }
        Text(
            text = if (isBoostActive) {
                resolveGameText(
                    gameText(
                        GameTextKey.LaunchBoostActive,
                        gameState.launchBar.boostTurnsRemaining
                    )
                )
            } else {
                resolveGameText(gameText(GameTextKey.LaunchSpecialChance, progressPercent))
            },
            style = MaterialTheme.typography.labelSmall,
            color = uiColors.subtitle,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HoldAndQueueStrip(
    queue: List<Piece>,
    cellSizePx: Float,
) {
    val density = LocalDensity.current
    val nextPiece = queue.firstOrNull()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        QueueSlot(
            title = resolveGameText(gameText(GameTextKey.QueueNextShort)),
            piece = nextPiece,
            cellSizePx = cellSizePx * NextPieceScale,
            density = density,
        )
    }
}

@Composable
private fun QueueSlot(
    title: String,
    piece: Piece?,
    cellSizePx: Float,
    density: androidx.compose.ui.unit.Density,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = uiColors.panelMuted),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (title.isNotBlank()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = uiColors.subtitle,
                )
            }
            if (piece != null) {
                PieceBlocks(
                    piece = piece,
                    cellSize = with(density) { (cellSizePx.coerceAtLeast(1f)).toDp() },
                    alpha = 0.92f,
                )
            } else {
                Text(
                    resolveGameText(gameText(GameTextKey.QueueEmpty)),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            if (piece?.special != null && piece.special != SpecialBlockType.None) {
                Text(
                    text = resolveGameText(piece.special.shortLabel()),
                    style = MaterialTheme.typography.labelSmall,
                    color = uiColors.warning,
                )
            }
        }
    }
}

@Composable
private fun FloatingFeedbackBubble(
    text: String,
    modifier: Modifier = Modifier,
    isBonus: Boolean,
    alpha: Float,
    driftY: Float,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    val pulse by animateFloatAsState(
        targetValue = if (isBonus) 1f else 0.92f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "feedbackPulse",
    )
    Card(
        modifier = modifier.graphicsLayer(
            scaleX = pulse,
            scaleY = pulse,
            alpha = alpha.coerceIn(0f, 1f),
            translationY = driftY,
        ),
        shape = RoundedCornerShape(999.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isBonus) uiColors.panelHighlight else uiColors.panel,
        ),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = if (isBonus) uiColors.success else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun resolveGameText(text: GameText): String {
    val resource = text.key.stringResourceId()
    return if (text.args.isEmpty()) {
        stringResource(resource)
    } else {
        stringResource(resource, *text.args.toTypedArray())
    }
}

private fun SpecialBlockType.shortLabel(): GameText = when (this) {
    SpecialBlockType.None -> gameText(GameTextKey.QueueEmpty)
    SpecialBlockType.ColumnClearer -> gameText(GameTextKey.SpecialColumnClearer)
    SpecialBlockType.RowClearer -> gameText(GameTextKey.SpecialRowClearer)
    SpecialBlockType.Ghost -> gameText(GameTextKey.SpecialGhost)
    SpecialBlockType.Heavy -> gameText(GameTextKey.SpecialHeavy)
}


private fun GameTextKey.stringResourceId(): StringResource = when (this) {
    GameTextKey.AppTitle -> Res.string.app_title
    GameTextKey.Hold -> Res.string.hold
    GameTextKey.Pause -> Res.string.pause
    GameTextKey.Resume -> Res.string.resume
    GameTextKey.Restart -> Res.string.restart
    GameTextKey.RestartConfirmTitle -> Res.string.restart_confirm_title
    GameTextKey.RestartConfirmBody -> Res.string.restart_confirm_body
    GameTextKey.RestartConfirm -> Res.string.restart_confirm
    GameTextKey.RestartCancel -> Res.string.restart_cancel
    GameTextKey.Score -> Res.string.score
    GameTextKey.HighScore -> Res.string.high_score
    GameTextKey.GameOverNewHighScore -> Res.string.game_over_new_high_score
    GameTextKey.Lines -> Res.string.lines
    GameTextKey.Boost -> Res.string.boost
    GameTextKey.Danger -> Res.string.danger
    GameTextKey.DangerNone -> Res.string.danger_none
    GameTextKey.Launch -> Res.string.launch
    GameTextKey.LaunchBar -> Res.string.launch_bar
    GameTextKey.LaunchBoostActive -> Res.string.launch_boost_active
    GameTextKey.LaunchSpecialChance -> Res.string.launch_special_chance
    GameTextKey.LaunchSoftLockMessage -> Res.string.launch_soft_lock_message
    GameTextKey.LaunchChainMessage -> Res.string.launch_chain_message
    GameTextKey.LaunchPaused -> Res.string.launch_paused
    GameTextKey.LaunchGameOver -> Res.string.launch_game_over
    GameTextKey.LaunchDragHint -> Res.string.launch_drag_hint
    GameTextKey.QueueHold -> Res.string.queue_hold
    GameTextKey.QueueNextShort -> Res.string.queue_next_short
    GameTextKey.QueueEmpty -> Res.string.queue_empty
    GameTextKey.PauseTitle -> Res.string.pause_title
    GameTextKey.GameOverTitle -> Res.string.game_over_title
    GameTextKey.Continue -> Res.string.continue_label
    GameTextKey.PlayAgain -> Res.string.play_again
    GameTextKey.GameMessageSelectColumn -> Res.string.game_message_select_column
    GameTextKey.GameMessageNoOpening -> Res.string.game_message_no_opening
    GameTextKey.GameMessageSoftLock -> Res.string.game_message_soft_lock
    GameTextKey.GameMessageOverflow -> Res.string.game_message_overflow
    GameTextKey.GameMessageSpecialChainBoard -> Res.string.game_message_special_chain_board
    GameTextKey.GameMessagePressureGameOver -> Res.string.game_message_pressure_game_over
    GameTextKey.GameMessageSpecialLines -> Res.string.game_message_special_lines
    GameTextKey.GameMessageSpecialTriggered -> Res.string.game_message_special_triggered
    GameTextKey.GameMessagePerfectDrop -> Res.string.game_message_perfect_drop
    GameTextKey.GameMessageChainLines -> Res.string.game_message_chain_lines
    GameTextKey.GameMessageLinesCleared -> Res.string.game_message_lines_cleared
    GameTextKey.GameMessageGoodShot -> Res.string.game_message_good_shot
    GameTextKey.GameMessageHoldUpdated -> Res.string.game_message_hold_updated
    GameTextKey.GameMessageTempoCritical -> Res.string.game_message_tempo_critical
    GameTextKey.GameMessageTempoUp -> Res.string.game_message_tempo_up
    GameTextKey.GameMessagePaused -> Res.string.game_message_paused
    GameTextKey.GameMessageResumed -> Res.string.game_message_resumed
    GameTextKey.FeedbackOverflow -> Res.string.feedback_overflow
    GameTextKey.FeedbackPerfectLane -> Res.string.feedback_perfect_lane
    GameTextKey.FeedbackMicroAdjust -> Res.string.feedback_micro_adjust
    GameTextKey.FeedbackSoftLock -> Res.string.feedback_soft_lock
    GameTextKey.FeedbackHoldArmed -> Res.string.feedback_hold_armed
    GameTextKey.FeedbackSwap -> Res.string.feedback_swap
    GameTextKey.FeedbackSpecialChain -> Res.string.feedback_special_chain
    GameTextKey.FeedbackSpecial -> Res.string.feedback_special
    GameTextKey.FeedbackPerfect -> Res.string.feedback_perfect
    GameTextKey.FeedbackChain -> Res.string.feedback_chain
    GameTextKey.FeedbackClear -> Res.string.feedback_clear
    GameTextKey.FeedbackScoreOnly -> Res.string.feedback_score_only
    GameTextKey.SpecialColumnClearer -> Res.string.special_column_clearer
    GameTextKey.SpecialRowClearer -> Res.string.special_row_clearer
    GameTextKey.SpecialGhost -> Res.string.special_ghost
    GameTextKey.SpecialHeavy -> Res.string.special_heavy
}

@Composable
private fun rememberGameHaptics(): GameHaptics {
    val hapticFeedback = LocalHapticFeedback.current
    return remember(hapticFeedback) {
        object : GameHaptics {
            override fun perform(effect: GameHaptic) {
                when (effect) {
                    GameHaptic.Light -> hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    GameHaptic.Success -> hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    GameHaptic.Warning -> hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }
        }
    }
}

private fun dispatchFeedback(
    feedback: InteractionFeedback,
    soundPlayer: SoundEffectPlayer,
    haptics: GameHaptics,
) {
    feedback.sounds.forEach(soundPlayer::play)
    feedback.haptics.forEach(haptics::perform)
}

private fun resolveSelectedColumn(
    piece: Piece?,
    overlayTopLeft: Offset?,
    boardRect: Rect,
    cellSizePx: Float,
    boardColumns: Int,
): Int? {
    if (piece == null || overlayTopLeft == null || boardRect == Rect.Zero || cellSizePx <= 0f) return null
    val maxColumn = boardColumns - piece.width
    if (maxColumn < 0) return null
    val approximateColumn = ((overlayTopLeft.x - boardRect.left) / cellSizePx).roundToInt()
    return approximateColumn.coerceIn(0, maxColumn)
}

private fun pieceSpawnTopLeft(
    piece: Piece?,
    trayRect: Rect,
    boardRect: Rect,
    cellSizePx: Float,
    column: Int?,
): Offset? {
    if (piece == null || trayRect == Rect.Zero || boardRect == Rect.Zero || cellSizePx <= 0f || column == null) return null
    return Offset(
        x = columnToLeft(column, boardRect, cellSizePx),
        y = trayRect.center.y - (piece.height * cellSizePx) / 2f,
    )
}

private fun resolveSpawnColumn(
    piece: Piece?,
    boardColumns: Int,
    lastPlacementColumn: Int?,
): Int? {
    if (piece == null) return null
    val maxColumn = boardColumns - piece.width
    if (maxColumn < 0) return null
    return lastPlacementColumn?.coerceIn(0, maxColumn)
        ?: ((boardColumns - piece.width) / 2f).roundToInt().coerceIn(0, maxColumn)
}

private fun columnToLeft(
    column: Int,
    boardRect: Rect,
    cellSizePx: Float,
): Float = boardRect.left + (column * cellSizePx)

private fun Rect.toLocalRect(hostRect: Rect): Rect {
    if (this == Rect.Zero || hostRect == Rect.Zero) return Rect.Zero
    return Rect(
        left = left - hostRect.left,
        top = top - hostRect.top,
        right = right - hostRect.left,
        bottom = bottom - hostRect.top,
    )
}

private fun GridPoint.toTopLeft(
    boardRect: Rect,
    cellSizePx: Float,
): Offset = Offset(
    x = boardRect.left + (column * cellSizePx),
    y = boardRect.top + (row * cellSizePx),
)

@Preview
@Composable
private fun GameScreenRunningPreview() {
    StackShiftTheme(settings = AppSettings()) {
        GameScreen(
            gameState = previewGameState(),
            onRequestPreview = { previewPlacementPreview() },
            onResolvePreviewImpact = { previewImpactPointsPreview() },
            onPlacePiece = { InteractionFeedback.None },
            onHoldPiece = { InteractionFeedback.None },
            onPauseToggle = { InteractionFeedback.None },
            onRestart = { InteractionFeedback.None },
            onOpenSettings = {},
            onBlockProperties = {},
            soundPlayer = NoOpSoundEffectPlayer,
            haptics = NoOpGameHaptics,
            highestScore = 1420,
        )
    }
}

@Preview
@Composable
private fun GameScreenPausedPreview() {
    StackShiftTheme(settings = AppSettings()) {
        GameScreen(
            gameState = previewGameState(status = GameStatus.Paused),
            onRequestPreview = { previewPlacementPreview() },
            onResolvePreviewImpact = { emptySet() },
            onPlacePiece = { InteractionFeedback.None },
            onHoldPiece = { InteractionFeedback.None },
            onPauseToggle = { InteractionFeedback.None },
            onRestart = { InteractionFeedback.None },
            onOpenSettings = {},
            onBlockProperties = {},
            soundPlayer = NoOpSoundEffectPlayer,
            haptics = NoOpGameHaptics,
            highestScore = 1420,
        )
    }
}

@Preview
@Composable
private fun GameScreenGameOverPreview() {
    StackShiftTheme(settings = AppSettings()) {
        GameScreen(
            gameState = previewGameState(status = GameStatus.GameOver),
            onRequestPreview = { previewPlacementPreview() },
            onResolvePreviewImpact = { emptySet() },
            onPlacePiece = { InteractionFeedback.None },
            onHoldPiece = { InteractionFeedback.None },
            onPauseToggle = { InteractionFeedback.None },
            onRestart = { InteractionFeedback.None },
            onOpenSettings = {},
            onBlockProperties = {},
            soundPlayer = NoOpSoundEffectPlayer,
            haptics = NoOpGameHaptics,
            highestScore = 1420,
        )
    }
}

private fun previewGameState(status: GameStatus = GameStatus.Running): GameState {
    val config = GameConfig(columns = 10, rows = 16)
    val board = BoardMatrix.empty(columns = 10, rows = 16)
        .fill(
            points = listOf(
                GridPoint(0, 15),
                GridPoint(0, 14),
                GridPoint(1, 15),
                GridPoint(1, 14),
                GridPoint(2, 15),
            ),
            tone = CellTone.Blue,
        )
        .fill(
            points = listOf(
                GridPoint(2, 12),
                GridPoint(2, 11),
                GridPoint(3, 12),
                GridPoint(4, 12),
                GridPoint(5, 11),
                GridPoint(6, 10),
            ),
            tone = CellTone.Emerald,
        )
        .fill(
            points = listOf(
                GridPoint(2, 9),
                GridPoint(6, 9),
                GridPoint(7, 8),
            ),
            tone = CellTone.Gold,
        )
        .fill(
            points = listOf(
                GridPoint(8, 14),
                GridPoint(8, 13),
                GridPoint(9, 14),
            ),
            tone = CellTone.Coral,
        )
    return GameState(
        config = config,
        board = board,
        activePiece = Piece(
            id = 1,
            kind = PieceKind.T,
            tone = CellTone.Gold,
            cells = listOf(
                GridPoint(0, 0),
                GridPoint(1, 0),
                GridPoint(2, 0),
                GridPoint(1, 1),
            ),
            width = 3,
            height = 2,
            special = SpecialBlockType.RowClearer,
        ),
        nextQueue = listOf(
            Piece(
                id = 2,
                kind = PieceKind.Domino,
                tone = CellTone.Cyan,
                cells = listOf(GridPoint(0, 0), GridPoint(1, 0)),
                width = 2,
                height = 1,
                special = SpecialBlockType.Ghost,
            ),
            Piece(
                id = 3,
                kind = PieceKind.Square,
                tone = CellTone.Violet,
                cells = listOf(GridPoint(0, 0), GridPoint(1, 0), GridPoint(0, 1), GridPoint(1, 1)),
                width = 2,
                height = 2,
                special = SpecialBlockType.None,
            ),
            Piece(
                id = 4,
                kind = PieceKind.I,
                tone = CellTone.Amber,
                cells = listOf(GridPoint(0, 0), GridPoint(1, 0), GridPoint(2, 0), GridPoint(3, 0)),
                width = 4,
                height = 1,
                special = SpecialBlockType.Heavy,
            ),
        ),
        holdPiece = Piece(
            id = 5,
            kind = PieceKind.Domino,
            tone = CellTone.Blue,
            cells = listOf(GridPoint(0, 0), GridPoint(1, 0)),
            width = 2,
            height = 1,
            special = SpecialBlockType.ColumnClearer,
        ),
        canHold = true,
        score = 1420,
        lastMoveScore = 280,
        linesCleared = 12,
        level = 3,
        difficultyStage = 1,
        secondsUntilDifficultyIncrease = 9,
        combo = ComboState(chain = 2, best = 4),
        perfectDropStreak = 2,
        launchBar = com.ugurbuga.stackshift.game.model.LaunchBarState(
            progress = 0.76f,
            boostTurnsRemaining = 1,
            lastGain = 0.22f
        ),
        columnPressure = (0 until 10).map { column ->
            com.ugurbuga.stackshift.game.model.ColumnPressure(
                column = column,
                filledCells = when (column) {
                    4, 5 -> 13
                    7 -> 10
                    else -> 4
                },
                fillRatio = when (column) {
                    4, 5 -> 0.84f
                    7 -> 0.64f
                    else -> 0.25f
                },
                level = when (column) {
                    4, 5 -> PressureLevel.Critical
                    7 -> PressureLevel.Warning
                    else -> PressureLevel.Calm
                },
            )
        },
        softLock = null,
        status = status,
        recentlyClearedRows = setOf(14),
        lastResolvedLines = 2,
        lastChainDepth = 2,
        specialChainCount = 1,
        clearAnimationToken = 1,
        screenShakeToken = 1L,
        impactFlashToken = 1L,
        comboPopupToken = 1L,
        floatingFeedback = com.ugurbuga.stackshift.game.model.FloatingFeedback(
            text = gameText(GameTextKey.FeedbackPerfect, 2, 280),
            emphasis = FeedbackEmphasis.Bonus,
            token = 1L,
        ),
        feedbackToken = 1L,
        message = when (status) {
            GameStatus.Running -> gameText(GameTextKey.GameMessageSelectColumn)
            GameStatus.Paused -> gameText(GameTextKey.GameMessagePaused)
            GameStatus.GameOver -> gameText(GameTextKey.GameMessagePressureGameOver)
        },
    )
}

private fun previewImpactPointsPreview(): Set<GridPoint> = setOf(
    GridPoint(2, 9),
    GridPoint(6, 9),
    GridPoint(7, 8),
)

private fun previewPlacementPreview(): PlacementPreview = PlacementPreview(
    selectedColumn = 4,
    entryAnchor = GridPoint(4, 14),
    landingAnchor = GridPoint(4, 8),
    occupiedCells = listOf(
        GridPoint(4, 8),
        GridPoint(5, 8),
        GridPoint(6, 8),
        GridPoint(5, 9),
    ),
    coveredColumns = 4..6,
)
