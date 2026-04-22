package com.ugurbuga.stackshift.game.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource
import stackshift.composeapp.generated.resources.Res
import stackshift.composeapp.generated.resources.app_language_arabic
import stackshift.composeapp.generated.resources.app_language_chinese_simplified
import stackshift.composeapp.generated.resources.app_language_english
import stackshift.composeapp.generated.resources.app_language_french
import stackshift.composeapp.generated.resources.app_language_german
import stackshift.composeapp.generated.resources.app_language_hindi
import stackshift.composeapp.generated.resources.app_language_indonesian
import stackshift.composeapp.generated.resources.app_language_portuguese
import stackshift.composeapp.generated.resources.app_language_russian
import stackshift.composeapp.generated.resources.app_language_spanish
import stackshift.composeapp.generated.resources.app_language_turkish
import kotlin.math.max

enum class AppLanguage(
    val localeTag: String,
    val labelRes: StringResource,
) {
    English(localeTag = "en", labelRes = Res.string.app_language_english),
    Turkish(localeTag = "tr", labelRes = Res.string.app_language_turkish),
    Spanish(localeTag = "es", labelRes = Res.string.app_language_spanish),
    French(localeTag = "fr", labelRes = Res.string.app_language_french),
    German(localeTag = "de", labelRes = Res.string.app_language_german),
    Russian(localeTag = "ru", labelRes = Res.string.app_language_russian),
    ChineseSimplified(localeTag = "zh-Hans", labelRes = Res.string.app_language_chinese_simplified),
    Hindi(localeTag = "hi", labelRes = Res.string.app_language_hindi),
    Arabic(localeTag = "ar", labelRes = Res.string.app_language_arabic),
    Portuguese(localeTag = "pt", labelRes = Res.string.app_language_portuguese),
    Indonesian(localeTag = "id", labelRes = Res.string.app_language_indonesian),

    ;

    companion object {
        fun fromDeviceLocaleTag(localeTag: String?): AppLanguage? {
            val normalized = localeTag
                ?.trim()
                ?.replace('_', '-')
                ?.takeIf(String::isNotBlank)
                ?: return null

            entries.firstOrNull { it.localeTag.equals(normalized, ignoreCase = true) }?.let { return it }

            val lowerTag = normalized.lowercase()
            when {
                lowerTag == "zh" ||
                        lowerTag.startsWith("zh-cn") ||
                        lowerTag.startsWith("zh-sg") ||
                        lowerTag.startsWith("zh-hans") -> return ChineseSimplified

                lowerTag.startsWith("zh-tw") ||
                        lowerTag.startsWith("zh-hk") ||
                        lowerTag.startsWith("zh-mo") ||
                        lowerTag.startsWith("zh-hant") -> return null
            }

            val languageCode = lowerTag.substringBefore('-')
            return entries.firstOrNull { candidate ->
                candidate.localeTag.lowercase().substringBefore('-') == languageCode
            }
        }
    }
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
    ModernNeon,
    SoftPastel,
    MinimalMonochrome,
}

enum class BlockColorPalette {
    Classic,
    Candy,
    Neon,
    Earth,
    Monochrome,
}

enum class BlockVisualStyle {
    Flat,
    Bubble,
    Outline,
    Sharp3D,
    Wood,
    PixelArt,
    Crystal,
    DynamicLiquid,
    MatteSoft,
    NeonGlow,
    Metallic,
    StoneTexture,
    HoneycombTexture,
    LightBurst,
    LiquidMarble,
    Lava,
    SpiderWeb,
    Cosmic,
    Bamboo,
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
    LaunchGameOver,
    LaunchDragHint,
    QueueHold,
    QueueNextShort,
    QueueEmpty,
    GameOverTitle,
    Continue,
    GameOverExtraLife,
    GameOverExtraLifeLoading,
    PlayAgain,
    GameOverNewHighScore,
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
    GameMessageExtraLifeUsed,
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
    FeedbackExtraLife,
    SpecialColumnClearer,
    SpecialRowClearer,
    SpecialGhost,
    SpecialHeavy,
    PiecePropertiesNone,
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
) {
    val isBoostActive: Boolean get() = boostTurnsRemaining > 0
    val specialPieceChance: Float get() = (0.06f + (progress * 0.18f) + if (isBoostActive) 0.18f else 0f).coerceAtMost(0.55f)
}

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

    val occupiedCount: Int by lazy {
        var count = 0
        for (i in cells.indices) {
            if (cells[i] != EMPTY_CELL) count++
        }
        count
    }

    fun isEmpty(): Boolean = occupiedCount == 0

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
    val clearedRows: Set<Int> = emptySet(),
    val clearedColumns: Set<Int> = emptySet(),
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
    val recentlyClearedColumns: Set<Int> = emptySet(),
    val lastResolvedLines: Int = 0,
    val lastChainDepth: Int = 0,
    val specialChainCount: Int = 0,
    val clearAnimationToken: Long = 0L,
    val screenShakeToken: Long = 0L,
    val impactFlashToken: Long = 0L,
    val comboPopupToken: Long = 0L,
    val floatingFeedback: FloatingFeedback? = null,
    val feedbackToken: Long = 0L,
    val rewardedReviveUsed: Boolean = false,
    val lastActionTime: Long = 0L,
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

fun GridPoint.toTopLeft(
    boardRect: Rect,
    cellSizePx: Float,
): Offset = Offset(
    x = boardRect.left + (column * cellSizePx),
    y = boardRect.top + (row * cellSizePx),
)

fun CellTone.paletteColor(palette: BlockColorPalette): Color = when (palette) {
    BlockColorPalette.Classic -> when (this) {
        CellTone.Cyan -> Color(0xFF4FC3F7)
        CellTone.Gold -> Color(0xFFFFD166)
        CellTone.Violet -> Color(0xFF9B8CFF)
        CellTone.Emerald -> Color(0xFF57E389)
        CellTone.Coral -> Color(0xFFFF7A90)
        CellTone.Blue -> Color(0xFF6AA7FF)
        CellTone.Rose -> Color(0xFFFF8FAB)
        CellTone.Lime -> Color(0xFFB8F15F)
        CellTone.Amber -> Color(0xFFFFB74D)
    }

    BlockColorPalette.Candy -> when (this) {
        CellTone.Cyan -> Color(0xFFFF7A90)
        CellTone.Gold -> Color(0xFFFFD166)
        CellTone.Violet -> Color(0xFFC77DFF)
        CellTone.Emerald -> Color(0xFF7AE582)
        CellTone.Coral -> Color(0xFF9B8CFF)
        CellTone.Blue -> Color(0xFF5BC0EB)
        CellTone.Rose -> Color(0xFFFFB5E8)
        CellTone.Lime -> Color(0xFFFEE440)
        CellTone.Amber -> Color(0xFFF7B267)
    }

    BlockColorPalette.Neon -> when (this) {
        CellTone.Cyan -> Color(0xFF00F5D4)
        CellTone.Gold -> Color(0xFFFFE66D)
        CellTone.Violet -> Color(0xFF9B5DE5)
        CellTone.Emerald -> Color(0xFF00F5A0)
        CellTone.Coral -> Color(0xFFFF5C8A)
        CellTone.Blue -> Color(0xFF00BBF9)
        CellTone.Rose -> Color(0xFFFF4D6D)
        CellTone.Lime -> Color(0xFFB9F700)
        CellTone.Amber -> Color(0xFFFFBE0B)
    }

    BlockColorPalette.Earth -> when (this) {
        CellTone.Cyan -> Color(0xFF4D908E)
        CellTone.Gold -> Color(0xFFE9C46A)
        CellTone.Violet -> Color(0xFF8D6A9F)
        CellTone.Emerald -> Color(0xFF7A9E7E)
        CellTone.Coral -> Color(0xFFCE8460)
        CellTone.Blue -> Color(0xFF5E7CE2)
        CellTone.Rose -> Color(0xFFB56B83)
        CellTone.Lime -> Color(0xFFA7C957)
        CellTone.Amber -> Color(0xFFDDA15E)
    }

    BlockColorPalette.Monochrome -> when (this) {
        CellTone.Cyan -> Color(0xFFF1F5F9)
        CellTone.Gold -> Color(0xFFDCE3EA)
        CellTone.Violet -> Color(0xFFC4CDD6)
        CellTone.Emerald -> Color(0xFFADB7C2)
        CellTone.Coral -> Color(0xFF97A1AC)
        CellTone.Blue -> Color(0xFF818B96)
        CellTone.Rose -> Color(0xFF6C7682)
        CellTone.Lime -> Color(0xFF59616E)
        CellTone.Amber -> Color(0xFF464D59)
    }
}

fun resolveBoardBlockStyle(
    selectedStyle: BlockVisualStyle,
    mode: BoardBlockStyleMode,
): BlockVisualStyle = when (mode) {
    BoardBlockStyleMode.AlwaysFlat -> BlockVisualStyle.Flat
    BoardBlockStyleMode.MatchSelectedBlockStyle -> normalizeBlockVisualStyle(selectedStyle)
}

fun normalizeBlockVisualStyle(style: BlockVisualStyle): BlockVisualStyle = when (style) {
    BlockVisualStyle.MatteSoft -> BlockVisualStyle.Flat
    BlockVisualStyle.NeonGlow -> BlockVisualStyle.Bubble
    BlockVisualStyle.StoneTexture -> BlockVisualStyle.Wood
    BlockVisualStyle.LightBurst -> BlockVisualStyle.Outline
    BlockVisualStyle.LiquidMarble -> BlockVisualStyle.Crystal
    else -> style
}

fun boardSpecialIcon(type: SpecialBlockType): ImageVector = when (type) {
    SpecialBlockType.ColumnClearer -> Icons.Filled.SwapVert
    SpecialBlockType.RowClearer -> Icons.Filled.SwapHoriz
    SpecialBlockType.Ghost -> Icons.Filled.ViewModule
    SpecialBlockType.Heavy -> Icons.Filled.FitnessCenter
    SpecialBlockType.None -> Icons.AutoMirrored.Filled.HelpOutline
}
