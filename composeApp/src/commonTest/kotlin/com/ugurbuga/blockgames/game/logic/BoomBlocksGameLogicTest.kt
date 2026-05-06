package com.ugurbuga.blockgames.game.logic

import com.ugurbuga.blockgames.game.model.BoardMatrix
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameMode
import com.ugurbuga.blockgames.game.model.GameStatus
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.GridPoint
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class BoomBlocksGameLogicTest {
    private val logic = BoomBlocksGameLogic(random = Random(7))

    @Test
    fun boomBlocks_defaultConfigIsSixByFourteen() {
        val config = GameConfig.default(GameplayStyle.BoomBlocks)

        assertEquals(6, config.columns)
        assertEquals(14, config.rows)
    }

    @Test
    fun boomBlocks_clearAppliesDownwardGravityBeforeRefill() {
        val board = BoardMatrix.empty(columns = 3, rows = 4)
            .fill(
                points = listOf(GridPoint(column = 0, row = 0)),
                tone = CellTone.Cyan,
                value = 1,
            )
            .fill(
                points = listOf(GridPoint(column = 0, row = 1)),
                tone = CellTone.Gold,
                value = 2,
            )
            .fill(
                points = listOf(GridPoint(column = 0, row = 2)),
                tone = CellTone.Gold,
                value = 3,
            )
            .fill(
                points = listOf(GridPoint(column = 0, row = 3)),
                tone = CellTone.Gold,
                value = 4,
            )

        val state = logic.newGame(
            config = GameConfig(columns = 3, rows = 4),
            challenge = null,
            mode = GameMode.Classic,
        ).copy(
            board = board,
            status = GameStatus.Running,
            nextPieceId = 10L,
        )

        val resolved = logic.placePiece(
            state = state,
            pieceId = 0L,
            origin = GridPoint(column = 0, row = 2),
        ).state

        assertEquals(1, resolved.board.cellAt(column = 0, row = 3)?.value)
        assertEquals(setOf(
            GridPoint(column = 0, row = 1),
            GridPoint(column = 0, row = 2),
            GridPoint(column = 0, row = 3),
        ), resolved.recentlyExplodedPoints)
    }
}

