package com.ugurbuga.blockgames.ui.game

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ugurbuga.blockgames.ads.GameAdController
import com.ugurbuga.blockgames.ads.NoOpGameAdController
import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.game.model.GridPoint
import com.ugurbuga.blockgames.game.model.PlacementPreview
import com.ugurbuga.blockgames.game.model.SpecialBlockType
import com.ugurbuga.blockgames.platform.feedback.GameHaptics
import com.ugurbuga.blockgames.platform.feedback.NoOpGameHaptics
import com.ugurbuga.blockgames.platform.feedback.NoOpSoundEffectPlayer
import com.ugurbuga.blockgames.platform.feedback.SoundEffectPlayer
import com.ugurbuga.blockgames.settings.StackShiftOnboardingScene
import com.ugurbuga.blockgames.telemetry.AppTelemetry
import com.ugurbuga.blockgames.telemetry.NoOpAppTelemetry

@Composable
fun StackShiftGameScreen(
    gameState: GameState,
    onRequestPreview: (Int) -> PlacementPreview?,
    onResolvePreviewImpact: (PlacementPreview?) -> Set<GridPoint>,
    onPlacePiece: (Int) -> com.ugurbuga.blockgames.presentation.game.GameDispatchResult,
    onHoldPiece: () -> com.ugurbuga.blockgames.presentation.game.InteractionFeedback,
    onReplaceActivePiece: (SpecialBlockType) -> com.ugurbuga.blockgames.presentation.game.InteractionFeedback,
    onRestart: () -> com.ugurbuga.blockgames.presentation.game.InteractionFeedback,
    onRewardedRevive: () -> com.ugurbuga.blockgames.presentation.game.InteractionFeedback,
    onBack: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenTutorial: () -> Unit,
    telemetry: AppTelemetry = NoOpAppTelemetry,
    adController: GameAdController = NoOpGameAdController,
    soundPlayer: SoundEffectPlayer = NoOpSoundEffectPlayer,
    haptics: GameHaptics = NoOpGameHaptics,
    highestScore: Int,
    showLaunchOverlayInitially: Boolean = false,
    onLaunchOverlayFinished: () -> Unit = {},
    showNewHighScoreMessage: Boolean = false,
    interactiveOnboardingScene: StackShiftOnboardingScene? = null,
    interactiveOnboardingCurrentStep: Int = 0,
    interactiveOnboardingTotalSteps: Int = 0,
    interactiveOnboardingAwaitingCommit: Boolean = false,
    interactiveOnboardingCompletionDialogVisible: Boolean = false,
    onInteractiveOnboardingStartGame: () -> Unit = {},
    onInteractiveOnboardingReturnHome: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    GameScreenWithLaunchOverlay(
        gameState = gameState,
        onRequestPreview = onRequestPreview,
        onResolvePreviewImpact = onResolvePreviewImpact,
        onPlacePiece = onPlacePiece,
        onHoldPiece = onHoldPiece,
        onReplaceActivePiece = onReplaceActivePiece,
        onRestart = onRestart,
        onRewardedRevive = onRewardedRevive,
        onBack = onBack,
        onOpenSettings = onOpenSettings,
        onOpenTutorial = onOpenTutorial,
        telemetry = telemetry,
        adController = adController,
        soundPlayer = soundPlayer,
        haptics = haptics,
        highestScore = highestScore,
        showLaunchOverlayInitially = showLaunchOverlayInitially,
        onLaunchOverlayFinished = onLaunchOverlayFinished,
        showNewHighScoreMessage = showNewHighScoreMessage,
        interactiveOnboardingScene = interactiveOnboardingScene,
        interactiveOnboardingCurrentStep = interactiveOnboardingCurrentStep,
        interactiveOnboardingTotalSteps = interactiveOnboardingTotalSteps,
        interactiveOnboardingAwaitingCommit = interactiveOnboardingAwaitingCommit,
        interactiveOnboardingCompletionDialogVisible = interactiveOnboardingCompletionDialogVisible,
        onInteractiveOnboardingStartGame = onInteractiveOnboardingStartGame,
        onInteractiveOnboardingReturnHome = onInteractiveOnboardingReturnHome,
        modifier = modifier,
    )
}

