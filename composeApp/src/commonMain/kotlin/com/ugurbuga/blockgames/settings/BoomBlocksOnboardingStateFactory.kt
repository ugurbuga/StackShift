package com.ugurbuga.blockgames.settings

import androidx.compose.runtime.Immutable
import com.ugurbuga.blockgames.game.logic.BoomBlocksGameLogic
import com.ugurbuga.blockgames.game.model.BoardMatrix
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.game.model.GameStatus
import com.ugurbuga.blockgames.game.model.GameTextKey
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.GridPoint
import com.ugurbuga.blockgames.game.model.gameText

@Immutable
data class BoomBlocksOnboardingScene(
    val stage: BoomBlocksOnboardingStage,
    val gameState: GameState,
    val guidePoint: GridPoint?,
)

@Immutable
enum class BoomBlocksOnboardingStage : OnboardingStage {
    BasicExplosion,
    LargeExplosion,
    GravityShift,
    StrategicClears,
}

object BoomBlocksOnboardingStateFactory {
    private val guideLogic = BoomBlocksGameLogic()
    private val config = GameConfig(
        columns = 8,
        rows = 10,
        difficultyIntervalSeconds = 9_999,
        linesPerLevel = 9_999,
    )

    val stages: List<BoomBlocksOnboardingStage> = listOf(
        BoomBlocksOnboardingStage.BasicExplosion,
        BoomBlocksOnboardingStage.LargeExplosion,
        BoomBlocksOnboardingStage.GravityShift,
        BoomBlocksOnboardingStage.StrategicClears,
    )

    private val sceneCache: Map<BoomBlocksOnboardingStage, BoomBlocksOnboardingScene> =
        stages.associateWith(::buildScene)

    fun initialState(): GameState =
        scene(stages.first()).gameState

    fun cleanGameState(): GameState =
        guideLogic.newGame(config = config)

    fun scene(stage: BoomBlocksOnboardingStage): BoomBlocksOnboardingScene = sceneCache.getValue(stage)

    private fun buildScene(stage: BoomBlocksOnboardingStage): BoomBlocksOnboardingScene = when (stage) {
        BoomBlocksOnboardingStage.BasicExplosion -> {
            val target = setOf(GridPoint(3, 4), GridPoint(4, 4), GridPoint(3, 5))
            BoomBlocksOnboardingScene(
                stage = stage,
                gameState = scriptedState(
                    board = BoardMatrix.empty(columns = config.columns, rows = config.rows)
                        .fillAllNonExplodable(target)
                        .fill(GridPoint(3, 4), CellTone.Cyan)
                        .fill(GridPoint(4, 4), CellTone.Cyan)
                        .fill(GridPoint(3, 5), CellTone.Cyan)
                ),
                guidePoint = GridPoint(3, 4),
            )
        }

        BoomBlocksOnboardingStage.LargeExplosion -> {
            val target = setOf(
                GridPoint(2, 3), GridPoint(3, 3), GridPoint(4, 3),
                GridPoint(2, 4), GridPoint(3, 4), GridPoint(4, 4),
                GridPoint(3, 5)
            )
            BoomBlocksOnboardingScene(
                stage = stage,
                gameState = scriptedState(
                    board = BoardMatrix.empty(columns = config.columns, rows = config.rows)
                        .fillAllNonExplodable(target)
                        .fill(GridPoint(2, 3), CellTone.Gold)
                        .fill(GridPoint(3, 3), CellTone.Gold)
                        .fill(GridPoint(4, 3), CellTone.Gold)
                        .fill(GridPoint(2, 4), CellTone.Gold)
                        .fill(GridPoint(3, 4), CellTone.Gold)
                        .fill(GridPoint(4, 4), CellTone.Gold)
                        .fill(GridPoint(3, 5), CellTone.Gold)
                ),
                guidePoint = GridPoint(3, 4),
            )
        }

        BoomBlocksOnboardingStage.GravityShift -> {
            val target = setOf(GridPoint(0, 0), GridPoint(1, 0), GridPoint(0, 1))
            BoomBlocksOnboardingScene(
                stage = stage,
                gameState = scriptedState(
                    board = BoardMatrix.empty(columns = config.columns, rows = config.rows)
                        .fillAllNonExplodable(target)
                        .fill(GridPoint(0, 0), CellTone.Coral)
                        .fill(GridPoint(1, 0), CellTone.Coral)
                        .fill(GridPoint(0, 1), CellTone.Coral)
                ),
                guidePoint = GridPoint(0, 0),
            )
        }

        BoomBlocksOnboardingStage.StrategicClears -> {
            val target = setOf(GridPoint(3, 4), GridPoint(4, 4), GridPoint(3, 5))
            BoomBlocksOnboardingScene(
                stage = stage,
                gameState = scriptedState(
                    board = BoardMatrix.empty(columns = config.columns, rows = config.rows)
                        .fillAllNonExplodable(target)
                        .fill(GridPoint(3, 4), CellTone.Violet)
                        .fill(GridPoint(4, 4), CellTone.Violet)
                        .fill(GridPoint(3, 5), CellTone.Violet)
                        .fill(GridPoint(3, 3), CellTone.Emerald)
                        .fill(GridPoint(4, 3), CellTone.Emerald)
                        .fill(GridPoint(4, 5), CellTone.Emerald)
                ),
                guidePoint = GridPoint(3, 4),
            )
        }
    }

    private fun scriptedState(
        board: BoardMatrix,
    ): GameState {
        return GameState(
            config = config,
            gameplayStyle = GameplayStyle.BoomBlocks,
            board = board,
            activePiece = null,
            nextQueue = emptyList(),
            score = 0,
            linesCleared = 0,
            level = 1,
            difficultyStage = 0,
            secondsUntilDifficultyIncrease = config.difficultyIntervalSeconds,
            status = GameStatus.Running,
            message = gameText(GameTextKey.GameMessageSelectColumn),
        )
    }

    private fun BoardMatrix.fillAllNonExplodable(exclude: Set<GridPoint>): BoardMatrix {
        var result = this
        var id = 1000
        val tones = listOf(
            CellTone.Cyan,
            CellTone.Gold,
            CellTone.Violet,
            CellTone.Emerald,
            CellTone.Coral,
        )
        for (c in 0 until columns) {
            for (r in 0 until rows) {
                if (GridPoint(c, r) in exclude) continue

                val tone = tones[(c * 2 + r) % tones.size]
                result = result.fill(listOf(GridPoint(c, r)), tone, value = id++)
            }
        }
        return result
    }

    private fun BoardMatrix.fill(point: GridPoint, tone: CellTone): BoardMatrix {
        return this.fill(listOf(point), tone, value = (point.column * 100 + point.row + 5000))
    }
}
