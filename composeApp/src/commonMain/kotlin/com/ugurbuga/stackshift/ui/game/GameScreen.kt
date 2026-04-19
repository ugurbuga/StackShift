package com.ugurbuga.stackshift.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ugurbuga.stackshift.StackShiftTheme
import com.ugurbuga.stackshift.ads.GameAdController
import com.ugurbuga.stackshift.ads.NoOpGameAdController
import com.ugurbuga.stackshift.game.logic.GameEvent
import com.ugurbuga.stackshift.game.model.BoardMatrix
import com.ugurbuga.stackshift.game.model.CellTone
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
import com.ugurbuga.stackshift.game.model.SpecialBlockType
import com.ugurbuga.stackshift.game.model.gameText
import com.ugurbuga.stackshift.game.model.toTopLeft
import com.ugurbuga.stackshift.localization.LocalAppSettings
import com.ugurbuga.stackshift.platform.feedback.GameHaptic
import com.ugurbuga.stackshift.platform.feedback.GameHaptics
import com.ugurbuga.stackshift.platform.feedback.GameSound
import com.ugurbuga.stackshift.platform.feedback.NoOpGameHaptics
import com.ugurbuga.stackshift.platform.feedback.NoOpSoundEffectPlayer
import com.ugurbuga.stackshift.platform.feedback.SoundEffectPlayer
import com.ugurbuga.stackshift.presentation.game.GameDispatchResult
import com.ugurbuga.stackshift.presentation.game.GameIntent
import com.ugurbuga.stackshift.presentation.game.GameViewModel
import com.ugurbuga.stackshift.presentation.game.InteractionFeedback
import com.ugurbuga.stackshift.presentation.game.mergeWith
import com.ugurbuga.stackshift.settings.AppSettings
import com.ugurbuga.stackshift.settings.FirstRunGameOnboardingStateFactory
import com.ugurbuga.stackshift.settings.FirstRunOnboardingScene
import com.ugurbuga.stackshift.settings.FirstRunOnboardingStage
import com.ugurbuga.stackshift.settings.HighScoreStorage
import com.ugurbuga.stackshift.telemetry.AppTelemetry
import com.ugurbuga.stackshift.telemetry.LogScreen
import com.ugurbuga.stackshift.telemetry.NoOpAppTelemetry
import com.ugurbuga.stackshift.telemetry.TelemetryActionNames
import com.ugurbuga.stackshift.telemetry.TelemetryScreenNames
import com.ugurbuga.stackshift.ui.theme.StackShiftThemeTokens
import com.ugurbuga.stackshift.ui.theme.appBackgroundBrush
import com.ugurbuga.stackshift.ui.theme.isStackShiftDarkTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import stackshift.composeapp.generated.resources.Res
import stackshift.composeapp.generated.resources.app_title
import stackshift.composeapp.generated.resources.boost
import stackshift.composeapp.generated.resources.continue_label
import stackshift.composeapp.generated.resources.danger
import stackshift.composeapp.generated.resources.danger_none
import stackshift.composeapp.generated.resources.feedback_chain
import stackshift.composeapp.generated.resources.feedback_clear
import stackshift.composeapp.generated.resources.feedback_extra_life
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
import stackshift.composeapp.generated.resources.game_message_extra_life_used
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
import stackshift.composeapp.generated.resources.game_over_extra_life
import stackshift.composeapp.generated.resources.game_over_extra_life_loading
import stackshift.composeapp.generated.resources.game_over_new_high_score
import stackshift.composeapp.generated.resources.game_over_title
import stackshift.composeapp.generated.resources.high_score
import stackshift.composeapp.generated.resources.high_score_new_record
import stackshift.composeapp.generated.resources.hold
import stackshift.composeapp.generated.resources.launch_bar
import stackshift.composeapp.generated.resources.launch_boost_active
import stackshift.composeapp.generated.resources.launch_chain_message
import stackshift.composeapp.generated.resources.launch_drag_hint
import stackshift.composeapp.generated.resources.launch_game_over
import stackshift.composeapp.generated.resources.launch_label
import stackshift.composeapp.generated.resources.launch_paused
import stackshift.composeapp.generated.resources.launch_soft_lock_message
import stackshift.composeapp.generated.resources.launch_special_chance
import stackshift.composeapp.generated.resources.lines
import stackshift.composeapp.generated.resources.pause
import stackshift.composeapp.generated.resources.pause_title
import stackshift.composeapp.generated.resources.piece_properties_none
import stackshift.composeapp.generated.resources.play_again
import stackshift.composeapp.generated.resources.queue_empty
import stackshift.composeapp.generated.resources.queue_next_short
import stackshift.composeapp.generated.resources.restart
import stackshift.composeapp.generated.resources.restart_cancel
import stackshift.composeapp.generated.resources.restart_confirm
import stackshift.composeapp.generated.resources.restart_confirm_body
import stackshift.composeapp.generated.resources.restart_confirm_title
import stackshift.composeapp.generated.resources.resume
import stackshift.composeapp.generated.resources.score
import stackshift.composeapp.generated.resources.settings_title
import stackshift.composeapp.generated.resources.settings_tutorial
import stackshift.composeapp.generated.resources.special_column_clearer
import stackshift.composeapp.generated.resources.special_ghost
import stackshift.composeapp.generated.resources.special_heavy
import stackshift.composeapp.generated.resources.special_row_clearer
import stackshift.composeapp.generated.resources.tutorial_finish
import stackshift.composeapp.generated.resources.tutorial_ready_body
import stackshift.composeapp.generated.resources.tutorial_ready_title
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

private const val LaunchAnimationMillis = 140L
private const val EntryAnimationMillis = 70L
private const val LaunchOverlayDisplayDelayMillis = 250L
private const val NextPieceScale = 0.5f
private const val LaunchPreviewAlpha = 1f
private const val QueuePreviewAlpha = 0.58f

private val BottomDockHeight = 148.dp
private val TopBarVerticalPadding = 6.dp
private val TopBarRowSpacing = 4.dp
private val TopBarIconSpacing = 4.dp
private val TopBarMetricLaunchSpacing = 8.dp
private val TopBarActionIconSize = 16.dp
private val MetricChipHorizontalPadding = 16.dp
private val MetricChipVerticalPadding = 7.dp
private val RecordIndicatorIconSize = 12.dp
private val RecordIndicatorSideGap = 6.dp
private const val MetricHighlightThreshold = 0.08f
private const val MetricHighlightPulseScale = 1.08f
private const val MetricHighlightPulseUpDurationMillis = 180
private const val MetricHighlightPulseDownDurationMillis = 520
private const val TrayPulseScaleBoost = 0.055f
private const val TrayPulseUpDurationMillis = 760
private const val TrayPulseDownDurationMillis = 920
private const val GameOverDialogRevealDurationMillis = 260

private val GameOverDialogWidth = 420.dp
private val GameOverDialogRevealOffsetDp = 12.dp
private val GameOverDialogCardPadding = 24.dp
private val GameOverDialogIconSize = 40.dp
private const val TopBarPanelAlpha = 0.88f
private const val TopBarPanelStrokeAlpha = 0.72f
private const val TopBarPanelGlowAlpha = 0.14f
private const val DockPanelAlpha = 0.90f
private const val DockPanelStrokeAlpha = 0.74f
private const val DockPanelGlowAlpha = 0.12f
private const val GameOverBoardRowCoverAlpha = 0.92f
private const val GameOverBoardRowClearDurationMillis = 92
private const val ScreenShakeStepDurationMillis = 42
private const val ScreenShakeFinalStepDurationMillis = 48
private const val InteractiveOnboardingStageAdvanceDelayMillis = 720L
private const val InteractiveOnboardingClearAnimationDurationMillis = 620
private const val InteractiveOnboardingBoardShiftDurationMillis = 360
private val TopBarActionBlockSize = 28.dp
private val TopBarActionBlockTones = listOf(
    CellTone.Cyan,
    CellTone.Gold,
    CellTone.Violet,
    CellTone.Lime,
    CellTone.Amber,
)

private data class InteractiveOnboardingAdvanceRequest(
    val completedStage: FirstRunOnboardingStage,
    val nextStage: FirstRunOnboardingStage?,
)

private fun GameState.hiddenOnboardingPreviewState(): GameState = copy(
    activePiece = null,
    nextQueue = emptyList(),
    holdPiece = null,
    canHold = false,
)

@Composable
private fun resolveActivePieceProperties(piece: Piece?): String {
    if (piece == null) return "—"
    val kind = piece.kind.name
    val special = resolveGameText(piece.special.shortLabel())
    return buildString {
        append(kind)
        if (special != "—") {
            append(" • ")
            append(special)
        }
        append(" • ")
        append(piece.width)
        append("×")
        append(piece.height)
    }
}

@Composable
fun GameOverBoardClearOverlay(
    revealProgress: Float,
    rowCount: Int,
    modifier: Modifier = Modifier,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    val fullyClearedRows = (revealProgress * rowCount).toInt()
    val partialClearAlpha = 1f - ((revealProgress * rowCount) - fullyClearedRows)

    Canvas(modifier = modifier.fillMaxSize()) {
        val cellHeight = size.height / rowCount
        for (i in 0 until fullyClearedRows) {
            drawRect(
                color = uiColors.panel.copy(alpha = GameOverBoardRowCoverAlpha),
                topLeft = Offset(0f, size.height - (i + 1) * cellHeight),
                size = Size(size.width, cellHeight)
            )
        }
        if (fullyClearedRows < rowCount) {
            drawRect(
                color = uiColors.panel.copy(alpha = GameOverBoardRowCoverAlpha * (1f - partialClearAlpha)),
                topLeft = Offset(0f, size.height - (fullyClearedRows + 1) * cellHeight),
                size = Size(size.width, cellHeight),
            )
        }
    }
}

@Composable
fun StackShiftGameApp(
    modifier: Modifier = Modifier,
    soundPlayer: SoundEffectPlayer = NoOpSoundEffectPlayer,
    telemetry: AppTelemetry = NoOpAppTelemetry,
    adController: GameAdController = NoOpGameAdController,
    viewModel: GameViewModel = remember { GameViewModel() },
    interactiveOnboardingEnabled: Boolean = false,
    onInteractiveOnboardingFinished: (GameState) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenTutorial: () -> Unit = {},
) {
    val haptics = rememberGameHaptics()
    val uiState by viewModel.uiState.collectAsState()
    val onboardingStages = remember { FirstRunGameOnboardingStateFactory.stages }
    var onboardingStage by remember(interactiveOnboardingEnabled) {
        mutableStateOf(
            if (interactiveOnboardingEnabled) onboardingStages.firstOrNull() else null,
        )
    }
    var onboardingAwaitingCommit by remember(interactiveOnboardingEnabled, onboardingStage) {
        mutableStateOf(value = false)
    }
    var onboardingAdvanceRequest by remember(interactiveOnboardingEnabled) {
        mutableStateOf<InteractiveOnboardingAdvanceRequest?>(null)
    }
    var showOnboardingCompletionDialog by remember(interactiveOnboardingEnabled) {
        mutableStateOf(value = false)
    }
    var pendingOnboardingCompletionState by remember(interactiveOnboardingEnabled) {
        mutableStateOf<GameState?>(null)
    }
    var shouldShowLaunchOverlay by rememberSaveable { mutableStateOf(value = true) }
    val onboardingScene = remember(interactiveOnboardingEnabled, onboardingStage) {
        onboardingStage?.takeIf { interactiveOnboardingEnabled }
            ?.let(FirstRunGameOnboardingStateFactory::scene)
    }
    val displayGameState by remember(
        uiState.gameState,
        onboardingAdvanceRequest,
        showOnboardingCompletionDialog,
    ) {
        derivedStateOf {
            if (onboardingAdvanceRequest != null || showOnboardingCompletionDialog) {
                uiState.gameState.hiddenOnboardingPreviewState()
            } else {
                uiState.gameState
            }
        }
    }

    LaunchedEffect(interactiveOnboardingEnabled, onboardingScene) {
        if (!interactiveOnboardingEnabled || onboardingScene == null) return@LaunchedEffect
        onboardingAwaitingCommit = false
        onboardingAdvanceRequest = null
        showOnboardingCompletionDialog = false
        pendingOnboardingCompletionState = null
        viewModel.replaceState(onboardingScene.gameState)
        telemetry.logUserAction(
            action = "interactive_onboarding_stage_shown",
            parameters = mapOf("stage" to onboardingScene.stage.name),
        )
    }

    LaunchedEffect(interactiveOnboardingEnabled, onboardingAdvanceRequest) {
        val request = onboardingAdvanceRequest ?: return@LaunchedEffect
        if (!interactiveOnboardingEnabled) {
            onboardingAdvanceRequest = null
            onboardingAwaitingCommit = false
            return@LaunchedEffect
        }

        delay(InteractiveOnboardingStageAdvanceDelayMillis.milliseconds)

        if (onboardingAdvanceRequest != request || onboardingStage != request.completedStage) return@LaunchedEffect

        telemetry.logUserAction(
            action = "interactive_onboarding_stage_completed",
            parameters = mapOf("stage" to request.completedStage.name),
        )

        if (request.nextStage != null) {
            onboardingStage = request.nextStage
        } else {
            pendingOnboardingCompletionState = FirstRunGameOnboardingStateFactory.cleanGameState()
            showOnboardingCompletionDialog = true
            onboardingStage = null
        }

        onboardingAwaitingCommit = false
        onboardingAdvanceRequest = null
    }

    var highestScore by remember { mutableIntStateOf(HighScoreStorage.load()) }
    var newHighScoreReached by remember { mutableStateOf(value = false) }
    LaunchedEffect(uiState.gameState.score) {
        if (uiState.gameState.score > highestScore) {
            telemetry.logHighScoreReached(
                newScore = uiState.gameState.score,
                previousHighScore = highestScore,
            )
            highestScore = uiState.gameState.score
            HighScoreStorage.save(highestScore)
            newHighScoreReached = true
        }
    }
    LaunchedEffect(
        uiState.gameState.status,
        uiState.gameState.score,
        uiState.gameState.linesCleared,
    ) {
        if (uiState.gameState.status == GameStatus.Running && uiState.gameState.score == 0 && uiState.gameState.linesCleared == 0) {
            newHighScoreReached = false
        }
    }

    LogScreen(telemetry, TelemetryScreenNames.Game)
    GameScreenWithLaunchOverlay(
        modifier = modifier,
        gameState = displayGameState,
        onRequestPreview = viewModel::previewPlacement,
        onResolvePreviewImpact = viewModel::previewImpactPoints,
        onPlacePiece = { column ->
            if (interactiveOnboardingEnabled && onboardingScene != null) {
                onboardingAwaitingCommit = true
                onboardingAdvanceRequest = null
            }
            val result = viewModel.placePieceResult(column)
            if (interactiveOnboardingEnabled && onboardingScene != null) {
                val currentIndex = onboardingStages.indexOf(onboardingScene.stage)
                val nextStage = onboardingStages.getOrNull(currentIndex + 1)
                when {
                    GameEvent.SoftLockStarted in result.events || GameEvent.SoftLockAdjusted in result.events -> {
                        val commitResult = viewModel.dispatchResult(GameIntent.CommitSoftLock)
                        if (GameEvent.PlacementAccepted in commitResult.events) {
                            onboardingAwaitingCommit = true
                            onboardingAdvanceRequest = InteractiveOnboardingAdvanceRequest(
                                completedStage = onboardingScene.stage,
                                nextStage = nextStage,
                            )
                        } else {
                            onboardingAwaitingCommit = false
                            onboardingAdvanceRequest = null
                        }

                        result.mergeWith(commitResult)
                    }

                    else -> {
                        onboardingAwaitingCommit = false
                        onboardingAdvanceRequest = null
                        result
                    }
                }
            } else {
                result
            }
        },
        onHoldPiece = {
            telemetry.logUserAction(TelemetryActionNames.HoldPiece)
            viewModel.holdPiece()
        },
        onPauseToggle = {
            telemetry.logUserAction(TelemetryActionNames.TogglePause)
            viewModel.togglePause()
        },
        onRestart = {
            telemetry.logUserAction(TelemetryActionNames.RestartGame)
            viewModel.restart(uiState.gameState.config)
        },
        onOpenSettings = onOpenSettings,
        onOpenTutorial = onOpenTutorial,
        onRewardedRevive = {
            telemetry.logUserAction("rewarded_revive")
            viewModel.reviveFromReward()
        },
        telemetry = telemetry,
        adController = adController,
        soundPlayer = soundPlayer,
        haptics = haptics,
        highestScore = highestScore,
        showLaunchOverlayInitially = shouldShowLaunchOverlay,
        onLaunchOverlayFinished = { shouldShowLaunchOverlay = false },
        showNewHighScoreMessage = newHighScoreReached,
        interactiveOnboardingScene = if (showOnboardingCompletionDialog) null else onboardingScene,
        interactiveOnboardingCurrentStep = onboardingScene?.stage?.let { onboardingStages.indexOf(it) + 1 }
            ?: 0,
        interactiveOnboardingTotalSteps = onboardingStages.size,
        interactiveOnboardingAwaitingCommit = onboardingAwaitingCommit,
        interactiveOnboardingCompletionDialogVisible = showOnboardingCompletionDialog,
        onInteractiveOnboardingStartGame = {
            val cleanState = pendingOnboardingCompletionState
                ?: FirstRunGameOnboardingStateFactory.cleanGameState()
            viewModel.replaceState(cleanState)
            showOnboardingCompletionDialog = false
            pendingOnboardingCompletionState = null
            onInteractiveOnboardingFinished(cleanState)
        },
    )
}

@Composable
fun GameScreenWithLaunchOverlay(
    gameState: GameState,
    onRequestPreview: (Int) -> PlacementPreview?,
    onResolvePreviewImpact: (PlacementPreview?) -> Set<GridPoint>,
    onPlacePiece: (Int) -> GameDispatchResult,
    onHoldPiece: () -> InteractionFeedback,
    onPauseToggle: () -> InteractionFeedback,
    onRestart: () -> InteractionFeedback,
    onRewardedRevive: () -> InteractionFeedback,
    onOpenSettings: () -> Unit,
    onOpenTutorial: () -> Unit,
    telemetry: AppTelemetry = NoOpAppTelemetry,
    adController: GameAdController = NoOpGameAdController,
    soundPlayer: SoundEffectPlayer,
    haptics: GameHaptics,
    highestScore: Int,
    showLaunchOverlayInitially: Boolean = false,
    onLaunchOverlayFinished: () -> Unit = {},
    showNewHighScoreMessage: Boolean = false,
    interactiveOnboardingScene: FirstRunOnboardingScene? = null,
    interactiveOnboardingCurrentStep: Int = 0,
    interactiveOnboardingTotalSteps: Int = 0,
    interactiveOnboardingAwaitingCommit: Boolean = false,
    interactiveOnboardingCompletionDialogVisible: Boolean = false,
    onInteractiveOnboardingStartGame: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var overlayVisible by rememberSaveable { mutableStateOf(showLaunchOverlayInitially) }
    val uiColors = StackShiftThemeTokens.uiColors

    Box(modifier = modifier.fillMaxSize()) {
        GameScreen(
            gameState = gameState,
            onRequestPreview = onRequestPreview,
            onResolvePreviewImpact = onResolvePreviewImpact,
            onPlacePiece = onPlacePiece,
            onHoldPiece = onHoldPiece,
            onPauseToggle = onPauseToggle,
            onRestart = onRestart,
            onRewardedRevive = onRewardedRevive,
            onOpenSettings = onOpenSettings,
            onOpenTutorial = onOpenTutorial,
            telemetry = telemetry,
            adController = adController,
            soundPlayer = soundPlayer,
            haptics = haptics,
            highestScore = highestScore,
            showNewHighScoreMessage = showNewHighScoreMessage,
            interactiveOnboardingScene = interactiveOnboardingScene,
            interactiveOnboardingCurrentStep = interactiveOnboardingCurrentStep,
            interactiveOnboardingTotalSteps = interactiveOnboardingTotalSteps,
            interactiveOnboardingAwaitingCommit = interactiveOnboardingAwaitingCommit,
            interactiveOnboardingCompletionDialogVisible = interactiveOnboardingCompletionDialogVisible,
            onInteractiveOnboardingStartGame = onInteractiveOnboardingStartGame,
        )

        if (overlayVisible) {
            GameLaunchOverlay(
                uiColors = uiColors,
                onFinished = {
                    overlayVisible = false
                    onLaunchOverlayFinished()
                },
            )
        }
    }
}

@Composable
fun GameLaunchOverlay(
    uiColors: com.ugurbuga.stackshift.ui.theme.StackShiftUiColors,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        delay(LaunchOverlayDisplayDelayMillis.milliseconds)
        onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(uiColors.overlay),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(Res.string.app_title),
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(Res.string.launch_drag_hint),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.88f),
            )
        }
    }
}

@Composable
fun GameScreen(
    gameState: GameState,
    onRequestPreview: (Int) -> PlacementPreview?,
    onResolvePreviewImpact: (PlacementPreview?) -> Set<GridPoint>,
    onPlacePiece: (Int) -> GameDispatchResult,
    onHoldPiece: () -> InteractionFeedback,
    onPauseToggle: () -> InteractionFeedback,
    onRestart: () -> InteractionFeedback,
    onRewardedRevive: () -> InteractionFeedback,
    onOpenSettings: () -> Unit,
    onOpenTutorial: () -> Unit,
    telemetry: AppTelemetry = NoOpAppTelemetry,
    adController: GameAdController = NoOpGameAdController,
    soundPlayer: SoundEffectPlayer,
    haptics: GameHaptics,
    highestScore: Int,
    showNewHighScoreMessage: Boolean = false,
    interactiveOnboardingScene: FirstRunOnboardingScene? = null,
    interactiveOnboardingCurrentStep: Int = 0,
    interactiveOnboardingTotalSteps: Int = 0,
    interactiveOnboardingAwaitingCommit: Boolean = false,
    interactiveOnboardingCompletionDialogVisible: Boolean = false,
    onInteractiveOnboardingStartGame: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val uiColors = StackShiftThemeTokens.uiColors
    val colorScheme = MaterialTheme.colorScheme
    val updatedPreviewProvider by rememberUpdatedState(onRequestPreview)
    val updatedPreviewImpactProvider by rememberUpdatedState(onResolvePreviewImpact)
    val updatedPlacePiece by rememberUpdatedState(onPlacePiece)
    val updatedHoldPiece by rememberUpdatedState(onHoldPiece)
    val updatedPauseToggle by rememberUpdatedState(onPauseToggle)
    val updatedRestart by rememberUpdatedState(onRestart)
    val updatedRewardedRevive by rememberUpdatedState(onRewardedRevive)
    var showRestartDialog by remember { mutableStateOf(value = false) }
    val screenShakeX = remember { Animatable(0f) }
    val screenShakeY = remember { Animatable(0f) }
    val impactFlashAlpha = remember { Animatable(0f) }
    val comboDriftY = remember { Animatable(18f) }
    val comboAlpha = remember { Animatable(0f) }
    val metricPulsePhase = remember { Animatable(0f) }
    val gameOverBoardClearProgress = remember { Animatable(0f) }
    val gameOverDialogRevealProgress = remember { Animatable(0f) }
    var highScoreHighlightActive by remember { mutableStateOf(value = false) }
    var celebratedHighScore by remember { mutableIntStateOf(highestScore) }
    var showGameOverDialog by remember { mutableStateOf(value = false) }
    var rewardedReviveLoading by remember { mutableStateOf(value = false) }

    val stylePulseState = rememberInfiniteTransition(label = "stylePulse")
        .animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "stylePulse",
        )
    val stylePulse = stylePulseState.value

    var overlayHostRectInRoot by remember { mutableStateOf(Rect.Zero) }
    var boardRectInRoot by remember { mutableStateOf(Rect.Zero) }
    var trayRectInRoot by remember { mutableStateOf(Rect.Zero) }
    val overlayTopLeftState = remember { mutableStateOf<Offset?>(null) }
    var isDragging by remember { mutableStateOf(value = false) }
    var isLaunching by remember { mutableStateOf(value = false) }
    val interactiveOnboardingEnabled = interactiveOnboardingScene != null
    val interactiveOnboardingAcceptedColumns = interactiveOnboardingScene?.acceptedColumns.orEmpty()
    val topBarControlsEnabled = !interactiveOnboardingEnabled

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
    val spawnRect by remember(spawnTopLeft, activePiece?.id, cellSizePx) {
        derivedStateOf {
            val topLeft = spawnTopLeft ?: return@derivedStateOf Rect.Zero
            val piece = activePiece ?: return@derivedStateOf Rect.Zero
            if (cellSizePx <= 0f) return@derivedStateOf Rect.Zero
            Rect(
                left = topLeft.x,
                top = topLeft.y,
                right = topLeft.x + (piece.width * cellSizePx),
                bottom = topLeft.y + (piece.height * cellSizePx),
            )
        }
    }

    LaunchedEffect(activePiece?.id, spawnTopLeft) {
        if (spawnTopLeft != null) {
            overlayTopLeftState.value = spawnTopLeft
            isDragging = false
            isLaunching = false
        }
    }

    val selectedColumn by remember(activePiece?.id, boardRect, cellSizePx) {
        derivedStateOf {
            resolveSelectedColumn(
                piece = activePiece,
                overlayTopLeft = overlayTopLeftState.value,
                boardRect = boardRect,
                cellSizePx = cellSizePx,
                boardColumns = gameState.config.columns,
            )
        }
    }
    val hasDraggedAwayFromSpawn by remember(spawnTopLeft, cellSizePx) {
        derivedStateOf {
            val spawn = spawnTopLeft ?: return@derivedStateOf false
            val overlay = overlayTopLeftState.value ?: return@derivedStateOf false
            kotlin.math.abs(overlay.x - spawn.x) >= (cellSizePx * 0.45f)
        }
    }

    val placementPreview by remember(
        selectedColumn,
        activePiece?.id,
        gameState.status,
        gameState.board,
        interactiveOnboardingAcceptedColumns,
    ) {
        derivedStateOf {
            if (gameState.status != GameStatus.Running) return@derivedStateOf null
            if (interactiveOnboardingAcceptedColumns.isNotEmpty() && selectedColumn !in interactiveOnboardingAcceptedColumns) {
                return@derivedStateOf null
            }
            if (gameState.softLock != null && !isDragging) {
                gameState.softLock.preview
            } else {
                selectedColumn?.let(updatedPreviewProvider)
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
                    interactiveOnboardingScene.stage != FirstRunOnboardingStage.DragAndLaunch &&
                    isDragging &&
                    placementPreview != null &&
                    selectedColumn != null &&
                    (interactiveOnboardingAcceptedColumns.isEmpty() || selectedColumn in interactiveOnboardingAcceptedColumns)
        }
    }

    val scoreHighlightActive = highScoreHighlightActive
    val anyMetricHighlightActive = highScoreHighlightActive && !interactiveOnboardingEnabled

    LaunchedEffect(anyMetricHighlightActive, gameState.status) {
        if (!anyMetricHighlightActive || gameState.status == GameStatus.GameOver) {
            metricPulsePhase.snapTo(0f)
            return@LaunchedEffect
        }

        metricPulsePhase.snapTo(0f)
        while (isActive && anyMetricHighlightActive && gameState.status != GameStatus.GameOver) {
            metricPulsePhase.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = MetricHighlightPulseUpDurationMillis,
                    easing = FastOutSlowInEasing,
                ),
            )
            metricPulsePhase.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = MetricHighlightPulseDownDurationMillis,
                    easing = FastOutSlowInEasing,
                ),
            )
        }
    }

    val previewImpactPoints by remember(
        placementPreview,
        activePiece?.id,
        gameState.board,
        gameState.status,
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
            current.copy(x = columnToLeft(snappedColumn, boardRect, cellSizePx))
        }
    }
    val shouldPulseTrayPiece =
        activePiece != null &&
                spawnTopLeft != null &&
                gameState.status == GameStatus.Running &&
                gameState.softLock == null &&
                !interactiveOnboardingEnabled &&
                !isDragging &&
                !isLaunching

    LaunchedEffect(gameState.screenShakeToken) {
        if (gameState.screenShakeToken == 0L) return@LaunchedEffect
        if (interactiveOnboardingEnabled) {
            screenShakeX.snapTo(0f)
            screenShakeY.snapTo(0f)
            return@LaunchedEffect
        }
        repeat(3) {
            screenShakeX.animateTo(
                targetValue = ((-8..8).random()).toFloat(),
                animationSpec = tween(durationMillis = ScreenShakeStepDurationMillis),
            )
            screenShakeY.animateTo(
                targetValue = ((-8..8).random()).toFloat(),
                animationSpec = tween(durationMillis = ScreenShakeStepDurationMillis),
            )
        }
        screenShakeX.animateTo(
            0f,
            animationSpec = tween(durationMillis = ScreenShakeFinalStepDurationMillis)
        )
        screenShakeY.animateTo(
            0f,
            animationSpec = tween(durationMillis = ScreenShakeFinalStepDurationMillis)
        )
    }

    LaunchedEffect(gameState.impactFlashToken) {
        if (gameState.impactFlashToken == 0L) return@LaunchedEffect
        impactFlashAlpha.snapTo(0.42f)
        impactFlashAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        )
    }

    LaunchedEffect(gameState.comboPopupToken) {
        if (gameState.comboPopupToken == 0L) return@LaunchedEffect
        comboAlpha.snapTo(0f)
        comboDriftY.snapTo(18f)
        coroutineScope.launch {
            comboAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
            )
            delay(800)
            comboAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
            )
        }
        comboDriftY.animateTo(
            targetValue = -32f,
            animationSpec = tween(durationMillis = 1460, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(gameState.status) {
        if (gameState.status == GameStatus.GameOver) {
            gameOverBoardClearProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = (GameOverBoardRowClearDurationMillis * gameState.config.rows).coerceAtLeast(
                        1200
                    ),
                    easing = FastOutSlowInEasing,
                ),
            )
            showGameOverDialog = true
            gameOverDialogRevealProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = GameOverDialogRevealDurationMillis,
                    easing = FastOutSlowInEasing,
                ),
            )
        } else {
            gameOverBoardClearProgress.snapTo(0f)
            gameOverDialogRevealProgress.snapTo(0f)
            showGameOverDialog = false
        }
    }

    LaunchedEffect(highestScore, gameState.score, gameState.status, gameState.linesCleared) {
        when {
            gameState.status == GameStatus.GameOver ||
                    (gameState.status == GameStatus.Running && gameState.score == 0 && gameState.linesCleared == 0) -> {
                highScoreHighlightActive = false
                celebratedHighScore = highestScore
                metricPulsePhase.animateTo(0f, animationSpec = snap())
            }

            highestScore > celebratedHighScore -> {
                celebratedHighScore = highestScore
                highScoreHighlightActive = true
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = colorScheme.background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(appBackgroundBrush(uiColors))
                .graphicsLayer {
                    translationX = screenShakeX.value
                    translationY = screenShakeY.value
                }
                .safeDrawingPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coordinates ->
                        val newBounds = coordinates.boundsInRoot()
                        if (overlayHostRectInRoot != newBounds) {
                            overlayHostRectInRoot = newBounds
                        }
                    },
            ) {
                val highScoreHighlightStrengthProvider =
                    { if (highScoreHighlightActive) 1f else 0f }
                val scoreHighlightStrengthProvider = { if (scoreHighlightActive) 1f else 0f }
                val highScorePulseScaleProvider = {
                    if (highScoreHighlightActive) {
                        1f + (MetricHighlightPulseScale - 1f) * metricPulsePhase.value
                    } else {
                        1f
                    }
                }
                val scorePulseScaleProvider = {
                    if (scoreHighlightActive) {
                        1f + (MetricHighlightPulseScale - 1f) * metricPulsePhase.value
                    } else {
                        1f
                    }
                }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (!interactiveOnboardingEnabled) {
                        MinimalTopBar(
                            gameState = gameState,
                            highestScore = highestScore,
                            highScoreHighlightStrengthProvider = highScoreHighlightStrengthProvider,
                            highScoreHighlightScaleProvider = highScorePulseScaleProvider,
                            scoreHighlightStrengthProvider = scoreHighlightStrengthProvider,
                            scoreHighlightScaleProvider = scorePulseScaleProvider,
                            onHoldPiece = {
                                if (!topBarControlsEnabled) return@MinimalTopBar
                                dispatchFeedback(
                                    updatedHoldPiece(),
                                    soundPlayer,
                                    haptics
                                )
                            },
                            onPauseToggle = {
                                if (!topBarControlsEnabled) return@MinimalTopBar
                                dispatchFeedback(
                                    updatedPauseToggle(),
                                    soundPlayer,
                                    haptics
                                )
                            },
                            onRestart = {
                                if (!topBarControlsEnabled) return@MinimalTopBar
                                showRestartDialog = true
                            },
                            onOpenSettings = {
                                if (!topBarControlsEnabled) return@MinimalTopBar
                                onOpenSettings()
                            },
                            onOpenTutorial = {
                                if (!topBarControlsEnabled) return@MinimalTopBar
                                onOpenTutorial()
                            },
                            controlsEnabled = topBarControlsEnabled,
                            stylePulse = stylePulse,
                        )
                    }

                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = if (interactiveOnboardingEnabled) Alignment.BottomCenter else Alignment.Center,
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
                                        val newBounds = coordinates.boundsInRoot()
                                        if (boardRectInRoot != newBounds) {
                                            boardRectInRoot = newBounds
                                        }
                                    },
                                gameState = gameState,
                                preview = placementPreview,
                                impactedPreviewCells = previewImpactPoints,
                                guidedColumns = emptySet(),
                                activeColumn = selectedColumn,
                                activePiece = activePiece,
                                isDragging = isDragging,
                                gameOverClearProgressProvider = { gameOverBoardClearProgress.value },
                                showClearFlash = true,
                                stylePulse = stylePulse,
                            )

                            if (gameState.status == GameStatus.GameOver || gameOverBoardClearProgress.value > 0f) {
                                GameOverBoardClearOverlay(
                                    revealProgress = gameOverBoardClearProgress.value,
                                    rowCount = gameState.config.rows,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }

                    MinimalBottomDock(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(BottomDockHeight)
                            .onGloballyPositioned { coordinates ->
                                val newBounds = coordinates.boundsInRoot()
                                if (trayRectInRoot != newBounds) {
                                    trayRectInRoot = newBounds
                                }
                            },
                        gameState = gameState,
                        cellSizePx = cellSizePx,
                        showNextPiece = !interactiveOnboardingEnabled,
                        stylePulse = stylePulse,
                    )
                }

                interactiveOnboardingScene?.let { scene ->
                    InteractiveGameOnboardingOverlay(
                        ui = GameInteractiveOnboardingUi(
                            scene = scene,
                            currentStep = interactiveOnboardingCurrentStep,
                            totalSteps = interactiveOnboardingTotalSteps,
                            isAwaitingPlacementCommit = interactiveOnboardingAwaitingCommit,
                            hasDraggedAwayFromSpawn = hasDraggedAwayFromSpawn,
                            isTargetAligned = interactiveOnboardingTargetAligned,
                        ),
                        boardRect = boardRect,
                        trayRect = trayRect,
                        spawnRect = spawnRect,
                        cellSizePx = cellSizePx,
                    )
                }

                gameState.floatingFeedback?.let { floatingFeedback ->
                    FloatingFeedbackOverlay(
                        text = resolveGameText(floatingFeedback.text),
                        isBonus = floatingFeedback.emphasis != FeedbackEmphasis.Info,
                        alpha = comboAlpha.value,
                        driftY = comboDriftY.value,
                    )
                }

                ImpactFlashOverlay(alphaProvider = { impactFlashAlpha.value })

                if (activePiece != null && overlayTopLeftState.value != null && cellSizePx > 0f) {
                    ActivePieceOverlay(
                        piece = activePiece,
                        cellSizePx = cellSizePx,
                        displayOverlayTopLeft = displayOverlayTopLeft,
                        isDragging = isDragging,
                        isLaunching = isLaunching,
                        isSoftLockActive = gameState.isSoftLockActive,
                        isPaused = gameState.status == GameStatus.Paused,
                        shouldPulseTrayPiece = shouldPulseTrayPiece,
                        pointerModifier = Modifier.pointerInput(
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
                                    val current =
                                        overlayTopLeftState.value ?: return@detectDragGestures
                                    overlayTopLeftState.value =
                                        current.copy(x = current.x + dragAmount.x)
                                },
                                onDragEnd = {
                                    if (gameState.status != GameStatus.Running || isLaunching) return@detectDragGestures
                                    isDragging = false

                                    val preview = placementPreview
                                    val column = selectedColumn
                                    val currentSpawn = spawnTopLeft
                                    if (preview == null || column == null || currentSpawn == null) {
                                        overlayTopLeftState.value = currentSpawn
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
                                        overlayTopLeftState.value = preview.landingAnchor.toTopLeft(
                                            boardRect = boardRect,
                                            cellSizePx = cellSizePx
                                        )
                                        val result = updatedPlacePiece(column)
                                        dispatchFeedback(result.feedback, soundPlayer, haptics)
                                    } else {
                                        isLaunching = true
                                        overlayTopLeftState.value = preview.entryAnchor.toTopLeft(
                                            boardRect = boardRect,
                                            cellSizePx = cellSizePx
                                        )
                                        coroutineScope.launch {
                                            delay(EntryAnimationMillis)
                                            overlayTopLeftState.value =
                                                preview.landingAnchor.toTopLeft(
                                                    boardRect = boardRect,
                                                    cellSizePx = cellSizePx
                                                )
                                            delay(LaunchAnimationMillis)
                                            val result = updatedPlacePiece(column)
                                            dispatchFeedback(result.feedback, soundPlayer, haptics)
                                            isLaunching = false
                                        }
                                    }
                                },
                                onDragCancel = {
                                    isDragging = false
                                    spawnTopLeft?.let { overlayTopLeftState.value = it }
                                },
                            )
                        },
                        stylePulse = stylePulse,
                    )
                }

                if (showGameOverDialog) {
                    GameOverDialog(
                        gameState = gameState,
                        highestScore = highestScore,
                        showNewHighScoreMessage = showNewHighScoreMessage,
                        revealProgressProvider = { gameOverDialogRevealProgress.value },
                        canUseExtraLife = !gameState.rewardedReviveUsed,
                        isExtraLifeLoading = rewardedReviveLoading,
                        showExtraLifeButton = adController !== NoOpGameAdController,
                        onPlayAgain = {
                            telemetry.logUserAction(TelemetryActionNames.PlayAgain)
                            adController.showRestartInterstitial {
                                dispatchFeedback(updatedRestart(), soundPlayer, haptics)
                            }
                        },
                        onUseExtraLife = {
                            if (rewardedReviveLoading || gameState.rewardedReviveUsed) return@GameOverDialog
                            rewardedReviveLoading = true
                            adController.showRewardedRevive { rewarded ->
                                rewardedReviveLoading = false
                                if (rewarded) {
                                    dispatchFeedback(updatedRewardedRevive(), soundPlayer, haptics)
                                }
                            }
                        },
                    )
                } else if (gameState.status == GameStatus.Paused) {
                    PauseOverlay(
                        onPrimaryAction = {
                            dispatchFeedback(updatedPauseToggle(), soundPlayer, haptics)
                        }
                    )
                }

                if (interactiveOnboardingCompletionDialogVisible) {
                    InteractiveOnboardingCompletionDialog(
                        onStartGame = onInteractiveOnboardingStartGame,
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
                            dispatchFeedback(updatedRestart(), soundPlayer, haptics)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxScope.FloatingFeedbackOverlay(
    text: String,
    isBonus: Boolean,
    alpha: Float,
    driftY: Float,
    modifier: Modifier = Modifier,
) {
    if (alpha <= 0.001f) return
    FloatingFeedbackBubble(
        text = text,
        modifier = modifier
            .align(Alignment.TopCenter)
            .padding(top = 96.dp),
        isBonus = isBonus,
        alpha = alpha,
        driftY = driftY,
    )
}

@Composable
private fun ImpactFlashOverlay(
    alphaProvider: () -> Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = alphaProvider().coerceIn(0f, 1f)
            }
            .background(Color.White),
    )
}

@Composable
private fun ActivePieceOverlay(
    piece: Piece,
    cellSizePx: Float,
    displayOverlayTopLeft: Offset?,
    isDragging: Boolean,
    isLaunching: Boolean,
    isSoftLockActive: Boolean,
    isPaused: Boolean,
    shouldPulseTrayPiece: Boolean,
    pointerModifier: Modifier,
    stylePulse: Float = 0f,
    modifier: Modifier = Modifier,
) {
    if (displayOverlayTopLeft == null || cellSizePx <= 0f) return
    val density = LocalDensity.current
    val settings = LocalAppSettings.current
    val overlayPulsePhase = remember { Animatable(0f) }
    val overlayX by animateFloatAsState(
        targetValue = displayOverlayTopLeft.x,
        animationSpec = if (isDragging) snap() else tween(
            durationMillis = 180,
            easing = FastOutSlowInEasing,
        ),
        label = "overlayX",
    )
    val overlayY by animateFloatAsState(
        targetValue = displayOverlayTopLeft.y,
        animationSpec = if (isDragging) snap() else tween(
            durationMillis = 180,
            easing = FastOutSlowInEasing,
        ),
        label = "overlayY",
    )
    val overlayScale by animateFloatAsState(
        targetValue = when {
            isLaunching -> 1.06f
            isSoftLockActive -> 0.98f
            else -> 1f
        },
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "overlayScale",
    )
    LaunchedEffect(shouldPulseTrayPiece, piece.id) {
        if (!shouldPulseTrayPiece) {
            overlayPulsePhase.snapTo(0f)
            return@LaunchedEffect
        }

        overlayPulsePhase.snapTo(0f)
        while (isActive && shouldPulseTrayPiece) {
            overlayPulsePhase.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = TrayPulseUpDurationMillis,
                    easing = FastOutSlowInEasing,
                ),
            )
            overlayPulsePhase.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = TrayPulseDownDurationMillis,
                    easing = FastOutSlowInEasing,
                ),
            )
        }
    }
    val trayPulseScaleProvider = {
        if (shouldPulseTrayPiece) {
            1f + (TrayPulseScaleBoost * overlayPulsePhase.value)
        } else {
            1f
        }
    }
    val pieceCellDp = with(density) { cellSizePx.toDp() }
    val resolvedPreviewStyle = resolveBoardBlockStyle(
        selectedStyle = settings.blockVisualStyle,
        mode = settings.boardBlockStyleMode,
    )
    val launchCellCornerRadius = boardCellCornerRadiusDp(
        cellSize = pieceCellDp,
        style = resolvedPreviewStyle,
    )

    PieceBlocks(
        piece = piece,
        cellSize = pieceCellDp,
        cellCornerRadius = launchCellCornerRadius,
        modifier = modifier
            .graphicsLayer {
                translationX = overlayX
                translationY = overlayY
                val trayScale = trayPulseScaleProvider()
                scaleX = overlayScale * trayScale
                scaleY = overlayScale * trayScale
                transformOrigin = TransformOrigin(0f, 0f)
            }
            .then(pointerModifier),
        alpha = when {
            isPaused -> 0.42f
            isLaunching -> LaunchPreviewAlpha
            else -> 1f
        },
        pulse = stylePulse,
    )
}

@Composable
private fun MinimalTopBar(
    gameState: GameState,
    highestScore: Int,
    highScoreHighlightStrengthProvider: () -> Float,
    highScoreHighlightScaleProvider: () -> Float,
    scoreHighlightStrengthProvider: () -> Float,
    scoreHighlightScaleProvider: () -> Float,
    onHoldPiece: () -> Unit,
    onPauseToggle: () -> Unit,
    onRestart: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTutorial: () -> Unit,
    controlsEnabled: Boolean = true,
    stylePulse: Float = 0f,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    val isNewRecordHighlight = highScoreHighlightStrengthProvider() > 0.08f
    val holdLabel = resolveGameText(gameText(GameTextKey.Hold))
    val pauseLabel =
        resolveGameText(gameText(if (gameState.status == GameStatus.Paused) GameTextKey.Resume else GameTextKey.Pause))
    val restartLabel = resolveGameText(gameText(GameTextKey.Restart))
    val dockMessage = resolveActivePieceProperties(piece = gameState.activePiece)
    val dockMessageColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f)
    val panelStrokeColor = uiColors.panelStroke.copy(alpha = TopBarPanelStrokeAlpha)
    val panelGlow = Brush.verticalGradient(
        colors = listOf(
            uiColors.panelHighlight.copy(alpha = TopBarPanelGlowAlpha),
            uiColors.launchGlow.copy(alpha = 0.10f),
            Color.Transparent,
        ),
    )
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = uiColors.gameSurface.copy(alpha = TopBarPanelAlpha)),
        border = BorderStroke(1.dp, panelStrokeColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(panelGlow)
                .padding(horizontal = 12.dp, vertical = TopBarVerticalPadding + 1.dp),
            verticalArrangement = Arrangement.spacedBy(TopBarRowSpacing),
        ) {
            MetricLaunchRow(
                highScoreTitle = if (isNewRecordHighlight) {
                    stringResource(Res.string.high_score_new_record)
                } else {
                    resolveGameText(gameText(GameTextKey.HighScore))
                },
                highScoreValue = highestScore.toString(),
                scoreTitle = resolveGameText(gameText(GameTextKey.Score)),
                scoreValue = gameState.score.toString(),
                highScoreHighlightStrengthProvider = highScoreHighlightStrengthProvider,
                highScoreHighlightScaleProvider = highScoreHighlightScaleProvider,
                scoreHighlightStrengthProvider = scoreHighlightStrengthProvider,
                scoreHighlightScaleProvider = scoreHighlightScaleProvider,
                launchContent = {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = uiColors.gameSurface.copy(
                                alpha = 0.66f
                            )
                        ),
                        border = BorderStroke(
                            1.dp,
                            uiColors.boardEmptyCellBorder.copy(alpha = 0.82f)
                        ),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            uiColors.launchGlow.copy(alpha = 0.14f),
                                            Color.Transparent,
                                        ),
                                    ),
                                )
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            TopBarActionBay(
                                modifier = Modifier.align(Alignment.End),
                            ) {
                                TopBarActionBlockButton(
                                    tone = TopBarActionBlockTones[0],
                                    icon = Icons.Filled.Settings,
                                    contentDescription = stringResource(Res.string.settings_title),
                                    onClick = onOpenSettings,
                                    enabled = controlsEnabled,
                                    pulse = stylePulse,
                                )
                                TopBarActionBlockButton(
                                    tone = TopBarActionBlockTones[1],
                                    icon = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = stringResource(Res.string.settings_tutorial),
                                    onClick = onOpenTutorial,
                                    enabled = controlsEnabled,
                                    pulse = stylePulse,
                                )
                                TopBarActionBlockButton(
                                    tone = TopBarActionBlockTones[2],
                                    icon = Icons.Filled.SwapHoriz,
                                    contentDescription = holdLabel,
                                    onClick = onHoldPiece,
                                    enabled = controlsEnabled && gameState.canHold && !gameState.isSoftLockActive,
                                    pulse = stylePulse,
                                )
                                TopBarActionBlockButton(
                                    tone = TopBarActionBlockTones[3],
                                    icon = if (gameState.status == GameStatus.Paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                    contentDescription = pauseLabel,
                                    onClick = onPauseToggle,
                                    enabled = controlsEnabled,
                                    pulse = stylePulse,
                                )
                                TopBarActionBlockButton(
                                    tone = TopBarActionBlockTones[4],
                                    icon = Icons.Filled.Refresh,
                                    contentDescription = restartLabel,
                                    onClick = onRestart,
                                    enabled = controlsEnabled,
                                    pulse = stylePulse,
                                )
                            }
                            LaunchBarView(gameState = gameState, stylePulse = stylePulse)
                            Text(
                                text = dockMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = dockMessageColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun TopBarActionBay(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(TopBarIconSpacing),
        verticalAlignment = Alignment.CenterVertically,
        content = { content() },
    )
}

@Composable
private fun MetricLaunchRow(
    highScoreTitle: String,
    highScoreValue: String,
    scoreTitle: String,
    scoreValue: String,
    highScoreHighlightStrengthProvider: () -> Float,
    highScoreHighlightScaleProvider: () -> Float,
    scoreHighlightStrengthProvider: () -> Float,
    scoreHighlightScaleProvider: () -> Float,
    launchContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(TopBarMetricLaunchSpacing),
    ) {
        EqualWidthMetricColumn(
            highScoreTitle = highScoreTitle,
            highScoreValue = highScoreValue,
            scoreTitle = scoreTitle,
            scoreValue = scoreValue,
            highScoreHighlightStrengthProvider = highScoreHighlightStrengthProvider,
            highScoreHighlightScaleProvider = highScoreHighlightScaleProvider,
            scoreHighlightStrengthProvider = scoreHighlightStrengthProvider,
            scoreHighlightScaleProvider = scoreHighlightScaleProvider,
            modifier = Modifier.width(IntrinsicSize.Max),
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            launchContent()
        }
    }
}

@Composable
private fun EqualWidthMetricColumn(
    highScoreTitle: String,
    highScoreValue: String,
    scoreTitle: String,
    scoreValue: String,
    highScoreHighlightStrengthProvider: () -> Float,
    highScoreHighlightScaleProvider: () -> Float,
    scoreHighlightStrengthProvider: () -> Float,
    scoreHighlightScaleProvider: () -> Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CompactMetricChip(
            title = highScoreTitle,
            value = highScoreValue,
            highlightStrengthProvider = highScoreHighlightStrengthProvider,
            scaleProvider = highScoreHighlightScaleProvider,
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
        CompactMetricChip(
            title = scoreTitle,
            value = scoreValue,
            highlightStrengthProvider = scoreHighlightStrengthProvider,
            scaleProvider = scoreHighlightScaleProvider,
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
    }
}

@Composable
private fun MinimalBottomDock(
    gameState: GameState,
    cellSizePx: Float,
    showNextPiece: Boolean,
    stylePulse: Float = 0f,
    modifier: Modifier = Modifier,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = uiColors.panel.copy(alpha = DockPanelAlpha)),
        border = BorderStroke(1.dp, uiColors.panelStroke.copy(alpha = DockPanelStrokeAlpha)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            uiColors.panelHighlight.copy(alpha = DockPanelGlowAlpha),
                            Color.Transparent,
                        ),
                    ),
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showNextPiece) {
                    HoldAndQueueStrip(
                        queue = gameState.nextQueue,
                        cellSize = (cellSizePx * NextPieceScale),
                        stylePulse = stylePulse,
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactMetricChip(
    title: String,
    value: String,
    highlightStrengthProvider: () -> Float = { 0f },
    scaleProvider: () -> Float = { 1f },
    modifier: Modifier = Modifier,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    val highlightStrength = highlightStrengthProvider()
    val isHighlighted = highlightStrength > MetricHighlightThreshold

    Card(
        modifier = modifier.graphicsLayer {
            val scale = scaleProvider()
            scaleX = scale
            scaleY = scale
        },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = uiColors.metricCard.copy(alpha = 0.92f)),
        border = BorderStroke(
            width = if (isHighlighted) 1.6.dp else 1.dp,
            color = if (isHighlighted) {
                uiColors.success.copy(alpha = (0.50f + highlightStrength * 0.50f).coerceIn(0f, 1f))
            } else {
                uiColors.panelStroke.copy(alpha = 0.72f)
            }
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .graphicsLayer {
                    alpha = (0.92f + highlightStrength * 0.08f).coerceIn(0f, 1f)
                }
                .padding(
                    horizontal = MetricChipHorizontalPadding,
                    vertical = MetricChipVerticalPadding
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isHighlighted) {
                Icon(
                    imageVector = Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    tint = uiColors.success,
                    modifier = Modifier
                        .size(RecordIndicatorIconSize)
                        .graphicsLayer { alpha = highlightStrengthProvider().coerceIn(0f, 1f) },
                )
                Spacer(modifier = Modifier.width(RecordIndicatorSideGap))
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    text = title,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelSmall,
                    color = uiColors.subtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = value,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }

            if (isHighlighted) {
                Spacer(modifier = Modifier.width(RecordIndicatorSideGap))
                Icon(
                    imageVector = Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    tint = uiColors.success,
                    modifier = Modifier
                        .size(RecordIndicatorIconSize)
                        .graphicsLayer { alpha = highlightStrength.coerceIn(0f, 1f) },
                )
            }
        }
    }
}

@Preview
@Composable
fun GameScreenRunningRecordIconsPreview() {
    val uiColors = StackShiftThemeTokens.uiColors
    Surface(color = Color.Black) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(uiColors.panel),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                CompactMetricChip(
                    title = "NEW RECORD",
                    value = "142,000",
                    highlightStrengthProvider = { 1f },
                    scaleProvider = { 1.03f },
                    modifier = Modifier.width(160.dp).height(54.dp)
                )

                CompactMetricChip(
                    title = "SCORE",
                    value = "128,450",
                    highlightStrengthProvider = { 0f },
                    scaleProvider = { 1f },
                    modifier = Modifier.width(160.dp).height(54.dp)
                )
            }
        }
    }
}

@Composable
private fun PauseOverlay(
    onPrimaryAction: () -> Unit,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(uiColors.overlay),
        contentAlignment = Alignment.Center,
    ) {
        GameEventDialogCard(
            modifier = Modifier.widthIn(max = GameOverDialogWidth),
            icon = Icons.Filled.Pause,
            iconColors = listOf(
                uiColors.warning.copy(alpha = 0.92f),
                uiColors.launchGlow.copy(alpha = 0.84f),
                uiColors.gameSurface.copy(alpha = 0.12f),
            ),
            title = resolveGameText(gameText(GameTextKey.PauseTitle)),
            message = resolveGameText(gameText(GameTextKey.GameMessagePaused)),
            buttonLabel = resolveGameText(gameText(GameTextKey.Continue)),
            onAction = onPrimaryAction,
        )
    }
}

@Composable
private fun TopBarActionBlockButton(
    tone: CellTone,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    pulse: Float = 0f,
) {
    val settings = LocalAppSettings.current
    val resolvedBlockStyle = resolveBoardBlockStyle(
        selectedStyle = settings.blockVisualStyle,
        mode = settings.boardBlockStyleMode,
    )
    val isDarkTheme = isStackShiftDarkTheme(settings)
    val iconColor = specialBlockIconTint(
        style = resolvedBlockStyle,
        isDarkTheme = isDarkTheme,
    ).copy(alpha = if (enabled) 1f else 0.58f)

    val launchCellCornerRadius = boardCellCornerRadiusDp(
        cellSize = TopBarActionBlockSize,
        style = resolvedBlockStyle,
    )

    Box(
        modifier = Modifier
            .size(TopBarActionBlockSize)
            .graphicsLayer { alpha = if (enabled) 1f else 0.72f }
            .clip(RoundedCornerShape(launchCellCornerRadius))
            .clickable(enabled = enabled) { onClick.invoke() },
        contentAlignment = Alignment.Center,
    ) {
        BlockCellPreview(
            tone = tone,
            palette = settings.blockColorPalette,
            style = resolvedBlockStyle,
            size = TopBarActionBlockSize,
            modifier = Modifier.size(TopBarActionBlockSize),
            alpha = if (enabled) 1f else 0.52f,
            pulse = pulse,
        )
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.size(TopBarActionIconSize),
        )
    }
}

@Composable
private fun RestartConfirmDialog(
    onDismissRequest: () -> Unit,
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        GameEventDialogCard(
            modifier = Modifier.widthIn(max = GameOverDialogWidth),
            title = title,
            message = message,
            buttonLabel = confirmLabel,
            onAction = onConfirm,
            secondaryButtonLabel = dismissLabel,
            onSecondaryAction = onDismissRequest,
            icon = Icons.Filled.Refresh,
            iconColors = listOf(
                MaterialTheme.colorScheme.error,
                MaterialTheme.colorScheme.errorContainer,
            )
        )
    }
}

@Composable
private fun GameEventDialogCard(
    title: String,
    message: String,
    buttonLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.EmojiEvents,
    iconColors: List<Color> = listOf(Color.Yellow, Color.Red),
    secondaryButtonLabel: String? = null,
    secondaryButtonColor: Color? = null,
    onSecondaryAction: (() -> Unit)? = null,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = uiColors.panel.copy(alpha = 0.98f)),
        border = BorderStroke(1.dp, uiColors.panelStroke),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            uiColors.panelHighlight.copy(alpha = 0.12f),
                            Color.Transparent,
                        ),
                    ),
                )
                .padding(GameOverDialogCardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DialogHeroIcon(
                icon = icon,
                iconColors = iconColors,
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = uiColors.subtitle,
                    textAlign = TextAlign.Center,
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DialogActionButton(
                    text = buttonLabel,
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth(),
                    emphasized = true,
                )

                if (secondaryButtonLabel != null && onSecondaryAction != null) {
                    DialogActionButton(
                        text = secondaryButtonLabel,
                        onClick = onSecondaryAction,
                        modifier = Modifier.fillMaxWidth(),
                        emphasized = false,
                        textColor = secondaryButtonColor ?: uiColors.subtitle,
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogHeroIcon(
    icon: ImageVector,
    iconColors: List<Color>,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(78.dp)
            .background(
                brush = Brush.radialGradient(iconColors),
                shape = RoundedCornerShape(24.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(GameOverDialogIconSize),
        )
    }
}

@Composable
private fun DialogActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    emphasized: Boolean = true,
    icon: ImageVector? = null,
    textColor: Color? = null,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    val containerColor = when {
        !enabled -> uiColors.actionButtonDisabled
        emphasized -> MaterialTheme.colorScheme.primary.copy(alpha = 0.92f)
        else -> uiColors.panelMuted.copy(alpha = 0.88f)
    }
    val contentColor = textColor ?: when {
        emphasized -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val borderColor = when {
        !enabled -> uiColors.panelStroke.copy(alpha = 0.32f)
        emphasized -> Color.White.copy(alpha = 0.28f)
        else -> uiColors.panelStroke.copy(alpha = 0.72f)
    }

    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        enabled = enabled,
        shape = CircleShape,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
        border = BorderStroke(1.dp, borderColor),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = uiColors.panelMuted.copy(alpha = 0.74f),
            disabledContentColor = contentColor.copy(alpha = 0.58f),
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun LaunchBarView(
    gameState: GameState,
    stylePulse: Float = 0f,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    val progressPercent = (gameState.launchBar.progress * 100).toInt()
    val isBoostActive = gameState.launchBar.isBoostActive
    val animatedProgress by animateFloatAsState(
        targetValue = gameState.launchBar.progress,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "launchBarProgress",
    )
    val boostGlow by rememberInfiniteTransition(label = "launchBarBoostGlow").animateFloat(
        initialValue = 0.45f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
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
                                uiColors.launchGlow.copy(alpha = if (isBoostActive) boostGlow else 0.8f + (0.2f * stylePulse)),
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
            text = if (isBoostActive) stringResource(
                Res.string.launch_boost_active,
                gameState.launchBar.boostTurnsRemaining
            ) else stringResource(
                Res.string.launch_special_chance,
                (gameState.launchBar.specialPieceChance * 100).toInt()
            ),
            style = MaterialTheme.typography.labelSmall,
            color = uiColors.subtitle.copy(alpha = 0.88f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HoldAndQueueStrip(
    queue: List<Piece>,
    cellSize: Float,
    stylePulse: Float = 0f,
) {
    Row(
        modifier = Modifier.fillMaxHeight(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        queue.take(1).forEachIndexed { index, piece ->
            QueueSlot(
                label = if (index == 0) resolveGameText(gameText(GameTextKey.QueueNextShort)) else "",
                piece = piece,
                cellSize = cellSize,
                alpha = 1f,
                stylePulse = stylePulse,
            )
        }
    }
}

@Composable
private fun QueueSlot(
    label: String,
    piece: Piece?,
    cellSize: Float,
    alpha: Float = 1f,
    stylePulse: Float = 0f,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    val density = LocalDensity.current
    val cellSizeDp = with(density) { cellSize.toDp() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = uiColors.subtitle.copy(alpha = alpha * 0.82f),
            )
        }
        Box(
            modifier = Modifier
                .size(width = cellSizeDp * 2.8f, height = cellSizeDp * 2.8f)
                .clip(RoundedCornerShape(14.dp))
                .background(uiColors.panelMuted.copy(alpha = alpha * 0.42f))
                .border(
                    width = 1.dp,
                    color = uiColors.boardEmptyCellBorder.copy(alpha = alpha * 0.52f),
                    shape = RoundedCornerShape(14.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (piece != null) {
                PieceBlocks(
                    piece = piece,
                    cellSize = cellSizeDp * 0.72f,
                    alpha = alpha * QueuePreviewAlpha,
                    pulse = stylePulse,
                )
            } else {
                Text(
                    text = resolveGameText(gameText(GameTextKey.QueueEmpty)),
                    style = MaterialTheme.typography.labelMedium,
                    color = uiColors.subtitle.copy(alpha = 0.32f),
                )
            }
        }
    }
}

@Composable
fun FloatingFeedbackBubble(
    text: String,
    modifier: Modifier = Modifier,
    isBonus: Boolean = false,
    alpha: Float = 1f,
    driftY: Float = 0f,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    Box(
        modifier = modifier
            .graphicsLayer {
                this.alpha = alpha
                translationY = driftY
            }
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isBonus) uiColors.success.copy(alpha = 0.94f) else uiColors.metricCard.copy(
                    alpha = 0.94f
                )
            )
            .border(
                width = 1.dp,
                color = if (isBonus) Color.White.copy(alpha = 0.42f) else uiColors.panelStroke.copy(
                    alpha = 0.52f
                ),
                shape = RoundedCornerShape(14.dp),
            )
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (isBonus) Color.White else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun GameOverDialog(
    gameState: GameState,
    highestScore: Int,
    showNewHighScoreMessage: Boolean,
    revealProgressProvider: () -> Float,
    canUseExtraLife: Boolean,
    isExtraLifeLoading: Boolean,
    showExtraLifeButton: Boolean,
    onPlayAgain: () -> Unit,
    onUseExtraLife: () -> Unit,
) {
    val density = LocalDensity.current
    val revealOffsetPx = with(density) { GameOverDialogRevealOffsetDp.toPx() }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        val revealProgress = revealProgressProvider()
        GameOverDialogContent(
            gameState = gameState,
            highestScore = highestScore,
            showNewHighScoreMessage = showNewHighScoreMessage,
            canUseExtraLife = canUseExtraLife,
            isExtraLifeLoading = isExtraLifeLoading,
            showExtraLifeButton = showExtraLifeButton,
            onPlayAgain = onPlayAgain,
            onUseExtraLife = onUseExtraLife,
            modifier = Modifier
                .widthIn(max = GameOverDialogWidth)
                .graphicsLayer {
                    alpha = revealProgress
                    scaleX = 0.90f + (0.10f * revealProgress)
                    scaleY = 0.90f + (0.10f * revealProgress)
                    translationY = (1f - revealProgress) * revealOffsetPx
                },
        )
    }
}

@Composable
private fun GameOverDialogContent(
    gameState: GameState,
    highestScore: Int,
    showNewHighScoreMessage: Boolean,
    canUseExtraLife: Boolean,
    isExtraLifeLoading: Boolean,
    showExtraLifeButton: Boolean,
    onPlayAgain: () -> Unit,
    onUseExtraLife: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    val recordPulsePhase = remember { Animatable(0f) }

    LaunchedEffect(showNewHighScoreMessage) {
        if (!showNewHighScoreMessage) {
            recordPulsePhase.snapTo(0f)
            return@LaunchedEffect
        }

        recordPulsePhase.snapTo(0f)
        while (isActive && showNewHighScoreMessage) {
            recordPulsePhase.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = MetricHighlightPulseUpDurationMillis,
                    easing = FastOutSlowInEasing,
                ),
            )
            recordPulsePhase.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = MetricHighlightPulseDownDurationMillis,
                    easing = FastOutSlowInEasing,
                ),
            )
        }
    }

    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = uiColors.panel.copy(alpha = 0.98f)),
        border = BorderStroke(1.dp, uiColors.panelStroke),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            uiColors.panelHighlight.copy(alpha = 0.12f),
                            Color.Transparent,
                        ),
                    ),
                )
                .padding(GameOverDialogCardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DialogHeroIcon(
                icon = Icons.Filled.EmojiEvents,
                iconColors = listOf(
                    if (showNewHighScoreMessage) uiColors.success.copy(alpha = 0.94f) else uiColors.warning.copy(alpha = 0.94f),
                    if (showNewHighScoreMessage) uiColors.success.copy(alpha = 0.70f) else uiColors.danger.copy(alpha = 0.72f),
                    uiColors.panel.copy(alpha = 0.12f),
                ),
                modifier = Modifier.graphicsLayer {
                    scaleX =
                        if (showNewHighScoreMessage) 1f + ((MetricHighlightPulseScale - 1f) * 0.28f * recordPulsePhase.value) else 1f
                    scaleY =
                        if (showNewHighScoreMessage) 1f + ((MetricHighlightPulseScale - 1f) * 0.28f * recordPulsePhase.value) else 1f
                },
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = if (showNewHighScoreMessage) stringResource(Res.string.high_score_new_record) else resolveGameText(
                        gameText(GameTextKey.GameOverTitle)
                    ),
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (showNewHighScoreMessage) uiColors.success else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (showNewHighScoreMessage) resolveGameText(gameText(GameTextKey.GameOverNewHighScore)) else resolveGameText(
                        gameState.message
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = uiColors.subtitle,
                    textAlign = TextAlign.Center,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GameOverStatChip(
                    title = if (showNewHighScoreMessage) stringResource(Res.string.high_score_new_record) else resolveGameText(
                        gameText(GameTextKey.HighScore)
                    ),
                    value = highestScore.toString(),
                    accentColor = if (showNewHighScoreMessage) uiColors.success else null,
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer {
                            scaleX =
                                if (showNewHighScoreMessage) 1f + ((MetricHighlightPulseScale - 1f) * 0.10f * recordPulsePhase.value) else 1f
                            scaleY =
                                if (showNewHighScoreMessage) 1f + ((MetricHighlightPulseScale - 1f) * 0.10f * recordPulsePhase.value) else 1f
                        }
                )
                GameOverStatChip(
                    title = resolveGameText(gameText(GameTextKey.Score)),
                    value = gameState.score.toString(),
                    modifier = Modifier.weight(1f),
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (showExtraLifeButton) {
                    DialogActionButton(
                        text = if (isExtraLifeLoading) {
                            stringResource(Res.string.game_over_extra_life_loading)
                        } else {
                            stringResource(Res.string.game_over_extra_life)
                        },
                        onClick = onUseExtraLife,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = canUseExtraLife && !isExtraLifeLoading,
                        emphasized = true,
                        icon = Icons.Filled.PlayArrow,
                    )
                }

                DialogActionButton(
                    text = stringResource(Res.string.play_again),
                    onClick = onPlayAgain,
                    modifier = Modifier.fillMaxWidth(),
                    emphasized = !showExtraLifeButton,
                )
            }
        }
    }
}

@Composable
private fun GameOverStatChip(
    title: String,
    value: String,
    accentColor: Color? = null,
    modifier: Modifier = Modifier,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = uiColors.metricCard.copy(alpha = 0.88f)),
        border = BorderStroke(
            1.dp,
            accentColor?.copy(alpha = 0.52f) ?: uiColors.panelStroke.copy(alpha = 0.52f)
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor ?: uiColors.subtitle,
                maxLines = 1,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun InteractiveOnboardingCompletionDialog(
    onStartGame: () -> Unit,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = uiColors.panel.copy(alpha = 0.98f)),
            border = BorderStroke(1.dp, uiColors.panelStroke),
            modifier = Modifier.widthIn(max = GameOverDialogWidth),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                uiColors.panelHighlight.copy(alpha = 0.12f),
                                Color.Transparent,
                            ),
                        ),
                    )
                    .padding(GameOverDialogCardPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                DialogHeroIcon(
                    icon = Icons.Filled.EmojiEvents,
                    iconColors = listOf(
                        uiColors.success.copy(alpha = 0.94f),
                        uiColors.launchGlow.copy(alpha = 0.84f),
                        uiColors.panel.copy(alpha = 0.12f),
                    ),
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.tutorial_ready_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(Res.string.tutorial_ready_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = uiColors.subtitle,
                        textAlign = TextAlign.Center,
                    )
                }

                DialogActionButton(
                    text = stringResource(Res.string.tutorial_finish),
                    onClick = onStartGame,
                    modifier = Modifier.fillMaxWidth(),
                    emphasized = true,
                )
            }
        }
    }
}

@Composable
private fun resolveGameText(text: GameText): String {
    return stringResource(text.key.stringResourceId(), *text.args.toTypedArray())
}

private fun SpecialBlockType.shortLabel(): GameText {
    return when (this) {
        SpecialBlockType.ColumnClearer -> gameText(GameTextKey.SpecialColumnClearer)
        SpecialBlockType.RowClearer -> gameText(GameTextKey.SpecialRowClearer)
        SpecialBlockType.Ghost -> gameText(GameTextKey.SpecialGhost)
        SpecialBlockType.Heavy -> gameText(GameTextKey.SpecialHeavy)
        SpecialBlockType.None -> gameText(GameTextKey.PiecePropertiesNone)
    }
}

fun GameTextKey.stringResourceId(): StringResource {
    return when (this) {
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
        GameTextKey.Lines -> Res.string.lines
        GameTextKey.Boost -> Res.string.boost
        GameTextKey.Danger -> Res.string.danger
        GameTextKey.DangerNone -> Res.string.danger_none
        GameTextKey.Launch -> Res.string.launch_label
        GameTextKey.LaunchBar -> Res.string.launch_bar
        GameTextKey.LaunchBoostActive -> Res.string.launch_boost_active
        GameTextKey.LaunchSpecialChance -> Res.string.launch_special_chance
        GameTextKey.LaunchSoftLockMessage -> Res.string.launch_soft_lock_message
        GameTextKey.LaunchChainMessage -> Res.string.launch_chain_message
        GameTextKey.LaunchPaused -> Res.string.launch_paused
        GameTextKey.LaunchGameOver -> Res.string.launch_game_over
        GameTextKey.LaunchDragHint -> Res.string.launch_drag_hint
        GameTextKey.QueueHold -> Res.string.hold
        GameTextKey.QueueNextShort -> Res.string.queue_next_short
        GameTextKey.QueueEmpty -> Res.string.queue_empty
        GameTextKey.PauseTitle -> Res.string.pause_title
        GameTextKey.GameOverTitle -> Res.string.game_over_title
        GameTextKey.Continue -> Res.string.continue_label
        GameTextKey.GameOverExtraLife -> Res.string.game_over_extra_life
        GameTextKey.GameOverExtraLifeLoading -> Res.string.game_over_extra_life_loading
        GameTextKey.PlayAgain -> Res.string.play_again
        GameTextKey.GameOverNewHighScore -> Res.string.game_over_new_high_score
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
        GameTextKey.GameMessageExtraLifeUsed -> Res.string.game_message_extra_life_used
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
        GameTextKey.FeedbackExtraLife -> Res.string.feedback_extra_life
        GameTextKey.SpecialColumnClearer -> Res.string.special_column_clearer
        GameTextKey.SpecialRowClearer -> Res.string.special_row_clearer
        GameTextKey.SpecialGhost -> Res.string.special_ghost
        GameTextKey.SpecialHeavy -> Res.string.special_heavy
        GameTextKey.PiecePropertiesNone -> Res.string.piece_properties_none
    }
}

@Composable
fun rememberGameHaptics(): GameHaptics {
    val hapticFeedback = LocalHapticFeedback.current
    return remember(hapticFeedback) {
        object : GameHaptics {
            override fun perform(effect: GameHaptic) {
                val type = when (effect) {
                    GameHaptic.Light -> HapticFeedbackType.TextHandleMove
                    GameHaptic.Medium -> HapticFeedbackType.LongPress
                    GameHaptic.Heavy -> HapticFeedbackType.LongPress
                    GameHaptic.Warning -> HapticFeedbackType.LongPress
                    GameHaptic.Success -> HapticFeedbackType.LongPress
                }
                hapticFeedback.performHapticFeedback(type)
            }
        }
    }
}

fun dispatchFeedback(
    feedback: InteractionFeedback,
    soundPlayer: SoundEffectPlayer,
    haptics: GameHaptics,
) {
    feedback.sounds.forEach { soundPlayer.play(it) }
    feedback.haptics.forEach { haptics.perform(it) }
}

fun resolveSelectedColumn(
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

fun pieceSpawnTopLeft(
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

fun resolveSpawnColumn(piece: Piece?, boardColumns: Int, lastPlacementColumn: Int?): Int? {
    if (piece == null) return null
    val last = lastPlacementColumn ?: (boardColumns / 2)
    return last.coerceIn(0, (boardColumns - piece.width).coerceAtLeast(0))
}

fun columnToLeft(column: Int, boardRect: Rect, cellSizePx: Float): Float =
    boardRect.left + (column * cellSizePx)

private fun Rect.toLocalRect(hostRect: Rect): Rect {
    if (this == Rect.Zero || hostRect == Rect.Zero) return Rect.Zero
    return Rect(
        left - hostRect.left,
        top - hostRect.top,
        right - hostRect.left,
        bottom - hostRect.top,
    )
}


@Preview
@Composable
private fun GameScreenRunningPreview() {
    StackShiftTheme(settings = AppSettings()) {
        GameScreen(
            gameState = previewGameState(),
            onRequestPreview = { previewPlacementPreview() },
            onResolvePreviewImpact = { previewImpactPointsPreview() },
            onPlacePiece = { GameDispatchResult() },
            onHoldPiece = { InteractionFeedback.None },
            onPauseToggle = { InteractionFeedback.None },
            onRestart = { InteractionFeedback.None },
            onRewardedRevive = { InteractionFeedback.None },
            onOpenSettings = {},
            onOpenTutorial = {},
            soundPlayer = NoOpSoundEffectPlayer,
            haptics = NoOpGameHaptics,
            highestScore = 142000,
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
            onPlacePiece = { GameDispatchResult() },
            onHoldPiece = { InteractionFeedback.None },
            onPauseToggle = { InteractionFeedback.None },
            onRestart = { InteractionFeedback.None },
            onRewardedRevive = { InteractionFeedback.None },
            onOpenSettings = {},
            onOpenTutorial = {},
            soundPlayer = NoOpSoundEffectPlayer,
            haptics = NoOpGameHaptics,
            highestScore = 142000,
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
            onPlacePiece = { GameDispatchResult() },
            onHoldPiece = { InteractionFeedback.None },
            onPauseToggle = { InteractionFeedback.None },
            onRestart = { InteractionFeedback.None },
            onRewardedRevive = { InteractionFeedback.None },
            onOpenSettings = {},
            onOpenTutorial = {},
            soundPlayer = NoOpSoundEffectPlayer,
            haptics = NoOpGameHaptics,
            highestScore = 142000,
        )
    }
}

@Preview
@Composable
private fun GameOverDialogPreview() {
    StackShiftTheme(settings = AppSettings()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            GameOverDialog(
                gameState = previewGameState(status = GameStatus.GameOver),
                highestScore = 142000,
                showNewHighScoreMessage = false,
                revealProgressProvider = { 1f },
                canUseExtraLife = true,
                isExtraLifeLoading = false,
                showExtraLifeButton = true,
                onPlayAgain = {},
                onUseExtraLife = {},
            )
        }
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
        score = 14200,
        level = 3,
        linesCleared = 18,
        difficultyStage = 2,
        secondsUntilDifficultyIncrease = 12,
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
        ),
        status = status,
    )
}

private fun previewPlacementPreview(): PlacementPreview = PlacementPreview(
    selectedColumn = 3,
    entryAnchor = GridPoint(3, 0),
    landingAnchor = GridPoint(3, 14),
    occupiedCells = listOf(GridPoint(3, 14), GridPoint(4, 14), GridPoint(5, 14), GridPoint(4, 15)),
    coveredColumns = 3..5,
)

private fun previewImpactPointsPreview(): Set<GridPoint> = setOf(
    GridPoint(3, 14),
    GridPoint(4, 14),
    GridPoint(5, 14),
    GridPoint(4, 15)
)
