package com.ugurbuga.blockgames.ui.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GridPoint
import com.ugurbuga.blockgames.game.model.Piece
import com.ugurbuga.blockgames.game.model.PieceKind
import com.ugurbuga.blockgames.game.model.PlacementPreview
import com.ugurbuga.blockgames.game.model.SpecialBlockType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FreePlacementGameScreenCommonTest {

    @Test
    fun freePlacementDragTopLeft_centersPieceAndKeepsItLiftedAboveFinger() {
        val piece = dominoPiece(id = 1)

        val topLeft = freePlacementDragTopLeft(
            pointerInHost = Offset(x = 120f, y = 200f),
            piece = piece,
            cellSizePx = 20f,
        )

        assertEquals(Offset(x = 100f, y = 90f), topLeft)
    }

    @Test
    fun resolveNearestFreePlacementPreview_picksClosestValidLandingAnchor() {
        val preview = resolveNearestFreePlacementPreview(
            pieceId = 1L,
            piece = dominoPiece(id = 1),
            overlayTopLeft = Offset(x = 49f, y = 61f),
            boardRect = Rect(left = 10f, top = 20f, right = 210f, bottom = 220f),
            cellSizePx = 20f,
            config = GameConfig(columns = 10, rows = 10),
            requestPreview = { _, origin ->
                if (origin == GridPoint(column = 2, row = 2) || origin == GridPoint(column = 6, row = 6)) {
                    previewFor(origin)
                } else {
                    null
                }
            },
        )

        assertNotNull(preview)
        assertEquals(GridPoint(column = 2, row = 2), preview.landingAnchor)
    }

    @Test
    fun resolveNearestFreePlacementPreview_prefersPlacementWithLargestVisibleOverlap() {
        val preview = resolveNearestFreePlacementPreview(
            pieceId = 1L,
            piece = dominoPiece(id = 1),
            overlayTopLeft = Offset(x = 73f, y = 62f),
            boardRect = Rect(left = 10f, top = 20f, right = 210f, bottom = 220f),
            cellSizePx = 20f,
            config = GameConfig(columns = 10, rows = 10),
            requestPreview = { _, origin ->
                if (origin == GridPoint(column = 2, row = 2) || origin == GridPoint(column = 3, row = 2)) {
                    previewFor(origin)
                } else {
                    null
                }
            },
        )

        assertNotNull(preview)
        assertEquals(GridPoint(column = 3, row = 2), preview.landingAnchor)
    }

    @Test
    fun resolveNearestFreePlacementPreview_prefersActualOccupiedBlocksWhenBoundingBoxesTie() {
        val preview = resolveNearestFreePlacementPreview(
            pieceId = 7L,
            piece = lPiece(id = 7),
            overlayTopLeft = Offset(x = 60f, y = 50f),
            boardRect = Rect(left = 10f, top = 20f, right = 210f, bottom = 220f),
            cellSizePx = 20f,
            config = GameConfig(columns = 10, rows = 10),
            requestPreview = { _, origin ->
                if (origin == GridPoint(column = 2, row = 2) || origin == GridPoint(column = 2, row = 1)) {
                    previewFor(origin)
                } else {
                    null
                }
            },
        )

        assertNotNull(preview)
        assertEquals(GridPoint(column = 2, row = 1), preview.landingAnchor)
    }

    @Test
    fun resolveNearestFreePlacementPreview_returnsNullWhenDraggedPieceDoesNotOverlapBoard() {
        val preview = resolveNearestFreePlacementPreview(
            pieceId = 1L,
            piece = dominoPiece(id = 1),
            overlayTopLeft = Offset(x = 40f, y = 320f),
            boardRect = Rect(left = 10f, top = 20f, right = 210f, bottom = 220f),
            cellSizePx = 20f,
            config = GameConfig(columns = 10, rows = 10),
            requestPreview = { _, origin -> previewFor(origin) },
        )

        assertNull(preview)
    }

    private fun dominoPiece(id: Long): Piece = Piece(
        id = id,
        kind = PieceKind.Domino,
        tone = CellTone.Cyan,
        cells = listOf(GridPoint(0, 0), GridPoint(1, 0)),
        width = 2,
        height = 1,
        special = SpecialBlockType.None,
    )

    private fun lPiece(id: Long): Piece = Piece(
        id = id,
        kind = PieceKind.TriL,
        tone = CellTone.Gold,
        cells = listOf(
            GridPoint(0, 0),
            GridPoint(0, 1),
            GridPoint(1, 1),
        ),
        width = 2,
        height = 2,
        special = SpecialBlockType.None,
    )

    private fun previewFor(origin: GridPoint): PlacementPreview = PlacementPreview(
        selectedColumn = origin.column,
        entryAnchor = origin,
        landingAnchor = origin,
        occupiedCells = listOf(origin, GridPoint(origin.column + 1, origin.row)),
        coveredColumns = origin.column..(origin.column + 1),
        isPerfectDrop = false,
    )
}


