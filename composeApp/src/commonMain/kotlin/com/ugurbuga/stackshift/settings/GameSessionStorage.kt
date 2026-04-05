package com.ugurbuga.stackshift.settings

import com.ugurbuga.stackshift.game.model.BoardCell
import com.ugurbuga.stackshift.game.model.BoardMatrix
import com.ugurbuga.stackshift.game.model.CellTone
import com.ugurbuga.stackshift.game.model.ComboState
import com.ugurbuga.stackshift.game.model.FloatingFeedback
import com.ugurbuga.stackshift.game.model.GameConfig
import com.ugurbuga.stackshift.game.model.GameState
import com.ugurbuga.stackshift.game.model.GameStatus
import com.ugurbuga.stackshift.game.model.GameText
import com.ugurbuga.stackshift.game.model.GameTextKey
import com.ugurbuga.stackshift.game.model.GridPoint
import com.ugurbuga.stackshift.game.model.LaunchBarState
import com.ugurbuga.stackshift.game.model.Piece
import com.ugurbuga.stackshift.game.model.PieceKind
import com.ugurbuga.stackshift.game.model.PlacementPreview
import com.ugurbuga.stackshift.game.model.PressureLevel
import com.ugurbuga.stackshift.game.model.SoftLockState
import com.ugurbuga.stackshift.game.model.SpecialBlockType

expect object GameSessionStorage {
    fun load(): GameState?
    fun save(state: GameState)
    fun clear()
}

internal object GameSessionCodec {
    private const val Version = 2
    private const val SectionSeparator = '|'
    private const val FieldSeparator = ','
    private const val ListSeparator = ';'
    private const val PieceListSeparator = '~'
    private const val PointSeparator = ':'
    private const val CellSeparator = '.'
    private const val EmptyToken = "-"

    fun encode(state: GameState): String = listOf(
        Version.toString(),
        encodeConfig(state.config),
        encodeBoard(state.board),
        encodePiece(state.activePiece),
        encodePieceList(state.nextQueue),
        encodePiece(state.holdPiece),
        encodeHoldState(state.canHold, state.lastPlacementColumn),
        encodeProgressState(state),
        encodeComboState(state.combo),
        state.perfectDropStreak.toString(),
        encodeLaunchBarState(state.launchBar),
        encodeColumnPressure(state.columnPressure),
        encodeSoftLock(state.softLock),
        state.status.ordinal.toString(),
        encodeIntSet(state.recentlyClearedRows),
        encodeSummaryState(state),
        encodeVisualState(state),
        encodeGameText(state.message),
    ).joinToString(separator = SectionSeparator.toString())

    fun decode(value: String): GameState? {
        val parts = value.split(SectionSeparator, limit = 18)
        if (parts.size != 18 || parts[0].toIntOrNull() != Version) return null

        val config = decodeConfig(parts[1]) ?: return null
        val board = decodeBoard(parts[2]) ?: return null
        val activePiece = decodePiece(parts[3])
        val nextQueue = decodePieceList(parts[4]) ?: return null
        val holdPiece = decodePieceOrNull(parts[5])
        val holdState = decodeHoldState(parts[6]) ?: return null
        val progress = decodeProgressState(parts[7]) ?: return null
        val combo = decodeComboState(parts[8]) ?: return null
        val perfectDropStreak = parts[9].toIntOrNull() ?: return null
        val launchBar = decodeLaunchBarState(parts[10]) ?: return null
        val columnPressure = decodeColumnPressure(parts[11]) ?: return null
        val softLock = decodeSoftLock(parts[12])
        val status = GameStatus.entries.getOrNull(parts[13].toIntOrNull() ?: return null) ?: return null
        val recentlyClearedRows = decodeIntSet(parts[14]) ?: return null
        val summary = decodeSummaryState(parts[15]) ?: return null
        val visual = decodeVisualState(parts[16]) ?: return null
        val message = decodeGameText(parts[17]) ?: GameText(GameTextKey.GameMessageSelectColumn)

        return GameState(
            config = config,
            board = board,
            activePiece = activePiece,
            nextQueue = nextQueue,
            holdPiece = holdPiece,
            canHold = holdState.canHold,
            lastPlacementColumn = holdState.lastPlacementColumn,
            score = progress.score,
            lastMoveScore = progress.lastMoveScore,
            linesCleared = progress.linesCleared,
            level = progress.level,
            difficultyStage = progress.difficultyStage,
            secondsUntilDifficultyIncrease = progress.secondsUntilDifficultyIncrease,
            combo = combo,
            perfectDropStreak = perfectDropStreak,
            launchBar = launchBar,
            columnPressure = columnPressure,
            softLock = softLock,
            status = status,
            recentlyClearedRows = recentlyClearedRows,
            lastResolvedLines = summary.lastResolvedLines,
            lastChainDepth = summary.lastChainDepth,
            specialChainCount = summary.specialChainCount,
            clearAnimationToken = visual.clearAnimationToken,
            screenShakeToken = visual.screenShakeToken,
            impactFlashToken = visual.impactFlashToken,
            comboPopupToken = visual.comboPopupToken,
            floatingFeedback = null,
            feedbackToken = visual.feedbackToken,
            message = message,
        )
    }

    fun maxPieceId(state: GameState): Long = buildList<Long> {
        state.activePiece?.let { add(it.id) }
        state.holdPiece?.let { add(it.id) }
        addAll(state.nextQueue.map(Piece::id))
        state.softLock?.let { add(it.pieceId) }
    }.maxOrNull() ?: 0L

    private fun encodeConfig(config: GameConfig): String = listOf(
        config.columns,
        config.rows,
        config.difficultyIntervalSeconds,
        config.linesPerLevel,
    ).joinToString(separator = FieldSeparator.toString())

    private fun decodeConfig(value: String): GameConfig? {
        val parts = value.split(FieldSeparator)
        if (parts.size != 4) return null
        return GameConfig(
            columns = parts[0].toIntOrNull() ?: return null,
            rows = parts[1].toIntOrNull() ?: return null,
            difficultyIntervalSeconds = parts[2].toIntOrNull() ?: return null,
            linesPerLevel = parts[3].toIntOrNull() ?: return null,
        )
    }

    private fun encodeBoard(board: BoardMatrix): String = buildList {
        add(board.columns.toString())
        add(board.rows.toString())
        for (row in 0 until board.rows) {
            for (column in 0 until board.columns) {
                val cell = board.cellAt(column, row)
                add(
                    cell?.let {
                        "${it.tone.ordinal}$CellSeparator${it.special.ordinal}"
                    } ?: EmptyToken,
                )
            }
        }
    }.joinToString(separator = FieldSeparator.toString())

    private fun decodeBoard(value: String): BoardMatrix? {
        val parts = value.split(FieldSeparator)
        if (parts.size < 2) return null
        val columns = parts[0].toIntOrNull() ?: return null
        val rows = parts[1].toIntOrNull() ?: return null
        val expectedCells = columns * rows
        if (parts.size != expectedCells + 2) return null

        var board = BoardMatrix.empty(columns = columns, rows = rows)
        var index = 2
        for (row in 0 until rows) {
            for (column in 0 until columns) {
                val token = parts[index++]
                if (token == EmptyToken) continue
                val cellParts = token.split(CellSeparator)
                if (cellParts.size != 2) return null
                val tone = CellTone.entries.getOrNull(cellParts[0].toIntOrNull() ?: return null) ?: return null
                val special = SpecialBlockType.entries.getOrNull(cellParts[1].toIntOrNull() ?: return null) ?: return null
                board = board.fill(listOf(GridPoint(column = column, row = row)), tone = tone, special = special)
            }
        }
        return board
    }

    private fun encodePiece(piece: Piece?): String {
        if (piece == null) return EmptyToken
        val cells = piece.cells.joinToString(separator = ListSeparator.toString()) { point ->
            "${point.column}$PointSeparator${point.row}"
        }
        return listOf(
            piece.id.toString(),
            piece.kind.ordinal.toString(),
            piece.tone.ordinal.toString(),
            piece.special.ordinal.toString(),
            cells,
        ).joinToString(separator = FieldSeparator.toString())
    }

    private fun encodePieceList(pieces: List<Piece>): String = pieces.joinToString(separator = PieceListSeparator.toString()) { piece ->
        encodePiece(piece)
    }

    private fun decodePieceOrNull(value: String): Piece? = if (value == EmptyToken || value.isBlank()) null else decodePiece(value)

    private fun decodePieceList(value: String): List<Piece>? {
        if (value.isBlank()) return emptyList()
        return value.split(PieceListSeparator).mapNotNull { token ->
            decodePieceOrNull(token)
        }
    }

    private fun decodePiece(value: String): Piece? {
        val parts = value.split(FieldSeparator)
        if (parts.size != 5) return null
        val id = parts[0].toLongOrNull() ?: return null
        val kind = PieceKind.entries.getOrNull(parts[1].toIntOrNull() ?: return null) ?: return null
        val tone = CellTone.entries.getOrNull(parts[2].toIntOrNull() ?: return null) ?: return null
        val special = SpecialBlockType.entries.getOrNull(parts[3].toIntOrNull() ?: return null) ?: return null
        val cells = decodePoints(parts[4]) ?: return null
        val width = cells.maxOfOrNull(GridPoint::column)?.plus(1) ?: 0
        val height = cells.maxOfOrNull(GridPoint::row)?.plus(1) ?: 0
        return Piece(
            id = id,
            kind = kind,
            tone = tone,
            cells = cells,
            width = width,
            height = height,
            special = special,
        )
    }

    private fun encodeHoldState(canHold: Boolean, lastPlacementColumn: Int?): String = listOf(
        canHold.toString(),
        lastPlacementColumn?.toString() ?: EmptyToken,
    ).joinToString(separator = FieldSeparator.toString())

    private data class HoldState(
        val canHold: Boolean,
        val lastPlacementColumn: Int?,
    )

    private fun decodeHoldState(value: String): HoldState? {
        val parts = value.split(FieldSeparator)
        if (parts.size != 2) return null
        return HoldState(
            canHold = parts[0].toBooleanStrictOrNull() ?: return null,
            lastPlacementColumn = parts[1].takeIf { it != EmptyToken }?.toIntOrNull(),
        )
    }

    private data class ProgressState(
        val score: Int,
        val lastMoveScore: Int,
        val linesCleared: Int,
        val level: Int,
        val difficultyStage: Int,
        val secondsUntilDifficultyIncrease: Int,
    )

    private fun encodeProgressState(state: GameState): String = listOf(
        state.score,
        state.lastMoveScore,
        state.linesCleared,
        state.level,
        state.difficultyStage,
        state.secondsUntilDifficultyIncrease,
    ).joinToString(separator = FieldSeparator.toString())

    private fun decodeProgressState(value: String): ProgressState? {
        val parts = value.split(FieldSeparator)
        if (parts.size != 6) return null
        return ProgressState(
            score = parts[0].toIntOrNull() ?: return null,
            lastMoveScore = parts[1].toIntOrNull() ?: return null,
            linesCleared = parts[2].toIntOrNull() ?: return null,
            level = parts[3].toIntOrNull() ?: return null,
            difficultyStage = parts[4].toIntOrNull() ?: return null,
            secondsUntilDifficultyIncrease = parts[5].toIntOrNull() ?: return null,
        )
    }

    private fun encodeComboState(combo: ComboState): String = listOf(combo.chain, combo.best).joinToString(separator = FieldSeparator.toString())

    private fun decodeComboState(value: String): ComboState? {
        val parts = value.split(FieldSeparator)
        if (parts.size != 2) return null
        return ComboState(
            chain = parts[0].toIntOrNull() ?: return null,
            best = parts[1].toIntOrNull() ?: return null,
        )
    }

    private fun encodeLaunchBarState(state: LaunchBarState): String = listOf(
        state.progress,
        state.boostTurnsRemaining,
        state.lastGain,
    ).joinToString(separator = FieldSeparator.toString())

    private fun decodeLaunchBarState(value: String): LaunchBarState? {
        val parts = value.split(FieldSeparator)
        if (parts.size != 3) return null
        return LaunchBarState(
            progress = parts[0].toFloatOrNull() ?: return null,
            boostTurnsRemaining = parts[1].toIntOrNull() ?: return null,
            lastGain = parts[2].toFloatOrNull() ?: return null,
        )
    }

    private fun encodeColumnPressure(pressures: List<com.ugurbuga.stackshift.game.model.ColumnPressure>): String = pressures.joinToString(separator = ListSeparator.toString()) { pressure ->
        listOf(
            pressure.column,
            pressure.filledCells,
            pressure.fillRatio,
            pressure.level.ordinal,
        ).joinToString(separator = FieldSeparator.toString())
    }

    private fun decodeColumnPressure(value: String): List<com.ugurbuga.stackshift.game.model.ColumnPressure>? {
        if (value.isBlank()) return emptyList()
        return value.split(ListSeparator).mapNotNull { token ->
            val parts = token.split(FieldSeparator)
            if (parts.size != 4) return null
            val level = PressureLevel.entries.getOrNull(parts[3].toIntOrNull() ?: return null) ?: return null
            com.ugurbuga.stackshift.game.model.ColumnPressure(
                column = parts[0].toIntOrNull() ?: return null,
                filledCells = parts[1].toIntOrNull() ?: return null,
                fillRatio = parts[2].toFloatOrNull() ?: return null,
                level = level,
            )
        }
    }

    private fun encodeSoftLock(softLock: SoftLockState?): String {
        if (softLock == null) return EmptyToken
        return listOf(
            softLock.pieceId,
            softLock.remainingMillis,
            softLock.revision,
            encodePreview(softLock.preview),
        ).joinToString(separator = FieldSeparator.toString())
    }

    private fun decodeSoftLock(value: String): SoftLockState? {
        if (value == EmptyToken || value.isBlank()) return null
        val parts = value.split(FieldSeparator, limit = 4)
        if (parts.size != 4) return null
        return SoftLockState(
            pieceId = parts[0].toLongOrNull() ?: return null,
            remainingMillis = parts[1].toLongOrNull() ?: return null,
            revision = parts[2].toLongOrNull() ?: return null,
            preview = decodePreview(parts[3]) ?: return null,
        )
    }

    private fun encodePreview(preview: PlacementPreview): String {
        val occupiedCells = preview.occupiedCells.joinToString(separator = ListSeparator.toString()) { point ->
            "${point.column}$PointSeparator${point.row}"
        }
        return listOf(
            preview.selectedColumn,
            preview.entryAnchor.column,
            preview.entryAnchor.row,
            preview.landingAnchor.column,
            preview.landingAnchor.row,
            preview.coveredColumns.first,
            preview.coveredColumns.last,
            preview.isPerfectDrop,
            occupiedCells,
        ).joinToString(separator = FieldSeparator.toString())
    }

    private fun decodePreview(value: String): PlacementPreview? {
        val parts = value.split(FieldSeparator, limit = 9)
        if (parts.size != 9) return null
        val occupiedCells = decodePoints(parts[8]) ?: return null
        return PlacementPreview(
            selectedColumn = parts[0].toIntOrNull() ?: return null,
            entryAnchor = GridPoint(
                column = parts[1].toIntOrNull() ?: return null,
                row = parts[2].toIntOrNull() ?: return null,
            ),
            landingAnchor = GridPoint(
                column = parts[3].toIntOrNull() ?: return null,
                row = parts[4].toIntOrNull() ?: return null,
            ),
            occupiedCells = occupiedCells,
            coveredColumns = (parts[5].toIntOrNull() ?: return null)..(parts[6].toIntOrNull() ?: return null),
            isPerfectDrop = parts[7].toBooleanStrictOrNull() ?: return null,
        )
    }

    private data class SummaryState(
        val lastResolvedLines: Int,
        val lastChainDepth: Int,
        val specialChainCount: Int,
    )

    private fun encodeSummaryState(state: GameState): String = listOf(
        state.lastResolvedLines,
        state.lastChainDepth,
        state.specialChainCount,
    ).joinToString(separator = FieldSeparator.toString())

    private fun decodeSummaryState(value: String): SummaryState? {
        val parts = value.split(FieldSeparator)
        if (parts.size != 3) return null
        return SummaryState(
            lastResolvedLines = parts[0].toIntOrNull() ?: return null,
            lastChainDepth = parts[1].toIntOrNull() ?: return null,
            specialChainCount = parts[2].toIntOrNull() ?: return null,
        )
    }

    private data class VisualState(
        val clearAnimationToken: Long,
        val screenShakeToken: Long,
        val impactFlashToken: Long,
        val comboPopupToken: Long,
        val feedbackToken: Long,
    )

    private fun encodeVisualState(state: GameState): String = listOf(
        state.clearAnimationToken,
        state.screenShakeToken,
        state.impactFlashToken,
        state.comboPopupToken,
        state.feedbackToken,
    ).joinToString(separator = FieldSeparator.toString())

    private fun decodeVisualState(value: String): VisualState? {
        val parts = value.split(FieldSeparator)
        if (parts.size != 5) return null
        return VisualState(
            clearAnimationToken = parts[0].toLongOrNull() ?: return null,
            screenShakeToken = parts[1].toLongOrNull() ?: return null,
            impactFlashToken = parts[2].toLongOrNull() ?: return null,
            comboPopupToken = parts[3].toLongOrNull() ?: return null,
            feedbackToken = parts[4].toLongOrNull() ?: return null,
        )
    }

    private fun encodeGameText(text: GameText): String = buildList {
        add(text.key.ordinal.toString())
        addAll(text.args)
    }.joinToString(separator = FieldSeparator.toString())

    private fun decodeGameText(value: String): GameText? {
        if (value.isBlank()) return null
        val parts = value.split(FieldSeparator)
        val key = GameTextKey.entries.getOrNull(parts.firstOrNull()?.toIntOrNull() ?: return null) ?: return null
        return GameText(key = key, args = parts.drop(1))
    }

    private fun encodeIntSet(values: Set<Int>): String = values.sorted().joinToString(separator = ListSeparator.toString())

    private fun decodeIntSet(value: String): Set<Int>? {
        if (value.isBlank()) return emptySet()
        return value.split(ListSeparator).mapNotNull { token -> token.toIntOrNull() }.toSet()
    }

    private fun decodePoints(value: String): List<GridPoint>? {
        if (value.isBlank()) return emptyList()
        return value.split(ListSeparator).mapNotNull { token ->
            val parts = token.split(PointSeparator)
            if (parts.size != 2) return null
            GridPoint(
                column = parts[0].toIntOrNull() ?: return null,
                row = parts[1].toIntOrNull() ?: return null,
            )
        }
    }
}
