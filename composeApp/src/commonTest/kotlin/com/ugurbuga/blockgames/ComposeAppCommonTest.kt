package com.ugurbuga.blockgames

import com.ugurbuga.blockgames.game.logic.GameLogic
import com.ugurbuga.blockgames.game.logic.GameEvent
import com.ugurbuga.blockgames.game.model.BoardMatrix
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.game.model.GameStatus
import com.ugurbuga.blockgames.game.model.GridPoint
import com.ugurbuga.blockgames.game.model.Piece
import com.ugurbuga.blockgames.game.model.PieceKind
import com.ugurbuga.blockgames.game.model.SpecialBlockType
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ComposeAppCommonTest {

    private val logic = GameLogic.create(random = Random(42))

    @Test
    fun previewPlacement_returnsRequestedOrigin_forValidFreePlacement() {
        val state = testState(
            config = GameConfig(columns = 4, rows = 4),
            board = BoardMatrix.empty(columns = 4, rows = 4)
                .fill(points = listOf(GridPoint(1, 2)), tone = CellTone.Coral),
            activePiece = dominoPiece(id = 1),
            nextQueue = listOf(dominoPiece(id = 2), squarePiece(id = 3)),
        )

        val preview = logic.previewPlacement(state = state, pieceId = 1, origin = GridPoint(1, 0))

        assertNotNull(preview)
        assertEquals(GridPoint(1, 0), preview.entryAnchor)
        assertEquals(GridPoint(1, 0), preview.landingAnchor)
        assertEquals(listOf(GridPoint(1, 0), GridPoint(2, 0)), preview.occupiedCells)
        assertTrue(preview.clearedRows.isEmpty())
        assertTrue(preview.clearedColumns.isEmpty())
    }

    @Test
    fun previewPlacement_returnsNull_whenAnyOccupiedCellBlocksTarget() {
        val state = testState(
            config = GameConfig(columns = 4, rows = 4),
            board = BoardMatrix.empty(columns = 4, rows = 4)
                .fill(points = listOf(GridPoint(2, 0)), tone = CellTone.Blue),
            activePiece = dominoPiece(id = 1),
            nextQueue = listOf(dominoPiece(id = 2), squarePiece(id = 3)),
        )

        val preview = logic.previewPlacement(state = state, pieceId = 1, origin = GridPoint(1, 0))

        assertNull(preview)
    }

    @Test
    fun compatibilityPreview_selectsLowestValidOriginInRequestedColumn() {
        val state = testState(
            config = GameConfig(columns = 4, rows = 4),
            board = BoardMatrix.empty(columns = 4, rows = 4)
                .fill(points = listOf(GridPoint(1, 0), GridPoint(2, 0)), tone = CellTone.Gold),
            activePiece = dominoPiece(id = 1),
            nextQueue = listOf(dominoPiece(id = 2), squarePiece(id = 3)),
        )

        val preview = logic.previewPlacement(state = state, column = 1)

        assertNotNull(preview)
        assertEquals(GridPoint(1, 3), preview.landingAnchor)
    }

    @Test
    fun placePiece_clearsRow_updatesCombo_andAdvancesTray() {
        val nextPiece = squarePiece(id = 100)
        val state = testState(
            config = GameConfig(columns = 4, rows = 4),
            board = BoardMatrix.empty(columns = 4, rows = 4)
                .fill(points = listOf(GridPoint(0, 0), GridPoint(1, 0)), tone = CellTone.Blue),
            activePiece = dominoPiece(id = 1),
            nextQueue = listOf(nextPiece, dominoPiece(id = 101)),
        )

        val result = logic.placePiece(state = state, pieceId = 1, origin = GridPoint(2, 0))

        assertTrue(GameEvent.PlacementAccepted in result.events)
        assertTrue(GameEvent.LineClear in result.events)
        assertEquals(1, result.state.linesCleared)
        assertEquals(1, result.state.combo.chain)
        assertEquals(1, result.state.lastResolvedLines)
        assertEquals(1, result.state.lastChainDepth)
        assertEquals(2, result.state.lastPlacementColumn)
        assertEquals(nextPiece.id, result.state.activePiece?.id)
        assertFalse(result.state.board.isOccupied(0, 0))
        assertFalse(result.state.board.isOccupied(1, 0))
        assertFalse(result.state.board.isOccupied(2, 0))
        assertFalse(result.state.board.isOccupied(3, 0))
        assertTrue(result.state.score > 0)
    }

    @Test
    fun placePiece_canClearRowAndColumnTogether() {
        val state = testState(
            config = GameConfig(columns = 3, rows = 3),
            board = BoardMatrix.empty(columns = 3, rows = 3)
                .fill(
                    points = listOf(
                        GridPoint(0, 1),
                        GridPoint(1, 2),
                    ),
                    tone = CellTone.Emerald,
                ),
            activePiece = triLPiece(id = 1),
            nextQueue = listOf(dominoPiece(id = 2), squarePiece(id = 3)),
        )

        val result = logic.placePiece(state = state, pieceId = 1, origin = GridPoint(1, 0))

        assertTrue(GameEvent.PlacementAccepted in result.events)
        assertTrue(GameEvent.LineClear in result.events)
        assertTrue(GameEvent.ChainReaction in result.events)
        assertEquals(setOf(1), result.state.recentlyClearedRows)
        assertEquals(setOf(1), result.state.recentlyClearedColumns)
        assertEquals(2, result.state.lastResolvedLines)
        assertEquals(2, result.state.lastChainDepth)
    }

    @Test
    fun previewImpactPoints_marksExistingCellsThatWouldBeCleared() {
        val state = testState(
            config = GameConfig(columns = 3, rows = 3),
            board = BoardMatrix.empty(columns = 3, rows = 3)
                .fill(
                    points = listOf(
                        GridPoint(0, 1),
                        GridPoint(1, 2),
                    ),
                    tone = CellTone.Emerald,
                ),
            activePiece = triLPiece(id = 1),
            nextQueue = listOf(dominoPiece(id = 2), squarePiece(id = 3)),
        )

        val preview = logic.previewPlacement(state = state, pieceId = 1, origin = GridPoint(1, 0))
        val impactPoints = logic.previewImpactPoints(state = state, preview = preview)

        assertEquals(
            setOf(
                GridPoint(0, 1),
                GridPoint(1, 2),
            ),
            impactPoints,
        )
    }

    @Test
    fun tick_setsGameOver_whenNoTrayPieceFits() {
        val blockedBoard = BoardMatrix.empty(columns = 3, rows = 3).fill(
            points = buildList {
                for (row in 0 until 3) {
                    for (column in 0 until 3) {
                        if (column == 2 && row == 2) continue
                        add(GridPoint(column, row))
                    }
                }
            },
            tone = CellTone.Coral,
        )
        val state = testState(
            config = GameConfig(columns = 3, rows = 3),
            board = blockedBoard,
            activePiece = dominoPiece(id = 1),
            nextQueue = listOf(squarePiece(id = 2), triLPiece(id = 3)),
        )

        val nextState = logic.tick(state)

        assertEquals(GameStatus.GameOver, nextState.status)
    }

    @Test
    fun holdPiece_isInvalid_inFreePlacementMode() {
        val state = testState(
            config = GameConfig(columns = 4, rows = 4),
            board = BoardMatrix.empty(columns = 4, rows = 4),
            activePiece = dominoPiece(id = 1),
            nextQueue = listOf(squarePiece(id = 2), triLPiece(id = 3)),
        )

        val result = logic.holdPiece(state)

        assertEquals(state, result.state)
        assertEquals(setOf(GameEvent.InvalidDrop), result.events)
    }

    private fun testState(
        config: GameConfig,
        board: BoardMatrix,
        activePiece: Piece,
        nextQueue: List<Piece>,
    ): GameState = logic.newGame(config).copy(
        board = board,
        activePiece = activePiece,
        nextQueue = nextQueue,
        holdPiece = null,
        canHold = false,
    )

    private fun dominoPiece(
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

    private fun squarePiece(id: Long): Piece = Piece(
        id = id,
        kind = PieceKind.Square,
        tone = CellTone.Violet,
        cells = listOf(
            GridPoint(0, 0),
            GridPoint(1, 0),
            GridPoint(0, 1),
            GridPoint(1, 1),
        ),
        width = 2,
        height = 2,
        special = SpecialBlockType.None,
    )

    private fun triLPiece(id: Long): Piece = Piece(
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
}