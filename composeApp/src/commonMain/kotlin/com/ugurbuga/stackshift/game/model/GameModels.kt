package com.ugurbuga.stackshift.game.model

import androidx.compose.runtime.Immutable
import kotlin.math.max

enum class AppLanguage(
    val localeTag: String,
    val endonym: String,
) {
    English(localeTag = "en", endonym = "English"),
    Turkish(localeTag = "tr", endonym = "Türkçe"),
    Spanish(localeTag = "es", endonym = "Español"),
    French(localeTag = "fr", endonym = "Français"),
    German(localeTag = "de", endonym = "Deutsch"),
    Russian(localeTag = "ru", endonym = "Русский"),
}

enum class AppThemeMode(
    val isDark: Boolean?,
) {
    System(isDark = null),
    Light(isDark = false),
    Dark(isDark = true),
}

enum class AppColorPalette {
    Classic,
    Aurora,
    Sunset,
}

enum class BlockColorPalette {
    Classic,
    Candy,
    Neon,
    Earth,
}

enum class BlockVisualStyle {
    Flat,
    Bubble,
    Outline,
    Sharp3D,
    Wood,
    LiquidGlass,
    Neon,
}

enum class BoardBlockStyleMode {
    AlwaysFlat,
    MatchSelectedBlockStyle,
}

@Immutable
data class GridPoint(
    val column: Int,
    val row: Int,
) {
    operator fun plus(other: GridPoint): GridPoint =
        GridPoint(column = column + other.column, row = row + other.row)
}

@Immutable
data class GameConfig(
    val columns: Int = 10,
    val rows: Int = 12,
    val difficultyIntervalSeconds: Int = 18,
    val linesPerLevel: Int = 6,
)

enum class GameStatus {
    Running,
    Paused,
    GameOver,
}

enum class SpecialBlockType {
    None,
    ColumnClearer,
    RowClearer,
    Ghost,
    Heavy,
}

enum class PressureLevel {
    Calm,
    Warning,
    Critical,
    Overflow,
}

enum class FeedbackEmphasis {
    Info,
    Bonus,
    Danger,
}

enum class GameTextKey {
    AppTitle,
    Hold,
    Pause,
    Resume,
    Restart,
    RestartConfirmTitle,
    RestartConfirmBody,
    RestartConfirm,
    RestartCancel,
    Score,
    HighScore,
    Lines,
    Boost,
    Danger,
    DangerNone,
    Launch,
    LaunchBar,
    LaunchBoostActive,
    LaunchSpecialChance,
    LaunchSoftLockMessage,
    LaunchChainMessage,
    LaunchPaused,
    LaunchGameOver,
    LaunchDragHint,
    QueueHold,
    QueueNextShort,
    QueueEmpty,
    PauseTitle,
    GameOverTitle,
    Continue,
    PlayAgain,
    GameMessageSelectColumn,
    GameMessageNoOpening,
    GameMessageSoftLock,
    GameMessageOverflow,
    GameMessageSpecialChainBoard,
    GameMessagePressureGameOver,
    GameMessageSpecialLines,
    GameMessageSpecialTriggered,
    GameMessagePerfectDrop,
    GameMessageChainLines,
    GameMessageLinesCleared,
    GameMessageGoodShot,
    GameMessageHoldUpdated,
    GameMessageTempoCritical,
    GameMessageTempoUp,
    GameMessagePaused,
    GameMessageResumed,
    FeedbackOverflow,
    FeedbackPerfectLane,
    FeedbackMicroAdjust,
    FeedbackSoftLock,
    FeedbackHoldArmed,
    FeedbackSwap,
    FeedbackSpecialChain,
    FeedbackSpecial,
    FeedbackPerfect,
    FeedbackChain,
    FeedbackClear,
    FeedbackScoreOnly,
    SpecialColumnClearer,
    SpecialRowClearer,
    SpecialGhost,
    SpecialHeavy,
}

@Immutable
data class GameText(
    val key: GameTextKey,
    val args: List<String> = emptyList(),
)

enum class CellTone {
    Cyan,
    Gold,
    Violet,
    Emerald,
    Coral,
    Blue,
    Rose,
    Lime,
    Amber,
}

enum class PieceKind(
    val unlockLevel: Int,
    val tone: CellTone,
    val template: List<GridPoint>,
) {
    Domino(
        unlockLevel = 1,
        tone = CellTone.Cyan,
        template = listOf(
            GridPoint(0, 0),
            GridPoint(1, 0),
        ),
    ),
    TriL(
        unlockLevel = 1,
        tone = CellTone.Gold,
        template = listOf(
            GridPoint(0, 0),
            GridPoint(0, 1),
            GridPoint(1, 1),
        ),
    ),
    Square(
        unlockLevel = 1,
        tone = CellTone.Violet,
        template = listOf(
            GridPoint(0, 0),
            GridPoint(1, 0),
            GridPoint(0, 1),
            GridPoint(1, 1),
        ),
    ),
    T(
        unlockLevel = 2,
        tone = CellTone.Emerald,
        template = listOf(
            GridPoint(0, 0),
            GridPoint(1, 0),
            GridPoint(2, 0),
            GridPoint(1, 1),
        ),
    ),
    L(
        unlockLevel = 2,
        tone = CellTone.Coral,
        template = listOf(
            GridPoint(0, 0),
            GridPoint(0, 1),
            GridPoint(0, 2),
            GridPoint(1, 2),
        ),
    ),
    J(
        unlockLevel = 2,
        tone = CellTone.Blue,
        template = listOf(
            GridPoint(1, 0),
            GridPoint(1, 1),
            GridPoint(1, 2),
            GridPoint(0, 2),
        ),
    ),
    S(
        unlockLevel = 3,
        tone = CellTone.Rose,
        template = listOf(
            GridPoint(1, 0),
            GridPoint(2, 0),
            GridPoint(0, 1),
            GridPoint(1, 1),
        ),
    ),
    Z(
        unlockLevel = 3,
        tone = CellTone.Lime,
        template = listOf(
            GridPoint(0, 0),
            GridPoint(1, 0),
            GridPoint(1, 1),
            GridPoint(2, 1),
        ),
    ),
    I(
        unlockLevel = 4,
        tone = CellTone.Amber,
        template = listOf(
            GridPoint(0, 0),
            GridPoint(1, 0),
            GridPoint(2, 0),
            GridPoint(3, 0),
        ),
    ),
    Plus(
        unlockLevel = 5,
        tone = CellTone.Cyan,
        template = listOf(
            GridPoint(1, 0),
            GridPoint(0, 1),
            GridPoint(1, 1),
            GridPoint(2, 1),
            GridPoint(1, 2),
        ),
    ),
}

@Immutable
data class Piece(
    val id: Long,
    val kind: PieceKind,
    val tone: CellTone,
    val cells: List<GridPoint>,
    val width: Int,
    val height: Int,
    val special: SpecialBlockType = SpecialBlockType.None,
) {
    fun cellsAt(anchor: GridPoint): List<GridPoint> = cells.map(anchor::plus)
}

@Immutable
data class ComboState(
    val chain: Int = 0,
    val best: Int = 0,
) {
    val multiplier: Int
        get() = max(1, chain)
}

@Immutable
data class ColumnPressure(
    val column: Int,
    val filledCells: Int,
    val fillRatio: Float,
    val level: PressureLevel,
)

@Immutable
data class LaunchBarState(
    val progress: Float = 0f,
    val boostTurnsRemaining: Int = 0,
    val lastGain: Float = 0f,
)

@Immutable
data class SoftLockState(
    val pieceId: Long,
    val preview: PlacementPreview,
    val remainingMillis: Long,
    val revision: Long,
)

@Immutable
data class FloatingFeedback(
    val text: GameText,
    val emphasis: FeedbackEmphasis,
    val token: Long,
)

@Immutable
data class BoardCell(
    val tone: CellTone,
    val special: SpecialBlockType,
)

class BoardMatrix private constructor(
    val columns: Int,
    val rows: Int,
    private val cells: IntArray,
) {
    fun cellAt(column: Int, row: Int): BoardCell? {
        if (column !in 0 until columns || row !in 0 until rows) return null
        val encoded = cells[indexOf(column, row)]
        return encoded.takeIf { it != EMPTY_CELL }?.let(::decodeCell)
    }

    fun toneAt(column: Int, row: Int): CellTone? {
        return cellAt(column, row)?.tone
    }

    fun specialAt(column: Int, row: Int): SpecialBlockType? = cellAt(column, row)?.special

    fun isOccupied(column: Int, row: Int): Boolean = toneAt(column, row) != null

    fun occupiedPointsInRows(rowsToRead: Set<Int>): List<GridPoint> = buildList {
        rowsToRead.forEach { row ->
            if (row !in 0 until rows) return@forEach
            for (column in 0 until columns) {
                if (cells[indexOf(column, row)] != EMPTY_CELL) {
                    add(GridPoint(column = column, row = row))
                }
            }
        }
    }

    fun occupiedPointsInColumns(columnsToRead: Set<Int>): List<GridPoint> = buildList {
        columnsToRead.forEach { column ->
            if (column !in 0 until columns) return@forEach
            for (row in 0 until rows) {
                if (cells[indexOf(column, row)] != EMPTY_CELL) {
                    add(GridPoint(column = column, row = row))
                }
            }
        }
    }

    fun occupiedNeighborPoints(center: GridPoint, radius: Int = 1): List<GridPoint> = buildList {
        for (row in (center.row - radius)..(center.row + radius)) {
            if (row !in 0 until rows) continue
            for (column in (center.column - radius)..(center.column + radius)) {
                if (column !in 0 until columns) continue
                if (column == center.column && row == center.row) continue
                if (cells[indexOf(column, row)] != EMPTY_CELL) {
                    add(GridPoint(column = column, row = row))
                }
            }
        }
    }

    fun topOccupiedRow(column: Int): Int? {
        if (column !in 0 until columns) return null
        for (row in 0 until rows) {
            if (cells[indexOf(column, row)] != EMPTY_CELL) return row
        }
        return null
    }

    fun filledCellCount(column: Int): Int {
        if (column !in 0 until columns) return 0
        var count = 0
        for (row in 0 until rows) {
            if (cells[indexOf(column, row)] != EMPTY_CELL) count += 1
        }
        return count
    }

    fun columnHeight(column: Int): Int = filledCellCount(column)

    fun isColumnEmpty(column: Int): Boolean = topOccupiedRow(column) == null

    fun clearColumns(columnsToClear: Set<Int>): BoardMatrix {
        if (columnsToClear.isEmpty()) return this
        val next = cells.copyOf()
        columnsToClear.forEach { column ->
            if (column !in 0 until columns) return@forEach
            for (row in 0 until rows) {
                next[indexOf(column, row)] = EMPTY_CELL
            }
        }
        return BoardMatrix(columns = columns, rows = rows, cells = next)
    }

    fun clearPoints(points: Set<GridPoint>): BoardMatrix {
        if (points.isEmpty()) return this
        val next = cells.copyOf()
        points.forEach { point ->
            if (point.column !in 0 until columns || point.row !in 0 until rows) return@forEach
            next[indexOf(point.column, point.row)] = EMPTY_CELL
        }
        return BoardMatrix(columns = columns, rows = rows, cells = next)
    }

    fun pushColumnsDown(columnsToPush: Set<Int>, protectedPoints: Set<GridPoint> = emptySet()): BoardMatrix {
        if (columnsToPush.isEmpty()) return this
        val next = cells.copyOf()
        val protectedLookup = protectedPoints.groupBy(GridPoint::column).mapValues { entry -> entry.value.map(GridPoint::row).toSet() }
        columnsToPush.forEach { column ->
            if (column !in 0 until columns) return@forEach
            val protectedRows = protectedLookup[column].orEmpty()
            for (row in rows - 2 downTo 0) {
                val currentIndex = indexOf(column, row)
                val belowIndex = indexOf(column, row + 1)
                if (row in protectedRows || (row + 1) in protectedRows) continue
                if (next[currentIndex] == EMPTY_CELL || next[belowIndex] != EMPTY_CELL) continue
                next[belowIndex] = next[currentIndex]
                next[currentIndex] = EMPTY_CELL
            }
        }
        return BoardMatrix(columns = columns, rows = rows, cells = next)
    }

    fun fill(
        points: List<GridPoint>,
        tone: CellTone,
        special: SpecialBlockType = SpecialBlockType.None,
    ): BoardMatrix {
        val next = cells.copyOf()
        val encoded = encodeCell(tone = tone, special = special)
        points.forEach { point ->
            next[indexOf(point.column, point.row)] = encoded
        }
        return BoardMatrix(columns = columns, rows = rows, cells = next)
    }

    fun fullRows(): List<Int> = buildList {
        for (row in 0 until rows) {
            var isFull = true
            for (column in 0 until columns) {
                if (cells[indexOf(column, row)] == EMPTY_CELL) {
                    isFull = false
                    break
                }
            }
            if (isFull) add(row)
        }
    }

    fun clearRows(rowsToClear: Set<Int>): BoardMatrix {
        if (rowsToClear.isEmpty()) return this

        val next = IntArray(cells.size) { EMPTY_CELL }
        for (column in 0 until columns) {
            var clearedRowsAbove = 0
            for (sourceRow in 0 until rows) {
                if (sourceRow in rowsToClear) {
                    clearedRowsAbove += 1
                    continue
                }

                val cellValue = cells[indexOf(column, sourceRow)]
                if (cellValue == EMPTY_CELL) continue

                val targetRow = sourceRow - clearedRowsAbove
                next[indexOf(column, targetRow)] = cellValue
            }
        }

        return BoardMatrix(columns = columns, rows = rows, cells = next)
    }

    private fun indexOf(column: Int, row: Int): Int = row * columns + column

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BoardMatrix) return false
        return columns == other.columns && rows == other.rows && cells.contentEquals(other.cells)
    }

    override fun hashCode(): Int {
        var result = columns
        result = 31 * result + rows
        result = 31 * result + cells.contentHashCode()
        return result
    }

    companion object {
        private const val EMPTY_CELL = -1
        private const val TONE_BITS = 8
        private const val SPECIAL_SHIFT = TONE_BITS

        fun empty(columns: Int, rows: Int): BoardMatrix =
            BoardMatrix(
                columns = columns,
                rows = rows,
                cells = IntArray(columns * rows) { EMPTY_CELL },
            )

        private fun encodeCell(
            tone: CellTone,
            special: SpecialBlockType,
        ): Int = tone.ordinal or (special.ordinal shl SPECIAL_SHIFT)

        private fun decodeCell(encoded: Int): BoardCell = BoardCell(
            tone = CellTone.entries[encoded and 0xFF],
            special = SpecialBlockType.entries[(encoded shr SPECIAL_SHIFT) and 0xFF],
        )
    }
}

@Immutable
data class PlacementPreview(
    val selectedColumn: Int,
    val entryAnchor: GridPoint,
    val landingAnchor: GridPoint,
    val occupiedCells: List<GridPoint>,
    val coveredColumns: IntRange,
    val isPerfectDrop: Boolean = false,
)

@Immutable
data class GameState(
    val config: GameConfig,
    val board: BoardMatrix,
    val activePiece: Piece?,
    val nextQueue: List<Piece>,
    val holdPiece: Piece? = null,
    val canHold: Boolean = true,
    val lastPlacementColumn: Int? = null,
    val score: Int,
    val lastMoveScore: Int = 0,
    val linesCleared: Int,
    val level: Int,
    val difficultyStage: Int,
    val secondsUntilDifficultyIncrease: Int,
    val combo: ComboState = ComboState(),
    val perfectDropStreak: Int = 0,
    val launchBar: LaunchBarState = LaunchBarState(),
    val columnPressure: List<ColumnPressure> = emptyList(),
    val softLock: SoftLockState? = null,
    val status: GameStatus = GameStatus.Running,
    val recentlyClearedRows: Set<Int> = emptySet(),
    val lastResolvedLines: Int = 0,
    val lastChainDepth: Int = 0,
    val specialChainCount: Int = 0,
    val clearAnimationToken: Long = 0L,
    val screenShakeToken: Long = 0L,
    val impactFlashToken: Long = 0L,
    val comboPopupToken: Long = 0L,
    val floatingFeedback: FloatingFeedback? = null,
    val feedbackToken: Long = 0L,
    val message: GameText = GameText(GameTextKey.GameMessageSelectColumn),
) {
    val nextPiece: Piece?
        get() = nextQueue.firstOrNull()

    val isSoftLockActive: Boolean
        get() = softLock != null

    val criticalColumns: Set<Int>
        get() = columnPressure.filter { it.level == PressureLevel.Critical || it.level == PressureLevel.Overflow }.map(ColumnPressure::column).toSet()
}

fun gameText(
    key: GameTextKey,
    vararg args: Any,
): GameText = GameText(
    key = key,
    args = args.map(Any::toString),
)

