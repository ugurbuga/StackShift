package com.ugurbuga.stackshift.settings

import androidx.compose.runtime.Immutable
import com.ugurbuga.stackshift.game.logic.GameLogic
import com.ugurbuga.stackshift.game.model.BoardMatrix
import com.ugurbuga.stackshift.game.model.CellTone
import com.ugurbuga.stackshift.game.model.GameConfig
import com.ugurbuga.stackshift.game.model.GameState
import com.ugurbuga.stackshift.game.model.GameTextKey
import com.ugurbuga.stackshift.game.model.GridPoint
import com.ugurbuga.stackshift.game.model.Piece
import com.ugurbuga.stackshift.game.model.PieceKind
import com.ugurbuga.stackshift.game.model.SpecialBlockType
import com.ugurbuga.stackshift.game.model.gameText

@Immutable
enum class FirstRunOnboardingStage {
    DragAndLaunch,
    LineClear,
    ColumnClearer,
    RowClearer,
    Ghost,
    Heavy,
}

@Immutable
enum class FirstRunOnboardingTarget {
    Tray,
    Board,
}

@Immutable
data class FirstRunOnboardingScene(
    val stage: FirstRunOnboardingStage,
    val gameState: GameState,
    val target: FirstRunOnboardingTarget,
    val guideColumn: Int?,
    val acceptedColumns: Set<Int>,
)

object FirstRunGameOnboardingStateFactory {
    private val guideLogic = GameLogic()
    private val config = GameConfig(
        columns = 10,
        rows = 6,
        difficultyIntervalSeconds = 9_999,
        linesPerLevel = 9_999,
    )

    val stages: List<FirstRunOnboardingStage> = listOf(
        FirstRunOnboardingStage.DragAndLaunch,
        FirstRunOnboardingStage.LineClear,
        FirstRunOnboardingStage.RowClearer,
        FirstRunOnboardingStage.ColumnClearer,
        FirstRunOnboardingStage.Ghost,
        FirstRunOnboardingStage.Heavy,
    )

    private val sceneCache: Map<FirstRunOnboardingStage, FirstRunOnboardingScene> =
        stages.associateWith(::buildScene)

    fun initialState(): GameState = scene(stages.first()).gameState

    fun cleanGameState(): GameState = GameLogic().newGame()

    fun scene(stage: FirstRunOnboardingStage): FirstRunOnboardingScene = sceneCache.getValue(stage)

    private fun buildScene(stage: FirstRunOnboardingStage): FirstRunOnboardingScene = when (stage) {
        FirstRunOnboardingStage.DragAndLaunch -> FirstRunOnboardingScene(
            stage = stage,
            target = FirstRunOnboardingTarget.Tray,
            gameState = scriptedState(
                board = BoardMatrix.empty(columns = config.columns, rows = config.rows)
                    .fill(
                        points = raised(listOf(
                            GridPoint(1, 4),
                            GridPoint(2, 4),
                            GridPoint(3, 4),
                            GridPoint(6, 3),
                            GridPoint(7, 3),
                            GridPoint(8, 3),
                            GridPoint(4, 2),
                            GridPoint(5, 2),
                        )),
                        tone = CellTone.Blue,
                    )
                    .fill(
                        points = raised(listOf(
                            GridPoint(2, 3),
                            GridPoint(7, 2),
                        )),
                        tone = CellTone.Gold,
                    ),
                activePiece = piece(id = -10_001, kind = PieceKind.T, tone = CellTone.Cyan),
                nextQueue = listOf(
                    piece(id = -10_002, kind = PieceKind.Domino, tone = CellTone.Gold),
                    piece(id = -10_003, kind = PieceKind.Domino, tone = CellTone.Amber, special = SpecialBlockType.RowClearer),
                    piece(id = -10_004, kind = PieceKind.T, tone = CellTone.Emerald, special = SpecialBlockType.ColumnClearer),
                ),
                lastPlacementColumn = 0,
            ),
            guideColumn = null,
            acceptedColumns = emptySet(),
        ).withGuidance(
            preferredColumns = listOf(6, 5, 4, 3, 7, 2),
            acceptedColumns = { scene ->
                validColumns(scene.gameState)
                    .filterNot { it == scene.gameState.lastPlacementColumn }
                    .toSet()
            },
        )

        FirstRunOnboardingStage.LineClear -> FirstRunOnboardingScene(
            stage = stage,
            target = FirstRunOnboardingTarget.Board,
            gameState = scriptedState(
                board = BoardMatrix.empty(columns = config.columns, rows = config.rows)
                    .fill(
                        points = listOf(
                            GridPoint(0, 5),
                            GridPoint(1, 5),
                            GridPoint(2, 5),
                            GridPoint(3, 5),
                            GridPoint(4, 4),
                            GridPoint(5, 4),
                            GridPoint(6, 5),
                            GridPoint(7, 5),
                            GridPoint(8, 5),
                            GridPoint(9, 5),
                        ),
                        tone = CellTone.Blue,
                    )
                    .fill(
                        points = listOf(
                            GridPoint(1, 1),
                            GridPoint(8, 1),
                        ),
                        tone = CellTone.Gold,
                    ),
                activePiece = piece(
                    id = -10_101,
                    kind = PieceKind.Domino,
                    tone = CellTone.Gold,
                ),
                nextQueue = listOf(
                    piece(id = -10_102, kind = PieceKind.Domino, tone = CellTone.Amber, special = SpecialBlockType.RowClearer),
                    piece(id = -10_103, kind = PieceKind.T, tone = CellTone.Emerald, special = SpecialBlockType.ColumnClearer),
                    piece(id = -10_104, kind = PieceKind.L, tone = CellTone.Violet, special = SpecialBlockType.Ghost),
                ),
                lastPlacementColumn = 3,
            ),
            guideColumn = null,
            acceptedColumns = emptySet(),
        ).withGuidance(
            preferredColumns = listOf(4),
            lockGuideColumn = true,
            acceptedColumns = { setOf(4) },
        )

        FirstRunOnboardingStage.ColumnClearer -> FirstRunOnboardingScene(
            stage = stage,
            target = FirstRunOnboardingTarget.Board,
            gameState = scriptedState(
                board = BoardMatrix.empty(columns = config.columns, rows = config.rows)
                    .fill(
                        points = raised(listOf(
                            GridPoint(4, 2),
                            GridPoint(4, 3),
                            GridPoint(4, 4),
                            GridPoint(4, 5),
                            GridPoint(4, 6),
                            GridPoint(5, 4),
                            GridPoint(5, 5),
                            GridPoint(6, 6),
                        )),
                        tone = CellTone.Blue,
                    )
                    .fill(
                        points = raised(listOf(
                            GridPoint(5, 5),
                            GridPoint(6, 6),
                        )),
                        tone = CellTone.Gold,
                    ),
                activePiece = piece(
                    id = -11_001,
                    kind = PieceKind.T,
                    tone = CellTone.Emerald,
                    special = SpecialBlockType.ColumnClearer,
                ),
                nextQueue = listOf(
                    piece(id = -11_002, kind = PieceKind.L, tone = CellTone.Violet, special = SpecialBlockType.Ghost),
                    piece(id = -11_003, kind = PieceKind.T, tone = CellTone.Coral, special = SpecialBlockType.Heavy),
                    piece(id = -11_004, kind = PieceKind.Square, tone = CellTone.Blue),
                ),
                lastPlacementColumn = 3,
            ),
            guideColumn = null,
            acceptedColumns = emptySet(),
        ).withGuidance(preferredColumns = listOf(4, 3, 5), lockGuideColumn = true)

        FirstRunOnboardingStage.RowClearer -> FirstRunOnboardingScene(
            stage = stage,
            target = FirstRunOnboardingTarget.Board,
            gameState = scriptedState(
                board = BoardMatrix.empty(columns = config.columns, rows = config.rows)
                    .fill(
                        points = listOf(
                            GridPoint(0, 5),
                            GridPoint(3, 5),
                            GridPoint(4, 4),
                            GridPoint(5, 4),
                            GridPoint(6, 5),
                            GridPoint(9, 5),
                        ),
                        tone = CellTone.Emerald,
                    ),
                activePiece = piece(
                    id = -12_001,
                    kind = PieceKind.Domino,
                    tone = CellTone.Amber,
                    special = SpecialBlockType.RowClearer,
                ),
                nextQueue = listOf(
                    piece(id = -12_002, kind = PieceKind.T, tone = CellTone.Emerald, special = SpecialBlockType.ColumnClearer),
                    piece(id = -12_003, kind = PieceKind.L, tone = CellTone.Violet, special = SpecialBlockType.Ghost),
                    piece(id = -12_004, kind = PieceKind.T, tone = CellTone.Coral, special = SpecialBlockType.Heavy),
                ),
                lastPlacementColumn = 3,
            ),
            guideColumn = null,
            acceptedColumns = emptySet(),
        ).withGuidance(
            preferredColumns = listOf(4),
            lockGuideColumn = true,
            acceptedColumns = { setOf(4) },
        )

        FirstRunOnboardingStage.Ghost -> FirstRunOnboardingScene(
            stage = stage,
            target = FirstRunOnboardingTarget.Board,
            gameState = scriptedState(
                board = BoardMatrix.empty(columns = config.columns, rows = config.rows)
                    .fill(
                        points = raised(listOf(
                            GridPoint(2, 2),
                            GridPoint(3, 2),
                            GridPoint(5, 2),
                            GridPoint(6, 2),
                            GridPoint(2, 3),
                            GridPoint(6, 3),
                            GridPoint(2, 4),
                            GridPoint(6, 4),
                            GridPoint(3, 5),
                            GridPoint(4, 5),
                            GridPoint(5, 5),
                        )),
                        tone = CellTone.Violet,
                    ),
                activePiece = piece(
                    id = -13_001,
                    kind = PieceKind.L,
                    tone = CellTone.Violet,
                    special = SpecialBlockType.Ghost,
                ),
                nextQueue = listOf(
                    piece(id = -13_002, kind = PieceKind.T, tone = CellTone.Coral, special = SpecialBlockType.Heavy),
                    piece(id = -13_003, kind = PieceKind.Domino, tone = CellTone.Gold),
                    piece(id = -13_004, kind = PieceKind.Square, tone = CellTone.Emerald),
                ),
                lastPlacementColumn = 3,
            ),
            guideColumn = null,
            acceptedColumns = emptySet(),
        ).withGuidance(preferredColumns = listOf(4, 3, 5), lockGuideColumn = true)

        FirstRunOnboardingStage.Heavy -> FirstRunOnboardingScene(
            stage = stage,
            target = FirstRunOnboardingTarget.Board,
            gameState = scriptedState(
                board = BoardMatrix.empty(columns = config.columns, rows = config.rows)
                    .fill(
                        points = raised(listOf(
                            GridPoint(3, 3),
                            GridPoint(3, 4),
                            GridPoint(3, 5),
                            GridPoint(4, 3),
                            GridPoint(4, 5),
                            GridPoint(5, 3),
                            GridPoint(5, 4),
                            GridPoint(5, 5),
                            GridPoint(7, 4),
                            GridPoint(7, 5),
                        )),
                        tone = CellTone.Coral,
                    )
                    .fill(
                        points = raised(listOf(
                            GridPoint(6, 2),
                            GridPoint(7, 2),
                        )),
                        tone = CellTone.Gold,
                    ),
                activePiece = piece(
                    id = -14_001,
                    kind = PieceKind.T,
                    tone = CellTone.Coral,
                    special = SpecialBlockType.Heavy,
                ),
                nextQueue = listOf(
                    piece(id = -14_002, kind = PieceKind.Square, tone = CellTone.Blue),
                    piece(id = -14_003, kind = PieceKind.Domino, tone = CellTone.Gold),
                    piece(id = -14_004, kind = PieceKind.Z, tone = CellTone.Rose),
                    piece(id = -14_005, kind = PieceKind.L, tone = CellTone.Emerald),
                ),
                lastPlacementColumn = 4,
            ),
            guideColumn = null,
            acceptedColumns = emptySet(),
        ).withGuidance(preferredColumns = listOf(6, 5, 4), lockGuideColumn = true)
    }

    private fun FirstRunOnboardingScene.withGuidance(
        preferredColumns: List<Int>,
        lockGuideColumn: Boolean = false,
        acceptedColumns: ((FirstRunOnboardingScene) -> Set<Int>)? = null,
    ): FirstRunOnboardingScene {
        val validColumns = validColumns(gameState).toSet()
        val candidateAcceptedColumns = acceptedColumns?.invoke(this)?.intersect(validColumns).orEmpty()
        val resolvedAcceptedColumns = when {
            candidateAcceptedColumns.isNotEmpty() -> candidateAcceptedColumns
            validColumns.isNotEmpty() -> validColumns
            else -> emptySet()
        }
        val resolvedGuideColumn = preferredColumns.firstOrNull { it in resolvedAcceptedColumns }
            ?: preferredColumns.firstOrNull { it in validColumns }
            ?: resolvedAcceptedColumns.firstOrNull()
            ?: validColumns.firstOrNull()
        return copy(
            guideColumn = resolvedGuideColumn,
            acceptedColumns = if (lockGuideColumn) {
                resolvedGuideColumn?.let(::setOf).orEmpty()
            } else {
                resolvedAcceptedColumns
            },
        )
    }

    private fun validColumns(state: GameState): List<Int> {
        val piece = state.activePiece ?: return emptyList()
        val maxColumn = state.config.columns - piece.width
        if (maxColumn < 0) return emptyList()
        return (0..maxColumn).filter { column ->
            guideLogic.previewPlacement(state, column) != null
        }
    }

    private fun raised(
        points: List<GridPoint>,
        rows: Int = 1,
    ): List<GridPoint> = points.map { point ->
        point.copy(row = (point.row - rows).coerceAtLeast(0))
    }

    private fun scriptedState(
        board: BoardMatrix,
        activePiece: Piece,
        nextQueue: List<Piece>,
        lastPlacementColumn: Int,
    ): GameState {
        val baseState = GameLogic().newGame(config)
        return baseState.copy(
            board = board,
            activePiece = activePiece,
            nextQueue = nextQueue,
            holdPiece = null,
            canHold = false,
            lastPlacementColumn = lastPlacementColumn,
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

