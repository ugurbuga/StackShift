package com.ugurbuga.blockgames.game.logic

import com.ugurbuga.blockgames.game.model.BoardMatrix
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameMode
import com.ugurbuga.blockgames.game.model.GameStatus
import com.ugurbuga.blockgames.game.model.GridPoint
import com.ugurbuga.blockgames.game.model.Piece
import com.ugurbuga.blockgames.game.model.PieceKind
import com.ugurbuga.blockgames.game.model.SpecialBlockType
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TimeAttackGameLogicTest {
    private val logic = GameLogic.create(random = Random(13))

    @Test
    fun timeAttack_newGameStartsWithDefaultTimer() {
        val state = logic.newGame(mode = GameMode.TimeAttack)

        assertEquals(GameMode.TimeAttack, state.gameMode)
        assertEquals(GameLogic.DEFAULT_TIME_ATTACK_DURATION_MILLIS, state.remainingTimeMillis)
    }

    @Test
    fun timeAttack_rowClearAwardsTimePerClearedBlock() {
        val config = GameConfig(columns = 4, rows = 4)
        val board = BoardMatrix.empty(columns = 4, rows = 4).fill(
            points = listOf(
                GridPoint(column = 0, row = 0),
                GridPoint(column = 1, row = 0),
            ),
            tone = CellTone.Gold,
        )
        val state = logic.newGame(config = config, mode = GameMode.TimeAttack).copy(
            board = board,
            activePiece = domino(id = 1),
            nextQueue = listOf(domino(id = 2), domino(id = 3), domino(id = 4)),
            remainingTimeMillis = GameLogic.DEFAULT_TIME_ATTACK_DURATION_MILLIS,
        )

        val committed = logic.placePiece(
            state = state,
            pieceId = 1,
            origin = GridPoint(column = 2, row = 0),
        ).state

        assertEquals(1, committed.linesCleared)
        assertEquals(
            GameLogic.DEFAULT_TIME_ATTACK_DURATION_MILLIS + (4L * GameLogic.TIME_ATTACK_BONUS_PER_CLEARED_BLOCK_MILLIS),
            committed.remainingTimeMillis,
        )
    }

    @Test
    fun timeAttack_timeoutTriggersGameOverAndReviveAddsBonusTime() {
        val timedOut = logic.tick(
            logic.newGame(mode = GameMode.TimeAttack).copy(remainingTimeMillis = 1_000L),
        )
        val revived = logic.reviveFromReward(timedOut).state

        assertEquals(GameStatus.GameOver, timedOut.status)
        assertEquals(0L, timedOut.remainingTimeMillis)
        assertEquals(GameStatus.Running, revived.status)
        assertEquals(GameLogic.TIME_ATTACK_REVIVE_BONUS_MILLIS, revived.remainingTimeMillis)
        assertTrue(revived.rewardedReviveUsed)
    }

    private fun domino(
        id: Long,
        special: SpecialBlockType = SpecialBlockType.None,
    ): Piece = Piece(
        id = id,
        kind = PieceKind.Domino,
        tone = CellTone.Cyan,
        cells = listOf(GridPoint(0, 0), GridPoint(1, 0)),
        width = 2,
        height = 1,
        special = special,
    )
}

