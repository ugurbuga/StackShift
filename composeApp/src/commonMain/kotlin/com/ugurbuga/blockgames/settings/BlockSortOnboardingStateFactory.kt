package com.ugurbuga.blockgames.settings

import androidx.compose.runtime.Immutable
import com.ugurbuga.blockgames.game.logic.BlockSortGameLogic
import com.ugurbuga.blockgames.game.model.BoardMatrix
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.game.model.GameStatus
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.GridPoint

@Immutable
enum class BlockSortOnboardingStage : OnboardingStage {
	PickSource,
	MatchColor,
	FinishColumn,
}

@Immutable
data class BlockSortOnboardingScene(
	val stage: BlockSortOnboardingStage,
	val gameState: GameState,
	val guideSourceColumn: Int,
	val acceptedTargetColumns: Set<Int>,
)

object BlockSortOnboardingStateFactory {
	private val logic = BlockSortGameLogic()
	private val config = GameConfig(
		columns = 5,
		rows = 4,
		difficultyIntervalSeconds = 9_999,
		linesPerLevel = 9_999,
	)

	val stages: List<BlockSortOnboardingStage> = listOf(
		BlockSortOnboardingStage.PickSource,
		BlockSortOnboardingStage.MatchColor,
		BlockSortOnboardingStage.FinishColumn,
	)

	private val sceneCache: Map<BlockSortOnboardingStage, BlockSortOnboardingScene> =
		stages.associateWith(::buildScene)

	fun initialState(): GameState = scene(stages.first()).gameState

	fun cleanGameState(): GameState = logic.newGame(config = GameConfig.default(GameplayStyle.BlockSort))

	fun scene(stage: BlockSortOnboardingStage): BlockSortOnboardingScene = sceneCache.getValue(stage)

	private fun buildScene(stage: BlockSortOnboardingStage): BlockSortOnboardingScene = when (stage) {
		BlockSortOnboardingStage.PickSource -> BlockSortOnboardingScene(
			stage = stage,
			gameState = scriptedState(
				columns = listOf(
					listOf(CellTone.Gold, CellTone.Cyan),
					listOf(CellTone.Violet, CellTone.Gold),
					listOf(CellTone.Cyan, CellTone.Violet),
					emptyList(),
					listOf(CellTone.Gold, CellTone.Violet),
				),
			),
			guideSourceColumn = 0,
			acceptedTargetColumns = setOf(3),
		)

		BlockSortOnboardingStage.MatchColor -> BlockSortOnboardingScene(
			stage = stage,
			gameState = scriptedState(
				columns = listOf(
					listOf(CellTone.Gold, CellTone.Cyan),
					listOf(CellTone.Violet, CellTone.Gold),
					listOf(CellTone.Gold, CellTone.Violet),
					listOf(CellTone.Violet, CellTone.Cyan),
					emptyList(),
				),
			),
			guideSourceColumn = 0,
			acceptedTargetColumns = setOf(3),
		)

		BlockSortOnboardingStage.FinishColumn -> BlockSortOnboardingScene(
			stage = stage,
			gameState = scriptedState(
				columns = listOf(
					listOf(CellTone.Gold, CellTone.Cyan, CellTone.Cyan),
					listOf(CellTone.Violet, CellTone.Gold),
					emptyList(),
					listOf(CellTone.Cyan, CellTone.Cyan),
					listOf(CellTone.Gold, CellTone.Violet),
				),
			),
			guideSourceColumn = 0,
			acceptedTargetColumns = setOf(3),
		)
	}

	private fun scriptedState(columns: List<List<CellTone>>): GameState {
		return GameState(
			config = config,
			gameplayStyle = GameplayStyle.BlockSort,
			board = columns.toBoardMatrix(config.columns, config.rows),
			activePiece = null,
			nextQueue = emptyList(),
			score = 0,
			linesCleared = 0,
			level = 1,
			difficultyStage = 0,
			secondsUntilDifficultyIncrease = config.difficultyIntervalSeconds,
			status = GameStatus.Running,
		)
	}
}

private fun List<List<CellTone>>.toBoardMatrix(columns: Int, rows: Int): BoardMatrix {
	var board = BoardMatrix.empty(columns = columns, rows = rows)
	forEachIndexed { column, stack ->
		stack.forEachIndexed { index, tone ->
			board = board.fill(
				points = listOf(GridPoint(column, rows - 1 - index)),
				tone = tone,
				value = (column * 100) + index + 1,
			)
		}
	}
	return board
}

