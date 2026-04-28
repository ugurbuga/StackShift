package com.ugurbuga.stackshift.presentation.game

import com.ugurbuga.stackshift.game.logic.GameEvent
import com.ugurbuga.stackshift.game.logic.GameLogic
import com.ugurbuga.stackshift.game.logic.StackShiftGameLogic
import com.ugurbuga.stackshift.game.model.GameState
import com.ugurbuga.stackshift.game.model.GameplayStyle
import com.ugurbuga.stackshift.game.model.GridPoint
import com.ugurbuga.stackshift.game.model.PlacementPreview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class GameStore(
    private val blockWiseGameLogic: GameLogic,
    private val stackShiftGameLogic: StackShiftGameLogic = StackShiftGameLogic(),
    private val scope: CoroutineScope,
    initialState: GameState? = null,
    private val onStateChanged: (GameState) -> Unit = {},
    private val onEvents: (Set<GameEvent>) -> Unit = {},
) {
    private val reducer = GameReducer(blockWiseGameLogic, stackShiftGameLogic)
    private val _uiState = MutableStateFlow(
        GameUiState(
            gameState = restoreState(initialState ?: blockWiseGameLogic.newGame()),
        ),
    )
    private val effectHandler = GameEffectHandler(
        scope = scope,
        stateProvider = { _uiState.value.gameState },
        dispatchIntent = ::dispatch,
    )

    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        onStateChanged(_uiState.value.gameState)
    }

    fun previewPlacement(column: Int): PlacementPreview? {
        val state = _uiState.value.gameState
        return when (state.gameplayStyle) {
            GameplayStyle.BlockWise -> blockWiseGameLogic.previewPlacement(state = state, approximateColumn = column)
            GameplayStyle.StackShift -> stackShiftGameLogic.previewPlacement(state = state, approximateColumn = column)
        }
    }

    fun previewPlacement(
        pieceId: Long,
        origin: GridPoint,
    ): PlacementPreview? {
        val state = _uiState.value.gameState
        return when (state.gameplayStyle) {
            GameplayStyle.BlockWise -> blockWiseGameLogic.previewPlacement(state = state, pieceId = pieceId, origin = origin)
            GameplayStyle.StackShift -> stackShiftGameLogic.previewPlacement(state = state, approximateColumn = origin.column)
        }
    }

    fun previewImpactPoints(preview: PlacementPreview?): Set<GridPoint> {
        val state = _uiState.value.gameState
        return when (state.gameplayStyle) {
            GameplayStyle.BlockWise -> blockWiseGameLogic.previewImpactPoints(state = state, preview = preview)
            GameplayStyle.StackShift -> stackShiftGameLogic.previewImpactPoints(state = state, preview = preview)
        }
    }

    fun dispatch(intent: GameIntent): Set<GameEvent> {
        val result = reducer.reduce(
            state = _uiState.value.gameState,
            action = intent.toAction(),
        )
        updateState(result.state)
        onEvents(result.events)
        result.effects.forEach(effectHandler::handle)
        return result.events
    }

    fun tick() {
        val result = reducer.reduce(
            state = _uiState.value.gameState,
            action = GameAction.Tick,
        )
        updateState(result.state)
        onEvents(result.events)
        result.effects.forEach(effectHandler::handle)
    }

    fun replaceState(state: GameState) {
        effectHandler.handle(GameEffect.CancelSoftLockTimer)
        updateState(restoreState(state))
    }

    fun dispose() {
        effectHandler.dispose()
    }

    private fun updateState(state: GameState) {
        _uiState.update { current -> current.copy(gameState = state) }
        onStateChanged(state)
    }

    private fun restoreState(state: GameState): GameState = when (state.gameplayStyle) {
        GameplayStyle.BlockWise -> blockWiseGameLogic.restoreGame(state)
        GameplayStyle.StackShift -> stackShiftGameLogic.restoreGame(state)
    }
}
