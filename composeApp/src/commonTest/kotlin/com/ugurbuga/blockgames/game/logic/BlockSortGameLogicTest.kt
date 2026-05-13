package com.ugurbuga.blockgames.game.logic

import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameMode
import com.ugurbuga.blockgames.game.model.GameStatus
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.GridPoint
import com.ugurbuga.blockgames.game.model.BoardMatrix
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BlockSortGameLogicTest {

    @Test
    fun roundCapacity_cyclesAcrossRounds() {
        assertEquals(listOf(4, 5, 4, 6, 5, 6), (1..6).map(::blockSortRoundCapacity))
    }

    @Test
    fun roundColumns_increaseAcrossProgression() {
        assertEquals(listOf(6, 7, 7, 8, 8, 9, 9), (1..7).map(::blockSortRoundColumns))
    }

    @Test
    fun placePiece_recordsOnlyLatestMovedCellValuesForAnimation() {
        var board = BoardMatrix.empty(columns = 4, rows = 4)
        board = board.fill(points = listOf(GridPoint(0, 2)), tone = CellTone.Gold, value = 101)
        board = board.fill(points = listOf(GridPoint(0, 3)), tone = CellTone.Gold, value = 102)
        board = board.fill(points = listOf(GridPoint(2, 3)), tone = CellTone.Cyan, value = 201)

        val result = BlockSortGameLogic(random = Random(0)).placePiece(
            state = GameState(
                config = GameConfig(columns = 4, rows = 4, difficultyIntervalSeconds = 9_999, linesPerLevel = 9_999),
                gameplayStyle = GameplayStyle.BlockSort,
                board = board,
                activePiece = null,
                nextQueue = emptyList(),
                score = 0,
                linesCleared = 0,
                level = 1,
                difficultyStage = 0,
                secondsUntilDifficultyIncrease = 9_999,
                status = GameStatus.Running,
            ),
            pieceId = 0L,
            origin = GridPoint(1, 0),
        )

        assertEquals(setOf(101, 102), result.state.blockSortLastMovedCellValues)
    }

    @Test
    fun newGame_startsWithMixedOccupiedColumns() {
        val state = BlockSortGameLogic(random = Random(7)).newGame(
            config = GameConfig.default(GameplayStyle.BlockSort),
            mode = GameMode.Classic,
        )

        val occupiedColumns = (0 until state.config.columns).filter { column ->
            state.board.filledCellCount(column) > 0
        }
        assertTrue(occupiedColumns.isNotEmpty())

        occupiedColumns.forEach { column ->
            val tones = buildSet<CellTone> {
                for (row in 0 until state.config.rows) {
                    state.board.toneAt(column, row)?.let(::add)
                }
            }
            assertTrue(state.board.filledCellCount(column) > 1, "column $column should contain multiple pieces")
            assertTrue(tones.size > 1, "column $column should start mixed")
        }
    }

    @Test
    fun restoreGame_marksBoardWithoutMovesAsGameOver() {
        var board = BoardMatrix.empty(columns = 4, rows = 4)
        listOf(CellTone.Cyan, CellTone.Gold, CellTone.Violet, CellTone.Emerald).forEachIndexed { column, tone ->
            repeat(4) { index ->
                board = board.fill(
                    points = listOf(GridPoint(column, 3 - index)),
                    tone = tone,
                    value = (column * 10) + index,
                )
            }
        }

        val restored = BlockSortGameLogic(random = Random(0)).restoreGame(
            GameState(
                config = GameConfig(columns = 4, rows = 4, difficultyIntervalSeconds = 9_999, linesPerLevel = 9_999),
                gameplayStyle = GameplayStyle.BlockSort,
                board = board,
                activePiece = null,
                nextQueue = emptyList(),
                score = 0,
                linesCleared = 0,
                level = 1,
                difficultyStage = 0,
                secondsUntilDifficultyIncrease = 9_999,
                status = GameStatus.Running,
            ),
        )

        assertEquals(GameStatus.GameOver, restored.status)
    }
}

