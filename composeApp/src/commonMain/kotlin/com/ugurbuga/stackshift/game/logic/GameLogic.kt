package com.ugurbuga.stackshift.game.logic

import com.ugurbuga.stackshift.game.model.BoardMatrix
import com.ugurbuga.stackshift.game.model.ColumnPressure
import com.ugurbuga.stackshift.game.model.ComboState
import com.ugurbuga.stackshift.game.model.FeedbackEmphasis
import com.ugurbuga.stackshift.game.model.FloatingFeedback
import com.ugurbuga.stackshift.game.model.GameConfig
import com.ugurbuga.stackshift.game.model.GameState
import com.ugurbuga.stackshift.game.model.GameStatus
import com.ugurbuga.stackshift.game.model.GameTextKey
import com.ugurbuga.stackshift.game.model.GridPoint
import com.ugurbuga.stackshift.game.model.LaunchBarState
import com.ugurbuga.stackshift.game.model.Piece
import com.ugurbuga.stackshift.game.model.PieceKind
import com.ugurbuga.stackshift.game.model.PlacementPreview
import com.ugurbuga.stackshift.game.model.PressureLevel
import com.ugurbuga.stackshift.game.model.SoftLockState
import com.ugurbuga.stackshift.game.model.SpecialBlockType
import com.ugurbuga.stackshift.game.model.gameText
import com.ugurbuga.stackshift.settings.GameSessionCodec
import kotlin.math.min
import kotlin.random.Random

class GameLogic(
    private val random: Random = Random.Default,
) {
    private var nextPieceId: Long = 1L

    fun restoreGame(state: GameState): GameState {
        nextPieceId = maxOf(nextPieceId, GameSessionCodec.maxPieceId(state) + 1L)
        return state
    }

    fun newGame(config: GameConfig = GameConfig()): GameState {
        val level = 1
        val board = BoardMatrix.empty(columns = config.columns, rows = config.rows)
        val openingBag = List(QueueSize + 1) { createPiece(level = level, launchBar = LaunchBarState()) }
        val activePiece = openingBag.first()
        val nextQueue = openingBag.drop(1)
        val state = GameState(
            config = config,
            board = board,
            activePiece = activePiece,
            nextQueue = nextQueue,
            holdPiece = null,
            canHold = true,
            lastPlacementColumn = null,
            score = 0,
            lastMoveScore = 0,
            linesCleared = 0,
            level = level,
            difficultyStage = 0,
            secondsUntilDifficultyIncrease = config.difficultyIntervalSeconds,
            combo = ComboState(),
            perfectDropStreak = 0,
            launchBar = LaunchBarState(),
            columnPressure = computeColumnPressure(board, config),
            softLock = null,
            status = GameStatus.Running,
            recentlyClearedRows = emptySet(),
            recentlyClearedColumns = emptySet(),
            lastResolvedLines = 0,
            lastChainDepth = 0,
            clearAnimationToken = 0L,
            floatingFeedback = null,
            feedbackToken = 0L,
            message = gameText(GameTextKey.GameMessageSelectColumn),
        )

        return if (hasAnyValidPlacement(state.board, activePiece, state.config)) {
            state
        } else {
            state.copy(
                status = GameStatus.GameOver,
                message = gameText(GameTextKey.GameMessageNoOpening),
                floatingFeedback = FloatingFeedback(
                    text = gameText(GameTextKey.FeedbackOverflow),
                    emphasis = FeedbackEmphasis.Danger,
                    token = 1L,
                ),
                feedbackToken = 1L,
            )
        }
    }

    fun previewPlacement(
        state: GameState,
        approximateColumn: Int,
    ): PlacementPreview? {
        val piece = state.activePiece ?: return null
        if (state.status != GameStatus.Running) return null

        val maxColumn = state.config.columns - piece.width
        val selectedColumn = normalizeColumn(approximateColumn = approximateColumn, maxColumn = maxColumn)
            ?: return null

        return previewForColumn(
            board = state.board,
            piece = piece,
            config = state.config,
            selectedColumn = selectedColumn,
        )
    }

    fun previewImpactPoints(
        state: GameState,
        preview: PlacementPreview?,
    ): Set<GridPoint> {
        val piece = state.activePiece ?: return emptySet()
        val resolvedPreview = preview ?: return emptySet()
        if (state.status != GameStatus.Running) return emptySet()

        val originalOccupiedPoints = state.board.occupiedPoints().toSet()
        if (originalOccupiedPoints.isEmpty()) return emptySet()

        val specialResult = applyPlacementAndSpecial(
            board = state.board,
            piece = piece,
            preview = resolvedPreview,
        )
        val directSpecialImpact = collectPreviewImpactPoints(
            board = state.board,
            piece = piece,
            preview = resolvedPreview,
        )
        val burstImpact = collectPreviewBurstImpact(
            board = specialResult.board,
            pendingTriggers = specialResult.initialTriggeredSpecials,
            originalOccupiedPoints = originalOccupiedPoints,
        )
        val firstWaveFullRows = burstImpact.board.fullRows().toSet()
        val rowClearImpact = if (firstWaveFullRows.isEmpty()) {
            emptySet()
        } else {
            burstImpact.board.occupiedPointsInRows(firstWaveFullRows)
                .filter(originalOccupiedPoints::contains)
                .toSet()
        }

        return buildSet {
            addAll(directSpecialImpact)
            addAll(burstImpact.impactedPoints)
            addAll(rowClearImpact)
        }
    }

    fun placePiece(
        state: GameState,
        approximateColumn: Int,
    ): GameMoveResult {
        val piece = state.activePiece ?: return invalidMove(state)
        if (state.status != GameStatus.Running) return invalidMove(state)

        val preview = previewPlacement(state = state, approximateColumn = approximateColumn)
            ?: return invalidMove(state)

        val nextRevision = (state.softLock?.revision ?: 0L) + 1L
        val nextToken = state.feedbackToken + 1L
        val wasAdjustingSoftLock = state.softLock?.pieceId == piece.id
        val feedback = when {
            preview.isPerfectDrop -> FloatingFeedback(
                text = gameText(GameTextKey.FeedbackPerfectLane, state.perfectDropStreak + 1),
                emphasis = FeedbackEmphasis.Bonus,
                token = nextToken,
            )
            wasAdjustingSoftLock -> FloatingFeedback(
                text = gameText(GameTextKey.FeedbackMicroAdjust),
                emphasis = FeedbackEmphasis.Info,
                token = nextToken,
            )
            else -> FloatingFeedback(
                text = gameText(GameTextKey.FeedbackSoftLock),
                emphasis = FeedbackEmphasis.Info,
                token = nextToken,
            )
        }

        return GameMoveResult(
            state = state.copy(
                softLock = SoftLockState(
                    pieceId = piece.id,
                    preview = preview,
                    remainingMillis = SoftLockMillis,
                    revision = nextRevision,
                ),
                lastPlacementColumn = preview.selectedColumn,
                floatingFeedback = feedback,
                feedbackToken = nextToken,
                message = gameText(GameTextKey.GameMessageSoftLock),
            ),
            preview = preview,
            events = buildSet {
                add(if (wasAdjustingSoftLock) GameEvent.SoftLockAdjusted else GameEvent.SoftLockStarted)
                if (preview.isPerfectDrop) add(GameEvent.PerfectDrop)
            },
        )
    }

    fun commitSoftLock(state: GameState): GameMoveResult {
        val piece = state.activePiece ?: return invalidMove(state)
        val softLock = state.softLock ?: return invalidMove(state)
        if (softLock.pieceId != piece.id) return invalidMove(state)

        val preview = softLock.preview
        val specialResult = applyPlacementAndSpecial(
            board = state.board,
            piece = piece,
            preview = preview,
        )
        val resolution = resolveBoard(
            board = specialResult.board,
            specialTriggered = specialResult.specialTriggered,
            initialTriggeredSpecials = specialResult.initialTriggeredSpecials,
            initiallyClearedRows = specialResult.clearedRows,
            initiallyClearedColumns = specialResult.clearedColumns,
        )
        val perfectDrop = preview.isPerfectDrop
        val nextPerfectStreak = if (perfectDrop) state.perfectDropStreak + 1 else 0
        val nextComboChain = if (resolution.totalLinesCleared == 0) 0 else state.combo.chain + 1
        val comboState = ComboState(
            chain = nextComboChain,
            best = maxOf(state.combo.best, nextComboChain),
        )
        val nextLaunchBar = advanceLaunchBar(
            current = state.launchBar,
            clearedRows = resolution.totalLinesCleared,
            perfectDrop = perfectDrop,
        )
        val totalLines = state.linesCleared + resolution.totalLinesCleared
        val nextLevel = computeLevel(
            difficultyStage = state.difficultyStage,
            clearedLines = totalLines,
            config = state.config,
        )
        val queueAdvance = promoteQueue(
            queue = state.nextQueue,
            level = nextLevel,
            launchBar = nextLaunchBar,
        )
        val nextActivePiece = queueAdvance.activePiece
        val nextQueue = queueAdvance.nextQueue
        val nextPressure = computeColumnPressure(resolution.board, state.config)

        val events = linkedSetOf(GameEvent.PlacementAccepted)
        if (resolution.specialTriggered) events += GameEvent.SpecialTriggered
        if (resolution.totalLinesCleared > 0) events += GameEvent.LineClear
        if (resolution.chainDepth > 1) events += GameEvent.ChainReaction
        if (nextComboChain > 1) events += GameEvent.Combo
        if (perfectDrop) events += GameEvent.PerfectDrop
        if (nextLaunchBar.boostTurnsRemaining > state.launchBar.boostTurnsRemaining) events += GameEvent.LaunchBoostCharged
        if (nextPressure.any { it.level == PressureLevel.Critical || it.level == PressureLevel.Overflow }) {
            events += GameEvent.PressureCritical
        }

        val overflowed = nextPressure.any { it.level == PressureLevel.Overflow }
        val nextStatus = if (!overflowed && hasAnyValidPlacement(resolution.board, nextActivePiece, state.config)) {
            GameStatus.Running
        } else {
            events += GameEvent.GameOver
            GameStatus.GameOver
        }

        val nextScore = state.score + scoreFor(
            clearedRows = resolution.totalLinesCleared,
            level = nextLevel,
            comboChain = nextComboChain,
            chainDepth = resolution.chainDepth,
            perfectDropStreak = nextPerfectStreak,
            specialTriggered = resolution.specialTriggered,
            launchBoostActive = nextLaunchBar.boostTurnsRemaining > 0,
        )
        val lastMoveScore = nextScore - state.score
        val nextToken = state.feedbackToken + 1L
        val floatingFeedback = buildFloatingFeedback(
            state = state,
            lastMoveScore = lastMoveScore,
            resolution = resolution,
            perfectDrop = perfectDrop,
            nextPerfectStreak = nextPerfectStreak,
            token = nextToken,
        )
        val message = when {
            overflowed -> gameText(GameTextKey.GameMessageOverflow)
            resolution.triggeredSpecialCells > 1 -> gameText(GameTextKey.GameMessageSpecialChainBoard, resolution.triggeredSpecialCells)
            nextStatus == GameStatus.GameOver -> gameText(GameTextKey.GameMessagePressureGameOver)
            resolution.specialTriggered && resolution.totalLinesCleared > 0 ->
                gameText(GameTextKey.GameMessageSpecialLines, resolution.totalLinesCleared)
            resolution.specialTriggered -> gameText(GameTextKey.GameMessageSpecialTriggered)
            perfectDrop && nextPerfectStreak > 1 -> gameText(GameTextKey.GameMessagePerfectDrop, nextPerfectStreak)
            resolution.totalLinesCleared > 0 && resolution.chainDepth > 1 ->
                gameText(GameTextKey.GameMessageChainLines, resolution.chainDepth, resolution.totalLinesCleared)
            resolution.totalLinesCleared > 0 -> gameText(GameTextKey.GameMessageLinesCleared, resolution.totalLinesCleared)
            else -> gameText(GameTextKey.GameMessageGoodShot)
        }

        return GameMoveResult(
            state = state.copy(
                board = resolution.board,
                activePiece = nextActivePiece,
                nextQueue = nextQueue,
                canHold = true,
                lastPlacementColumn = preview.selectedColumn,
                score = nextScore,
                lastMoveScore = lastMoveScore,
                linesCleared = totalLines,
                level = nextLevel,
                combo = comboState,
                perfectDropStreak = nextPerfectStreak,
                launchBar = nextLaunchBar,
                columnPressure = nextPressure,
                softLock = null,
                status = nextStatus,
                recentlyClearedRows = resolution.firstClearedRows,
                recentlyClearedColumns = resolution.firstClearedColumns,
                lastResolvedLines = resolution.totalLinesCleared,
                lastChainDepth = resolution.chainDepth,
                specialChainCount = resolution.triggeredSpecialCells,
                clearAnimationToken = if (resolution.totalLinesCleared == 0 && resolution.triggeredSpecialCells == 0 && resolution.firstClearedRows.isEmpty() && resolution.firstClearedColumns.isEmpty()) {
                    state.clearAnimationToken
                } else {
                    state.clearAnimationToken + 1L
                },
                screenShakeToken = if (nextStatus == GameStatus.GameOver || resolution.totalLinesCleared > 0 || resolution.triggeredSpecialCells > 0 || resolution.firstClearedRows.isNotEmpty() || resolution.firstClearedColumns.isNotEmpty()) {
                    state.screenShakeToken + 1L
                } else {
                    state.screenShakeToken
                },
                impactFlashToken = if (piece.special != SpecialBlockType.None || resolution.totalLinesCleared > 0 || resolution.triggeredSpecialCells > 0) state.impactFlashToken + 1L else state.impactFlashToken,
                comboPopupToken = if (nextComboChain > 1 || perfectDrop || resolution.triggeredSpecialCells > 0) state.comboPopupToken + 1L else state.comboPopupToken,
                floatingFeedback = floatingFeedback,
                feedbackToken = nextToken,
                message = message,
            ),
            preview = preview,
            events = events,
        )
    }

    fun holdPiece(state: GameState): GameMoveResult {
        val activePiece = state.activePiece ?: return invalidMove(state)
        if (state.status != GameStatus.Running || state.softLock != null || !state.canHold) {
            return invalidMove(state)
        }

        val queueAdvance = if (state.holdPiece == null) {
            promoteQueue(queue = state.nextQueue, level = state.level, launchBar = state.launchBar)
        } else {
            QueueAdvance(activePiece = state.holdPiece, nextQueue = state.nextQueue)
        }
        val heldPiece = activePiece.copy(id = activePiece.id)
        val swappedPiece = queueAdvance.activePiece
        val nextToken = state.feedbackToken + 1L
        val nextState = state.copy(
            activePiece = swappedPiece,
            nextQueue = queueAdvance.nextQueue,
            holdPiece = heldPiece,
            canHold = false,
            softLock = null,
            floatingFeedback = FloatingFeedback(
                text = if (state.holdPiece == null) gameText(GameTextKey.FeedbackHoldArmed) else gameText(GameTextKey.FeedbackSwap),
                emphasis = FeedbackEmphasis.Info,
                token = nextToken,
            ),
            feedbackToken = nextToken,
            message = gameText(GameTextKey.GameMessageHoldUpdated),
        )
        return GameMoveResult(state = nextState, events = setOf(GameEvent.HoldUsed))
    }

    fun reviveFromReward(state: GameState): GameMoveResult {
        if (state.status != GameStatus.GameOver || state.rewardedReviveUsed) {
            return invalidMove(state)
        }

        val revivedRows = selectRewardRows(
            board = state.board,
            totalRows = state.config.rows,
            maxRowsToClear = RewardedReviveRowCount,
        )
        val revivedBoard = state.board.clearRows(revivedRows)
        val queueAdvance = ensurePlayableQueueAfterRevive(
            board = revivedBoard,
            activePiece = state.activePiece,
            nextQueue = state.nextQueue,
            level = state.level,
            launchBar = state.launchBar,
            config = state.config,
        )
        val nextPressure = computeColumnPressure(revivedBoard, state.config)
        val nextToken = state.feedbackToken + 1L

        return GameMoveResult(
            state = state.copy(
                board = revivedBoard,
                activePiece = queueAdvance.activePiece,
                nextQueue = queueAdvance.nextQueue,
                lastMoveScore = 0,
                columnPressure = nextPressure,
                softLock = null,
                status = GameStatus.Running,
                recentlyClearedRows = revivedRows,
                recentlyClearedColumns = emptySet(),
                lastResolvedLines = revivedRows.size,
                lastChainDepth = if (revivedRows.isEmpty()) 0 else 1,
                specialChainCount = 0,
                clearAnimationToken = state.clearAnimationToken + 1L,
                screenShakeToken = state.screenShakeToken + 1L,
                impactFlashToken = state.impactFlashToken + 1L,
                comboPopupToken = state.comboPopupToken + 1L,
                floatingFeedback = FloatingFeedback(
                    text = gameText(GameTextKey.FeedbackExtraLife, revivedRows.size),
                    emphasis = FeedbackEmphasis.Bonus,
                    token = nextToken,
                ),
                feedbackToken = nextToken,
                rewardedReviveUsed = true,
                message = gameText(GameTextKey.GameMessageExtraLifeUsed, revivedRows.size),
            ),
            events = setOf(GameEvent.Revived),
        )
    }

    fun tick(state: GameState): GameState {
        if (state.status != GameStatus.Running) return state

        val nextSeconds = state.secondsUntilDifficultyIncrease - 1
        if (nextSeconds > 0) {
            return state.copy(
                secondsUntilDifficultyIncrease = nextSeconds,
                columnPressure = computeColumnPressure(state.board, state.config),
            )
        }

        val nextDifficultyStage = state.difficultyStage + 1
        val nextLevel = computeLevel(
            difficultyStage = nextDifficultyStage,
            clearedLines = state.linesCleared,
            config = state.config,
        )
        val pressure = computeColumnPressure(state.board, state.config)
        return state.copy(
            difficultyStage = nextDifficultyStage,
            level = nextLevel,
            secondsUntilDifficultyIncrease = state.config.difficultyIntervalSeconds,
            columnPressure = pressure,
            status = if (pressure.any { it.level == PressureLevel.Overflow }) GameStatus.GameOver else state.status,
            message = if (pressure.any { it.level == PressureLevel.Critical || it.level == PressureLevel.Overflow }) {
                gameText(GameTextKey.GameMessageTempoCritical)
            } else {
                gameText(GameTextKey.GameMessageTempoUp)
            },
        )
    }

    fun pause(state: GameState): GameState {
        if (state.status != GameStatus.Running) return state
        return state.copy(status = GameStatus.Paused, message = gameText(GameTextKey.GameMessagePaused))
    }

    fun resume(state: GameState): GameState {
        if (state.status != GameStatus.Paused) return state
        return state.copy(status = GameStatus.Running, message = gameText(GameTextKey.GameMessageResumed))
    }

    fun hasAnyValidPlacement(
        board: BoardMatrix,
        piece: Piece,
        config: GameConfig,
    ): Boolean {
        val maxColumn = config.columns - piece.width
        if (maxColumn < 0 || config.rows - piece.height < 0) return false

        for (column in 0..maxColumn) {
            if (
                previewForColumn(
                    board = board,
                    piece = piece,
                    config = config,
                    selectedColumn = column,
                ) != null
            ) {
                return true
            }
        }
        return false
    }

    private fun previewForColumn(
        board: BoardMatrix,
        piece: Piece,
        config: GameConfig,
        selectedColumn: Int,
    ): PlacementPreview? {
        val maxColumn = config.columns - piece.width
        val entryRow = config.rows - piece.height
        if (maxColumn < 0 || entryRow < 0) return null
        if (selectedColumn !in 0..maxColumn) return null

        var ghostPassesRemaining = if (piece.special == SpecialBlockType.Ghost) 1 else 0
        val entryAnchor = GridPoint(column = selectedColumn, row = entryRow)
        if (!isValidPlacement(board, piece, entryAnchor)) {
            if (ghostPassesRemaining == 0 || !canGhostPass(board, piece, entryAnchor)) return null
            ghostPassesRemaining -= 1
        }

        var landingRow = entryRow
        while (landingRow > 0) {
            val candidate = GridPoint(column = selectedColumn, row = landingRow - 1)
            when {
                isValidPlacement(board, piece, candidate) -> landingRow -= 1
                ghostPassesRemaining > 0 && canGhostPass(board, piece, candidate) -> {
                    landingRow -= 1
                    ghostPassesRemaining -= 1
                }
                else -> break
            }
        }

        val landingAnchor = GridPoint(column = selectedColumn, row = landingRow)
        val coveredColumns = selectedColumn..(selectedColumn + piece.width - 1)
        return PlacementPreview(
            selectedColumn = selectedColumn,
            entryAnchor = entryAnchor,
            landingAnchor = landingAnchor,
            occupiedCells = piece.cellsAt(landingAnchor),
            coveredColumns = coveredColumns,
            isPerfectDrop = coveredColumns.all(board::isColumnEmpty),
        )
    }

    private fun resolveBoard(
        board: BoardMatrix,
        specialTriggered: Boolean,
        initialTriggeredSpecials: List<TriggeredSpecial>,
        initiallyClearedRows: Set<Int> = emptySet(),
        initiallyClearedColumns: Set<Int> = emptySet(),
    ): ResolutionResult {
        var resolvedBoard = board
        var firstWaveRows = initiallyClearedRows
        var firstWaveColumns = initiallyClearedColumns
        var totalLinesCleared = 0
        var chainDepth = 0
        var triggeredSpecialCells = 0
        var pendingTriggers = initialTriggeredSpecials
        var didTriggerSpecial = specialTriggered

        while (true) {
            if (pendingTriggers.isNotEmpty()) {
                val burstResult = applyTriggeredSpecialBursts(
                    board = resolvedBoard,
                    pendingTriggers = pendingTriggers,
                )
                resolvedBoard = burstResult.board
                triggeredSpecialCells += burstResult.triggeredCount
                didTriggerSpecial = didTriggerSpecial || burstResult.triggeredCount > 0

                if (firstWaveRows.isEmpty() && firstWaveColumns.isEmpty()) {
                    pendingTriggers.forEach { trigger ->
                        when (trigger.special) {
                            SpecialBlockType.RowClearer -> firstWaveRows = firstWaveRows + trigger.point.row
                            SpecialBlockType.ColumnClearer -> firstWaveColumns = firstWaveColumns + trigger.point.column
                            else -> {}
                        }
                    }
                }
                pendingTriggers = emptyList()
            }

            val fullRows = resolvedBoard.fullRows()
            if (fullRows.isEmpty()) break

            if (firstWaveRows.isEmpty()) {
                firstWaveRows = fullRows.toSet()
            }

            pendingTriggers = collectTriggeredSpecialsFromRows(
                board = resolvedBoard,
                rows = fullRows.toSet(),
            )
            totalLinesCleared += fullRows.size
            chainDepth += 1
            resolvedBoard = resolvedBoard.clearRows(fullRows.toSet())
        }

        return ResolutionResult(
            board = resolvedBoard,
            firstClearedRows = firstWaveRows,
            firstClearedColumns = firstWaveColumns,
            totalLinesCleared = totalLinesCleared,
            chainDepth = chainDepth,
            triggeredSpecialCells = triggeredSpecialCells,
            specialTriggered = didTriggerSpecial,
        )
    }

    private fun applyPlacementAndSpecial(
        board: BoardMatrix,
        piece: Piece,
        preview: PlacementPreview,
    ): SpecialResolution {
        val placedBoard = board.fill(points = preview.occupiedCells, tone = piece.tone, special = piece.special)
        val protectedPoints = preview.occupiedCells.toSet()
        val initialTriggeredSpecials = when (piece.special) {
            SpecialBlockType.ColumnClearer -> collectTriggeredSpecialsFromPoints(
                board = placedBoard,
                points = placedBoard.occupiedPointsInColumns(preview.coveredColumns.toSet()).filterNot(protectedPoints::contains),
            )
            SpecialBlockType.RowClearer -> collectTriggeredSpecialsFromPoints(
                board = placedBoard,
                points = placedBoard.occupiedPointsInRows(preview.occupiedCells.map(GridPoint::row).toSet()).filterNot(protectedPoints::contains),
            )
            else -> emptyList()
        }
        val clearedRows = if (piece.special == SpecialBlockType.RowClearer) {
            preview.occupiedCells.map(GridPoint::row).toSet()
        } else {
            emptySet()
        }
        val clearedColumns = if (piece.special == SpecialBlockType.ColumnClearer) {
            preview.coveredColumns.toSet()
        } else {
            emptySet()
        }
        val resolvedBoard = when (piece.special) {
            SpecialBlockType.None,
            SpecialBlockType.Ghost,
            -> placedBoard
            SpecialBlockType.ColumnClearer -> placedBoard.clearColumns(clearedColumns)
            SpecialBlockType.RowClearer -> placedBoard.clearRows(clearedRows)
            SpecialBlockType.Heavy -> placedBoard.pushColumnsDown(
                columnsToPush = preview.coveredColumns.toSet(),
                protectedPoints = preview.occupiedCells.toSet(),
            )
        }
        return SpecialResolution(
            board = resolvedBoard,
            specialTriggered = piece.special != SpecialBlockType.None,
            initialTriggeredSpecials = initialTriggeredSpecials,
            clearedRows = clearedRows,
            clearedColumns = clearedColumns,
        )
    }

    private fun collectTriggeredSpecialsFromRows(
        board: BoardMatrix,
        rows: Set<Int>,
    ): List<TriggeredSpecial> = collectTriggeredSpecialsFromPoints(
        board = board,
        points = board.occupiedPointsInRows(rows),
    )

    private fun collectTriggeredSpecialsFromPoints(
        board: BoardMatrix,
        points: List<GridPoint>,
    ): List<TriggeredSpecial> = points.mapNotNull { point ->
        board.specialAt(point.column, point.row)
            ?.takeIf { it != SpecialBlockType.None }
            ?.let { special -> TriggeredSpecial(point = point, special = special) }
    }

    private fun applyTriggeredSpecialBursts(
        board: BoardMatrix,
        pendingTriggers: List<TriggeredSpecial>,
    ): TriggerBurstResult {
        var resolvedBoard = board
        var triggeredCount = 0
        val visited = mutableSetOf<GridPoint>()
        val queue = ArrayDeque(pendingTriggers)

        while (queue.isNotEmpty()) {
            val trigger = queue.removeFirst()
            if (!visited.add(trigger.point)) continue

            val effect = buildTriggeredEffect(resolvedBoard, trigger)
            val nextTriggers = collectTriggeredSpecialsForEffect(
                board = resolvedBoard,
                effect = effect,
            )
            resolvedBoard = applyTriggeredEffect(resolvedBoard, effect)
            triggeredCount += 1
            nextTriggers.filterNot { it.point in visited }.forEach(queue::addLast)
        }

        return TriggerBurstResult(
            board = resolvedBoard,
            triggeredCount = triggeredCount,
        )
    }

    private fun collectPreviewImpactPoints(
        board: BoardMatrix,
        piece: Piece,
        preview: PlacementPreview,
    ): Set<GridPoint> {
        val protectedPoints = preview.occupiedCells.toSet()
        return when (piece.special) {
            SpecialBlockType.None,
            SpecialBlockType.Ghost,
            SpecialBlockType.Heavy,
            -> emptySet()
            SpecialBlockType.ColumnClearer -> board.occupiedPointsInColumns(preview.coveredColumns.toSet())
                .filterNot(protectedPoints::contains)
                .toSet()
            SpecialBlockType.RowClearer -> board.occupiedPointsInRows(preview.occupiedCells.map(GridPoint::row).toSet())
                .filterNot(protectedPoints::contains)
                .toSet()
        }
    }

    private fun collectPreviewBurstImpact(
        board: BoardMatrix,
        pendingTriggers: List<TriggeredSpecial>,
        originalOccupiedPoints: Set<GridPoint>,
    ): PreviewImpactResult {
        var resolvedBoard = board
        val impactedPoints = mutableSetOf<GridPoint>()
        val visited = mutableSetOf<GridPoint>()
        val queue = ArrayDeque(pendingTriggers)

        while (queue.isNotEmpty()) {
            val trigger = queue.removeFirst()
            if (!visited.add(trigger.point)) continue

            val effect = buildTriggeredEffect(resolvedBoard, trigger)
            impactedPoints += previewImpactPointsForEffect(
                board = resolvedBoard,
                effect = effect,
            ).filter(originalOccupiedPoints::contains)
            val nextTriggers = collectTriggeredSpecialsForEffect(
                board = resolvedBoard,
                effect = effect,
            )
            resolvedBoard = applyTriggeredEffect(resolvedBoard, effect)
            nextTriggers.filterNot { it.point in visited }.forEach(queue::addLast)
        }

        return PreviewImpactResult(
            board = resolvedBoard,
            impactedPoints = impactedPoints,
        )
    }

    private fun buildTriggeredEffect(
        board: BoardMatrix,
        trigger: TriggeredSpecial,
    ): TriggeredEffect = when (trigger.special) {
        SpecialBlockType.None -> TriggeredEffect()
        SpecialBlockType.ColumnClearer -> TriggeredEffect(
            clearedColumns = setOf(trigger.point.column),
        )
        SpecialBlockType.RowClearer -> TriggeredEffect(
            clearedRows = setOf(trigger.point.row - 1, trigger.point.row + 1).filter { it in 0 until board.rows }.toSet(),
        )
        SpecialBlockType.Ghost -> TriggeredEffect(
            clearedPoints = board.occupiedNeighborPoints(trigger.point, radius = 1).toSet(),
        )
        SpecialBlockType.Heavy -> TriggeredEffect(
            pushedColumns = setOf(trigger.point.column - 1, trigger.point.column, trigger.point.column + 1)
                .filter { it in 0 until board.columns }
                .toSet(),
        )
    }

    private fun collectTriggeredSpecialsForEffect(
        board: BoardMatrix,
        effect: TriggeredEffect,
    ): List<TriggeredSpecial> {
        val pointsToScan = buildList {
            addAll(board.occupiedPointsInRows(effect.clearedRows))
            addAll(board.occupiedPointsInColumns(effect.clearedColumns))
            addAll(effect.clearedPoints)
        }.distinct()
        return collectTriggeredSpecialsFromPoints(board = board, points = pointsToScan)
    }

    private fun previewImpactPointsForEffect(
        board: BoardMatrix,
        effect: TriggeredEffect,
    ): Set<GridPoint> = buildSet {
        addAll(board.occupiedPointsInRows(effect.clearedRows))
        addAll(board.occupiedPointsInColumns(effect.clearedColumns))
        addAll(effect.clearedPoints.filter { point -> board.isOccupied(point.column, point.row) })
    }

    private fun applyTriggeredEffect(
        board: BoardMatrix,
        effect: TriggeredEffect,
    ): BoardMatrix {
        var nextBoard = board
        if (effect.clearedRows.isNotEmpty()) {
            nextBoard = nextBoard.clearRows(effect.clearedRows)
        }
        if (effect.clearedColumns.isNotEmpty()) {
            nextBoard = nextBoard.clearColumns(effect.clearedColumns)
        }
        if (effect.clearedPoints.isNotEmpty()) {
            nextBoard = nextBoard.clearPoints(effect.clearedPoints)
        }
        if (effect.pushedColumns.isNotEmpty()) {
            nextBoard = nextBoard.pushColumnsDown(effect.pushedColumns)
        }
        return nextBoard
    }

    private fun BoardMatrix.occupiedPoints(): List<GridPoint> = buildList {
        for (row in 0 until rows) {
            for (column in 0 until columns) {
                if (isOccupied(column, row)) {
                    add(GridPoint(column = column, row = row))
                }
            }
        }
    }

    private fun ensurePlayableQueueAfterRevive(
        board: BoardMatrix,
        activePiece: Piece?,
        nextQueue: List<Piece>,
        level: Int,
        launchBar: LaunchBarState,
        config: GameConfig,
    ): QueueAdvance {
        activePiece
            ?.takeIf { hasAnyValidPlacement(board = board, piece = it, config = config) }
            ?.let { return QueueAdvance(activePiece = it, nextQueue = nextQueue) }

        val nextPlayableIndex = nextQueue.indexOfFirst { piece ->
            hasAnyValidPlacement(board = board, piece = piece, config = config)
        }
        if (nextPlayableIndex >= 0) {
            val nextActivePiece = nextQueue[nextPlayableIndex]
            val reorderedQueue = nextQueue.toMutableList().apply { removeAt(nextPlayableIndex) }
            return QueueAdvance(
                activePiece = nextActivePiece,
                nextQueue = refillQueue(
                    queue = reorderedQueue,
                    level = level,
                    launchBar = launchBar,
                ),
            )
        }

        repeat(PlayablePieceGenerationAttempts) {
            val candidate = createPiece(level = level, launchBar = launchBar)
            if (hasAnyValidPlacement(board = board, piece = candidate, config = config)) {
                return QueueAdvance(
                    activePiece = candidate,
                    nextQueue = refillQueue(
                        queue = nextQueue,
                        level = level,
                        launchBar = launchBar,
                    ),
                )
            }
        }

        return QueueAdvance(
            activePiece = createFallbackPlayablePiece(board = board, config = config),
            nextQueue = refillQueue(
                queue = nextQueue,
                level = level,
                launchBar = launchBar,
            ),
        )
    }

    private fun createFallbackPlayablePiece(
        board: BoardMatrix,
        config: GameConfig,
    ): Piece {
        val candidateKinds = listOf(PieceKind.Domino, PieceKind.TriL, PieceKind.Square, PieceKind.T, PieceKind.I)
        candidateKinds.forEach { kind ->
            repeat(4) { rotation ->
                val rotatedCells = rotateAndNormalize(kind.template, rotation)
                val candidate = Piece(
                    id = nextPieceId++,
                    kind = kind,
                    tone = kind.tone,
                    cells = rotatedCells,
                    width = rotatedCells.maxOf { it.column } + 1,
                    height = rotatedCells.maxOf { it.row } + 1,
                    special = SpecialBlockType.None,
                )
                if (hasAnyValidPlacement(board = board, piece = candidate, config = config)) {
                    return candidate
                }
            }
        }

        return Piece(
            id = nextPieceId++,
            kind = PieceKind.Domino,
            tone = PieceKind.Domino.tone,
            cells = listOf(GridPoint(0, 0), GridPoint(1, 0)),
            width = 2,
            height = 1,
            special = SpecialBlockType.None,
        )
    }

    private fun selectRewardRows(
        board: BoardMatrix,
        totalRows: Int,
        maxRowsToClear: Int,
    ): Set<Int> {
        if (totalRows <= 0 || maxRowsToClear <= 0) return emptySet()

        val allRows = (0 until totalRows).toList()
        val occupiedRows = allRows.filter { row ->
            (0 until board.columns).any { column -> board.isOccupied(column, row) }
        }.shuffled(random)
        val emptyRows = allRows.filterNot(occupiedRows::contains).shuffled(random)

        return (occupiedRows + emptyRows)
            .take(maxRowsToClear.coerceAtMost(totalRows))
            .toSet()
    }

    private fun promoteQueue(
        queue: List<Piece>,
        level: Int,
        launchBar: LaunchBarState,
    ): QueueAdvance {
        val nextActive = queue.firstOrNull() ?: createPiece(level = level, launchBar = launchBar)
        val replenishedQueue = refillQueue(
            queue = queue.drop(1),
            level = level,
            launchBar = launchBar,
        )
        return QueueAdvance(activePiece = nextActive, nextQueue = replenishedQueue)
    }

    private fun refillQueue(
        queue: List<Piece>,
        level: Int,
        launchBar: LaunchBarState,
    ): List<Piece> {
        val next = queue.toMutableList()
        while (next.size < QueueSize) {
            next += createPiece(level = level, launchBar = launchBar)
        }
        return next.toList()
    }

    private fun createPiece(
        level: Int,
        launchBar: LaunchBarState,
    ): Piece {
        val availableKinds = PieceKind.entries.filter { it.unlockLevel <= level }
        val kind = availableKinds.random(random)
        val rotationCount = if (kind == PieceKind.Square || kind == PieceKind.Plus) 0 else random.nextInt(4)
        val rotatedCells = rotateAndNormalize(kind.template, rotationCount)
        val special = rollSpecialBlock(level = level, launchBar = launchBar)
        return Piece(
            id = nextPieceId++,
            kind = kind,
            tone = kind.tone,
            cells = rotatedCells,
            width = rotatedCells.maxOf { it.column } + 1,
            height = rotatedCells.maxOf { it.row } + 1,
            special = special,
        )
    }

    private fun rollSpecialBlock(
        level: Int,
        launchBar: LaunchBarState,
    ): SpecialBlockType {
        val baseChance = 0.06f + (launchBar.progress * 0.18f) + if (launchBar.boostTurnsRemaining > 0) 0.18f else 0f
        val levelBonus = (level - 1).coerceAtMost(5) * 0.012f
        if (random.nextFloat() > (baseChance + levelBonus).coerceAtMost(0.55f)) {
            return SpecialBlockType.None
        }
        return listOf(
            SpecialBlockType.ColumnClearer,
            SpecialBlockType.RowClearer,
            SpecialBlockType.Ghost,
            SpecialBlockType.Heavy,
        ).random(random)
    }

    private fun rotateAndNormalize(
        cells: List<GridPoint>,
        rotationCount: Int,
    ): List<GridPoint> {
        var rotated = cells
        repeat(rotationCount % 4) {
            rotated = rotated.map { point ->
                GridPoint(
                    column = -point.row,
                    row = point.column,
                )
            }
        }

        val minColumn = rotated.minOf { it.column }
        val minRow = rotated.minOf { it.row }
        return rotated
            .map { point ->
                GridPoint(
                    column = point.column - minColumn,
                    row = point.row - minRow,
                )
            }
            .sortedWith(compareBy(GridPoint::row, GridPoint::column))
    }

    private fun isValidPlacement(
        board: BoardMatrix,
        piece: Piece,
        anchor: GridPoint,
    ): Boolean {
        for (point in piece.cellsAt(anchor)) {
            if (point.column !in 0 until board.columns || point.row !in 0 until board.rows) {
                return false
            }
            if (board.isOccupied(point.column, point.row)) {
                return false
            }
        }
        return true
    }

    private fun canGhostPass(
        board: BoardMatrix,
        piece: Piece,
        anchor: GridPoint,
    ): Boolean {
        var collisionCount = 0
        for (point in piece.cellsAt(anchor)) {
            if (point.column !in 0 until board.columns || point.row !in 0 until board.rows) return false
            if (board.isOccupied(point.column, point.row)) collisionCount += 1
        }
        return collisionCount > 0
    }

    private fun computeColumnPressure(
        board: BoardMatrix,
        config: GameConfig,
    ): List<ColumnPressure> = buildList {
        for (column in 0 until config.columns) {
            val filledCells = board.filledCellCount(column)
            val ratio = if (config.rows == 0) 0f else filledCells / config.rows.toFloat()
            val level = when {
                filledCells >= config.rows -> PressureLevel.Overflow
                ratio >= 0.82f -> PressureLevel.Critical
                ratio >= 0.6f -> PressureLevel.Warning
                else -> PressureLevel.Calm
            }
            add(
                ColumnPressure(
                    column = column,
                    filledCells = filledCells,
                    fillRatio = ratio,
                    level = level,
                )
            )
        }
    }

    private fun advanceLaunchBar(
        current: LaunchBarState,
        clearedRows: Int,
        perfectDrop: Boolean,
    ): LaunchBarState {
        val gain = (0.18f + (clearedRows * 0.08f) + if (perfectDrop) 0.14f else 0f).coerceAtMost(0.55f)
        var progress = current.progress + gain
        var boostTurnsRemaining = (current.boostTurnsRemaining - 1).coerceAtLeast(0)
        if (progress >= 1f) {
            progress -= 1f
            boostTurnsRemaining = maxOf(boostTurnsRemaining, 2)
        }
        return LaunchBarState(
            progress = progress.coerceIn(0f, 1f),
            boostTurnsRemaining = boostTurnsRemaining,
            lastGain = gain,
        )
    }

    private fun computeLevel(
        difficultyStage: Int,
        clearedLines: Int,
        config: GameConfig,
    ): Int = 1 + difficultyStage + (clearedLines / config.linesPerLevel)

    private fun scoreFor(
        clearedRows: Int,
        level: Int,
        comboChain: Int,
        chainDepth: Int,
        perfectDropStreak: Int,
        specialTriggered: Boolean,
        launchBoostActive: Boolean,
    ): Int {
        val placementScore = 18 * level
        val clearBonus = when (min(clearedRows, 4)) {
            0 -> 0
            1 -> 140
            2 -> 340
            3 -> 820
            else -> 1500
        } * level
        val comboBonus = if (comboChain > 1) (comboChain - 1) * 120 * level else 0
        val chainBonus = if (chainDepth > 1) (chainDepth - 1) * 150 * level else 0
        val perfectBonus = if (perfectDropStreak > 0) (90 + (perfectDropStreak - 1) * 45) * level else 0
        val specialBonus = if (specialTriggered) 130 * level else 0
        val launchBonus = if (launchBoostActive) 70 * level else 0
        return placementScore + clearBonus + comboBonus + chainBonus + perfectBonus + specialBonus + launchBonus
    }

    private fun buildFloatingFeedback(
        state: GameState,
        lastMoveScore: Int,
        resolution: ResolutionResult,
        perfectDrop: Boolean,
        nextPerfectStreak: Int,
        token: Long,
    ): FloatingFeedback? {
        val text = when {
            resolution.specialTriggered && resolution.totalLinesCleared > 0 -> gameText(GameTextKey.FeedbackSpecialChain, lastMoveScore)
            resolution.specialTriggered -> gameText(GameTextKey.FeedbackSpecial, lastMoveScore)
            perfectDrop -> gameText(GameTextKey.FeedbackPerfect, nextPerfectStreak, lastMoveScore)
            resolution.chainDepth > 1 -> gameText(GameTextKey.FeedbackChain, resolution.chainDepth, lastMoveScore)
            resolution.totalLinesCleared > 0 -> gameText(GameTextKey.FeedbackClear, lastMoveScore)
            lastMoveScore > 0 -> gameText(GameTextKey.FeedbackScoreOnly, lastMoveScore)
            else -> state.floatingFeedback?.text
        } ?: return null
        val emphasis = when {
            resolution.specialTriggered || perfectDrop || resolution.chainDepth > 1 -> FeedbackEmphasis.Bonus
            resolution.totalLinesCleared > 0 -> FeedbackEmphasis.Info
            else -> FeedbackEmphasis.Info
        }
        return FloatingFeedback(text = text, emphasis = emphasis, token = token)
    }

    private fun normalizeColumn(
        approximateColumn: Int,
        maxColumn: Int,
    ): Int? = if (maxColumn < 0) null else approximateColumn.coerceIn(0, maxColumn)

    private fun invalidMove(state: GameState): GameMoveResult = GameMoveResult(
        state = state,
        events = setOf(GameEvent.InvalidDrop),
    )

    private companion object {
        const val QueueSize = 3
        const val SoftLockMillis = 260L
        const val RewardedReviveRowCount = 6
        const val PlayablePieceGenerationAttempts = 16
    }
}

enum class GameEvent {
    PlacementAccepted,
    InvalidDrop,
    LineClear,
    ChainReaction,
    Combo,
    PerfectDrop,
    HoldUsed,
    SoftLockStarted,
    SoftLockAdjusted,
    SpecialTriggered,
    LaunchBoostCharged,
    PressureCritical,
    GameOver,
    Revived,
    Paused,
    Resumed,
    Restarted,
}

private data class ResolutionResult(
    val board: BoardMatrix,
    val firstClearedRows: Set<Int>,
    val firstClearedColumns: Set<Int>,
    val totalLinesCleared: Int,
    val chainDepth: Int,
    val triggeredSpecialCells: Int,
    val specialTriggered: Boolean,
)

private data class QueueAdvance(
    val activePiece: Piece,
    val nextQueue: List<Piece>,
)

private data class SpecialResolution(
    val board: BoardMatrix,
    val specialTriggered: Boolean,
    val initialTriggeredSpecials: List<TriggeredSpecial>,
    val clearedRows: Set<Int>,
    val clearedColumns: Set<Int>,
)

private data class TriggeredSpecial(
    val point: GridPoint,
    val special: SpecialBlockType,
)

private data class TriggeredEffect(
    val clearedRows: Set<Int> = emptySet(),
    val clearedColumns: Set<Int> = emptySet(),
    val clearedPoints: Set<GridPoint> = emptySet(),
    val pushedColumns: Set<Int> = emptySet(),
)

private data class TriggerBurstResult(
    val board: BoardMatrix,
    val triggeredCount: Int,
)

private data class PreviewImpactResult(
    val board: BoardMatrix,
    val impactedPoints: Set<GridPoint>,
)

data class GameMoveResult(
    val state: GameState,
    val preview: PlacementPreview? = null,
    val events: Set<GameEvent> = emptySet(),
)
