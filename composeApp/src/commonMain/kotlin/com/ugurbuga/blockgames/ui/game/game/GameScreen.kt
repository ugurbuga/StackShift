package com.ugurbuga.blockgames.ui.game.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ugurbuga.blockgames.ads.GameAdController
import com.ugurbuga.blockgames.ads.NoOpGameAdController
import com.ugurbuga.blockgames.game.logic.GameEvent
import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.game.model.GameStatus
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.SpecialBlockType
import com.ugurbuga.blockgames.platform.GlobalPlatformConfig
import com.ugurbuga.blockgames.platform.feedback.NoOpSoundEffectPlayer
import com.ugurbuga.blockgames.platform.feedback.SoundEffectPlayer
import com.ugurbuga.blockgames.presentation.game.GameViewModel
import com.ugurbuga.blockgames.presentation.game.InteractionFeedback
import com.ugurbuga.blockgames.settings.BlockWiseOnboardingStage
import com.ugurbuga.blockgames.settings.BlockWiseOnboardingStateFactory
import com.ugurbuga.blockgames.settings.HighScoreStorage
import com.ugurbuga.blockgames.settings.MergeShiftOnboardingStage
import com.ugurbuga.blockgames.settings.MergeShiftOnboardingStateFactory
import com.ugurbuga.blockgames.settings.OnboardingStage
import com.ugurbuga.blockgames.settings.StackShiftGameOnboardingStateFactory
import com.ugurbuga.blockgames.settings.StackShiftOnboardingStage
import com.ugurbuga.blockgames.telemetry.AppTelemetry
import com.ugurbuga.blockgames.telemetry.LogScreen
import com.ugurbuga.blockgames.telemetry.NoOpAppTelemetry
import com.ugurbuga.blockgames.telemetry.TelemetryActionNames
import com.ugurbuga.blockgames.telemetry.TelemetryScreenNames
import com.ugurbuga.blockgames.ui.theme.BlockGamesThemeTokens
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds


private const val InteractiveOnboardingStageAdvanceDelayMillis = 720L
private const val InteractiveOnboardingClearAnimationDurationMillis = 620
private const val InteractiveOnboardingBoardShiftDurationMillis = 360

@Composable
fun BlockGamesGameApp(
    modifier: Modifier = Modifier,
    soundPlayer: SoundEffectPlayer = NoOpSoundEffectPlayer,
    telemetry: AppTelemetry = NoOpAppTelemetry,
    adController: GameAdController = NoOpGameAdController,
    viewModel: GameViewModel = remember { GameViewModel() },
    interactiveOnboardingEnabled: Boolean = false,
    onInteractiveOnboardingFinished: (GameState) -> Unit = {},
    onInteractiveOnboardingReturnHome: (GameState) -> Unit = {},
    onReplaceActivePieceRewarded: (SpecialBlockType) -> Unit = {},
    onBack: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenTutorial: () -> Unit = {},
) {
    val gameplayStyle = GlobalPlatformConfig.gameplayStyle
    val haptics = rememberGameHaptics()
    val uiState by viewModel.uiState.collectAsState()
    val uiColors = BlockGamesThemeTokens.uiColors
    val onboardingStages: List<OnboardingStage> = remember(gameplayStyle) {
        when (gameplayStyle) {
            GameplayStyle.BlockWise -> BlockWiseOnboardingStateFactory.stages
            GameplayStyle.MergeShift -> MergeShiftOnboardingStateFactory.stages
            else -> StackShiftGameOnboardingStateFactory.stages
        }
    }
    var onboardingStage by remember(interactiveOnboardingEnabled, gameplayStyle) {
        mutableStateOf<OnboardingStage?>(
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

    LaunchedEffect(uiState.gameState.status) {
        while (uiState.gameState.status == GameStatus.Running) {
            delay(1000.milliseconds)
            viewModel.tick()
        }
    }

    val stackShiftOnboardingScene = remember(interactiveOnboardingEnabled, onboardingStage) {
        if (interactiveOnboardingEnabled && onboardingStage is StackShiftOnboardingStage) {
            StackShiftGameOnboardingStateFactory.scene(onboardingStage as StackShiftOnboardingStage)
        } else null
    }

    val blockWiseOnboardingScene = remember(interactiveOnboardingEnabled, onboardingStage) {
        if (interactiveOnboardingEnabled && onboardingStage is BlockWiseOnboardingStage) {
            BlockWiseOnboardingStateFactory.scene(onboardingStage as BlockWiseOnboardingStage)
        } else null
    }

    val mergeShiftOnboardingScene = remember(interactiveOnboardingEnabled, onboardingStage) {
        if (interactiveOnboardingEnabled && onboardingStage is MergeShiftOnboardingStage) {
            MergeShiftOnboardingStateFactory.scene(onboardingStage as MergeShiftOnboardingStage)
        } else null
    }

    val onboardingSceneGameState = stackShiftOnboardingScene?.gameState
        ?: blockWiseOnboardingScene?.gameState
        ?: mergeShiftOnboardingScene?.gameState

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

    LaunchedEffect(interactiveOnboardingEnabled, onboardingSceneGameState) {
        if (!interactiveOnboardingEnabled || onboardingSceneGameState == null) return@LaunchedEffect
        onboardingAwaitingCommit = false
        onboardingAdvanceRequest = null
        showOnboardingCompletionDialog = false
        pendingOnboardingCompletionState = null
        viewModel.replaceState(onboardingSceneGameState)

        val stageName = onboardingStage?.name ?: "unknown"

        telemetry.logUserAction(
            action = "interactive_onboarding_stage_shown",
            parameters = mapOf("stage" to stageName),
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

        val stageName = request.completedStage.name

        telemetry.logUserAction(
            action = "interactive_onboarding_stage_completed",
            parameters = mapOf("stage" to stageName),
        )

        if (request.nextStage != null) {
            onboardingStage = request.nextStage
        } else {
            pendingOnboardingCompletionState = when (gameplayStyle) {
                GameplayStyle.BlockWise -> BlockWiseOnboardingStateFactory.cleanGameState()
                GameplayStyle.MergeShift -> MergeShiftOnboardingStateFactory.cleanGameState()
                else -> StackShiftGameOnboardingStateFactory.cleanGameState()
            }
            showOnboardingCompletionDialog = true
            onboardingStage = null
        }

        onboardingAwaitingCommit = false
        onboardingAdvanceRequest = null
    }

    LaunchedEffect(
        interactiveOnboardingEnabled,
        onboardingStage,
        onboardingAwaitingCommit,
        onboardingAdvanceRequest,
        uiState.gameState.softLock,
        uiState.gameState.board,
        uiState.gameState.activePiece?.id,
        uiState.gameState.score,
        uiState.gameState.linesCleared,
    ) {
        if (!interactiveOnboardingEnabled || !onboardingAwaitingCommit || onboardingAdvanceRequest != null) {
            return@LaunchedEffect
        }
        val sceneGameState = onboardingSceneGameState ?: return@LaunchedEffect

        val placementCommitted = uiState.gameState.softLock == null && (
                uiState.gameState.board != sceneGameState.board ||
                        uiState.gameState.activePiece?.id != sceneGameState.activePiece?.id ||
                        uiState.gameState.score != sceneGameState.score ||
                        uiState.gameState.linesCleared != sceneGameState.linesCleared
                )
        if (!placementCommitted) return@LaunchedEffect

        val currentStage = onboardingStage ?: return@LaunchedEffect
        onboardingAdvanceRequest = InteractiveOnboardingAdvanceRequest(
            completedStage = currentStage,
            nextStage = nextInteractiveOnboardingStage(currentStage, onboardingStages),
        )
    }

    var highestScore by remember(uiState.gameState.gameMode, uiState.gameState.gameplayStyle) {
        mutableIntStateOf(
            HighScoreStorage.load(
                uiState.gameState.gameMode
            )
        )
    }
    var newHighScoreReached by remember(
        uiState.gameState.gameMode,
        uiState.gameState.gameplayStyle
    ) { mutableStateOf(value = false) }
    LaunchedEffect(
        uiState.gameState.score,
        uiState.gameState.gameMode,
        uiState.gameState.gameplayStyle
    ) {
        if (uiState.gameState.score > highestScore) {
            telemetry.logHighScoreReached(
                newScore = uiState.gameState.score,
                previousHighScore = highestScore,
            )
            highestScore = uiState.gameState.score
            HighScoreStorage.save(
                highestScore,
                uiState.gameState.gameMode
            )
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
    when (gameplayStyle) {
        GameplayStyle.BlockWise -> BlockWiseGameScreen(
            modifier = modifier,
            gameState = displayGameState,
            onRequestPreview = { pieceId, origin -> viewModel.previewPlacement(pieceId, origin) },
            onResolvePreviewImpact = viewModel::previewImpactPoints,
            onPlacePiece = { pieceId, origin ->
                telemetry.logUserAction("place_piece_free")
                val result = viewModel.placePieceResult(pieceId, origin)
                dispatchFeedback(result.feedback, soundPlayer, haptics)

                if (interactiveOnboardingEnabled && blockWiseOnboardingScene != null) {
                    if (GameEvent.PlacementAccepted in result.events) {
                        onboardingAdvanceRequest = InteractiveOnboardingAdvanceRequest(
                            completedStage = blockWiseOnboardingScene.stage,
                            nextStage = nextInteractiveOnboardingStage(
                                blockWiseOnboardingScene.stage,
                                onboardingStages
                            ),
                        )
                    }
                }
            },
            onRestart = {
                telemetry.logUserAction(TelemetryActionNames.RestartGame)
                dispatchFeedback(
                    viewModel.restart(
                        config = uiState.gameState.config,
                    ),
                    soundPlayer,
                    haptics,
                )
            },
            onRewardedRevive = {
                telemetry.logUserAction("rewarded_revive")
                dispatchFeedback(viewModel.reviveFromReward(), soundPlayer, haptics)
            },
            onBack = onBack,
            highestScore = highestScore,
            showNewHighScoreMessage = newHighScoreReached,
            adController = adController,
            telemetry = telemetry,
            interactiveOnboardingScene = blockWiseOnboardingScene,
            interactiveOnboardingCurrentStep = onboardingStages.indexOf(onboardingStage)
                .takeIf { it >= 0 }?.plus(1) ?: 0,
            interactiveOnboardingTotalSteps = onboardingStages.size,
            interactiveOnboardingAwaitingCommit = onboardingAwaitingCommit,
            interactiveOnboardingCompletionDialogVisible = showOnboardingCompletionDialog,
            onInteractiveOnboardingStartGame = {
                pendingOnboardingCompletionState?.let(onInteractiveOnboardingFinished)
            },
            onInteractiveOnboardingReturnHome = {
                pendingOnboardingCompletionState?.let(onInteractiveOnboardingReturnHome)
            },
        )

        GameplayStyle.StackShift -> StackShiftGameScreen(
            modifier = modifier,
            gameState = displayGameState,
            onRequestPreview = viewModel::previewPlacement,
            onResolvePreviewImpact = viewModel::previewImpactPoints,
            onPlacePiece = { column ->
                telemetry.logUserAction("place_piece_stackshift")
                viewModel.placePieceResult(column).also { result ->
                    val scene = stackShiftOnboardingScene
                    if (!interactiveOnboardingEnabled || scene == null) return@also

                    when {
                        GameEvent.PlacementAccepted in result.events -> {
                            onboardingAwaitingCommit = false
                            onboardingAdvanceRequest = InteractiveOnboardingAdvanceRequest(
                                completedStage = scene.stage,
                                nextStage = nextInteractiveOnboardingStage(
                                    scene.stage,
                                    onboardingStages
                                ),
                            )
                        }

                        GameEvent.SoftLockStarted in result.events || GameEvent.SoftLockAdjusted in result.events -> {
                            onboardingAwaitingCommit = true
                            onboardingAdvanceRequest = null
                        }
                    }
                }
            },
            onHoldPiece = viewModel::holdPiece,
            onReplaceActivePiece = { specialType ->
                onReplaceActivePieceRewarded(specialType)
                InteractionFeedback.None
            },
            onRestart = {
                telemetry.logUserAction(TelemetryActionNames.RestartGame)
                viewModel.restart(
                    config = uiState.gameState.config,
                )
            },
            onRewardedRevive = {
                telemetry.logUserAction("rewarded_revive")
                viewModel.reviveFromReward()
            },
            onBack = onBack,
            onOpenSettings = onOpenSettings,
            onOpenTutorial = onOpenTutorial,
            telemetry = telemetry,
            adController = adController,
            soundPlayer = soundPlayer,
            haptics = haptics,
            highestScore = highestScore,
            showLaunchOverlayInitially = shouldShowLaunchOverlay,
            onLaunchOverlayFinished = { shouldShowLaunchOverlay = false },
            showNewHighScoreMessage = newHighScoreReached,
            interactiveOnboardingScene = stackShiftOnboardingScene,
            interactiveOnboardingCurrentStep = onboardingStages.indexOf(onboardingStage)
                .takeIf { it >= 0 }?.plus(1) ?: 0,
            interactiveOnboardingTotalSteps = onboardingStages.size,
            interactiveOnboardingAwaitingCommit = onboardingAwaitingCommit,
            interactiveOnboardingCompletionDialogVisible = showOnboardingCompletionDialog,
            onInteractiveOnboardingStartGame = {
                pendingOnboardingCompletionState?.let(onInteractiveOnboardingFinished)
            },
            onInteractiveOnboardingReturnHome = {
                pendingOnboardingCompletionState?.let(onInteractiveOnboardingReturnHome)
            },
        )

        GameplayStyle.MergeShift -> MergeShiftGameScreen(
            modifier = modifier,
            gameState = displayGameState,
            onRequestPreview = viewModel::previewPlacement,
            onRequestImpactPoints = viewModel::previewImpactPoints,
            onPlacePiece = { column ->
                telemetry.logUserAction("place_piece_mergeshift")
                viewModel.placePieceResult(column).also { result ->
                    val scene = mergeShiftOnboardingScene
                    if (!interactiveOnboardingEnabled || scene == null) return@also

                    if (GameEvent.PlacementAccepted in result.events) {
                        onboardingAdvanceRequest = InteractiveOnboardingAdvanceRequest(
                            completedStage = scene.stage,
                            nextStage = nextInteractiveOnboardingStage(
                                scene.stage,
                                onboardingStages
                            ),
                        )
                    }
                }
            },
            onRestart = {
                telemetry.logUserAction(TelemetryActionNames.RestartGame)
                viewModel.restart()
            },
            onTick = viewModel::tick,
            onBack = onBack,
            telemetry = telemetry,
            soundPlayer = soundPlayer,
            haptics = haptics,
            highestScore = highestScore,
            interactiveOnboardingScene = mergeShiftOnboardingScene,
            interactiveOnboardingCurrentStep = onboardingStages.indexOf(onboardingStage)
                .takeIf { it >= 0 }?.plus(1) ?: 0,
            interactiveOnboardingTotalSteps = onboardingStages.size,
            interactiveOnboardingAwaitingCommit = onboardingAwaitingCommit,
            interactiveOnboardingCompletionDialogVisible = showOnboardingCompletionDialog,
            onInteractiveOnboardingStartGame = {
                pendingOnboardingCompletionState?.let(onInteractiveOnboardingFinished)
            },
            onInteractiveOnboardingReturnHome = {
                pendingOnboardingCompletionState?.let(onInteractiveOnboardingReturnHome)
            },
        )
    }
}

private fun GameState.hiddenOnboardingPreviewState(): GameState = copy(
    activePiece = null,
    nextQueue = emptyList(),
    holdPiece = null,
    canHold = false,
)

private data class InteractiveOnboardingAdvanceRequest(
    val completedStage: OnboardingStage,
    val nextStage: OnboardingStage?,
)

private fun nextInteractiveOnboardingStage(
    currentStage: OnboardingStage,
    stages: List<OnboardingStage>,
): OnboardingStage? {
    val currentIndex = stages.indexOf(currentStage)
    if (currentIndex < 0) return null
    return stages.getOrNull(currentIndex + 1)
}