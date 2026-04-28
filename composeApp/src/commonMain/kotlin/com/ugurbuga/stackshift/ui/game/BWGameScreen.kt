package com.ugurbuga.stackshift.ui.game

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ugurbuga.stackshift.ads.GameAdController
import com.ugurbuga.stackshift.ads.NoOpGameAdController
import com.ugurbuga.stackshift.game.model.GameState
import com.ugurbuga.stackshift.game.model.GridPoint
import com.ugurbuga.stackshift.game.model.PlacementPreview
import com.ugurbuga.stackshift.telemetry.AppTelemetry
import com.ugurbuga.stackshift.telemetry.NoOpAppTelemetry

@Composable
fun BWGameScreen(
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
        modifier = modifier,
    )
}

