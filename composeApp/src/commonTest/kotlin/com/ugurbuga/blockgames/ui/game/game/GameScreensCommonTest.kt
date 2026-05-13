package com.ugurbuga.blockgames.ui.game.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.ugurbuga.blockgames.game.logic.GameLogic
import com.ugurbuga.blockgames.game.model.BoardMatrix
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.GridPoint
import com.ugurbuga.blockgames.game.model.Piece
import com.ugurbuga.blockgames.game.model.PieceKind
import com.ugurbuga.blockgames.game.model.PlacementPreview
import com.ugurbuga.blockgames.game.model.SpecialBlockType
import com.ugurbuga.blockgames.settings.BlockWiseOnboardingStage
import com.ugurbuga.blockgames.settings.BlockWiseOnboardingStateFactory
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameScreensCommonTest {

    private val logic = GameLogic.create(random = Random(42))

    @Test
    fun hiddenOnboardingPreviewState_clearsPreviewSpecificInventory() {
        val state = logic.newGame(GameConfig.default(GameplayStyle.BlockWise)).copy(
            gameplayStyle = GameplayStyle.BlockWise,
            holdPiece = dominoPiece(),
            canHold = true,
        )

        val hidden = state.hiddenOnboardingPreviewState()

        assertNull(hidden.activePiece)
        assertTrue(hidden.nextQueue.isEmpty())
        assertNull(hidden.holdPiece)
        assertEquals(false, hidden.canHold)
        assertEquals(state.board, hidden.board)
    }

    @Test
    fun restartConfigForStyle_reusesMatchingConfigOtherwiseFallsBackToDefault() {
        val matching = logic.newGame(GameConfig.default(GameplayStyle.MergeShift)).copy(
            gameplayStyle = GameplayStyle.MergeShift,
        )
        val mismatched = matching.copy(
            gameplayStyle = GameplayStyle.BlockWise,
            config = GameConfig(columns = 9, rows = 9),
        )

        assertEquals(matching.config, restartConfigForStyle(matching, GameplayStyle.MergeShift))
        assertEquals(GameConfig.default(GameplayStyle.MergeShift), restartConfigForStyle(mismatched, GameplayStyle.MergeShift))
    }

    @Test
    fun nextInteractiveOnboardingStage_returnsFollowingStageOrNullWhenMissing() {
        val stages = BlockWiseOnboardingStateFactory.stages

        assertEquals(BlockWiseOnboardingStage.LineClear, nextInteractiveOnboardingStage(BlockWiseOnboardingStage.DragToBoard, stages))
        assertNull(nextInteractiveOnboardingStage(BlockWiseOnboardingStage.CrossClear, stages))
    }

    @Test
    fun boomBlocksFindConnectedGroup_traversesOrthogonalNeighborsOnly() {
        val board = BoardMatrix.empty(columns = 4, rows = 4)
            .fill(
                points = listOf(
                    GridPoint(1, 1),
                    GridPoint(2, 1),
                    GridPoint(2, 2),
                    GridPoint(0, 0),
                ),
                tone = CellTone.Cyan,
            )
            .fill(points = listOf(GridPoint(1, 2)), tone = CellTone.Gold)

        val group = findConnectedGroup(board, GridPoint(1, 1))

        assertEquals(
            setOf(
                GridPoint(1, 1),
                GridPoint(2, 1),
                GridPoint(2, 2),
            ),
            group,
        )
    }

    @Test
    fun blockWiseHelpers_computePointerLocalityAndOverlap() {
        val pieceRect = Rect(left = 120f, top = 220f, right = 180f, bottom = 260f)
        val hostRect = Rect(left = 100f, top = 200f, right = 300f, bottom = 500f)

        assertEquals(
            Offset(30f, 35f),
            initialDragPointerPosition(pieceRect, Offset(10f, 15f), hostRect),
        )
        assertNull(initialDragPointerPosition(Rect.Zero, Offset.Zero, hostRect))
        assertEquals(
            100f,
            overlapArea(
                Rect(left = 0f, top = 0f, right = 20f, bottom = 20f),
                Rect(left = 10f, top = 10f, right = 30f, bottom = 30f),
            ),
            absoluteTolerance = 0.0001f,
        )
        assertEquals(
            Rect(left = 20f, top = 20f, right = 80f, bottom = 60f),
            pieceRect.toBlockWiseLocalRect(hostRect),
        )
    }

    @Test
    fun mergeShiftLocalRect_translation_returnsRelativeRectOrZero() {
        val rect = Rect(left = 40f, top = 70f, right = 80f, bottom = 130f)
        val host = Rect(left = 10f, top = 20f, right = 200f, bottom = 200f)

        assertEquals(Rect(left = 30f, top = 50f, right = 70f, bottom = 110f), rect.toMergeShiftLocalRect(host))
        assertEquals(Rect.Zero, Rect.Zero.toMergeShiftLocalRect(host))
    }

    @Test
    fun stackShiftLaunchGuideSegments_buildOneGuidePerOccupiedPieceColumn() {
        val preview = PlacementPreview(
            selectedColumn = 2,
            entryAnchor = GridPoint(2, 0),
            landingAnchor = GridPoint(2, 2),
            occupiedCells = listOf(
                GridPoint(2, 2),
                GridPoint(2, 3),
                GridPoint(3, 3),
            ),
            coveredColumns = 2..3,
        )
        val piece = Piece(
            id = 1L,
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

        val segments = launchGuideSegments(
            preview = preview,
            activePiece = piece,
            pieceTopLeft = Offset(40f, 100f),
            boardRect = Rect(left = 20f, top = 20f, right = 220f, bottom = 220f),
            cellSizePx = 20f,
        )

        assertEquals(2, segments.size)
        assertTrue(segments.all { it.height > 0f })
        assertEquals(40f, segments.first().left, absoluteTolerance = 0.0001f)
        assertEquals(60f, segments.last().left, absoluteTolerance = 0.0001f)
    }

    @Test
    fun stackShiftLocalRect_translation_matchesSharedGeometryExpectation() {
        val rect = Rect(left = 24f, top = 48f, right = 64f, bottom = 108f)
        val host = Rect(left = 4f, top = 8f, right = 200f, bottom = 300f)

        assertEquals(Rect(left = 20f, top = 40f, right = 60f, bottom = 100f), rect.toStackShiftLocalRect(host))
    }

    private fun dominoPiece(): Piece = Piece(
        id = 99L,
        kind = PieceKind.Domino,
        tone = CellTone.Cyan,
        cells = listOf(GridPoint(0, 0), GridPoint(1, 0)),
        width = 2,
        height = 1,
        special = SpecialBlockType.None,
    )
}

