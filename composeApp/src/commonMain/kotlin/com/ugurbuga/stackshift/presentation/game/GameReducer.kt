package com.ugurbuga.stackshift.presentation.game

import com.ugurbuga.stackshift.game.logic.GameEvent
import com.ugurbuga.stackshift.game.logic.GameLogic
import com.ugurbuga.stackshift.game.logic.StackShiftGameLogic
import com.ugurbuga.stackshift.game.model.DailyChallenge
import com.ugurbuga.stackshift.game.model.GameConfig
import com.ugurbuga.stackshift.game.model.GameMode
import com.ugurbuga.stackshift.game.model.GameState
import com.ugurbuga.stackshift.game.model.GameStatus
import com.ugurbuga.stackshift.game.model.GameplayStyle
import com.ugurbuga.stackshift.game.model.GridPoint
import com.ugurbuga.stackshift.game.model.SpecialBlockType

internal class GameReducer(
    private val blockWiseGameLogic: GameLogic,
    private val stackShiftGameLogic: StackShiftGameLogic,
) {
    fun reduce(
        state: GameState,
        action: GameAction,
    ): ReduceResult = when (action) {
        is GameAction.PlacePiece -> {
            val result = when (state.gameplayStyle) {
                GameplayStyle.BlockWise -> blockWiseGameLogic.placePiece(
                    state = state,
                    pieceId = action.pieceId,
                    origin = action.origin,
                )
                GameplayStyle.StackShift -> stackShiftGameLogic.placePiece(
                    state = state,
                    approximateColumn = action.origin.column,
                )
            }
            val effects = when (state.gameplayStyle) {
                GameplayStyle.BlockWise -> emptyList()
                GameplayStyle.StackShift -> result.state.softLock?.let { softLock ->
                    listOf(
                        GameEffect.CancelSoftLockTimer,
                        GameEffect.StartSoftLockTimer(
                            revision = softLock.revision,
                            delayMillis = softLock.remainingMillis,
                        ),
                    )
                } ?: emptyList()
            }
            ReduceResult(
                state = result.state,
                events = result.events,
                effects = effects,
            )
        }

        GameAction.CommitSoftLock -> {
            val result = logicFor(state).commitSoftLock(state)
            ReduceResult(
                state = result.state,
                events = result.events,
                effects = listOf(GameEffect.CancelSoftLockTimer),
            )
        }

        GameAction.HoldPiece -> {
            val result = logicFor(state).holdPiece(state)
            ReduceResult(
                state = result.state,
                events = result.events,
                effects = listOf(GameEffect.CancelSoftLockTimer),
            )
        }

        GameAction.ReviveFromReward -> {
            val result = logicFor(state).reviveFromReward(state)
            ReduceResult(
                state = result.state,
                events = result.events,
                effects = listOf(GameEffect.CancelSoftLockTimer),
            )
        }

        is GameAction.ReplaceActivePiece -> {
            val result = logicFor(state).replaceActivePiece(state, action.specialType)
            ReduceResult(
                state = result.state,
                events = result.events,
                effects = listOf(GameEffect.CancelSoftLockTimer),
            )
        }

        is GameAction.Restart -> ReduceResult(
            state = when (action.gameplayStyle) {
                GameplayStyle.BlockWise -> blockWiseGameLogic.newGame(action.config, action.challenge, action.mode, action.gameplayStyle)
                GameplayStyle.StackShift -> stackShiftGameLogic.newGame(action.config, action.challenge, action.mode)
            },
            events = setOf(GameEvent.Restarted),
            effects = listOf(GameEffect.CancelSoftLockTimer),
        )

        GameAction.TogglePause -> {
            val nextStatus = if (state.status == GameStatus.Paused) GameStatus.Running else GameStatus.Paused
            ReduceResult(
                state = state.copy(status = nextStatus),
                events = setOf(if (nextStatus == GameStatus.Paused) GameEvent.Paused else GameEvent.Resumed),
                effects = emptyList(),
            )
        }

        GameAction.Tick -> {
            val nextState = logicFor(state).tick(state)
            ReduceResult(
                state = nextState,
                events = buildSet {
                    if (state.status != GameStatus.GameOver && nextState.status == GameStatus.GameOver) {
                        add(GameEvent.GameOver)
                    }
                },
                effects = emptyList(),
            )
        }
    }

    private fun logicFor(state: GameState): AnyGameLogic = when (state.gameplayStyle) {
        GameplayStyle.BlockWise -> BlockWiseLogicAdapter(blockWiseGameLogic)
        GameplayStyle.StackShift -> StackShiftLogicAdapter(stackShiftGameLogic)
    }
}

sealed interface GameIntent {
    data class PlacePiece(val pieceId: Long, val origin: GridPoint) : GameIntent
    data object CommitSoftLock : GameIntent
    data object HoldPiece : GameIntent
    data object ReviveFromReward : GameIntent
    data class Restart(
        val config: GameConfig,
        val challenge: DailyChallenge? = null,
        val mode: GameMode = GameMode.Classic,
        val gameplayStyle: GameplayStyle = GameplayStyle.BlockWise,
    ) : GameIntent
    data class ReplaceActivePiece(val specialType: SpecialBlockType) : GameIntent
    data object TogglePause : GameIntent
}

internal sealed interface GameAction {
    data class PlacePiece(val pieceId: Long, val origin: GridPoint) : GameAction
    data object CommitSoftLock : GameAction
    data object HoldPiece : GameAction
    data object ReviveFromReward : GameAction
    data class Restart(
        val config: GameConfig,
        val challenge: DailyChallenge? = null,
        val mode: GameMode = GameMode.Classic,
        val gameplayStyle: GameplayStyle = GameplayStyle.BlockWise,
    ) : GameAction
    data object Tick : GameAction
    data class ReplaceActivePiece(val specialType: SpecialBlockType) : GameAction
    data object TogglePause : GameAction
}

internal sealed interface GameEffect {
    data class StartSoftLockTimer(
        val revision: Long,
        val delayMillis: Long,
    ) : GameEffect

    data object CancelSoftLockTimer : GameEffect
}

internal data class ReduceResult(
    val state: GameState,
    val events: Set<GameEvent>,
    val effects: List<GameEffect>,
)

internal fun GameIntent.toAction(): GameAction = when (this) {
    is GameIntent.PlacePiece -> GameAction.PlacePiece(pieceId, origin)
    GameIntent.CommitSoftLock -> GameAction.CommitSoftLock
    GameIntent.HoldPiece -> GameAction.HoldPiece
    GameIntent.ReviveFromReward -> GameAction.ReviveFromReward
    is GameIntent.Restart -> GameAction.Restart(config, challenge, mode, gameplayStyle)
    is GameIntent.ReplaceActivePiece -> GameAction.ReplaceActivePiece(specialType)
    GameIntent.TogglePause -> GameAction.TogglePause
}

private sealed interface AnyGameLogic {
    fun commitSoftLock(state: GameState): com.ugurbuga.stackshift.game.logic.GameMoveResult
    fun holdPiece(state: GameState): com.ugurbuga.stackshift.game.logic.GameMoveResult
    fun reviveFromReward(state: GameState): com.ugurbuga.stackshift.game.logic.GameMoveResult
    fun replaceActivePiece(state: GameState, specialType: SpecialBlockType): com.ugurbuga.stackshift.game.logic.GameMoveResult
    fun tick(state: GameState): GameState
}

private class BlockWiseLogicAdapter(
    private val logic: GameLogic,
) : AnyGameLogic {
    override fun commitSoftLock(state: GameState) = logic.commitSoftLock(state)
    override fun holdPiece(state: GameState) = logic.holdPiece(state)
    override fun reviveFromReward(state: GameState) = logic.reviveFromReward(state)
    override fun replaceActivePiece(state: GameState, specialType: SpecialBlockType) = logic.replaceActivePiece(state, specialType)
    override fun tick(state: GameState): GameState = logic.tick(state)
}

private class StackShiftLogicAdapter(
    private val logic: StackShiftGameLogic,
) : AnyGameLogic {
    override fun commitSoftLock(state: GameState) = logic.commitSoftLock(state)
    override fun holdPiece(state: GameState) = logic.holdPiece(state)
    override fun reviveFromReward(state: GameState) = logic.reviveFromReward(state)
    override fun replaceActivePiece(state: GameState, specialType: SpecialBlockType) = logic.replaceActivePiece(state, specialType)
    override fun tick(state: GameState): GameState = logic.tick(state)
}


