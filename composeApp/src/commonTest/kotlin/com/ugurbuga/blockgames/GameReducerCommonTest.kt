package com.ugurbuga.blockgames

import com.ugurbuga.blockgames.game.logic.GameEvent
import com.ugurbuga.blockgames.game.logic.GameLogic
import com.ugurbuga.blockgames.game.model.BoardMatrix
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.GridPoint
import com.ugurbuga.blockgames.game.model.LaunchBarState
import com.ugurbuga.blockgames.game.model.Piece
import com.ugurbuga.blockgames.game.model.PieceKind
import com.ugurbuga.blockgames.game.model.SpecialBlockType
import com.ugurbuga.blockgames.platform.GlobalPlatformConfig
import com.ugurbuga.blockgames.presentation.game.GameAction
import com.ugurbuga.blockgames.presentation.game.GameEffect
import com.ugurbuga.blockgames.presentation.game.GameFeedbackMapper
import com.ugurbuga.blockgames.presentation.game.GameIntent
import com.ugurbuga.blockgames.presentation.game.GameReducer
import com.ugurbuga.blockgames.presentation.game.GameStore
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

    private val logic = GameLogic.create(random = Random(7))
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
        withGameplayStyle(GameplayStyle.StackShift) {
            val state = logic.newGame(config = GameConfig(columns = 6, rows = 8))
            val preview = logic.previewPlacement(state = state, column = state.config.columns / 2)

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
    }

    @Test
    fun reducer_commitSoftLock_placesStackShiftPieceAndAdvancesQueue() {
        withGameplayStyle(GameplayStyle.StackShift) {
            val state = logic.newGame(config = GameConfig(columns = 6, rows = 8))
            val preview = logic.previewPlacement(state = state, column = state.config.columns / 2)

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
    }

    @Test
    fun reducer_restart_usesCurrentGlobalGameplayStyle() {
        withGameplayStyle(GameplayStyle.BlockWise) {
            val result = reducer.reduce(
                state = testState(),
                action = GameAction.Restart(config = GameConfig.default()),
            )

            assertEquals(GameplayStyle.BlockWise, result.state.gameplayStyle)
            assertEquals(GameConfig.default(), result.state.config)
            assertTrue(GameEvent.Restarted in result.events)
        }
    }

    @Test
    fun store_dispatch_updatesUiStateThroughReducer() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val store = GameStore(gameLogic = logic, scope = scope)

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
    ) = withGameplayStyle(GameplayStyle.BlockWise) {
        logic.newGame(config = GameConfig(columns = 4, rows = 4)).copy(
            board = BoardMatrix.empty(columns = 4, rows = 4).fill(
                points = listOf(GridPoint(0, 0), GridPoint(1, 0)),
                tone = CellTone.Blue,
            ),
            activePiece = activePiece,
            nextQueue = listOf(dominoPiece(id = 2), dominoPiece(id = 3), dominoPiece(id = 4)),
            launchBar = LaunchBarState(),
        )
    }

    private inline fun <T> withGameplayStyle(
        gameplayStyle: GameplayStyle,
        block: () -> T,
    ): T {
        val previousGameplayStyle = GlobalPlatformConfig.gameplayStyle
        GlobalPlatformConfig.gameplayStyle = gameplayStyle
        return try {
            block()
        } finally {
            GlobalPlatformConfig.gameplayStyle = previousGameplayStyle
        }
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

