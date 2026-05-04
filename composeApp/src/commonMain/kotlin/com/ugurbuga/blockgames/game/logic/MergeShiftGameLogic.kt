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
        gameplayStyle: GameplayStyle,
        config: GameConfig,
        challenge: DailyChallenge?,
        mode: GameMode,
    ): GameState {
        val board = BoardMatrix.empty(columns = config.columns, rows = config.rows)
        var currentNextId = 1L
        val openingBag = List(QUEUE_SIZE + 1) {
            val (piece, nextId) = createPiece(nextPieceId = currentNextId)
            currentNextId = nextId
            piece
        }
        val activePiece = openingBag.first()
        val nextQueue = openingBag.drop(1)
        return GameState(
            config = config,
            gameMode = mode,
            gameplayStyle = gameplayStyle,
            board = board,
            activePiece = activePiece,
            nextQueue = nextQueue,
            holdPiece = null,
            canHold = true,
            lastPlacementColumn = config.columns / 2,
            score = 0,
            lastMoveScore = 0,
            linesCleared = 0,
            level = 1,
            difficultyStage = 0,
            secondsUntilDifficultyIncrease = config.difficultyIntervalSeconds,
            combo = ComboState(),
            perfectDropStreak = 0,
            launchBar = LaunchBarState(),
            columnPressure = computeColumnPressure(board, config),
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
        
        // Find the lowest empty row in this column
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

        // Priority 1: Vertical (Above) - in this system row-1 is older/above
        val above = GridPoint(anchor.column, anchor.row - 1)
        if (state.board.cellAt(above.column, above.row)?.value == v) return setOf(above)

        // Priority 2: Left
        val left = GridPoint(anchor.column - 1, anchor.row)
        if (state.board.cellAt(left.column, left.row)?.value == v) return setOf(left)

        // Priority 3: Right
        val right = GridPoint(anchor.column + 1, anchor.row)
        if (state.board.cellAt(right.column, right.row)?.value == v) return setOf(right)

        return emptySet()
    }

    override fun placePiece(state: GameState, column: Int): GameMoveResult {
        val piece = state.activePiece ?: return invalidMove(state)
        val preview = previewPlacement(state, column) ?: return invalidMove(state)

        val board = state.board.fill(preview.occupiedCells, piece.tone, value = piece.value)

        // Try to perform the first merge immediately
        val nextMerge = findMergeForPoint(board, preview.landingAnchor) ?: findNextMergeGlobal(board)

        return if (nextMerge != null) {
            val (from, to, value) = nextMerge
            val nextValue = value * 2
            var mergedBoard = board.clearPoints(setOf(from, to))
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
            val targetConfig = getTargetConfig(board)
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
            val (from, to, value) = nextMerge
            val nextValue = value * 2
            var mergedBoard = board.clearPoints(setOf(from, to))
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
            val targetConfig = getTargetConfig(board)
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
                val (from, to, value) = nextMerge
                val nextValue = value * 2
                var board = state.board.clearPoints(setOf(from, to))
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
                val targetConfig = getTargetConfig(state.board)
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

    private fun getTargetConfig(board: BoardMatrix): GameConfig {
        val maxValue = findMaxValue(board)
        return when {
            maxValue >= 1024 -> GameConfig(columns = 5, rows = 7)
            maxValue >= 512 -> GameConfig(columns = 5, rows = 6)
            maxValue >= 256 -> GameConfig(columns = 4, rows = 6)
            maxValue >= 128 -> GameConfig(columns = 4, rows = 5)
            maxValue >= 64 -> GameConfig(columns = 3, rows = 6)
            else -> GameConfig(columns = 3, rows = 5)
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

    private fun findMergeForPoint(board: BoardMatrix, p: GridPoint): Triple<GridPoint, GridPoint, Int>? {
        val cell = board.cellAt(p.column, p.row) ?: return null
        val v = cell.value

        // 1. Above (Older)
        val above = GridPoint(p.column, p.row - 1)
        if (board.cellAt(above.column, above.row)?.value == v) {
            return Triple(p, above, v)
        }
        // 2. Left (Older if already on board)
        val left = GridPoint(p.column - 1, p.row)
        if (board.cellAt(left.column, left.row)?.value == v) {
            return Triple(p, left, v)
        }
        // 3. Right (Older if already on board)
        val right = GridPoint(p.column + 1, p.row)
        if (board.cellAt(right.column, right.row)?.value == v) {
            return Triple(p, right, v)
        }
        return null
    }

    private fun findNextMergeGlobal(board: BoardMatrix): Triple<GridPoint, GridPoint, Int>? {
        // Priority 1: Vertical (Above) - Merge into the top one (older)
        for (r in 1 until board.rows) {
            for (c in 0 until board.columns) {
                val cell = board.cellAt(c, r) ?: continue
                val above = board.cellAt(c, r - 1)
                if (above != null && above.value == cell.value) {
                    return Triple(GridPoint(c, r), GridPoint(c, r - 1), cell.value)
                }
            }
        }
        // Priority 2: Horizontal (Left) - Merge into the left one (older)
        for (r in 0 until board.rows) {
            for (c in 1 until board.columns) {
                val cell = board.cellAt(c, r) ?: continue
                val left = board.cellAt(c - 1, r)
                if (left != null && left.value == cell.value) {
                    return Triple(GridPoint(c, r), GridPoint(c - 1, r), cell.value)
                }
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
