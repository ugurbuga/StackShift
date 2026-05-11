package com.ugurbuga.blockgames.game.logic

import com.ugurbuga.blockgames.game.model.BoardMatrix
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameMode
import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.game.model.GameStatus
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.GridPoint
import com.ugurbuga.blockgames.game.model.Piece
import com.ugurbuga.blockgames.game.model.PieceKind
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MergeShiftGameLogicTest {
    private val logic = MergeShiftGameLogic(random = Random(0))

    @Test
    fun reviveFromReward_resolvesMergesCreatedByRewardClear() {
        val config = GameConfig(columns = 3, rows = 5)
        val board = boardOf(
            listOf(1, 2, 8, 14, 15),
            listOf(3, 4, 8, 11, 13),
            listOf(5, 6, 9, 10, 12),
        )
        val activePiece = singlePiece(id = 100, value = 4)
        val nextQueue = listOf(
            singlePiece(id = 101, value = 2),
            singlePiece(id = 102, value = 4),
            singlePiece(id = 103, value = 8),
        )
        val gameOverState = GameState(
            config = config,
            gameMode = GameMode.Classic,
            gameplayStyle = GameplayStyle.MergeShift,
            board = board,
            activePiece = activePiece,
            nextQueue = nextQueue,
            score = 0,
            linesCleared = 0,
            level = 1,
            difficultyStage = 0,
            secondsUntilDifficultyIncrease = config.difficultyIntervalSeconds,
            status = GameStatus.GameOver,
        )

        val revived = logic.reviveFromReward(gameOverState).state

        assertEquals(GameStatus.Running, revived.status)
        assertTrue(revived.rewardedReviveUsed)
        assertEquals(activePiece.id, revived.activePiece?.id)
        assertEquals(16, revived.score)
        assertEquals(16, revived.lastMoveScore)
        assertEquals(16, revived.board.cellAt(0, 0)?.value)
        assertTrue(GridPoint(0, 0) in revived.recentlyMergedPoints)
    }

    private fun boardOf(vararg columns: List<Int>): BoardMatrix {
        val rows = columns.firstOrNull()?.size ?: 0
        var board = BoardMatrix.empty(columns = columns.size, rows = rows)
        columns.forEachIndexed { column, values ->
            values.forEachIndexed { row, value ->
                board = board.fill(
                    points = listOf(GridPoint(column, row)),
                    tone = toneForValue(value),
                    value = value,
                )
            }
        }
        return board
    }

    private fun singlePiece(id: Long, value: Int): Piece = Piece(
        id = id,
        kind = PieceKind.Single,
        tone = toneForValue(value),
        cells = listOf(GridPoint(0, 0)),
        width = 1,
        height = 1,
        value = value,
    )

    private fun toneForValue(value: Int): CellTone = when (value) {
        2 -> CellTone.Cyan
        4 -> CellTone.Gold
        8 -> CellTone.Violet
        16 -> CellTone.Emerald
        32 -> CellTone.Coral
        else -> CellTone.Blue
    }
}

