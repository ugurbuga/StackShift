package com.ugurbuga.stackshift

import com.ugurbuga.stackshift.game.logic.GameEvent
import com.ugurbuga.stackshift.game.logic.GameLogic
import com.ugurbuga.stackshift.game.model.BoardMatrix
import com.ugurbuga.stackshift.game.model.CellTone
import com.ugurbuga.stackshift.game.model.GameConfig
import com.ugurbuga.stackshift.game.model.GameState
import com.ugurbuga.stackshift.game.model.GridPoint
import com.ugurbuga.stackshift.game.model.LaunchBarState
import com.ugurbuga.stackshift.game.model.Piece
import com.ugurbuga.stackshift.game.model.PieceKind
import com.ugurbuga.stackshift.game.model.SpecialBlockType
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ComposeAppCommonTest {

    private val logic = GameLogic(random = Random(42))

    @Test
    fun previewPlacement_returnsTopmostReachableAnchor_forSelectedColumn() {
        val config = GameConfig(columns = 4, rows = 4, difficultyIntervalSeconds = 10, linesPerLevel = 2)
        val board = BoardMatrix.empty(columns = 4, rows = 4).fill(
            points = listOf(GridPoint(1, 1)),
            tone = CellTone.Coral,
        )
        val state = testState(
            config = config,
            board = board,
            activePiece = dominoPiece(id = 1),
            nextQueue = listOf(dominoPiece(id = 2), dominoPiece(id = 3), dominoPiece(id = 4)),
        )

        val preview = logic.previewPlacement(state = state, approximateColumn = 1)

        assertNotNull(preview)
        assertEquals(1, preview.selectedColumn)
        assertEquals(GridPoint(1, 3), preview.entryAnchor)
        assertEquals(GridPoint(1, 2), preview.landingAnchor)
        assertEquals(1..2, preview.coveredColumns)
    }

    @Test
    fun previewPlacement_returnsNull_whenEntryRowIsBlocked() {
        val config = GameConfig(columns = 4, rows = 4, difficultyIntervalSeconds = 10, linesPerLevel = 2)
        val board = BoardMatrix.empty(columns = 4, rows = 4).fill(
            points = listOf(GridPoint(0, 3)),
            tone = CellTone.Blue,
        )
        val state = testState(
            config = config,
            board = board,
            activePiece = dominoPiece(id = 1),
            nextQueue = listOf(dominoPiece(id = 2), dominoPiece(id = 3), dominoPiece(id = 4)),
        )

        val preview = logic.previewPlacement(state = state, approximateColumn = 0)

        assertNull(preview)
    }

    @Test
    fun placePiece_clearsRow_updatesComboAndPromotesNextPiece() {
        val config = GameConfig(columns = 4, rows = 4, difficultyIntervalSeconds = 10, linesPerLevel = 2)
        val board = BoardMatrix.empty(columns = 4, rows = 4).fill(
            points = listOf(
                GridPoint(0, 3),
                GridPoint(1, 3),
                GridPoint(2, 2),
            ),
            tone = CellTone.Blue,
        )
        val nextPiece = squarePiece(id = 100)
        val state = testState(
            config = config,
            board = board,
            activePiece = dominoPiece(id = 1),
            nextQueue = listOf(nextPiece, dominoPiece(id = 101), dominoPiece(id = 102)),
        )

        val softLocked = logic.placePiece(state = state, approximateColumn = 2)
        val result = logic.commitSoftLock(softLocked.state)

        assertEquals(1, result.state.linesCleared)
        assertEquals(1, result.state.combo.chain)
        assertEquals(1, result.state.lastResolvedLines)
        assertEquals(1, result.state.lastChainDepth)
        assertEquals(2, result.state.lastPlacementColumn)
        assertTrue(GameEvent.LineClear in result.events)
        assertEquals(nextPiece.id, result.state.activePiece?.id)
        assertFalse(result.state.board.isOccupied(0, 3))
        assertTrue(result.state.score > 0)
    }

    @Test
    fun placePiece_clearsBottomRow_withoutPullingUpperRowsDown() {
        val config = GameConfig(columns = 3, rows = 4, difficultyIntervalSeconds = 10, linesPerLevel = 2)
        val board = BoardMatrix.empty(columns = 3, rows = 4).fill(
            points = listOf(
                GridPoint(0, 3),
                GridPoint(0, 1),
                GridPoint(1, 2),
                GridPoint(2, 0),
            ),
            tone = CellTone.Emerald,
        )
        val state = testState(
            config = config,
            board = board,
            activePiece = dominoPiece(id = 1),
            nextQueue = listOf(dominoPiece(id = 2), dominoPiece(id = 3), dominoPiece(id = 4)),
        )

        val softLocked = logic.placePiece(state = state, approximateColumn = 1)
        val result = logic.commitSoftLock(softLocked.state)

        assertEquals(1, result.state.linesCleared)
        assertEquals(1, result.state.lastResolvedLines)
        assertEquals(1, result.state.lastChainDepth)
        assertTrue(GameEvent.LineClear in result.events)
        assertFalse(GameEvent.ChainReaction in result.events)
        assertFalse(result.state.board.isOccupied(0, 3))
        assertFalse(result.state.board.isOccupied(1, 3))
        assertFalse(result.state.board.isOccupied(2, 3))
        assertTrue(result.state.board.isOccupied(0, 1))
        assertTrue(result.state.board.isOccupied(1, 2))
        assertTrue(result.state.board.isOccupied(2, 0))
    }

    @Test
    fun clearRows_movesBlocksBelowClearedRow_upward() {
        val board = BoardMatrix.empty(columns = 3, rows = 5).fill(
            points = listOf(
                GridPoint(0, 0),
                GridPoint(1, 2),
                GridPoint(2, 4),
            ),
            tone = CellTone.Gold,
        )

        val cleared = board.clearRows(setOf(1))

        assertTrue(cleared.isOccupied(0, 0))
        assertTrue(cleared.isOccupied(1, 1))
        assertTrue(cleared.isOccupied(2, 3))
        assertFalse(cleared.isOccupied(1, 2))
        assertFalse(cleared.isOccupied(2, 4))
    }

    @Test
    fun tick_increasesDifficultyAndLevel_whenCountdownExpires() {
        val config = GameConfig(columns = 5, rows = 5, difficultyIntervalSeconds = 7, linesPerLevel = 3)
        val state = testState(
            config = config,
            board = BoardMatrix.empty(columns = 5, rows = 5),
            activePiece = dominoPiece(id = 1),
            nextQueue = listOf(dominoPiece(id = 2), dominoPiece(id = 3), dominoPiece(id = 4)),
        ).copy(
            secondsUntilDifficultyIncrease = 1,
        )

        val nextState = logic.tick(state)

        assertEquals(1, nextState.difficultyStage)
        assertEquals(2, nextState.level)
        assertEquals(config.difficultyIntervalSeconds, nextState.secondsUntilDifficultyIncrease)
    }

    @Test
    fun placePiece_entersSoftLock_beforeCommit() {
        val state = testState(
            config = GameConfig(columns = 4, rows = 4),
            board = BoardMatrix.empty(columns = 4, rows = 4),
            activePiece = dominoPiece(id = 1),
            nextQueue = listOf(dominoPiece(id = 2), dominoPiece(id = 3), dominoPiece(id = 4)),
        )

        val result = logic.placePiece(state = state, approximateColumn = 1)

        assertNotNull(result.state.softLock)
        assertEquals(1, result.state.softLock.preview.selectedColumn)
        assertTrue(GameEvent.SoftLockStarted in result.events)
    }

    @Test
    fun holdPiece_swapsActivePiece_andDisablesSecondHoldUntilLock() {
        val held = squarePiece(id = 9)
        val queueFirst = dominoPiece(id = 2)
        val state = testState(
            config = GameConfig(columns = 4, rows = 4),
            board = BoardMatrix.empty(columns = 4, rows = 4),
            activePiece = dominoPiece(id = 1),
            nextQueue = listOf(queueFirst, dominoPiece(id = 3), dominoPiece(id = 4)),
            holdPiece = held,
        )

        val result = logic.holdPiece(state)

        assertEquals(held.id, result.state.activePiece?.id)
        assertEquals(1, result.state.holdPiece?.id)
        assertFalse(result.state.canHold)
        assertTrue(GameEvent.HoldUsed in result.events)
    }

    @Test
    fun previewPlacement_marksPerfectDrop_whenCoveredColumnsAreEmpty() {
        val state = testState(
            config = GameConfig(columns = 5, rows = 5),
            board = BoardMatrix.empty(columns = 5, rows = 5).fill(points = listOf(GridPoint(0, 4)), tone = CellTone.Blue),
            activePiece = dominoPiece(id = 1),
            nextQueue = listOf(dominoPiece(id = 2), dominoPiece(id = 3), dominoPiece(id = 4)),
        )

        val preview = logic.previewPlacement(state = state, approximateColumn = 2)

        assertNotNull(preview)
        assertTrue(preview.isPerfectDrop)
    }

    @Test
    fun landedColumnClearer_inClearedRow_triggersSecondaryColumnChain() {
        val board = BoardMatrix.empty(columns = 4, rows = 4)
            .fill(points = listOf(GridPoint(0, 3)), tone = CellTone.Blue)
            .fill(points = listOf(GridPoint(1, 3)), tone = CellTone.Gold, special = SpecialBlockType.ColumnClearer)
            .fill(points = listOf(GridPoint(2, 2)), tone = CellTone.Emerald)
            .fill(points = listOf(GridPoint(1, 1)), tone = CellTone.Coral)
        val state = testState(
            config = GameConfig(columns = 4, rows = 4),
            board = board,
            activePiece = dominoPiece(id = 1),
            nextQueue = listOf(dominoPiece(id = 2), dominoPiece(id = 3), dominoPiece(id = 4)),
        )

        val softLocked = logic.placePiece(state = state, approximateColumn = 2)
        val result = logic.commitSoftLock(softLocked.state)

        assertTrue(GameEvent.SpecialTriggered in result.events)
        assertEquals(1, result.state.specialChainCount)
        assertFalse(result.state.board.isOccupied(1, 1))
        assertTrue(result.state.screenShakeToken > 0)
        assertTrue(result.state.impactFlashToken > 0)
    }

    @Test
    fun previewImpactPoints_marksExistingCellsInColumnClearerLane() {
        val board = BoardMatrix.empty(columns = 4, rows = 4).fill(
            points = listOf(
                GridPoint(1, 0),
                GridPoint(1, 1),
                GridPoint(2, 2),
                GridPoint(0, 3),
            ),
            tone = CellTone.Gold,
        )
        val state = testState(
            config = GameConfig(columns = 4, rows = 4),
            board = board,
            activePiece = dominoPiece(id = 1, special = SpecialBlockType.ColumnClearer),
            nextQueue = listOf(dominoPiece(id = 2), dominoPiece(id = 3), dominoPiece(id = 4)),
        )

        val preview = logic.previewPlacement(state = state, approximateColumn = 1)
        val impactPoints = logic.previewImpactPoints(state = state, preview = preview)

        assertEquals(
            setOf(
                GridPoint(1, 0),
                GridPoint(1, 1),
                GridPoint(2, 2),
            ),
            impactPoints,
        )
    }

    @Test
    fun previewImpactPoints_marksExistingCellsInCompletedRow_forNormalPiece() {
        val board = BoardMatrix.empty(columns = 4, rows = 4).fill(
            points = listOf(
                GridPoint(0, 3),
                GridPoint(1, 3),
                GridPoint(2, 2),
            ),
            tone = CellTone.Blue,
        )
        val state = testState(
            config = GameConfig(columns = 4, rows = 4),
            board = board,
            activePiece = dominoPiece(id = 1),
            nextQueue = listOf(dominoPiece(id = 2), dominoPiece(id = 3), dominoPiece(id = 4)),
        )

        val preview = logic.previewPlacement(state = state, approximateColumn = 2)
        assertNotNull(preview)
        assertEquals(GridPoint(2, 3), preview.landingAnchor)
        val impactPoints = logic.previewImpactPoints(state = state, preview = preview)

        assertEquals(
            setOf(
                GridPoint(0, 3),
                GridPoint(1, 3),
            ),
            impactPoints,
        )
    }

    private fun testState(
        config: GameConfig,
        board: BoardMatrix,
        activePiece: Piece,
        nextQueue: List<Piece>,
        holdPiece: Piece? = null,
    ): GameState = GameState(
        config = config,
        board = board,
        activePiece = activePiece,
        nextQueue = nextQueue,
        holdPiece = holdPiece,
        canHold = true,
        score = 0,
        lastMoveScore = 0,
        linesCleared = 0,
        level = 1,
        difficultyStage = 0,
        secondsUntilDifficultyIncrease = config.difficultyIntervalSeconds,
        launchBar = LaunchBarState(),
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
}