package com.ugurbuga.stackshift.game.logic

import com.ugurbuga.stackshift.game.model.BoardMatrix
import com.ugurbuga.stackshift.game.model.CellTone
import com.ugurbuga.stackshift.game.model.GameConfig
import com.ugurbuga.stackshift.game.model.GameMode
import com.ugurbuga.stackshift.game.model.GameStatus
import com.ugurbuga.stackshift.game.model.GridPoint
import com.ugurbuga.stackshift.game.model.Piece
import com.ugurbuga.stackshift.game.model.PieceKind
import com.ugurbuga.stackshift.game.model.SpecialBlockType
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TimeAttackGameLogicTest {
    private val logic = GameLogic(random = Random(13))

    @Test
    fun timeAttack_newGameStartsWithDefaultTimer() {
        val state = logic.newGame(mode = GameMode.TimeAttack)

        assertEquals(GameMode.TimeAttack, state.gameMode)
        assertEquals(GameLogic.DefaultTimeAttackDurationMillis, state.remainingTimeMillis)
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
            remainingTimeMillis = GameLogic.DefaultTimeAttackDurationMillis,
        )

        val launched = logic.placePiece(state = state, approximateColumn = 2).state
        val committed = logic.commitSoftLock(launched).state

        assertEquals(1, committed.linesCleared)
        assertEquals(
            GameLogic.DefaultTimeAttackDurationMillis + (4L * GameLogic.TimeAttackBonusPerClearedBlockMillis),
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
        assertEquals(GameLogic.TimeAttackReviveBonusMillis, revived.remainingTimeMillis)
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

