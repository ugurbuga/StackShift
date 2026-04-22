package com.ugurbuga.stackshift.presentation.game

import com.ugurbuga.stackshift.game.logic.GameEvent
import com.ugurbuga.stackshift.game.logic.GameLogic
import com.ugurbuga.stackshift.game.model.GameConfig
import com.ugurbuga.stackshift.game.model.GameState
import com.ugurbuga.stackshift.game.model.GameStatus
import com.ugurbuga.stackshift.game.model.SpecialBlockType

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
                        add(GameEffect.StartSoftLockTimer(revision = softLock.revision, delayMillis = softLock.remainingMillis))
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
            state = gameLogic.newGame(action.config),
            events = setOf(GameEvent.Restarted),
            effects = listOf(GameEffect.CancelSoftLockTimer),
        )

        GameAction.Tick -> ReduceResult(
            state = gameLogic.tick(state),
            events = emptySet(),
            effects = emptyList(),
        )
    }
}

sealed interface GameIntent {
    data class LaunchColumn(val column: Int) : GameIntent
    data object CommitSoftLock : GameIntent
    data object HoldPiece : GameIntent
    data object ReviveFromReward : GameIntent
    data class Restart(val config: GameConfig) : GameIntent
    data class ReplaceActivePiece(val specialType: SpecialBlockType) : GameIntent
}

internal sealed interface GameAction {
    data class LaunchColumn(val column: Int) : GameAction
    data object CommitSoftLock : GameAction
    data object HoldPiece : GameAction
    data object ReviveFromReward : GameAction
    data class Restart(val config: GameConfig) : GameAction
    data object Tick : GameAction
    data class ReplaceActivePiece(val specialType: SpecialBlockType) : GameAction
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
    is GameIntent.Restart -> GameAction.Restart(config)
    is GameIntent.ReplaceActivePiece -> GameAction.ReplaceActivePiece(specialType)
}


