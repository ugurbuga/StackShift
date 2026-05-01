package com.ugurbuga.blockgames.settings

import androidx.compose.runtime.Immutable
import com.ugurbuga.blockgames.game.logic.GameLogic
import com.ugurbuga.blockgames.game.model.BoardMatrix
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.game.model.GameTextKey
import com.ugurbuga.blockgames.game.model.GameplayStyle
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
    private val guideLogic = GameLogic.create()
    private val config = GameConfig(
        columns = 10,
        rows = 12,
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
        guideLogic.newGame(config, gameplayStyle = GameplayStyle.BlockWise)

    fun scene(stage: BlockWiseOnboardingStage): BlockWiseOnboardingScene = sceneCache.getValue(stage)

    private fun buildScene(stage: BlockWiseOnboardingStage): BlockWiseOnboardingScene = when (stage) {
        BlockWiseOnboardingStage.DragToBoard -> BlockWiseOnboardingScene(
            stage = stage,
            target = StackShiftOnboardingTarget.Tray,
            gameState = scriptedState(
                board = BoardMatrix.empty(columns = config.columns, rows = config.rows)
                    .fill(
                        points = listOf(
                            GridPoint(1, 4), GridPoint(2, 4), GridPoint(3, 4),
                            GridPoint(6, 3), GridPoint(7, 3), GridPoint(8, 3),
                        ),
                        tone = CellTone.Blue,
                    ),
                trayPieces = listOf(
                    piece(id = -20_001, kind = PieceKind.T, tone = CellTone.Cyan),
                    piece(id = -20_002, kind = PieceKind.Domino, tone = CellTone.Gold),
                    piece(id = -20_003, kind = PieceKind.Square, tone = CellTone.Emerald),
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
                            GridPoint(0, 5), GridPoint(1, 5), GridPoint(2, 5), GridPoint(3, 5),
                            GridPoint(6, 5), GridPoint(7, 5), GridPoint(8, 5), GridPoint(9, 5),
                        ),
                        tone = CellTone.Blue,
                    ),
                trayPieces = listOf(
                    piece(id = -20_101, kind = PieceKind.Domino, tone = CellTone.Gold),
                    piece(id = -20_102, kind = PieceKind.Square, tone = CellTone.Emerald),
                    piece(id = -20_103, kind = PieceKind.TriL, tone = CellTone.Violet),
                ),
            ),
            guidePoint = GridPoint(4, 5),
        )

        BlockWiseOnboardingStage.ColumnClear -> BlockWiseOnboardingScene(
            stage = stage,
            target = StackShiftOnboardingTarget.Board,
            gameState = scriptedState(
                board = BoardMatrix.empty(columns = config.columns, rows = config.rows)
                    .fill(
                        points = listOf(
                            GridPoint(4, 2), GridPoint(4, 3), GridPoint(4, 4),
                            GridPoint(4, 6), GridPoint(4, 7), GridPoint(4, 8),
                        ),
                        tone = CellTone.Blue,
                    ),
                trayPieces = listOf(
                    piece(id = -20_201, kind = PieceKind.Domino, tone = CellTone.Emerald),
                    piece(id = -20_202, kind = PieceKind.Square, tone = CellTone.Violet),
                    piece(id = -20_203, kind = PieceKind.TriL, tone = CellTone.Gold),
                ),
            ),
            guidePoint = GridPoint(4, 5),
        )

        BlockWiseOnboardingStage.CrossClear -> BlockWiseOnboardingScene(
            stage = stage,
            target = StackShiftOnboardingTarget.Board,
            gameState = scriptedState(
                board = BoardMatrix.empty(columns = config.columns, rows = config.rows)
                    .fill(
                        points = listOf(
                            GridPoint(0, 5), GridPoint(1, 5), GridPoint(2, 5), GridPoint(3, 5),
                            GridPoint(5, 5), GridPoint(6, 5), GridPoint(7, 5), GridPoint(8, 5), GridPoint(9, 5),
                            GridPoint(4, 2), GridPoint(4, 3), GridPoint(4, 4),
                            GridPoint(4, 6), GridPoint(4, 7), GridPoint(4, 8),
                        ),
                        tone = CellTone.Blue,
                    ),
                trayPieces = listOf(
                    piece(id = -20_301, kind = PieceKind.TriL, tone = CellTone.Amber),
                    piece(id = -20_302, kind = PieceKind.Square, tone = CellTone.Violet),
                    piece(id = -20_303, kind = PieceKind.TriL, tone = CellTone.Gold),
                ),
            ),
            guidePoint = GridPoint(4, 5),
        )
    }

    private fun scriptedState(
        board: BoardMatrix,
        trayPieces: List<Piece>,
    ): GameState {
        val baseState = guideLogic.newGame(config = config, gameplayStyle = GameplayStyle.BlockWise)
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
