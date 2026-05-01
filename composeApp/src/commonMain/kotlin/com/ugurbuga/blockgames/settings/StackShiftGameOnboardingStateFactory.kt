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
enum class StackShiftOnboardingStage : OnboardingStage {
    DragAndLaunch,
    LineClear,
    ColumnClearer,
    RowClearer,
    Ghost,
    Heavy,
}

@Immutable
enum class StackShiftOnboardingTarget {
    Tray,
    Board,
}

@Immutable
data class StackShiftOnboardingScene(
    val stage: StackShiftOnboardingStage,
    val gameState: GameState,
    val target: StackShiftOnboardingTarget,
    val guideColumn: Int?,
    val acceptedColumns: Set<Int>,
)

object StackShiftGameOnboardingStateFactory {
    private val guideLogic = GameLogic.create()
    private val config = GameConfig(
        columns = 10,
        rows = 12,
        difficultyIntervalSeconds = 9_999,
        linesPerLevel = 9_999,
    )

    val stages: List<StackShiftOnboardingStage> = listOf(
        StackShiftOnboardingStage.DragAndLaunch,
        StackShiftOnboardingStage.LineClear,
        StackShiftOnboardingStage.RowClearer,
        StackShiftOnboardingStage.ColumnClearer,
        StackShiftOnboardingStage.Ghost,
        StackShiftOnboardingStage.Heavy,
    )

    private val sceneCache: Map<StackShiftOnboardingStage, StackShiftOnboardingScene> =
        stages.associateWith(::buildScene)

    fun initialState(gameplayStyle: GameplayStyle = GameplayStyle.StackShift): GameState =
        scene(stages.first()).gameState.copy(gameplayStyle = gameplayStyle)

    fun cleanGameState(gameplayStyle: GameplayStyle): GameState =
        guideLogic.newGame(config, gameplayStyle = gameplayStyle)

    fun scene(stage: StackShiftOnboardingStage): StackShiftOnboardingScene = sceneCache.getValue(stage)

    private fun buildScene(stage: StackShiftOnboardingStage): StackShiftOnboardingScene = when (stage) {
        StackShiftOnboardingStage.DragAndLaunch -> StackShiftOnboardingScene(
            stage = stage,
            target = StackShiftOnboardingTarget.Tray,
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

        StackShiftOnboardingStage.LineClear -> StackShiftOnboardingScene(
            stage = stage,
            target = StackShiftOnboardingTarget.Board,
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

        StackShiftOnboardingStage.ColumnClearer -> StackShiftOnboardingScene(
            stage = stage,
            target = StackShiftOnboardingTarget.Board,
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

        StackShiftOnboardingStage.RowClearer -> StackShiftOnboardingScene(
            stage = stage,
            target = StackShiftOnboardingTarget.Board,
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

        StackShiftOnboardingStage.Ghost -> StackShiftOnboardingScene(
            stage = stage,
            target = StackShiftOnboardingTarget.Board,
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

        StackShiftOnboardingStage.Heavy -> StackShiftOnboardingScene(
            stage = stage,
            target = StackShiftOnboardingTarget.Board,
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
        ).withGuidance(preferredColumns = listOf(5, 4, 6), lockGuideColumn = true)
    }

    private fun StackShiftOnboardingScene.withGuidance(
        preferredColumns: List<Int>,
        lockGuideColumn: Boolean = false,
        acceptedColumns: ((StackShiftOnboardingScene) -> Set<Int>)? = null,
    ): StackShiftOnboardingScene {
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
        val baseState = guideLogic.newGame(config = config, gameplayStyle = GameplayStyle.StackShift)
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

