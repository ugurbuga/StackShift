package com.ugurbuga.stackshift.presentation.game

import com.ugurbuga.stackshift.game.logic.GameEvent
import com.ugurbuga.stackshift.game.logic.GameLogic
import com.ugurbuga.stackshift.game.model.GameConfig
import com.ugurbuga.stackshift.game.model.GameMode
import com.ugurbuga.stackshift.game.model.GameState
import com.ugurbuga.stackshift.game.model.GameStatus
import com.ugurbuga.stackshift.game.model.SpecialBlockType
import com.ugurbuga.stackshift.game.model.DailyChallenge

internal class GameReducer(
    private val gameLogic: GameLogic,
) {
    fun reduce(
        state: GameState,
        action: GameAction,
    ): ReduceResult = when (action) {
        is GameAction.LaunchColumn -> {
            val result = gameLogic.placePiece(state = state, approximateColumn = action.column)
            ReduceResult(
                state = result.state,
                events = result.events,
                effects = buildList {
                    result.state.softLock?.let { softLock ->
                        add(
                            GameEffect.StartSoftLockTimer(
                                revision = softLock.revision,
                                delayMillis = softLock.remainingMillis
                            )
                        )
                    }
                },
            )
        }

        GameAction.CommitSoftLock -> {
            val result = gameLogic.commitSoftLock(state)
            ReduceResult(
                state = result.state,
                events = result.events,
                effects = listOf(GameEffect.CancelSoftLockTimer),
            )
        }

        GameAction.HoldPiece -> {
            val result = gameLogic.holdPiece(state)
            ReduceResult(
                state = result.state,
                events = result.events,
                effects = listOf(GameEffect.CancelSoftLockTimer),
            )
        }

        GameAction.ReviveFromReward -> {
            val result = gameLogic.reviveFromReward(state)
            ReduceResult(
                state = result.state,
                events = result.events,
                effects = listOf(GameEffect.CancelSoftLockTimer),
            )
        }

        is GameAction.ReplaceActivePiece -> {
            val result = gameLogic.replaceActivePiece(state, action.specialType)
            ReduceResult(
                state = result.state,
                events = result.events,
                effects = listOf(GameEffect.CancelSoftLockTimer),
            )
        }

        is GameAction.Restart -> ReduceResult(
            state = gameLogic.newGame(action.config, action.challenge, action.mode),
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
            val nextState = gameLogic.tick(state)
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
}

sealed interface GameIntent {
    data class LaunchColumn(val column: Int) : GameIntent
    data object CommitSoftLock : GameIntent
    data object HoldPiece : GameIntent
    data object ReviveFromReward : GameIntent
    data class Restart(
        val config: GameConfig,
        val challenge: DailyChallenge? = null,
        val mode: GameMode = GameMode.Classic,
    ) : GameIntent
    data class ReplaceActivePiece(val specialType: SpecialBlockType) : GameIntent
    data object TogglePause : GameIntent
}

internal sealed interface GameAction {
    data class LaunchColumn(val column: Int) : GameAction
    data object CommitSoftLock : GameAction
    data object HoldPiece : GameAction
    data object ReviveFromReward : GameAction
    data class Restart(
        val config: GameConfig,
        val challenge: DailyChallenge? = null,
        val mode: GameMode = GameMode.Classic,
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
    is GameIntent.LaunchColumn -> GameAction.LaunchColumn(column)
    GameIntent.CommitSoftLock -> GameAction.CommitSoftLock
    GameIntent.HoldPiece -> GameAction.HoldPiece
    GameIntent.ReviveFromReward -> GameAction.ReviveFromReward
    is GameIntent.Restart -> GameAction.Restart(config, challenge, mode)
    is GameIntent.ReplaceActivePiece -> GameAction.ReplaceActivePiece(specialType)
    GameIntent.TogglePause -> GameAction.TogglePause
}


