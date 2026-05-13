package com.ugurbuga.blockgames.ui.game.gametutorial

import com.ugurbuga.blockgames.game.model.BoardMatrix
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.GridPoint
import com.ugurbuga.blockgames.game.model.Piece
import com.ugurbuga.blockgames.game.model.PieceKind
import com.ugurbuga.blockgames.game.model.SpecialBlockType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameTutorialScreenCommonTest {

    @Test
    fun fillPattern_respectsExcludedPointsAndDisallowedTones() {
        val board = BoardMatrix.empty(columns = 3, rows = 3).fillPattern(
            exclude = setOf(GridPoint(1, 1)),
            disallowedTones = setOf(CellTone.Cyan, CellTone.Gold),
        )

        assertNull(board.cellAt(1, 1))
        for (row in 0 until board.rows) {
            for (column in 0 until board.columns) {
                if (column == 1 && row == 1) continue
                val tone = board.toneAt(column, row)
                assertTrue(tone != null)
                assertFalse(tone == CellTone.Cyan || tone == CellTone.Gold)
            }
        }
    }

    @Test
    fun tutorialPreviewGameState_buildsRunningStateWithRequestedMetadata() {
        val board = BoardMatrix.empty(columns = 4, rows = 5)
        val piece = Piece(
            id = 1L,
            kind = PieceKind.Single,
            tone = CellTone.Coral,
            cells = listOf(GridPoint(0, 0)),
            width = 1,
            height = 1,
            special = SpecialBlockType.None,
        )

        val state = tutorialPreviewGameState(
            board = board,
            activePiece = piece,
            spawnColumn = 2,
            gameplayStyle = GameplayStyle.MergeShift,
        )

        assertEquals(4, state.config.columns)
        assertEquals(5, state.config.rows)
        assertEquals(GameplayStyle.MergeShift, state.gameplayStyle)
        assertEquals(2, state.lastPlacementColumn)
        assertEquals(piece, state.activePiece)
        assertTrue(state.canHold)
    }

    @Test
    fun tutorialBoomBlocksGravityScene_usesBoomBlocksStateAndSingleAutoColumn() {
        val scene = tutorialBoomBlocksGravityScene()

        assertEquals(GameplayStyle.BoomBlocks, scene.gameState.gameplayStyle)
        assertEquals(0, scene.spawnColumn)
        assertEquals(listOf(0), scene.autoColumns)
        assertEquals(CellTone.Coral, scene.gameState.activePiece?.tone)
    }
}

