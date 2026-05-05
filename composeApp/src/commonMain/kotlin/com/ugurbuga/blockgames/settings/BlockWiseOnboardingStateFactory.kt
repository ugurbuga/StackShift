package com.ugurbuga.blockgames.settings

import androidx.compose.runtime.Immutable
import com.ugurbuga.blockgames.game.logic.BlockWiseGameLogic
import com.ugurbuga.blockgames.game.model.BoardMatrix
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.game.model.GameTextKey
import com.ugurbuga.blockgames.game.model.GridPoint
import com.ugurbuga.blockgames.game.model.Piece
import com.ugurbuga.blockgames.game.model.PieceKind
import com.ugurbuga.blockgames.game.model.SpecialBlockType
import com.ugurbuga.blockgames.game.model.gameText

@Immutable
data class BlockWiseOnboardingScene(
    val stage: BlockWiseOnboardingStage,
    val gameState: GameState,
    val target: StackShiftOnboardingTarget,
    val guidePoint: GridPoint?,
)


@Immutable
enum class BlockWiseOnboardingStage : OnboardingStage {
    DragToBoard,
    LineClear,
    ColumnClear,
    CrossClear,
}

object BlockWiseOnboardingStateFactory {
    private val guideLogic = BlockWiseGameLogic()
    private val config = GameConfig(
        columns = 8,
        rows = 10,
        difficultyIntervalSeconds = 9_999,
        linesPerLevel = 9_999,
    )

    val stages: List<BlockWiseOnboardingStage> = listOf(
        BlockWiseOnboardingStage.DragToBoard,
        BlockWiseOnboardingStage.LineClear,
        BlockWiseOnboardingStage.ColumnClear,
        BlockWiseOnboardingStage.CrossClear,
    )

    private val sceneCache: Map<BlockWiseOnboardingStage, BlockWiseOnboardingScene> =
        stages.associateWith(::buildScene)

    fun initialState(): GameState =
        scene(stages.first()).gameState

    fun cleanGameState(): GameState =
        guideLogic.newGame(config = config)

    fun scene(stage: BlockWiseOnboardingStage): BlockWiseOnboardingScene = sceneCache.getValue(stage)

    private fun buildScene(stage: BlockWiseOnboardingStage): BlockWiseOnboardingScene = when (stage) {
        BlockWiseOnboardingStage.DragToBoard -> BlockWiseOnboardingScene(
            stage = stage,
            target = StackShiftOnboardingTarget.Tray,
            gameState = scriptedState(
                board = BoardMatrix.empty(columns = config.columns, rows = config.rows)
                    .fill(
                        points = listOf(
                            GridPoint(0, 4), GridPoint(1, 4), GridPoint(2, 4),
                            GridPoint(5, 3), GridPoint(6, 3), GridPoint(7, 3),
                        ),
                        tone = CellTone.Blue,
                    ),
                trayPieces = listOf(
                    piece(id = -20_001, kind = PieceKind.T, tone = CellTone.Cyan),
                ),
            ),
            guidePoint = null,
        )

        BlockWiseOnboardingStage.LineClear -> BlockWiseOnboardingScene(
            stage = stage,
            target = StackShiftOnboardingTarget.Board,
            gameState = scriptedState(
                board = BoardMatrix.empty(columns = config.columns, rows = config.rows)
                    .fill(
                        points = listOf(
                            GridPoint(0, 5), GridPoint(1, 5), GridPoint(2, 5),
                            GridPoint(5, 5), GridPoint(6, 5), GridPoint(7, 5),
                        ),
                        tone = CellTone.Blue,
                    ),
                trayPieces = listOf(
                    piece(id = -20_101, kind = PieceKind.TriL, tone = CellTone.Gold),
                ),
            ),
            guidePoint = GridPoint(3, 4),
        )

        BlockWiseOnboardingStage.ColumnClear -> BlockWiseOnboardingScene(
            stage = stage,
            target = StackShiftOnboardingTarget.Board,
            gameState = scriptedState(
                board = BoardMatrix.empty(columns = config.columns, rows = config.rows)
                    .fill(
                        points = listOf(
                            GridPoint(3, 0), GridPoint(3, 1), GridPoint(3, 2), GridPoint(3, 3), GridPoint(3, 4),
                            GridPoint(3, 6), GridPoint(3, 7), GridPoint(3, 8), GridPoint(3, 9),
                        ),
                        tone = CellTone.Blue,
                    ),
                trayPieces = listOf(
                    piece(id = -20_201, kind = PieceKind.Domino, tone = CellTone.Emerald),
                ),
            ),
            guidePoint = GridPoint(3, 5),
        )

        BlockWiseOnboardingStage.CrossClear -> BlockWiseOnboardingScene(
            stage = stage,
            target = StackShiftOnboardingTarget.Board,
            gameState = scriptedState(
                board = BoardMatrix.empty(columns = config.columns, rows = config.rows)
                    .fill(
                        points = listOf(
                            GridPoint(0, 6), GridPoint(1, 6), GridPoint(2, 6),
                            GridPoint(5, 6), GridPoint(6, 6), GridPoint(7, 6),
                            GridPoint(3, 0), GridPoint(3, 1), GridPoint(3, 2), GridPoint(3, 3),
                            GridPoint(3, 7), GridPoint(3, 8), GridPoint(3, 9),
                        ),
                        tone = CellTone.Blue,
                    ),
                trayPieces = listOf(
                    piece(id = -20_301, kind = PieceKind.L, tone = CellTone.Amber),
                ),
            ),
            guidePoint = GridPoint(3, 4),
        )
    }

    private fun scriptedState(
        board: BoardMatrix,
        trayPieces: List<Piece>,
    ): GameState {
        val baseState = guideLogic.newGame(config = config)
        return baseState.copy(
            board = board,
            activePiece = trayPieces.firstOrNull(),
            nextQueue = trayPieces.drop(1),
            holdPiece = null,
            canHold = false,
            message = gameText(GameTextKey.GameMessageSelectColumn),
            secondsUntilDifficultyIncrease = config.difficultyIntervalSeconds,
        )
    }

    private fun piece(
        id: Long,
        kind: PieceKind,
        tone: CellTone,
        special: SpecialBlockType = SpecialBlockType.None,
    ): Piece {
        val width = kind.template.maxOf(GridPoint::column) + 1
        val height = kind.template.maxOf(GridPoint::row) + 1
        return Piece(
            id = id,
            kind = kind,
            tone = tone,
            cells = kind.template,
            width = width,
            height = height,
            special = special,
        )
    }
}
