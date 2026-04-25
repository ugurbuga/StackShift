package com.ugurbuga.stackshift.presentation.game

import com.ugurbuga.stackshift.game.logic.GameEvent
import com.ugurbuga.stackshift.game.logic.GameLogic
import com.ugurbuga.stackshift.game.model.DailyChallenge
import com.ugurbuga.stackshift.game.model.GameConfig
import com.ugurbuga.stackshift.game.model.GameState
import com.ugurbuga.stackshift.game.model.GridPoint
import com.ugurbuga.stackshift.game.model.PlacementPreview
import com.ugurbuga.stackshift.game.model.SpecialBlockType
import com.ugurbuga.stackshift.platform.feedback.GameHaptic
import com.ugurbuga.stackshift.platform.feedback.GameSound
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GameViewModel(
    private val gameLogic: GameLogic = GameLogic(),
    initialState: GameState? = null,
    private val onStateChanged: (GameState) -> Unit = {},
    private val onChallengeCompleted: (DailyChallenge) -> Unit = {},
    private val onGameOver: (GameState) -> Unit = {},
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) {
    private val store = GameStore(
        gameLogic = gameLogic,
        scope = scope,
        initialState = initialState,
        onStateChanged = onStateChanged,
        onEvents = { events ->
            if (GameEvent.GameOver in events) {
                onGameOver(uiState.value.gameState)
            }
            if (GameEvent.ChallengeCompleted in events) {
                uiState.value.gameState.activeChallenge?.let(onChallengeCompleted)
            }
        }
    )
    private val feedbackMapper = GameFeedbackMapper()

    val uiState: StateFlow<GameUiState> = store.uiState

    init {
        startGameLoop()
    }

    fun previewPlacement(column: Int): PlacementPreview? {
        return store.previewPlacement(column)
    }

    fun previewImpactPoints(preview: PlacementPreview?): Set<GridPoint> {
        return store.previewImpactPoints(preview)
    }

    fun placePiece(column: Int): InteractionFeedback = dispatch(GameIntent.LaunchColumn(column))

    fun placePieceResult(column: Int): GameDispatchResult = dispatchResult(GameIntent.LaunchColumn(column))

    fun holdPiece(): InteractionFeedback = dispatch(GameIntent.HoldPiece)

    fun reviveFromReward(): InteractionFeedback = dispatch(GameIntent.ReviveFromReward)

    fun replaceActivePiece(specialType: SpecialBlockType): InteractionFeedback =
        dispatch(GameIntent.ReplaceActivePiece(specialType))

    fun restart(
        config: GameConfig = uiState.value.gameState.config,
        challenge: DailyChallenge? = uiState.value.gameState.activeChallenge
    ): InteractionFeedback {
        return dispatch(GameIntent.Restart(config, challenge))
    }

    fun replaceState(state: GameState) {
        store.replaceState(gameLogic.restoreGame(state))
    }

    fun snapshotState(): GameState = uiState.value.gameState

    fun dispose() {
        store.dispose()
        scope.cancel()
    }

    fun dispatch(intent: GameIntent): InteractionFeedback {
        return dispatchResult(intent).feedback
    }

    fun dispatchResult(intent: GameIntent): GameDispatchResult {
        val events = store.dispatch(intent)
        return GameDispatchResult(
            events = events,
            feedback = feedbackMapper.map(events),
        )
    }

    private fun startGameLoop() {
        scope.launch {
            while (isActive) {
                delay(1_000)
                store.tick()
            }
        }
    }
}

data class GameUiState(
    val gameState: GameState,
)


data class InteractionFeedback(
    val sounds: Set<GameSound> = emptySet(),
    val haptics: Set<GameHaptic> = emptySet(),
) {
    companion object {
        val None = InteractionFeedback()
    }
}

data class GameDispatchResult(
    val events: Set<GameEvent> = emptySet(),
    val feedback: InteractionFeedback = InteractionFeedback.None,
)

fun GameDispatchResult.mergeWith(other: GameDispatchResult): GameDispatchResult = GameDispatchResult(
    events = events + other.events,
    feedback = InteractionFeedback(
        sounds = feedback.sounds + other.feedback.sounds,
        haptics = feedback.haptics + other.feedback.haptics,
    ),
)

