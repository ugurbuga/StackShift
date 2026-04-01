package com.ugurbuga.stackshift

import com.ugurbuga.stackshift.game.logic.GameEvent
import com.ugurbuga.stackshift.game.logic.GameLogic
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
    private val reducer = GameReducer(logic)

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
    fun reducer_launchColumn_emitsSoftLockTimerEffect() {
        val state = testState()

        val result = reducer.reduce(
            state = state,
            action = GameAction.LaunchColumn(column = 1),
        )

        assertNotNull(result.state.softLock)
        assertTrue(GameEvent.SoftLockStarted in result.events)
        assertTrue(result.effects.any { it is GameEffect.StartSoftLockTimer })
    }

    @Test
    fun reducer_commitSoftLock_cancelsTimerAndPlacesEncodedSpecialCells() {
        val launched = reducer.reduce(
            state = testState(activePiece = heavyDomino(id = 1)),
            action = GameAction.LaunchColumn(column = 1),
        )

        val committed = reducer.reduce(
            state = launched.state,
            action = GameAction.CommitSoftLock,
        )

        assertTrue(GameEffect.CancelSoftLockTimer in committed.effects)
        assertTrue(GameEvent.PlacementAccepted in committed.events)
        val occupiedSpecials = buildList {
            for (row in 0 until committed.state.config.rows) {
                for (column in 0 until committed.state.config.columns) {
                    committed.state.board.cellAt(column, row)
                        ?.takeIf { it.special == SpecialBlockType.Heavy }
                        ?.let { add(it) }
                }
            }
        }
        assertTrue(occupiedSpecials.isNotEmpty())
    }

    @Test
    fun store_dispatch_updatesUiStateThroughReducer() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val store = GameStore(gameLogic = logic, scope = scope)

        try {
            store.replaceState(testState())

            val events = store.dispatch(GameIntent.LaunchColumn(column = 1))

            assertTrue(GameEvent.SoftLockStarted in events)
            assertNotNull(store.uiState.value.gameState.softLock)
        } finally {
            store.dispose()
            scope.cancel()
        }
    }

    @Test
    fun feedbackMapper_mapsPauseResumeAndGameOverSignals() {
        val feedback = GameFeedbackMapper().map(
            setOf(GameEvent.Paused, GameEvent.Resumed, GameEvent.GameOver),
        )

        assertTrue(feedback.sounds.isNotEmpty())
        assertTrue(feedback.haptics.isNotEmpty())
        assertFalse(feedback.sounds.isEmpty())
    }

    private fun testState(
        activePiece: Piece = dominoPiece(id = 1),
    ) = logic.newGame(GameConfig(columns = 4, rows = 4)).copy(
        board = BoardMatrix.empty(columns = 4, rows = 4),
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

