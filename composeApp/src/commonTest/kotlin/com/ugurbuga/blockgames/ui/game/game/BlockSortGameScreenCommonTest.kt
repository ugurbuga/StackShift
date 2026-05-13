package com.ugurbuga.blockgames.ui.game.game

import com.ugurbuga.blockgames.game.model.BoardMatrix
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.GridPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BlockSortGameScreenCommonTest {

    @Test
    fun blockSortColumnGroups_wrapWideBoardsIntoTwoRows() {
        assertEquals(listOf(listOf(0, 1, 2), listOf(3, 4)), blockSortColumnGroups(5))
        assertEquals(listOf(listOf(0, 1, 2), listOf(3, 4, 5)), blockSortColumnGroups(6))
        assertEquals(listOf(listOf(0, 1, 2, 3), listOf(4, 5, 6)), blockSortColumnGroups(7))
    }

    @Test
    fun blockSortColumnGroups_keepSmallBoardsOnSingleRow() {
        assertEquals(listOf(listOf(0, 1, 2, 3)), blockSortColumnGroups(4))
    }

    @Test
    fun resolveMovableStackCells_returnsOnlyTopContiguousRunOfSelectedColumn() {
        var board = BoardMatrix.empty(columns = 4, rows = 5)
        board = board.fill(listOf(GridPoint(0, 4)), tone = CellTone.Cyan, value = 1)
        board = board.fill(listOf(GridPoint(0, 3)), tone = CellTone.Gold, value = 2)
        board = board.fill(listOf(GridPoint(0, 2)), tone = CellTone.Gold, value = 3)
        board = board.fill(listOf(GridPoint(0, 1)), tone = CellTone.Gold, value = 4)

        assertEquals(
            setOf(GridPoint(0, 1), GridPoint(0, 2), GridPoint(0, 3)),
            resolveMovableStackCells(board = board, selectedSourceColumn = 0),
        )
        assertEquals(emptySet(), resolveMovableStackCells(board = board, selectedSourceColumn = 3))
        assertEquals(emptySet(), resolveMovableStackCells(board = board, selectedSourceColumn = null))
    }

    @Test
    fun rowRevealAlpha_revealsRowsFromTopToBottom() {
        assertEquals(0f, rowRevealAlpha(row = 0, rowCount = 4, revealProgress = 0f))
        assertEquals(1f, rowRevealAlpha(row = 0, rowCount = 4, revealProgress = 0.25f))
        assertEquals(0f, rowRevealAlpha(row = 1, rowCount = 4, revealProgress = 0.25f))

        val midTop = rowRevealAlpha(row = 1, rowCount = 4, revealProgress = 0.5f)
        val midBottom = rowRevealAlpha(row = 3, rowCount = 4, revealProgress = 0.5f)
        assertTrue(midTop > midBottom)

        assertEquals(1f, rowRevealAlpha(row = 3, rowCount = 4, revealProgress = 1f))
    }

    @Test
    fun rowClearAlpha_clearsRowsFromTopToBottom() {
        assertEquals(1f, rowClearAlpha(row = 0, rowCount = 4, clearProgress = 0f))
        assertEquals(0f, rowClearAlpha(row = 0, rowCount = 4, clearProgress = 0.25f))
        assertEquals(1f, rowClearAlpha(row = 1, rowCount = 4, clearProgress = 0.25f))

        val midTop = rowClearAlpha(row = 1, rowCount = 4, clearProgress = 0.5f)
        val midBottom = rowClearAlpha(row = 3, rowCount = 4, clearProgress = 0.5f)
        assertTrue(midTop < midBottom)

        assertEquals(0f, rowClearAlpha(row = 3, rowCount = 4, clearProgress = 1f))
    }
}

