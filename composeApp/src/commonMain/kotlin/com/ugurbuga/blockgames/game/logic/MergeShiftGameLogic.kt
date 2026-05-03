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
import com.ugurbuga.blockgames.game.model.SoftLockState
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
        private const val SOFT_LOCK_MILLIS = 260L
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
            lastPlacementColumn = null,
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
        val bottomOccupied = state.board.bottomOccupiedRow(selectedColumn)
        
        val landingRow = if (bottomOccupied == null) 0 else bottomOccupied + 1
        if (landingRow >= state.config.rows) return null // Column full

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
        return emptySet()
    }

    override fun placePiece(state: GameState, column: Int): GameMoveResult {
        val piece = state.activePiece ?: return invalidMove(state)
        val preview = previewPlacement(state, column) ?: return invalidMove(state)

        return GameMoveResult(
            state = state.copy(
                softLock = SoftLockState(
                    pieceId = piece.id,
                    preview = preview,
                    remainingMillis = SOFT_LOCK_MILLIS,
                    revision = (state.softLock?.revision ?: 0L) + 1L,
                ),
                lastPlacementColumn = preview.selectedColumn,
                message = gameText(GameTextKey.GameMessageSoftLock),
            ),
            preview = preview,
            events = setOf(GameEvent.SoftLockStarted),
        )
    }

    override fun placePiece(state: GameState, pieceId: Long, origin: GridPoint): GameMoveResult {
        return placePiece(state, origin.column)
    }

    override fun commitSoftLock(state: GameState): GameMoveResult {
        val piece = state.activePiece ?: return invalidMove(state)
        val softLock = state.softLock ?: return invalidMove(state)
        val preview = softLock.preview
        
        var board = state.board.fill(preview.occupiedCells, piece.tone, value = piece.value)
        
        val mergeResult = resolveMerges(board, preview.landingAnchor)
        board = mergeResult.board

        val (nextActive, nextQueue, nextId) = advanceQueue(state)
        
        val nextPressure = computeColumnPressure(board, state.config)
        val overflowed = nextPressure.any { it.level == PressureLevel.Overflow }
        
        val events = mutableSetOf<GameEvent>(GameEvent.PlacementAccepted)
        if (mergeResult.merges > 0) events.add(GameEvent.SpecialTriggered)
        
        val nextStatus = if (overflowed) GameStatus.GameOver else GameStatus.Running
        if (overflowed) events.add(GameEvent.GameOver)

        val moveScore = mergeResult.score + piece.value
        
        return GameMoveResult(
            state = state.copy(
                board = board,
                activePiece = nextActive,
                nextQueue = nextQueue,
                score = state.score + moveScore,
                lastMoveScore = moveScore,
                status = nextStatus,
                nextPieceId = nextId,
                softLock = null,
                columnPressure = nextPressure,
                recentlyMergedPoints = mergeResult.mergedPoints,
                clearAnimationToken = if (mergeResult.merges > 0) state.clearAnimationToken + 1 else state.clearAnimationToken
            ),
            preview = preview,
            events = events
        )
    }

    private fun resolveMerges(board: BoardMatrix, startPoint: GridPoint): MergeResult {
        var currentBoard = board
        var totalScore = 0
        var totalMerges = 0
        val mergedPoints = mutableSetOf<GridPoint>()
        
        var currentTarget = startPoint
        
        while (true) {
            val cell = currentBoard.cellAt(currentTarget.column, currentTarget.row) ?: break
            val currentValue = cell.value
            
            val neighbors = listOf(
                GridPoint(currentTarget.column - 1, currentTarget.row),
                GridPoint(currentTarget.column + 1, currentTarget.row),
                GridPoint(currentTarget.column, currentTarget.row - 1),
                GridPoint(currentTarget.column, currentTarget.row + 1),
            ).filter { it.column in 0 until board.columns && it.row in 0 until board.rows }
            
            val matchingNeighbors = neighbors.filter { p ->
                currentBoard.cellAt(p.column, p.row)?.value == currentValue
            }
            
            if (matchingNeighbors.isEmpty()) break
            
            // Merge matching neighbors into the topmost point
            val allInvolved = matchingNeighbors + currentTarget
            val topmost = allInvolved.minBy { it.row }
            
            currentBoard = currentBoard.clearPoints(allInvolved.toSet())
            val nextValue = currentValue * 2
            val nextTone = getToneForValue(nextValue)
            currentBoard = currentBoard.fill(listOf(topmost), nextTone, value = nextValue)
            
            mergedPoints.addAll(allInvolved)
            totalMerges += matchingNeighbors.size
            totalScore += nextValue * matchingNeighbors.size
            
            currentTarget = topmost
            // Continue merging from the new topmost point as value increased
        }
        
        return MergeResult(currentBoard, totalScore, totalMerges, mergedPoints)
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
    override fun tick(state: GameState): GameState = state

    private fun invalidMove(state: GameState) = GameMoveResult(state, events = setOf(GameEvent.InvalidDrop))

    private data class MergeResult(
        val board: BoardMatrix,
        val score: Int,
        val merges: Int,
        val mergedPoints: Set<GridPoint> = emptySet()
    )
}
