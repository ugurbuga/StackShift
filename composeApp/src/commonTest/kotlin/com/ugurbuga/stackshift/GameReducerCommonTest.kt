package com.ugurbuga.stackshift

import com.ugurbuga.stackshift.game.logic.GameEvent
import com.ugurbuga.stackshift.game.logic.GameLogic
import com.ugurbuga.stackshift.game.logic.StackShiftGameLogic
import com.ugurbuga.stackshift.game.model.BoardMatrix
import com.ugurbuga.stackshift.game.model.CellTone
import com.ugurbuga.stackshift.game.model.GameConfig
import com.ugurbuga.stackshift.game.model.GridPoint
import com.ugurbuga.stackshift.game.model.LaunchBarState
import com.ugurbuga.stackshift.game.model.Piece
import com.ugurbuga.stackshift.game.model.PieceKind
import com.ugurbuga.stackshift.game.model.SpecialBlockType
import com.ugurbuga.stackshift.presentation.game.GameAction
import com.ugurbuga.stackshift.presentation.game.GameEffect
import com.ugurbuga.stackshift.presentation.game.GameFeedbackMapper
import com.ugurbuga.stackshift.presentation.game.GameIntent
import com.ugurbuga.stackshift.presentation.game.GameReducer
import com.ugurbuga.stackshift.presentation.game.GameStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GameReducerCommonTest {

    private val logic = GameLogic(random = Random(7))
    private val stackShiftLogic = StackShiftGameLogic(random = Random(7))
    private val reducer = GameReducer(logic, stackShiftLogic)

    @Test
    fun board_fill_persistsCellLevelSpecialMetadata() {
        val board = BoardMatrix.empty(columns = 4, rows = 4).fill(
            points = listOf(GridPoint(1, 2)),
            tone = CellTone.Coral,
            special = SpecialBlockType.Heavy,
        )

        val cell = board.cellAt(1, 2)

        assertNotNull(cell)
        assertEquals(CellTone.Coral, cell.tone)
        assertEquals(SpecialBlockType.Heavy, cell.special)
    }

    @Test
    fun reducer_placePiece_acceptsValidPlacementImmediately() {
        val state = testState()

        val result = reducer.reduce(
            state = state,
            action = GameAction.PlacePiece(pieceId = 1, origin = GridPoint(2, 1)),
        )

        assertTrue(GameEvent.PlacementAccepted in result.events)
        assertTrue(result.state.board.isOccupied(2, 1))
        assertTrue(result.state.board.isOccupied(3, 1))
        assertTrue(result.effects.isEmpty())
    }

    @Test
    fun reducer_placePiece_clearsLine_andAdvancesTray() {
        val placed = reducer.reduce(
            state = testState(activePiece = heavyDomino(id = 1)),
            action = GameAction.PlacePiece(pieceId = 1, origin = GridPoint(2, 0)),
        )

        assertTrue(GameEvent.PlacementAccepted in placed.events)
        assertTrue(GameEvent.LineClear in placed.events)
        assertEquals(1, placed.state.linesCleared)
        assertEquals(1, placed.state.combo.chain)
        assertFalse(placed.state.board.isOccupied(0, 0))
        assertFalse(placed.state.board.isOccupied(1, 0))
        assertFalse(placed.state.board.isOccupied(2, 0))
        assertFalse(placed.state.board.isOccupied(3, 0))
        assertNotNull(placed.state.activePiece)
    }

    @Test
    fun reducer_placePiece_startsSoftLockTimerForStackShift() {
        val state = stackShiftLogic.newGame(GameConfig(columns = 6, rows = 8))
        val preview = stackShiftLogic.previewPlacement(state = state, approximateColumn = state.config.columns / 2)

        assertNotNull(preview)

        val result = reducer.reduce(
            state = state,
            action = GameAction.PlacePiece(
                pieceId = state.activePiece?.id ?: -1L,
                origin = GridPoint(preview.selectedColumn, 0),
            ),
        )

        assertNotNull(result.state.softLock)
        assertTrue(GameEvent.SoftLockStarted in result.events || GameEvent.SoftLockAdjusted in result.events)
        assertTrue(result.effects.any { it is GameEffect.CancelSoftLockTimer })
        assertTrue(result.effects.any { it is GameEffect.StartSoftLockTimer })
    }

    @Test
    fun reducer_commitSoftLock_placesStackShiftPieceAndAdvancesQueue() {
        val state = stackShiftLogic.newGame(GameConfig(columns = 6, rows = 8))
        val preview = stackShiftLogic.previewPlacement(state = state, approximateColumn = state.config.columns / 2)

        assertNotNull(preview)

        val placed = reducer.reduce(
            state = state,
            action = GameAction.PlacePiece(
                pieceId = state.activePiece?.id ?: -1L,
                origin = GridPoint(preview.selectedColumn, 0),
            ),
        )
        val softLockedPieceId = placed.state.activePiece?.id

        val committed = reducer.reduce(
            state = placed.state,
            action = GameAction.CommitSoftLock,
        )

        assertTrue(GameEvent.PlacementAccepted in committed.events)
        assertEquals(null, committed.state.softLock)
        assertTrue(committed.effects.any { it is GameEffect.CancelSoftLockTimer })
        assertTrue(committed.state.board.occupiedCount > state.board.occupiedCount)
        assertNotNull(committed.state.activePiece)
        assertTrue(committed.state.activePiece.id != softLockedPieceId)
    }

    @Test
    fun store_dispatch_updatesUiStateThroughReducer() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val store = GameStore(blockWiseGameLogic = logic, stackShiftGameLogic = stackShiftLogic, scope = scope)

        try {
            store.replaceState(testState())

            val events = store.dispatch(GameIntent.PlacePiece(pieceId = 1, origin = GridPoint(2, 1)))

            assertTrue(GameEvent.PlacementAccepted in events)
            assertTrue(store.uiState.value.gameState.board.isOccupied(2, 1))
        } finally {
            store.dispose()
            scope.cancel()
        }
    }

    @Test
    fun feedbackMapper_mapsPauseResumeAndGameOverSignals() {
        val feedback = GameFeedbackMapper().map(
            setOf(GameEvent.PlacementAccepted, GameEvent.Restarted, GameEvent.GameOver),
        )

        assertTrue(feedback.sounds.isNotEmpty())
        assertTrue(feedback.haptics.isNotEmpty())
        assertFalse(feedback.sounds.isEmpty())
    }

    private fun testState(
        activePiece: Piece = dominoPiece(id = 1),
    ) = logic.newGame(GameConfig(columns = 4, rows = 4)).copy(
        board = BoardMatrix.empty(columns = 4, rows = 4).fill(
            points = listOf(GridPoint(0, 0), GridPoint(1, 0)),
            tone = CellTone.Blue,
        ),
        activePiece = activePiece,
        nextQueue = listOf(dominoPiece(id = 2), dominoPiece(id = 3), dominoPiece(id = 4)),
        launchBar = LaunchBarState(),
    )

    private fun dominoPiece(id: Long): Piece = Piece(
        id = id,
        kind = PieceKind.Domino,
        tone = CellTone.Cyan,
        cells = listOf(GridPoint(0, 0), GridPoint(1, 0)),
        width = 2,
        height = 1,
        special = SpecialBlockType.None,
    )

    private fun heavyDomino(id: Long): Piece = Piece(
        id = id,
        kind = PieceKind.Domino,
        tone = CellTone.Gold,
        cells = listOf(GridPoint(0, 0), GridPoint(1, 0)),
        width = 2,
        height = 1,
        special = SpecialBlockType.Heavy,
    )
}

