package com.ugurbuga.stackshift.ui.game

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ugurbuga.stackshift.game.model.BlockColorPalette
import com.ugurbuga.stackshift.game.model.BlockVisualStyle
import com.ugurbuga.stackshift.game.model.BoardBlockStyleMode
import com.ugurbuga.stackshift.game.model.CellTone
import com.ugurbuga.stackshift.game.model.GameState
import com.ugurbuga.stackshift.game.model.GridPoint
import com.ugurbuga.stackshift.game.model.Piece
import com.ugurbuga.stackshift.game.model.PlacementPreview
import com.ugurbuga.stackshift.game.model.PressureLevel
import com.ugurbuga.stackshift.game.model.SpecialBlockType
import com.ugurbuga.stackshift.localization.LocalAppSettings
import com.ugurbuga.stackshift.ui.theme.StackShiftThemeTokens

private const val PreviewLineAlpha = 0.16f

@Composable
fun BoardGrid(
    modifier: Modifier = Modifier,
    gameState: GameState,
    preview: PlacementPreview?,
    impactedPreviewCells: Set<GridPoint>,
    activeColumn: Int?,
    activePiece: Piece?,
    isColumnValid: Boolean,
    isDragging: Boolean,
) {
    val settings = LocalAppSettings.current
    val colorScheme = MaterialTheme.colorScheme
    val uiColors = StackShiftThemeTokens.uiColors
    val boardBlockStyle = settings.blockVisualStyle
    var clearFlashAlpha by remember { mutableFloatStateOf(0f) }

    val dangerTransition = rememberInfiniteTransition(label = "dangerTransition")
    val dangerPulse by dangerTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 900, easing = FastOutSlowInEasing)),
        label = "dangerPulseAlpha",
    )
    val dangerSweep by dangerTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing)),
        label = "dangerSweep",
    )
    val impactPulse by dangerTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 560, easing = FastOutSlowInEasing)),
        label = "impactPulse",
    )
    val impactSweep by dangerTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 920, easing = FastOutSlowInEasing)),
        label = "impactSweep",
    )

    LaunchedEffect(gameState.clearAnimationToken) {
        if (gameState.recentlyClearedRows.isEmpty()) return@LaunchedEffect
        animate(
            initialValue = 0.9f,
            targetValue = 0f,
            animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        ) { value, _ ->
            clearFlashAlpha = value
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(uiColors.gameSurface)
    ) {
        val cellWidth = maxWidth / gameState.config.columns
        val cellHeight = maxHeight / gameState.config.rows
        val cellWidthPx = with(LocalDensity.current) { cellWidth.toPx() }
        val cellHeightPx = with(LocalDensity.current) { cellHeight.toPx() }
        val cellVisual = boardCellVisual(cellWidth = cellWidthPx, cellHeight = cellHeightPx)
        val cornerRadius = cellVisual.cornerRadius

        Canvas(modifier = Modifier.fillMaxSize()) {
            val boardWidthPx = size.width
            val boardHeightPx = size.height

            val pieceToneColor = activePiece?.tone?.paletteColor(settings.blockColorPalette)
            val effectivePreviewColor = pieceToneColor ?: uiColors.success
            val previewColumnBounds = preview?.occupiedCells
                ?.groupBy(GridPoint::column)
                ?.mapValues { (_, points) ->
                    val highestOccupiedRow = points.minOf(GridPoint::row)
                    val top = (highestOccupiedRow * cellHeightPx) + cellVisual.previewInsetPx
                    val bottom = (preview.entryAnchor.row * cellHeightPx) + cellHeightPx - cellVisual.previewInsetPx
                    top to bottom
                }

            val highlightedColumns = preview?.coveredColumns ?: activePiece?.let { piece ->
                activeColumn?.coerceIn(0, (gameState.config.columns - piece.width).coerceAtLeast(0))?.let { column ->
                    column..(column + piece.width - 1)
                }
            }

            highlightedColumns?.forEach { column ->
                val isPrimaryColumn = column == (preview?.selectedColumn ?: activeColumn)
                val highlightColor = when {
                    preview != null -> effectivePreviewColor.copy(alpha = 0.10f)
                    isDragging -> activePiece?.tone?.paletteColor(settings.blockColorPalette) ?: uiColors.success
                    isColumnValid -> uiColors.success
                    else -> uiColors.danger
                }.copy(alpha = if (preview != null) 0.10f else if (isPrimaryColumn) 0.18f else 0.10f)

                val (barTop, barBottom) = previewColumnBounds?.get(column) ?: (0f to boardHeightPx)
                val barHeight = (barBottom - barTop).coerceAtLeast(0f)
                if (barHeight <= 0f) return@forEach

                drawRoundRect(
                    color = highlightColor,
                    topLeft = Offset(x = column * cellWidthPx, y = barTop),
                    size = Size(width = cellWidthPx, height = barHeight),
                    cornerRadius = cornerRadius,
                )
            }

            gameState.columnPressure.forEach { pressure ->
                val alpha = when (pressure.level) {
                    PressureLevel.Calm -> 0f
                    PressureLevel.Warning -> 0.08f
                    PressureLevel.Critical -> dangerPulse
                    PressureLevel.Overflow -> dangerPulse + 0.12f
                }
                if (alpha <= 0f) return@forEach
                val color = when (pressure.level) {
                    PressureLevel.Warning -> uiColors.warning
                    PressureLevel.Critical -> uiColors.danger.copy(alpha = 0.92f)
                    PressureLevel.Overflow -> uiColors.danger
                    PressureLevel.Calm -> Color.Transparent
                }
                drawRoundRect(
                    color = color.copy(alpha = alpha.coerceAtMost(0.72f)),
                    topLeft = Offset(x = pressure.column * cellWidthPx, y = 0f),
                    size = Size(width = cellWidthPx, height = boardHeightPx),
                    cornerRadius = cornerRadius,
                )

                if (pressure.level == PressureLevel.Critical || pressure.level == PressureLevel.Overflow) {
                    val sweepTop = (boardHeightPx * dangerSweep) - cellHeightPx
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                color.copy(alpha = 0f),
                                color.copy(alpha = (alpha * 0.35f).coerceAtMost(0.3f)),
                                color.copy(alpha = 0f),
                            ),
                        ),
                        topLeft = Offset(x = pressure.column * cellWidthPx, y = sweepTop.coerceIn(0f, boardHeightPx - cellHeightPx)),
                        size = Size(width = cellWidthPx, height = cellHeightPx * 1.6f),
                        cornerRadius = cornerRadius,
                    )
                }
            }

            if (gameState.criticalColumns.isNotEmpty()) {
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            uiColors.danger.copy(alpha = dangerPulse),
                            uiColors.warning.copy(alpha = (dangerPulse * 0.9f).coerceAtMost(0.65f)),
                            colorScheme.secondary.copy(alpha = (dangerPulse * 0.8f).coerceAtMost(0.55f)),
                            uiColors.danger.copy(alpha = dangerPulse),
                        ),
                        start = Offset(x = boardWidthPx * dangerSweep, y = 0f),
                        end = Offset(x = boardWidthPx * (1f - dangerSweep), y = boardHeightPx),
                    ),
                    cornerRadius = CornerRadius(28f, 28f),
                    style = Stroke(width = 2.6f),
                )
            } else {
                drawRoundRect(
                    color = uiColors.boardOutlineGlow,
                    topLeft = Offset(2f, 2f),
                    size = Size(width = boardWidthPx - 4f, height = boardHeightPx - 4f),
                    cornerRadius = CornerRadius(26f, 26f),
                )
                drawRoundRect(
                    color = uiColors.boardOutline.copy(alpha = 0.32f),
                    cornerRadius = CornerRadius(28f, 28f),
                    style = Stroke(width = 2f),
                )
            }

            for (row in 0 until gameState.config.rows) {
                for (column in 0 until gameState.config.columns) {
                    val topLeft = Offset(x = column * cellWidthPx, y = row * cellHeightPx)
                    val cellSize = Size(width = cellWidthPx, height = cellHeightPx)
                    drawRoundRect(
                        color = uiColors.boardGridLine.copy(alpha = 0.52f),
                        topLeft = topLeft,
                        size = cellSize,
                        cornerRadius = cornerRadius,
                        style = Stroke(width = 1f),
                    )

                    gameState.board.cellAt(column, row)?.let { cell ->
                        val point = GridPoint(column = column, row = row)
                        val isImpactedByPreview = point in impactedPreviewCells

                        drawCellBody(
                            tone = cell.tone,
                            palette = settings.blockColorPalette,
                            style = boardBlockStyle,
                            special = cell.special,
                            topLeft = topLeft + Offset(cellVisual.fillInsetPx, cellVisual.fillInsetPx),
                            size = Size(
                                width = cellWidthPx - (cellVisual.fillInsetPx * 2),
                                height = cellHeightPx - (cellVisual.fillInsetPx * 2),
                            ),
                            cornerRadius = cornerRadius,
                        )

                        if (isImpactedByPreview) {
                            val warningColor = uiColors.danger
                            val warningInset = (cellVisual.previewInsetPx - (impactPulse * 0.8f)).coerceAtLeast(0.7f)
                            val diagonalInset = warningInset + (minOf(cellWidthPx, cellHeightPx) * 0.18f)
                            val crossStroke = 1.8f + (impactPulse * 1.8f)
                            drawRoundRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        uiColors.warning.copy(alpha = 0.22f + (impactPulse * 0.16f)),
                                        warningColor.copy(alpha = 0.16f + (impactPulse * 0.22f)),
                                        Color.Transparent,
                                    ),
                                    center = topLeft + Offset(cellWidthPx / 2f, cellHeightPx / 2f),
                                    radius = minOf(cellWidthPx, cellHeightPx) * (0.98f + impactPulse * 0.18f),
                                ),
                                topLeft = topLeft,
                                size = cellSize,
                                cornerRadius = cornerRadius,
                            )
                            drawRoundRect(
                                color = warningColor.copy(alpha = 0.10f + impactPulse * 0.20f),
                                topLeft = topLeft + Offset(warningInset, warningInset),
                                size = Size(
                                    width = cellWidthPx - (warningInset * 2),
                                    height = cellHeightPx - (warningInset * 2),
                                ),
                                cornerRadius = cornerRadius,
                            )
                            drawRoundRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        uiColors.warning.copy(alpha = 0.95f),
                                        warningColor.copy(alpha = 1f),
                                    ),
                                    start = topLeft + Offset(cellWidthPx * impactSweep, 0f),
                                    end = topLeft + Offset(cellWidthPx * (1f - impactSweep), cellHeightPx),
                                ),
                                topLeft = topLeft + Offset(warningInset, warningInset),
                                size = Size(
                                    width = cellWidthPx - (warningInset * 2),
                                    height = cellHeightPx - (warningInset * 2),
                                ),
                                cornerRadius = cornerRadius,
                                style = Stroke(width = 1.8f + impactPulse * 2.2f),
                            )
                            drawLine(
                                color = Color.White.copy(alpha = 0.40f + impactPulse * 0.34f),
                                start = topLeft + Offset(diagonalInset, diagonalInset),
                                end = topLeft + Offset(cellWidthPx - diagonalInset, cellHeightPx - diagonalInset),
                                strokeWidth = crossStroke,
                            )
                            drawLine(
                                color = uiColors.warning.copy(alpha = 0.42f + impactPulse * 0.32f),
                                start = topLeft + Offset(cellWidthPx - diagonalInset, diagonalInset),
                                end = topLeft + Offset(diagonalInset, cellHeightPx - diagonalInset),
                                strokeWidth = crossStroke,
                            )
                        }
                    }
                }
            }

            preview?.let { landing ->
                val previewColor = effectivePreviewColor
                val previewStyle = boardBlockStyle

                landing.occupiedCells.forEach { point ->
                    val topLeft = Offset(
                        x = point.column * cellWidthPx,
                        y = point.row * cellHeightPx,
                    )
                    drawPreviewCellBody(
                        baseColor = previewColor,
                        style = previewStyle,
                        topLeft = topLeft + Offset(cellVisual.previewInsetPx, cellVisual.previewInsetPx),
                        size = Size(
                            width = cellWidthPx - (cellVisual.previewInsetPx * 2),
                            height = cellHeightPx - (cellVisual.previewInsetPx * 2),
                        ),
                        cornerRadius = cornerRadius,
                    )
                    drawRoundRect(
                        color = previewColor.copy(alpha = PreviewLineAlpha),
                        topLeft = topLeft + Offset(cellVisual.previewInsetPx, cellVisual.previewInsetPx),
                        size = Size(
                            width = cellWidthPx - (cellVisual.previewInsetPx * 2),
                            height = cellHeightPx - (cellVisual.previewInsetPx * 2),
                        ),
                        cornerRadius = cornerRadius,
                        style = Stroke(width = 2f),
                    )
                }
            }

            if (clearFlashAlpha > 0f) {
                gameState.recentlyClearedRows.forEach { row ->
                    drawRoundRect(
                        color = Color.White.copy(alpha = clearFlashAlpha),
                        topLeft = Offset(x = 0f, y = row * cellHeightPx),
                        size = Size(width = boardWidthPx, height = cellHeightPx),
                        cornerRadius = cornerRadius,
                    )
                }
            }
        }

        for (row in 0 until gameState.config.rows) {
            for (column in 0 until gameState.config.columns) {
                val cell = gameState.board.cellAt(column, row) ?: continue
                if (cell.special == SpecialBlockType.None) continue

                Box(
                    modifier = Modifier
                        .offset(x = cellWidth * column, y = cellHeight * row)
                        .size(cellWidth, cellHeight)
                        .padding(2.dp),
                    contentAlignment = Alignment.TopEnd,
                ) {
                    Icon(
                        imageVector = boardSpecialIcon(cell.special),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(minOf(cellWidth, cellHeight) * 0.30f),
                    )
                }
            }
        }

        val previewSpecial = activePiece?.special
        if (previewSpecial != null && previewSpecial != SpecialBlockType.None) {
            preview?.occupiedCells?.forEach { point ->
                Box(
                    modifier = Modifier
                        .offset(x = cellWidth * point.column, y = cellHeight * point.row)
                        .size(cellWidth, cellHeight)
                        .padding(2.dp),
                    contentAlignment = Alignment.TopEnd,
                ) {
                    Icon(
                        imageVector = boardSpecialIcon(previewSpecial),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.92f),
                        modifier = Modifier.size(minOf(cellWidth, cellHeight) * 0.30f),
                    )
                }
            }
        }
    }
}

private fun pieceCellVisualModifier(
    style: BlockVisualStyle,
    baseColor: Color,
    specialColor: Color,
    shape: RoundedCornerShape,
    hasSpecial: Boolean,
): Modifier = when (style) {
    BlockVisualStyle.Flat -> Modifier
        .border(
            width = if (hasSpecial) 1.5.dp else 0.dp,
            color = specialColor,
            shape = shape,
        )
        .background(baseColor)

    BlockVisualStyle.Bubble -> Modifier
        .border(
            width = if (hasSpecial) 1.5.dp else 0.75.dp,
            color = if (hasSpecial) specialColor else baseColor.copy(alpha = 0.52f),
            shape = shape,
        )
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    baseColor.copy(alpha = 0.98f),
                    baseColor.copy(alpha = 0.80f),
                    baseColor.copy(alpha = 0.58f),
                ),
            ),
            shape = shape,
        )

    BlockVisualStyle.Outline -> Modifier
        .background(
            color = baseColor.copy(alpha = if (hasSpecial) 0.18f else 0.12f),
            shape = shape,
        )
        .border(
            width = if (hasSpecial) 1.9.dp else 1.5.dp,
            color = baseColor.copy(alpha = 0.96f),
            shape = shape,
        )

    BlockVisualStyle.Sharp3D -> Modifier
        .border(
            width = if (hasSpecial) 1.5.dp else 0.dp,
            color = specialColor,
            shape = shape,
        )
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    baseColor.copy(alpha = 1f),
                    baseColor.copy(alpha = 0.80f),
                    baseColor.copy(alpha = 0.52f),
                ),
            ),
            shape = shape,
        )

    BlockVisualStyle.Wood -> Modifier
        .border(
            width = if (hasSpecial) 1.5.dp else 0.dp,
            color = if (hasSpecial) specialColor else baseColor.copy(alpha = 0.58f),
            shape = shape,
        )
        .background(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    baseColor.copy(alpha = 0.98f),
                    baseColor.copy(alpha = 0.82f),
                    baseColor.copy(alpha = 0.60f),
                ),
            ),
            shape = shape,
        )

    BlockVisualStyle.LiquidGlass -> Modifier
        .border(
            width = if (hasSpecial) 1.5.dp else 0.dp,
            color = if (hasSpecial) specialColor else Color.White.copy(alpha = 0.42f),
            shape = shape,
        )
        .background(
            color = baseColor.copy(alpha = 0.36f),
            shape = shape,
        )

    BlockVisualStyle.Neon -> Modifier
        .border(
            width = if (hasSpecial) 1.5.dp else 0.dp,
            color = specialColor,
            shape = shape,
        )
        .background(
            brush = Brush.radialGradient(
                colors = listOf(
                    baseColor.copy(alpha = 0.98f),
                    baseColor.copy(alpha = 0.78f),
                    Color.Transparent,
                ),
            ),
            shape = shape,
        )
}

@Composable
internal fun BlockCellPreview(
    tone: CellTone,
    palette: BlockColorPalette,
    style: BlockVisualStyle,
    size: Dp,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    special: SpecialBlockType = SpecialBlockType.None,
) {
    val cellShape = RoundedCornerShape(boardCellCornerRadiusDp(size))
    Box(
        modifier = modifier
            .size(size)
            .padding(boardCellInsetDp(size))
            .clip(cellShape)
            .then(
                pieceCellVisualModifier(
                    style = style,
                    baseColor = tone.paletteColor(palette).copy(alpha = alpha),
                    specialColor = specialAccentColor(special).copy(alpha = alpha),
                    shape = cellShape,
                    hasSpecial = special != SpecialBlockType.None,
                ),
            ),
        contentAlignment = Alignment.TopEnd,
    ) {
        if (special != SpecialBlockType.None) {
            Icon(
                imageVector = boardSpecialIcon(special),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .padding(2.dp)
                    .size((size.value * 0.30f).dp),
            )
        }
    }
}

@Composable
internal fun MiniBoardStylePreview(
    boardStyleMode: BoardBlockStyleMode,
    selectedBlockStyle: BlockVisualStyle,
    palette: BlockColorPalette,
    modifier: Modifier = Modifier,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    val resolvedStyle = resolveBoardBlockStyle(selectedBlockStyle, boardStyleMode)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(uiColors.panelMuted)
            .padding(horizontal = 4.dp, vertical = 3.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(2) { index ->
            if (index == 0) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(uiColors.boardEmptyCell)
                        .border(width = 1.dp, color = uiColors.boardEmptyCellBorder, shape = RoundedCornerShape(6.dp)),
                )
            } else {
                BlockCellPreview(
                    tone = CellTone.Blue,
                    palette = palette,
                    style = resolvedStyle,
                    size = 16.dp,
                )
            }
        }
    }
}

@Composable
fun PieceBlocks(
    piece: Piece,
    cellSize: Dp,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    cellInset: Dp = boardCellInsetDp(cellSize),
    cellCornerRadius: Dp = boardCellCornerRadiusDp(cellSize),
    blockContent: @Composable BoxScope.() -> Unit = {},
) {
    Box(
        modifier = modifier.size(
            width = cellSize * piece.width,
            height = cellSize * piece.height,
        ),
    ) {
        val settings = LocalAppSettings.current
        val baseColor = piece.tone.paletteColor(settings.blockColorPalette)
        val visualStyle = settings.blockVisualStyle
        val cellShape = RoundedCornerShape(cellCornerRadius)
        piece.cells.forEach { cell ->
            Box(
                modifier = Modifier
                    .offset(x = cellSize * cell.column, y = cellSize * cell.row)
                    .size(cellSize)
                    .padding(cellInset)
                    .clip(cellShape)
                    .then(
                        pieceCellVisualModifier(
                            style = visualStyle,
                            baseColor = baseColor.copy(alpha = alpha),
                            specialColor = piece.previewColor().copy(alpha = alpha),
                            shape = cellShape,
                            hasSpecial = piece.special != SpecialBlockType.None,
                        ),
                    ),
                contentAlignment = Alignment.TopEnd,
            ) {
                if (piece.special != SpecialBlockType.None) {
                    Icon(
                        imageVector = boardSpecialIcon(piece.special),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .padding(2.dp)
                            .size((cellSize.value * 0.36f).dp),
                    )
                }
            }
        }
        blockContent()
    }
}

private fun Piece?.previewColor(): Color = specialAccentColor(this?.special ?: SpecialBlockType.None)

internal fun specialAccentColor(type: SpecialBlockType): Color = when (type) {
    SpecialBlockType.ColumnClearer -> Color(0xFF57E389)
    SpecialBlockType.RowClearer -> Color(0xFFFFD166)
    SpecialBlockType.Ghost -> Color(0xFF9B8CFF)
    SpecialBlockType.Heavy -> Color(0xFFFF7A90)
    SpecialBlockType.None -> Color(0xFF7CFFB2)
}

private fun DrawScope.drawCellBody(
    tone: CellTone,
    palette: BlockColorPalette,
    style: BlockVisualStyle,
    special: SpecialBlockType,
    topLeft: Offset,
    size: Size,
    cornerRadius: CornerRadius,
) {
    val baseColor = tone.paletteColor(palette)
    val specialColor = specialAccentColor(special)
    when (style) {
        BlockVisualStyle.Flat -> drawRoundRect(
            color = baseColor,
            topLeft = topLeft,
            size = size,
            cornerRadius = cornerRadius,
        ).also {
            if (special != SpecialBlockType.None) {
                drawRoundRect(
                    color = specialColor,
                    topLeft = topLeft,
                    size = size,
                    cornerRadius = cornerRadius,
                    style = Stroke(width = 1.4f),
                )
            }
        }
        BlockVisualStyle.Bubble -> {
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        baseColor.copy(alpha = 1f),
                        baseColor.copy(alpha = 0.84f),
                        baseColor.copy(alpha = 0.60f),
                    ),
                ),
                topLeft = topLeft,
                size = size,
                cornerRadius = CornerRadius(cornerRadius.x * 1.22f, cornerRadius.y * 1.22f),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.18f),
                topLeft = topLeft + Offset(size.width * 0.10f, size.height * 0.10f),
                size = Size(size.width * 0.34f, size.height * 0.26f),
                cornerRadius = CornerRadius(cornerRadius.x * 0.94f, cornerRadius.y * 0.94f),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.08f),
                topLeft = topLeft + Offset(size.width * 0.14f, size.height * 0.22f),
                size = Size(size.width * 0.64f, size.height * 0.12f),
                cornerRadius = CornerRadius(cornerRadius.x * 0.42f, cornerRadius.y * 0.42f),
            )
            if (special != SpecialBlockType.None) {
                drawRoundRect(
                    color = specialColor,
                    topLeft = topLeft,
                    size = size,
                    cornerRadius = cornerRadius,
                    style = Stroke(width = 1.4f),
                )
            }
        }
        BlockVisualStyle.Outline -> {
            drawRoundRect(
                color = baseColor.copy(alpha = 0.18f),
                topLeft = topLeft,
                size = size,
                cornerRadius = cornerRadius,
            )
            drawRoundRect(
                color = baseColor.copy(alpha = 0.96f),
                topLeft = topLeft,
                size = size,
                cornerRadius = cornerRadius,
                style = Stroke(width = 2f),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.08f),
                topLeft = topLeft + Offset(size.width * 0.10f, size.height * 0.10f),
                size = Size(size.width * 0.32f, size.height * 0.18f),
                cornerRadius = CornerRadius(cornerRadius.x * 0.7f, cornerRadius.y * 0.7f),
            )
            if (special != SpecialBlockType.None) {
                drawRoundRect(
                    color = specialColor,
                    topLeft = topLeft,
                    size = size,
                    cornerRadius = cornerRadius,
                    style = Stroke(width = 1.4f),
                )
            }
        }
        BlockVisualStyle.Sharp3D -> drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    baseColor.copy(alpha = 1f),
                    baseColor.copy(alpha = 0.82f),
                    baseColor.copy(alpha = 0.48f),
                ),
            ),
            topLeft = topLeft,
            size = size,
            cornerRadius = CornerRadius(cornerRadius.x * 0.62f, cornerRadius.y * 0.62f),
        ).also {
            drawRoundRect(
                color = Color.White.copy(alpha = 0.10f),
                topLeft = topLeft + Offset(size.width * 0.06f, size.height * 0.06f),
                size = Size(size.width * 0.90f, size.height * 0.24f),
                cornerRadius = CornerRadius(cornerRadius.x * 0.40f, cornerRadius.y * 0.40f),
            )
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.10f),
                topLeft = topLeft + Offset(size.width * 0.02f, size.height * 0.58f),
                size = Size(size.width * 0.98f, size.height * 0.40f),
                cornerRadius = CornerRadius(cornerRadius.x * 0.56f, cornerRadius.y * 0.56f),
            )
            if (special != SpecialBlockType.None) {
                drawRoundRect(
                    color = specialColor,
                    topLeft = topLeft,
                    size = size,
                    cornerRadius = CornerRadius(cornerRadius.x * 0.7f, cornerRadius.y * 0.7f),
                    style = Stroke(width = 1.4f),
                )
            }
        }
        BlockVisualStyle.Wood -> drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    baseColor.copy(alpha = 0.96f),
                    baseColor.copy(alpha = 0.82f),
                    baseColor.copy(alpha = 0.62f),
                ),
            ),
            topLeft = topLeft,
            size = size,
            cornerRadius = CornerRadius(cornerRadius.x * 0.88f, cornerRadius.y * 0.82f),
        ).also {
            drawLine(
                color = Color.White.copy(alpha = 0.10f),
                start = topLeft + Offset(size.width * 0.08f, size.height * 0.28f),
                end = topLeft + Offset(size.width * 0.92f, size.height * 0.28f),
                strokeWidth = size.height * 0.08f,
            )
            drawLine(
                color = Color.Black.copy(alpha = 0.08f),
                start = topLeft + Offset(size.width * 0.08f, size.height * 0.62f),
                end = topLeft + Offset(size.width * 0.92f, size.height * 0.62f),
                strokeWidth = size.height * 0.07f,
            )
            if (special != SpecialBlockType.None) {
                drawRoundRect(
                    color = specialColor,
                    topLeft = topLeft,
                    size = size,
                    cornerRadius = CornerRadius(cornerRadius.x * 0.82f, cornerRadius.y * 0.82f),
                    style = Stroke(width = 1.4f),
                )
            }
        }
        BlockVisualStyle.LiquidGlass -> {
            drawRoundRect(
                color = baseColor.copy(alpha = 0.42f),
                topLeft = topLeft,
                size = size,
                cornerRadius = CornerRadius(cornerRadius.x * 1.08f, cornerRadius.y * 1.08f),
            )
            drawRoundRect(
                color = baseColor.copy(alpha = 0.16f),
                topLeft = topLeft + Offset(size.width * 0.02f, size.height * 0.02f),
                size = Size(size.width * 0.96f, size.height * 0.96f),
                cornerRadius = CornerRadius(cornerRadius.x, cornerRadius.y),
                style = Stroke(width = 1.1f),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.18f),
                topLeft = topLeft + Offset(size.width * 0.06f, size.height * 0.06f),
                size = Size(size.width * 0.78f, size.height * 0.40f),
                cornerRadius = CornerRadius(cornerRadius.x * 0.8f, cornerRadius.y * 0.8f),
            )
            if (special != SpecialBlockType.None) {
                drawRoundRect(
                    color = specialColor,
                    topLeft = topLeft,
                    size = size,
                    cornerRadius = CornerRadius(cornerRadius.x * 1.08f, cornerRadius.y * 1.08f),
                    style = Stroke(width = 1.4f),
                )
            }
        }
        BlockVisualStyle.Neon -> {
            drawRoundRect(
                color = baseColor.copy(alpha = 0.12f),
                topLeft = topLeft - Offset(size.width * 0.04f, size.height * 0.04f),
                size = Size(size.width * 1.08f, size.height * 1.08f),
                cornerRadius = CornerRadius(cornerRadius.x * 1.15f, cornerRadius.y * 1.15f),
            )
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        baseColor.copy(alpha = 0.98f),
                        baseColor.copy(alpha = 0.78f),
                        Color.Transparent,
                    ),
                    center = topLeft + Offset(size.width / 2f, size.height / 2f),
                    radius = minOf(size.width, size.height) * 0.72f,
                ),
                topLeft = topLeft,
                size = size,
                cornerRadius = cornerRadius,
            )
            drawRoundRect(
                color = baseColor.copy(alpha = 0.40f),
                topLeft = topLeft,
                size = size,
                cornerRadius = cornerRadius,
                style = Stroke(width = 1.7f),
            )
            if (special != SpecialBlockType.None) {
                drawRoundRect(
                    color = specialColor,
                    topLeft = topLeft,
                    size = size,
                    cornerRadius = cornerRadius,
                    style = Stroke(width = 1.5f),
                )
            }
        }
    }
}

private fun DrawScope.drawPreviewCellBody(
    baseColor: Color,
    style: BlockVisualStyle,
    topLeft: Offset,
    size: Size,
    cornerRadius: CornerRadius,
) {
    when (style) {
        BlockVisualStyle.Flat -> drawRoundRect(
            color = baseColor.copy(alpha = 0.46f),
            topLeft = topLeft,
            size = size,
            cornerRadius = cornerRadius,
        )
        BlockVisualStyle.Bubble -> {
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        baseColor.copy(alpha = 0.62f),
                        baseColor.copy(alpha = 0.40f),
                        baseColor.copy(alpha = 0.24f),
                    ),
                ),
                topLeft = topLeft,
                size = size,
                cornerRadius = CornerRadius(cornerRadius.x * 1.22f, cornerRadius.y * 1.22f),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.16f),
                topLeft = topLeft + Offset(size.width * 0.10f, size.height * 0.10f),
                size = Size(size.width * 0.34f, size.height * 0.26f),
                cornerRadius = CornerRadius(cornerRadius.x * 0.94f, cornerRadius.y * 0.94f),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.06f),
                topLeft = topLeft + Offset(size.width * 0.14f, size.height * 0.22f),
                size = Size(size.width * 0.64f, size.height * 0.12f),
                cornerRadius = CornerRadius(cornerRadius.x * 0.42f, cornerRadius.y * 0.42f),
            )
        }
        BlockVisualStyle.Outline -> {
            drawRoundRect(
                color = baseColor.copy(alpha = 0.12f),
                topLeft = topLeft,
                size = size,
                cornerRadius = cornerRadius,
            )
            drawRoundRect(
                color = baseColor.copy(alpha = 0.86f),
                topLeft = topLeft,
                size = size,
                cornerRadius = cornerRadius,
                style = Stroke(width = 2.2f),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.10f),
                topLeft = topLeft + Offset(size.width * 0.10f, size.height * 0.10f),
                size = Size(size.width * 0.30f, size.height * 0.16f),
                cornerRadius = CornerRadius(cornerRadius.x * 0.7f, cornerRadius.y * 0.7f),
            )
        }
        BlockVisualStyle.Sharp3D -> drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    baseColor.copy(alpha = 0.72f),
                    baseColor.copy(alpha = 0.46f),
                    baseColor.copy(alpha = 0.20f),
                ),
            ),
            topLeft = topLeft,
            size = size,
            cornerRadius = CornerRadius(cornerRadius.x * 0.62f, cornerRadius.y * 0.62f),
        ).also {
            drawRoundRect(
                color = Color.White.copy(alpha = 0.08f),
                topLeft = topLeft + Offset(size.width * 0.06f, size.height * 0.06f),
                size = Size(size.width * 0.90f, size.height * 0.22f),
                cornerRadius = CornerRadius(cornerRadius.x * 0.40f, cornerRadius.y * 0.40f),
            )
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.08f),
                topLeft = topLeft + Offset(size.width * 0.02f, size.height * 0.58f),
                size = Size(size.width * 0.98f, size.height * 0.40f),
                cornerRadius = CornerRadius(cornerRadius.x * 0.56f, cornerRadius.y * 0.56f),
            )
        }
        BlockVisualStyle.Wood -> drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    baseColor.copy(alpha = 0.64f),
                    baseColor.copy(alpha = 0.42f),
                    baseColor.copy(alpha = 0.24f),
                ),
            ),
            topLeft = topLeft,
            size = size,
            cornerRadius = CornerRadius(cornerRadius.x * 0.88f, cornerRadius.y * 0.82f),
        ).also {
            drawLine(
                color = Color.White.copy(alpha = 0.08f),
                start = topLeft + Offset(size.width * 0.08f, size.height * 0.28f),
                end = topLeft + Offset(size.width * 0.92f, size.height * 0.28f),
                strokeWidth = size.height * 0.08f,
            )
            drawLine(
                color = Color.Black.copy(alpha = 0.08f),
                start = topLeft + Offset(size.width * 0.08f, size.height * 0.62f),
                end = topLeft + Offset(size.width * 0.92f, size.height * 0.62f),
                strokeWidth = size.height * 0.07f,
            )
        }
        BlockVisualStyle.LiquidGlass -> {
            drawRoundRect(
                color = baseColor.copy(alpha = 0.24f),
                topLeft = topLeft,
                size = size,
                cornerRadius = CornerRadius(cornerRadius.x * 1.16f, cornerRadius.y * 1.16f),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.12f),
                topLeft = topLeft + Offset(size.width * 0.04f, size.height * 0.05f),
                size = Size(size.width * 0.86f, size.height * 0.28f),
                cornerRadius = CornerRadius(cornerRadius.x, cornerRadius.y),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.10f),
                topLeft = topLeft + Offset(size.width * 0.10f, size.height * 0.50f),
                size = Size(size.width * 0.56f, size.height * 0.18f),
                cornerRadius = CornerRadius(cornerRadius.x * 0.72f, cornerRadius.y * 0.72f),
                style = Stroke(width = 1f),
            )
        }
        BlockVisualStyle.Neon -> {
            drawRoundRect(
                color = baseColor.copy(alpha = 0.10f),
                topLeft = topLeft - Offset(size.width * 0.04f, size.height * 0.04f),
                size = Size(size.width * 1.08f, size.height * 1.08f),
                cornerRadius = CornerRadius(cornerRadius.x * 1.15f, cornerRadius.y * 1.15f),
            )
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        baseColor.copy(alpha = 0.66f),
                        baseColor.copy(alpha = 0.34f),
                        Color.Transparent,
                    ),
                    center = topLeft + Offset(size.width / 2f, size.height / 2f),
                    radius = minOf(size.width, size.height) * 0.72f,
                ),
                topLeft = topLeft,
                size = size,
                cornerRadius = cornerRadius,
            )
            drawRoundRect(
                color = baseColor.copy(alpha = 0.34f),
                topLeft = topLeft,
                size = size,
                cornerRadius = cornerRadius,
                style = Stroke(width = 1.6f),
            )
        }
    }
}


@Immutable
private data class BoardCellVisual(
    val fillInsetPx: Float,
    val previewInsetPx: Float,
    val specialInsetPx: Float,
    val cornerRadius: CornerRadius,
    val innerCornerRadiusPx: Float,
)

private fun boardCellVisual(
    cellWidth: Float,
    cellHeight: Float,
): BoardCellVisual {
    val minSize = minOf(cellWidth, cellHeight)
    val fillInset = (minSize * 0.085f).coerceIn(1.5f, 3.2f)
    val previewInset = (minSize * 0.06f).coerceIn(1.4f, 2.4f)
    val specialInset = (minSize * 0.14f).coerceIn(4f, 6f)
    val outerRadius = minSize * 0.16f
    return BoardCellVisual(
        fillInsetPx = fillInset,
        previewInsetPx = previewInset,
        specialInsetPx = specialInset,
        cornerRadius = CornerRadius(outerRadius, outerRadius),
        innerCornerRadiusPx = outerRadius * 0.72f,
    )
}

@Composable
internal fun boardCellInsetDp(cellSize: Dp): Dp {
    val density = LocalDensity.current
    return with(density) {
        (cellSize.toPx() * 0.085f).coerceIn(1.5f, 3.2f).toDp()
    }
}

@Composable
internal fun boardCellCornerRadiusDp(cellSize: Dp): Dp {
    val density = LocalDensity.current
    return with(density) {
        (cellSize.toPx() * 0.16f).toDp()
    }
}

internal fun CellTone.color(): Color = paletteColor(BlockColorPalette.Classic)

internal fun CellTone.paletteColor(palette: BlockColorPalette): Color = when (palette) {
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
}

internal fun resolveBoardBlockStyle(
    selectedStyle: BlockVisualStyle,
    mode: BoardBlockStyleMode,
): BlockVisualStyle = when (mode) {
    BoardBlockStyleMode.AlwaysFlat -> BlockVisualStyle.Flat
    BoardBlockStyleMode.MatchSelectedBlockStyle -> selectedStyle
}

private fun boardSpecialIcon(type: SpecialBlockType) = when (type) {
    SpecialBlockType.ColumnClearer -> Icons.Filled.SwapVert
    SpecialBlockType.RowClearer -> Icons.Filled.SwapHoriz
    SpecialBlockType.Ghost -> Icons.Filled.ViewModule
    SpecialBlockType.Heavy -> Icons.Filled.FitnessCenter
    SpecialBlockType.None -> Icons.AutoMirrored.Filled.HelpOutline
}
