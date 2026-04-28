package com.ugurbuga.stackshift.ui.game

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ugurbuga.stackshift.ads.GameAdController
import com.ugurbuga.stackshift.ads.NoOpGameAdController
import com.ugurbuga.stackshift.game.model.GameState
import com.ugurbuga.stackshift.game.model.GridPoint
import com.ugurbuga.stackshift.game.model.PlacementPreview
import com.ugurbuga.stackshift.game.model.SpecialBlockType
import com.ugurbuga.stackshift.platform.feedback.GameHaptics
import com.ugurbuga.stackshift.platform.feedback.NoOpGameHaptics
import com.ugurbuga.stackshift.platform.feedback.NoOpSoundEffectPlayer
import com.ugurbuga.stackshift.platform.feedback.SoundEffectPlayer
import com.ugurbuga.stackshift.settings.FirstRunOnboardingScene
import com.ugurbuga.stackshift.telemetry.AppTelemetry
import com.ugurbuga.stackshift.telemetry.NoOpAppTelemetry

@Composable
fun SSGameScreen(
    gameState: GameState,
    onRequestPreview: (Int) -> PlacementPreview?,
    onResolvePreviewImpact: (PlacementPreview?) -> Set<GridPoint>,
    onPlacePiece: (Int) -> com.ugurbuga.stackshift.presentation.game.GameDispatchResult,
    onHoldPiece: () -> com.ugurbuga.stackshift.presentation.game.InteractionFeedback,
    onReplaceActivePiece: (SpecialBlockType) -> com.ugurbuga.stackshift.presentation.game.InteractionFeedback,
    onRestart: () -> com.ugurbuga.stackshift.presentation.game.InteractionFeedback,
    onRewardedRevive: () -> com.ugurbuga.stackshift.presentation.game.InteractionFeedback,
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
    interactiveOnboardingScene: FirstRunOnboardingScene? = null,
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

