package com.ugurbuga.blockgames.ui.game

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ugurbuga.blockgames.ads.GameAdController
import com.ugurbuga.blockgames.ads.NoOpGameAdController
import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.game.model.GridPoint
import com.ugurbuga.blockgames.game.model.PlacementPreview
import com.ugurbuga.blockgames.settings.BlockWiseOnboardingScene
import com.ugurbuga.blockgames.telemetry.AppTelemetry
import com.ugurbuga.blockgames.telemetry.NoOpAppTelemetry

@Composable
fun BlockWiseGameScreen(
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
    interactiveOnboardingScene: BlockWiseOnboardingScene? = null,
    interactiveOnboardingCurrentStep: Int = 0,
    interactiveOnboardingTotalSteps: Int = 0,
    interactiveOnboardingAwaitingCommit: Boolean = false,
    interactiveOnboardingCompletionDialogVisible: Boolean = false,
    onInteractiveOnboardingStartGame: () -> Unit = {},
    onInteractiveOnboardingReturnHome: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    FreePlacementGameScreen(
        gameState = gameState,
        onRequestPreview = onRequestPreview,
        onResolvePreviewImpact = onResolvePreviewImpact,
        onPlacePiece = onPlacePiece,
        onRestart = onRestart,
        onRewardedRevive = onRewardedRevive,
        onBack = onBack,
        highestScore = highestScore,
        showNewHighScoreMessage = showNewHighScoreMessage,
        adController = adController,
        telemetry = telemetry,
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

