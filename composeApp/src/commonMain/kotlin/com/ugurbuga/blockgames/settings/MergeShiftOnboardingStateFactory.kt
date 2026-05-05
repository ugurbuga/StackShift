package com.ugurbuga.blockgames.settings

import androidx.compose.runtime.Immutable
import com.ugurbuga.blockgames.game.model.BoardMatrix
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.ComboState
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.game.model.GameStatus
import com.ugurbuga.blockgames.game.model.GameTextKey
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.GridPoint
import com.ugurbuga.blockgames.game.model.LaunchBarState
import com.ugurbuga.blockgames.game.model.Piece
import com.ugurbuga.blockgames.game.model.PieceKind
import com.ugurbuga.blockgames.game.model.gameText
import kotlin.math.log2

@Immutable
enum class MergeShiftOnboardingStage : OnboardingStage {
    Launch,
    VerticalMerge,
    HorizontalMerge,
    MultiMerge,
}

@Immutable
data class MergeShiftOnboardingScene(
    val stage: MergeShiftOnboardingStage,
    val gameState: GameState,
    val guideColumn: Int?,
    val acceptedColumns: Set<Int>,
)

object MergeShiftOnboardingStateFactory {
    private val config = GameConfig(
        columns = 3,
        rows = 5,
        difficultyIntervalSeconds = 9_999,
        linesPerLevel = 9_999,
    )

    val stages: List<MergeShiftOnboardingStage> = listOf(
        MergeShiftOnboardingStage.Launch,
        MergeShiftOnboardingStage.VerticalMerge,
        MergeShiftOnboardingStage.HorizontalMerge,
        MergeShiftOnboardingStage.MultiMerge,
    )

    private val sceneCache: Map<MergeShiftOnboardingStage, MergeShiftOnboardingScene> =
        stages.associateWith(::buildScene)

    fun initialState(): GameState = scene(stages.first()).gameState

    fun scene(stage: MergeShiftOnboardingStage): MergeShiftOnboardingScene = sceneCache.getValue(stage)

    fun cleanGameState(): GameState = scriptedState(
        board = BoardMatrix.empty(columns = config.columns, rows = config.rows),
        activePiece = piece(id = 1, value = 2),
        nextQueue = listOf(
            piece(id = 2, value = 4),
            piece(id = 3, value = 8),
        ),
    ).copy(status = GameStatus.Running)

    private fun buildScene(stage: MergeShiftOnboardingStage): MergeShiftOnboardingScene = when (stage) {
        MergeShiftOnboardingStage.Launch -> MergeShiftOnboardingScene(
            stage = stage,
            gameState = scriptedState(
                board = BoardMatrix.empty(columns = config.columns, rows = config.rows),
                activePiece = piece(id = -10_001, value = 2),
                nextQueue = listOf(
                    piece(id = -10_002, value = 4),
                    piece(id = -10_003, value = 8),
                ),
            ),
            guideColumn = 1,
            acceptedColumns = setOf(1),
        )

        MergeShiftOnboardingStage.VerticalMerge -> MergeShiftOnboardingScene(
            stage = stage,
            gameState = scriptedState(
                board = BoardMatrix.empty(columns = config.columns, rows = config.rows)
                    .fill(listOf(GridPoint(1, 0)), tone = getToneForValue(2), value = 2),
                activePiece = piece(id = -10_101, value = 2),
                nextQueue = listOf(
                    piece(id = -10_102, value = 4),
                    piece(id = -10_103, value = 8),
                ),
            ),
            guideColumn = 1,
            acceptedColumns = setOf(1),
        )

        MergeShiftOnboardingStage.HorizontalMerge -> MergeShiftOnboardingScene(
            stage = stage,
            gameState = scriptedState(
                board = BoardMatrix.empty(columns = config.columns, rows = config.rows)
                    .fill(listOf(GridPoint(0, 0)), tone = getToneForValue(4), value = 4),
                activePiece = piece(id = -10_201, value = 4),
                nextQueue = listOf(
                    piece(id = -10_202, value = 8),
                    piece(id = -10_203, value = 16),
                ),
            ),
            guideColumn = 1,
            acceptedColumns = setOf(1),
        )

        MergeShiftOnboardingStage.MultiMerge -> MergeShiftOnboardingScene(
            stage = stage,
            gameState = scriptedState(
                board = BoardMatrix.empty(columns = config.columns, rows = config.rows)
                    .fill(listOf(GridPoint(0, 0)), tone = getToneForValue(128), value = 128)
                    .fill(listOf(GridPoint(1, 0)), tone = getToneForValue(8), value = 8)
                    .fill(listOf(GridPoint(2, 0)), tone = getToneForValue(32), value = 32)
                    .fill(listOf(GridPoint(0, 1)), tone = getToneForValue(8), value = 8)
                    .fill(listOf(GridPoint(2, 1)), tone = getToneForValue(8), value = 8),
                activePiece = piece(id = -10_301, value = 8),
                nextQueue = listOf(
                    piece(id = -10_302, value = 16),
                    piece(id = -10_303, value = 32),
                ),
            ),
            guideColumn = 1,
            acceptedColumns = setOf(1),
        )
    }

    private fun scriptedState(
        board: BoardMatrix,
        activePiece: Piece,
        nextQueue: List<Piece>,
    ): GameState {
        return GameState(
            config = config,
            gameplayStyle = GameplayStyle.MergeShift,
            board = board,
            activePiece = activePiece,
            nextQueue = nextQueue,
            score = 0,
            linesCleared = 0,
            level = 1,
            difficultyStage = 0,
            secondsUntilDifficultyIncrease = config.difficultyIntervalSeconds,
            combo = ComboState(),
            launchBar = LaunchBarState(),
            status = GameStatus.Running,
            message = gameText(GameTextKey.GameMessageSelectColumn),
        )
    }

    private fun piece(id: Long, value: Int): Piece {
        return Piece(
            id = id,
            kind = PieceKind.Single,
            tone = getToneForValue(value),
            cells = listOf(GridPoint(0, 0)),
            width = 1,
            height = 1,
            value = value,
        )
    }

    private fun getToneForValue(value: Int): CellTone {
        val power = (log2(value.toDouble())).toInt()
        return CellTone.entries[power % CellTone.entries.size]
    }
}
