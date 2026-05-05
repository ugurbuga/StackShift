package com.ugurbuga.blockgames.game.logic

import com.ugurbuga.blockgames.game.model.BoardMatrix
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.ColumnPressure
import com.ugurbuga.blockgames.game.model.ComboState
import com.ugurbuga.blockgames.game.model.DailyChallenge
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameMode
import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.game.model.GameStatus
import com.ugurbuga.blockgames.game.model.GameTextKey
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.GridPoint
import com.ugurbuga.blockgames.game.model.LaunchBarState
import com.ugurbuga.blockgames.game.model.Piece
import com.ugurbuga.blockgames.game.model.PieceKind
import com.ugurbuga.blockgames.game.model.PlacementPreview
import com.ugurbuga.blockgames.game.model.PressureLevel
import com.ugurbuga.blockgames.game.model.SpecialBlockType
import com.ugurbuga.blockgames.game.model.gameText
import com.ugurbuga.blockgames.settings.GameSessionCodec
import kotlin.math.log2
import kotlin.random.Random

internal class MergeShiftGameLogic(
    private val random: Random = Random.Default,
    private val scoreCalculator: ScoreCalculator = ScoreCalculator(),
) : GameLogic {

    companion object {
        private const val QUEUE_SIZE = 3
    }

    override fun restoreGame(state: GameState): GameState {
        return state.copy(nextPieceId = maxOf(state.nextPieceId, GameSessionCodec.maxPieceId(state) + 1L))
    }

    override fun newGame(
        config: GameConfig,
        challenge: DailyChallenge?,
        mode: GameMode,
    ): GameState {
        val startConfig = config.copy(columns = 3, rows = 5)
        val board = BoardMatrix.empty(columns = startConfig.columns, rows = startConfig.rows)
        var currentNextId = 1L
        val openingBag = List(QUEUE_SIZE + 1) {
            val (piece, nextId) = createPiece(nextPieceId = currentNextId)
            currentNextId = nextId
            piece
        }
        val activePiece = openingBag.first()
        val nextQueue = openingBag.drop(1)
        return GameState(
            config = startConfig,
            gameMode = mode,
            gameplayStyle = GameplayStyle.MergeShift,
            board = board,
            activePiece = activePiece,
            nextQueue = nextQueue,
            holdPiece = null,
            canHold = true,
            lastPlacementColumn = startConfig.columns / 2,
            score = 0,
            lastMoveScore = 0,
            linesCleared = 0,
            level = 1,
            difficultyStage = 0,
            secondsUntilDifficultyIncrease = startConfig.difficultyIntervalSeconds,
            combo = ComboState(),
            perfectDropStreak = 0,
            launchBar = LaunchBarState(),
            columnPressure = computeColumnPressure(board, startConfig),
            softLock = null,
            status = GameStatus.Running,
            nextPieceId = currentNextId,
            remainingTimeMillis = if (mode == GameMode.TimeAttack) GameLogic.DEFAULT_TIME_ATTACK_DURATION_MILLIS else null,
            message = gameText(GameTextKey.GameMessageSelectColumn),
            activeChallenge = challenge?.let { c ->
                c.copy(tasks = c.tasks.map { it.copy(current = 0) })
            },
        )
    }

    override fun previewPlacement(state: GameState, column: Int): PlacementPreview? {
        val piece = state.activePiece ?: return null
        if (state.status != GameStatus.Running) return null

        val selectedColumn = column.coerceIn(0, state.config.columns - 1)
        
        // Find the first empty row in this column (stacking from top)
        var landingRow: Int? = null
        for (r in 0 until state.config.rows) {
            if (state.board.cellAt(selectedColumn, r) == null) {
                landingRow = r
                break
            }
        }
        
        if (landingRow == null) return null // Column full

        val landingAnchor = GridPoint(column = selectedColumn, row = landingRow)
        
        return PlacementPreview(
            selectedColumn = selectedColumn,
            entryAnchor = GridPoint(column = selectedColumn, row = state.config.rows - 1),
            landingAnchor = landingAnchor,
            occupiedCells = listOf(landingAnchor),
            coveredColumns = selectedColumn..selectedColumn,
            isPerfectDrop = state.board.isColumnEmpty(selectedColumn),
        )
    }

    override fun previewPlacement(state: GameState, pieceId: Long, origin: GridPoint): PlacementPreview? {
        return previewPlacement(state, origin.column)
    }

    override fun previewImpactPoints(state: GameState, preview: PlacementPreview?): Set<GridPoint> {
        val piece = state.activePiece ?: return emptySet()
        val p = preview ?: return emptySet()
        val v = piece.value
        val anchor = p.landingAnchor
        val impactPoints = mutableSetOf<GridPoint>()

        // Check Above
        val above = GridPoint(anchor.column, anchor.row - 1)
        if (state.board.cellAt(above.column, above.row)?.value == v) impactPoints.add(above)

        // Check Left
        val left = GridPoint(anchor.column - 1, anchor.row)
        if (state.board.cellAt(left.column, left.row)?.value == v) impactPoints.add(left)

        // Check Right
        val right = GridPoint(anchor.column + 1, anchor.row)
        if (state.board.cellAt(right.column, right.row)?.value == v) impactPoints.add(right)

        // Check Below
        val below = GridPoint(anchor.column, anchor.row + 1)
        if (state.board.cellAt(below.column, below.row)?.value == v) impactPoints.add(below)

        return impactPoints
    }

    override fun placePiece(state: GameState, column: Int): GameMoveResult {
        val piece = state.activePiece ?: return invalidMove(state)
        val preview = previewPlacement(state, column) ?: return invalidMove(state)

        val board = state.board.fill(preview.occupiedCells, piece.tone, value = piece.value)

        // Try to perform the first merge immediately
        val nextMerge = findMergeForPoint(board, preview.landingAnchor) ?: findNextMergeGlobal(board)

        return if (nextMerge != null) {
            val (sources, to, value) = nextMerge
            val numNeighbors = sources.size - 1
            val nextValue = value * (1 shl numNeighbors)
            var mergedBoard = board.clearPoints(sources)
            mergedBoard = mergedBoard.fill(listOf(to), getToneForValue(nextValue), value = nextValue)
            mergedBoard = mergedBoard.applyGravity()

            // Find where the result piece ended up after gravity
            val finalPoint = findPiece(mergedBoard, to.column, nextValue) ?: to

            GameMoveResult(
                state = state.copy(
                    board = mergedBoard,
                    activePiece = null, // Disable input during resolution
                    recentlyMergedPoints = setOf(finalPoint),
                    clearAnimationToken = state.clearAnimationToken + 1,
                    softLock = null,
                    lastPlacementColumn = preview.selectedColumn,
                    score = state.score + nextValue,
                    lastMoveScore = nextValue,
                ),
                preview = preview,
                events = setOf(GameEvent.PlacementAccepted),
            )
        } else {
            // No merges, advance queue immediately to provide the next piece
            val (nextActive, nextQueue, nextId) = advanceQueue(state)
            
            // Growth logic
            val targetConfig = getTargetConfig(board, state.config)
            val finalBoard = if (targetConfig.columns != board.columns || targetConfig.rows != board.rows) {
                board.resize(targetConfig.columns, targetConfig.rows)
            } else {
                board
            }

            val nextPressure = computeColumnPressure(finalBoard, targetConfig)

            // Game over only when ALL columns are overflowed
            val allOverflowed = nextPressure.all { it.level == PressureLevel.Overflow }

            GameMoveResult(
                state = state.copy(
                    board = finalBoard,
                    config = targetConfig,
                    activePiece = nextActive,
                    nextQueue = nextQueue,
                    nextPieceId = nextId,
                    columnPressure = nextPressure,
                    status = if (allOverflowed) GameStatus.GameOver else GameStatus.Running,
                    recentlyMergedPoints = emptySet(),
                    softLock = null,
                    lastPlacementColumn = preview.selectedColumn,
                    lastMoveScore = 0,
                    message = if (allOverflowed) gameText(GameTextKey.GameMessageNoOpening) else state.message,
                ),
                preview = preview,
                events = setOf(GameEvent.PlacementAccepted),
            )
        }
    }

    override fun placePiece(state: GameState, pieceId: Long, origin: GridPoint): GameMoveResult {
        return placePiece(state, origin.column)
    }

    override fun commitSoftLock(state: GameState): GameMoveResult {
        val piece = state.activePiece ?: return invalidMove(state)
        val softLock = state.softLock ?: return invalidMove(state)
        val preview = softLock.preview

        val board = state.board.fill(preview.occupiedCells, piece.tone, value = piece.value)

        // Try to perform the first merge immediately
        val nextMerge = findMergeForPoint(board, preview.landingAnchor) ?: findNextMergeGlobal(board)

        return if (nextMerge != null) {
            val (sources, to, value) = nextMerge
            val numNeighbors = sources.size - 1
            val nextValue = value * (1 shl numNeighbors)
            var mergedBoard = board.clearPoints(sources)
            mergedBoard = mergedBoard.fill(listOf(to), getToneForValue(nextValue), value = nextValue)
            mergedBoard = mergedBoard.applyGravity()

            // Find where the result piece ended up after gravity
            val finalPoint = findPiece(mergedBoard, to.column, nextValue) ?: to

            GameMoveResult(
                state = state.copy(
                    board = mergedBoard,
                    activePiece = null, // Disable input during resolution
                    recentlyMergedPoints = setOf(finalPoint),
                    clearAnimationToken = state.clearAnimationToken + 1,
                    softLock = null,
                    lastPlacementColumn = preview.selectedColumn,
                    score = state.score + nextValue,
                    lastMoveScore = nextValue,
                ),
                preview = preview,
                events = setOf(GameEvent.PlacementAccepted),
            )
        } else {
            // No merges, advance queue immediately to provide the next piece
            val (nextActive, nextQueue, nextId) = advanceQueue(state)
            
            // Growth logic
            val targetConfig = getTargetConfig(board, state.config)
            val finalBoard = if (targetConfig.columns != board.columns || targetConfig.rows != board.rows) {
                board.resize(targetConfig.columns, targetConfig.rows)
            } else {
                board
            }

            val nextPressure = computeColumnPressure(finalBoard, targetConfig)

            // Game over only when ALL columns are overflowed
            val allOverflowed = nextPressure.all { it.level == PressureLevel.Overflow }

            GameMoveResult(
                state = state.copy(
                    board = finalBoard,
                    config = targetConfig,
                    activePiece = nextActive,
                    nextQueue = nextQueue,
                    nextPieceId = nextId,
                    columnPressure = nextPressure,
                    status = if (allOverflowed) GameStatus.GameOver else GameStatus.Running,
                    recentlyMergedPoints = emptySet(),
                    softLock = null,
                    lastPlacementColumn = preview.selectedColumn,
                    lastMoveScore = 0,
                    message = if (allOverflowed) gameText(GameTextKey.GameMessageNoOpening) else state.message,
                ),
                preview = preview,
                events = setOf(GameEvent.PlacementAccepted),
            )
        }
    }

    override fun tick(state: GameState): GameState {
        if (state.status != GameStatus.Running) return state

        // If activePiece is null, we are resolving merges
        if (state.activePiece == null) {
            // 1. Try to find a merge starting from the recently placed/merged point
            val activePoint = state.recentlyMergedPoints.firstOrNull()
            val nextMerge = if (activePoint != null) {
                findMergeForPoint(state.board, activePoint) ?: findNextMergeGlobal(state.board)
            } else {
                findNextMergeGlobal(state.board)
            }

            if (nextMerge != null) {
                val (sources, to, value) = nextMerge
                val numNeighbors = sources.size - 1
                val nextValue = value * (1 shl numNeighbors)
                var board = state.board.clearPoints(sources)
                board = board.fill(listOf(to), getToneForValue(nextValue), value = nextValue)
                board = board.applyGravity()

                // Find where the result piece ended up after gravity
                val finalPoint = findPiece(board, to.column, nextValue) ?: to

                return state.copy(
                    board = board,
                    recentlyMergedPoints = setOf(finalPoint),
                    clearAnimationToken = state.clearAnimationToken + 1,
                    score = state.score + nextValue,
                    lastMoveScore = nextValue,
                )
            } else {
                // No more merges, advance queue
                val (nextActive, nextQueue, nextId) = advanceQueue(state)
                
                // Growth logic
                val targetConfig = getTargetConfig(state.board, state.config)
                val finalBoard = if (targetConfig.columns != state.board.columns || targetConfig.rows != state.board.rows) {
                    state.board.resize(targetConfig.columns, targetConfig.rows)
                } else {
                    state.board
                }

                val nextPressure = computeColumnPressure(finalBoard, targetConfig)
                
                // Game over only when ALL columns are overflowed
                val allOverflowed = nextPressure.all { it.level == PressureLevel.Overflow }

                return state.copy(
                    board = finalBoard,
                    config = targetConfig,
                    activePiece = nextActive,
                    nextQueue = nextQueue,
                    nextPieceId = nextId,
                    columnPressure = nextPressure,
                    status = if (allOverflowed) GameStatus.GameOver else GameStatus.Running,
                    recentlyMergedPoints = emptySet(),
                    message = if (allOverflowed) gameText(GameTextKey.GameMessageNoOpening) else state.message
                )
            }
        }

        return state
    }

    private fun getTargetConfig(board: BoardMatrix, baseConfig: GameConfig): GameConfig {
        val maxValue = findMaxValue(board)
        return when {
            maxValue >= 32768 -> baseConfig.copy(columns = 5, rows = 7)
            maxValue >= 8192 -> baseConfig.copy(columns = 5, rows = 6)
            maxValue >= 2048 -> baseConfig.copy(columns = 4, rows = 6)
            maxValue >= 512 -> baseConfig.copy(columns = 4, rows = 5)
            maxValue >= 128 -> baseConfig.copy(columns = 3, rows = 6)
            else -> baseConfig.copy(columns = 3, rows = 5)
        }
    }

    private fun findMaxValue(board: BoardMatrix): Int {
        var max = 0
        for (c in 0 until board.columns) {
            for (r in 0 until board.rows) {
                val v = board.cellAt(c, r)?.value ?: 0
                if (v > max) max = v
            }
        }
        return max
    }

    private fun findMergeForPoint(board: BoardMatrix, p: GridPoint): Triple<Set<GridPoint>, GridPoint, Int>? {
        val cell = board.cellAt(p.column, p.row) ?: return null
        val v = cell.value
        val neighbors = mutableSetOf<GridPoint>()

        // Check Above
        val above = GridPoint(p.column, p.row - 1)
        if (board.cellAt(above.column, above.row)?.value == v) neighbors.add(above)

        // Check Left
        val left = GridPoint(p.column - 1, p.row)
        if (board.cellAt(left.column, left.row)?.value == v) neighbors.add(left)

        // Check Right
        val right = GridPoint(p.column + 1, p.row)
        if (board.cellAt(right.column, right.row)?.value == v) neighbors.add(right)

        // Check Below
        val below = GridPoint(p.column, p.row + 1)
        if (board.cellAt(below.column, below.row)?.value == v) neighbors.add(below)

        if (neighbors.isEmpty()) return null

        val target = p

        return Triple(neighbors + p, target, v)
    }

    private fun findNextMergeGlobal(board: BoardMatrix): Triple<Set<GridPoint>, GridPoint, Int>? {
        for (r in 0 until board.rows) {
            for (c in 0 until board.columns) {
                val p = GridPoint(c, r)
                val merge = findMergeForPoint(board, p)
                if (merge != null) return merge
            }
        }
        return null
    }

    private fun findPiece(board: BoardMatrix, column: Int, value: Int): GridPoint? {
        for (row in 0 until board.rows) {
            if (board.cellAt(column, row)?.value == value) {
                return GridPoint(column, row)
            }
        }
        return null
    }

    private fun getToneForValue(value: Int): CellTone {
        val power = (log2(value.toDouble())).toInt()
        return CellTone.entries[power % CellTone.entries.size]
    }

    private fun createPiece(nextPieceId: Long): Pair<Piece, Long> {
        val value = listOf(2, 4, 8, 16, 32).random(random)
        return Piece(
            id = nextPieceId,
            kind = PieceKind.Single,
            tone = getToneForValue(value),
            cells = listOf(GridPoint(0, 0)),
            width = 1,
            height = 1,
            value = value,
        ) to (nextPieceId + 1L)
    }

    private fun advanceQueue(state: GameState): Triple<Piece, List<Piece>, Long> {
        val nextActive = state.nextQueue.first()
        val (newPiece, nextId) = createPiece(state.nextPieceId)
        val nextQueue = state.nextQueue.drop(1) + newPiece
        return Triple(nextActive, nextQueue, nextId)
    }

    private fun computeColumnPressure(board: BoardMatrix, config: GameConfig): List<ColumnPressure> {
        return (0 until config.columns).map { col ->
            val height = board.columnHeight(col)
            val ratio = height.toFloat() / config.rows
            ColumnPressure(
                column = col,
                filledCells = height,
                fillRatio = ratio,
                level = when {
                    height >= config.rows -> PressureLevel.Overflow
                    ratio >= 0.8f -> PressureLevel.Critical
                    ratio >= 0.6f -> PressureLevel.Warning
                    else -> PressureLevel.Calm
                }
            )
        }
    }

    override fun holdPiece(state: GameState): GameMoveResult = invalidMove(state)
    override fun replaceActivePiece(state: GameState, specialType: SpecialBlockType): GameMoveResult = invalidMove(state)
    override fun reviveFromReward(state: GameState): GameMoveResult = invalidMove(state)

    private fun invalidMove(state: GameState) = GameMoveResult(state, events = setOf(GameEvent.InvalidDrop))
}
