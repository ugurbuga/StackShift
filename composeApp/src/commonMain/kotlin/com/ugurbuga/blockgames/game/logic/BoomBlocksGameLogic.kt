package com.ugurbuga.blockgames.game.logic

import com.ugurbuga.blockgames.game.model.BoardMatrix
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.DailyChallenge
import com.ugurbuga.blockgames.game.model.FeedbackEmphasis
import com.ugurbuga.blockgames.game.model.FloatingFeedback
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameMode
import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.game.model.GameStatus
import com.ugurbuga.blockgames.game.model.GameTextKey
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.GridPoint
import com.ugurbuga.blockgames.game.model.PlacementPreview
import com.ugurbuga.blockgames.game.model.SpecialBlockType
import com.ugurbuga.blockgames.game.model.gameText
import com.ugurbuga.blockgames.platform.currentEpochMillis
import com.ugurbuga.blockgames.settings.GameSessionCodec
import kotlin.random.Random

internal class BoomBlocksGameLogic(
    private val random: Random = Random.Default,
    private val scoreCalculator: ScoreCalculator = ScoreCalculator(),
) : GameLogic {

    companion object {
        private const val MIN_EXPLODE_SIZE = 3
        private val BOOM_BLOCKS_TONES = listOf(
            CellTone.Cyan,
            CellTone.Gold,
            CellTone.Violet,
            CellTone.Emerald,
            CellTone.Coral,
        )
    }

    override fun restoreGame(state: GameState): GameState {
        return state.copy(nextPieceId = maxOf(state.nextPieceId, GameSessionCodec.maxPieceId(state) + 1L))
    }

    override fun newGame(
        config: GameConfig,
        challenge: DailyChallenge?,
        mode: GameMode,
    ): GameState {
        val columns = config.columns
        val rows = config.rows
        var board = BoardMatrix.empty(columns, rows)
        var nextId = 1L
        
        for (row in 0 until rows) {
            for (col in 0 until columns) {
                board = board.fill(
                    points = listOf(GridPoint(col, row)),
                    tone = BOOM_BLOCKS_TONES.random(random),
                    value = nextId.toInt()
                )
                nextId++
            }
        }

        return GameState(
            config = config,
            gameMode = mode,
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
            lastActionTime = currentEpochMillis(),
            nextPieceId = nextId,
            activeChallenge = challenge?.copy(tasks = challenge.tasks.map { it.copy(current = 0) }),
        ).let { 
            if (hasAnyExplodableGroup(it.board)) it else it.copy(status = GameStatus.GameOver)
        }
    }

    override fun previewPlacement(state: GameState, column: Int): PlacementPreview? = null
    
    override fun previewPlacement(state: GameState, pieceId: Long, origin: GridPoint): PlacementPreview? {
        if (state.status != GameStatus.Running) return null
        val group = findConnectedGroup(state.board, origin)
        if (group.size < MIN_EXPLODE_SIZE) return null
        
        return PlacementPreview(
            selectedColumn = origin.column,
            entryAnchor = origin,
            landingAnchor = origin,
            occupiedCells = group.toList(),
            coveredColumns = group.minOf { it.column }..group.maxOf { it.column }
        )
    }

    override fun previewImpactPoints(state: GameState, preview: PlacementPreview?): Set<GridPoint> {
        return preview?.occupiedCells?.toSet() ?: emptySet()
    }

    override fun placePiece(state: GameState, column: Int): GameMoveResult = invalidMove(state)

    override fun placePiece(state: GameState, pieceId: Long, origin: GridPoint): GameMoveResult {
        if (state.status != GameStatus.Running) return invalidMove(state)
        
        val group = findConnectedGroup(state.board, origin)
        if (group.size < MIN_EXPLODE_SIZE) return invalidMove(state)
        
        // 1. Clear the exploded group
        val boardAfterClear = state.board.clearPoints(group)
        
        // 2. Determine gravity direction based on explosion center
        // We want new blocks to come from the opposite side of the explosion
        val avgRow = group.map { it.row }.average().toFloat()
        val avgCol = group.map { it.column }.average().toFloat()
        
        val rows = state.board.rows.toFloat()
        val cols = state.board.columns.toFloat()
        
        // Normalize distances based on board dimensions to ensure fairness
        val relDistToTop = avgRow / rows
        val relDistToBottom = (rows - 1 - avgRow) / rows
        val relDistToLeft = avgCol / cols
        val relDistToRight = (cols - 1 - avgCol) / cols
        
        val minDist = minOf(relDistToTop, relDistToBottom, relDistToLeft, relDistToRight)
        
        val boardAfterGravity = when {
            minDist == relDistToTop -> boardAfterClear.applyGravityUp() // Closest to top -> Pushed UP (comes from bottom)
            minDist == relDistToBottom -> boardAfterClear.applyGravityDown() // Closest to bottom -> Pushed DOWN (comes from top)
            minDist == relDistToLeft -> boardAfterClear.applyGravityLeft() // Closest to left -> Pushed LEFT (comes from right)
            else -> boardAfterClear.applyGravityRight() // Closest to right -> Pushed RIGHT (comes from left)
        }
        
        // 3. Refill the empty spaces
        var nextId = state.nextPieceId
        if (nextId < 1) nextId = 1 // Ensure positive IDs
        
        var finalBoard = boardAfterGravity
        for (col in 0 until finalBoard.columns) {
            for (row in 0 until finalBoard.rows) {
                if (!finalBoard.isOccupied(col, row)) {
                    finalBoard = finalBoard.fill(
                        points = listOf(GridPoint(col, row)),
                        tone = BOOM_BLOCKS_TONES.random(random),
                        value = nextId.toInt()
                    )
                    nextId++
                }
            }
        }
        
        val scoreGain = scoreCalculator.calculateScore(
            ScoreCalculator.ScoreParams(
                tilesPlaced = 0,
                linesCleared = 0,
                currentStreak = 0,
                specialBlocksTriggered = emptyList(),
                areaTilesCleared = group.size,
                isBoardCleared = finalBoard.isEmpty(),
                isPerfectPlacement = false,
                isHardPlacement = false,
                moveDurationMillis = null,
                boardFillRatio = 1f,
                chainReactionCount = 0
            )
        )
        
        val nextScore = state.score + scoreGain
        val hasMoves = hasAnyExplodableGroup(finalBoard)
        val nextStatus = if (hasMoves) GameStatus.Running else GameStatus.GameOver
        
        val explodedTones = group.associateWith { point ->
            state.board.toneAt(point.column, point.row) ?: CellTone.Cyan
        }
        
        return GameMoveResult(
            state = state.copy(
                board = finalBoard,
                score = nextScore,
                lastMoveScore = scoreGain,
                status = nextStatus,
                lastActionTime = currentEpochMillis(),
                nextPieceId = nextId,
                recentlyExplodedPoints = group,
                recentlyExplodedTones = explodedTones,
                clearAnimationToken = state.clearAnimationToken + 1,
                impactFlashToken = state.impactFlashToken + 1,
                feedbackToken = state.feedbackToken + 1,
                floatingFeedback = FloatingFeedback(
                    text = gameText(GameTextKey.FeedbackClear, scoreGain),
                    emphasis = FeedbackEmphasis.Bonus,
                    token = state.feedbackToken + 1
                )
            ),
            events = setOf(GameEvent.PlacementAccepted)
        )
    }


    private fun findConnectedGroup(board: BoardMatrix, start: GridPoint): Set<GridPoint> {
        val tone = board.toneAt(start.column, start.row) ?: return emptySet()
        val group = mutableSetOf<GridPoint>()
        val queue = mutableListOf(start)
        
        while (queue.isNotEmpty()) {
            val point = queue.removeAt(0)
            if (point in group) continue
            if (board.toneAt(point.column, point.row) == tone) {
                group.add(point)
                val neighbors = listOf(
                    GridPoint(point.column + 1, point.row),
                    GridPoint(point.column - 1, point.row),
                    GridPoint(point.column, point.row + 1),
                    GridPoint(point.column, point.row - 1)
                )
                for (neighbor in neighbors) {
                    if (neighbor.column in 0 until board.columns && 
                        neighbor.row in 0 until board.rows &&
                        neighbor !in group) {
                        queue.add(neighbor)
                    }
                }
            }
        }
        return group
    }

    private fun hasAnyExplodableGroup(board: BoardMatrix): Boolean {
        val visited = mutableSetOf<GridPoint>()
        for (row in 0 until board.rows) {
            for (col in 0 until board.columns) {
                val point = GridPoint(col, row)
                if (point !in visited) {
                    val group = findConnectedGroup(board, point)
                    if (group.size >= MIN_EXPLODE_SIZE) return true
                    visited.addAll(group)
                }
            }
        }
        return false
    }

    override fun holdPiece(state: GameState): GameMoveResult = invalidMove(state)
    override fun replaceActivePiece(state: GameState, specialType: SpecialBlockType): GameMoveResult = invalidMove(state)
    override fun commitSoftLock(state: GameState): GameMoveResult = invalidMove(state)
    override fun reviveFromReward(state: GameState): GameMoveResult = invalidMove(state)
    
    override fun tick(state: GameState): GameState {
        return state
    }

    private fun invalidMove(state: GameState): GameMoveResult = GameMoveResult(
        state = state,
        events = setOf(GameEvent.InvalidDrop),
    )
}
