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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ugurbuga.stackshift.game.model.CellTone
import com.ugurbuga.stackshift.game.model.GameState
import com.ugurbuga.stackshift.game.model.GridPoint
import com.ugurbuga.stackshift.game.model.Piece
import com.ugurbuga.stackshift.game.model.PlacementPreview
import com.ugurbuga.stackshift.game.model.PressureLevel
import com.ugurbuga.stackshift.game.model.SpecialBlockType

private const val PreviewBlockAlpha = 0.10f
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

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF12172A),
                        Color(0xFF0A1020),
                    ),
                ),
            ),
    ) {
        val cellWidth = size.width / gameState.config.columns
        val cellHeight = size.height / gameState.config.rows
        val cellVisual = boardCellVisual(cellWidth = cellWidth, cellHeight = cellHeight)
        val cornerRadius = cellVisual.cornerRadius

        val pieceToneColor = activePiece?.tone?.color()
        val effectivePreviewColor = pieceToneColor ?: Color(0xFF7CFFB2)
        val previewColumnBounds = preview?.occupiedCells
            ?.groupBy(GridPoint::column)
            ?.mapValues { (_, points) ->
                val highestOccupiedRow = points.minOf(GridPoint::row)
                val top = (highestOccupiedRow * cellHeight) + cellVisual.previewInsetPx
                val bottom = (preview.entryAnchor.row * cellHeight) + cellHeight - cellVisual.previewInsetPx
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
                isDragging -> activePiece?.tone?.color() ?: Color(0xFF7CFFB2)
                isColumnValid -> Color(0xFF7CFFB2)
                else -> Color(0xFFFF8B8B)
            }.copy(alpha = if (preview != null) 0.10f else if (isPrimaryColumn) 0.18f else 0.10f)

            val (barTop, barBottom) = previewColumnBounds?.get(column) ?: (0f to size.height)
            val barHeight = (barBottom - barTop).coerceAtLeast(0f)
            if (barHeight <= 0f) return@forEach

            drawRoundRect(
                color = highlightColor,
                topLeft = Offset(x = column * cellWidth, y = barTop),
                size = Size(width = cellWidth, height = barHeight),
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
                PressureLevel.Warning -> Color(0xFFFFD166)
                PressureLevel.Critical -> Color(0xFFFF8B8B)
                PressureLevel.Overflow -> Color(0xFFFF5D73)
                PressureLevel.Calm -> Color.Transparent
            }
            drawRoundRect(
                color = color.copy(alpha = alpha.coerceAtMost(0.72f)),
                topLeft = Offset(x = pressure.column * cellWidth, y = 0f),
                size = Size(width = cellWidth, height = size.height),
                cornerRadius = cornerRadius,
            )

            if (pressure.level == PressureLevel.Critical || pressure.level == PressureLevel.Overflow) {
                val sweepTop = (size.height * dangerSweep) - cellHeight
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            color.copy(alpha = 0f),
                            color.copy(alpha = (alpha * 0.35f).coerceAtMost(0.3f)),
                            color.copy(alpha = 0f),
                        ),
                    ),
                    topLeft = Offset(x = pressure.column * cellWidth, y = sweepTop.coerceIn(0f, size.height - cellHeight)),
                    size = Size(width = cellWidth, height = cellHeight * 1.6f),
                    cornerRadius = cornerRadius,
                )
            }
        }

        if (gameState.criticalColumns.isNotEmpty()) {
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFF5D73).copy(alpha = dangerPulse),
                        Color(0xFFFFD166).copy(alpha = (dangerPulse * 0.9f).coerceAtMost(0.65f)),
                        Color(0xFF9B8CFF).copy(alpha = (dangerPulse * 0.8f).coerceAtMost(0.55f)),
                        Color(0xFFFF5D73).copy(alpha = dangerPulse),
                    ),
                    start = Offset(x = size.width * dangerSweep, y = 0f),
                    end = Offset(x = size.width * (1f - dangerSweep), y = size.height),
                ),
                cornerRadius = CornerRadius(28f, 28f),
                style = Stroke(width = 2.6f),
            )
        } else {
            drawRoundRect(
                color = Color.White.copy(alpha = 0.05f),
                cornerRadius = CornerRadius(28f, 28f),
                style = Stroke(width = 2f),
            )
        }

        for (row in 0 until gameState.config.rows) {
            for (column in 0 until gameState.config.columns) {
                val topLeft = Offset(x = column * cellWidth, y = row * cellHeight)
                val cellSize = Size(width = cellWidth, height = cellHeight)
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.032f),
                    topLeft = topLeft,
                    size = cellSize,
                    cornerRadius = cornerRadius,
                    style = Stroke(width = 1f),
                )

                gameState.board.cellAt(column, row)?.let { cell ->
                    val point = GridPoint(column = column, row = row)
                    val isImpactedByPreview = point in impactedPreviewCells

                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                cell.tone.color().copy(alpha = 0.96f),
                                cell.tone.color().copy(alpha = 0.74f),
                            ),
                        ),
                        topLeft = topLeft + Offset(cellVisual.fillInsetPx, cellVisual.fillInsetPx),
                        size = Size(
                            width = cellWidth - (cellVisual.fillInsetPx * 2),
                            height = cellHeight - (cellVisual.fillInsetPx * 2),
                        ),
                        cornerRadius = cornerRadius,
                    )

                    if (isImpactedByPreview) {
                        val warningColor = Color(0xFFFF546E)
                        val warningInset = (cellVisual.previewInsetPx - (impactPulse * 0.8f)).coerceAtLeast(0.7f)
                        val diagonalInset = warningInset + (minOf(cellWidth, cellHeight) * 0.18f)
                        val crossStroke = 1.8f + (impactPulse * 1.8f)
                        drawRoundRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFD166).copy(alpha = 0.22f + (impactPulse * 0.16f)),
                                    warningColor.copy(alpha = 0.16f + (impactPulse * 0.22f)),
                                    Color.Transparent,
                                ),
                                center = topLeft + Offset(cellWidth / 2f, cellHeight / 2f),
                                radius = minOf(cellWidth, cellHeight) * (0.98f + impactPulse * 0.18f),
                            ),
                            topLeft = topLeft,
                            size = cellSize,
                            cornerRadius = cornerRadius,
                        )
                        drawRoundRect(
                            color = warningColor.copy(alpha = 0.10f + impactPulse * 0.20f),
                            topLeft = topLeft + Offset(warningInset, warningInset),
                            size = Size(
                                width = cellWidth - (warningInset * 2),
                                height = cellHeight - (warningInset * 2),
                            ),
                            cornerRadius = cornerRadius,
                        )
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFFD166).copy(alpha = 0.95f),
                                    warningColor.copy(alpha = 1f),
                                ),
                                start = topLeft + Offset(cellWidth * impactSweep, 0f),
                                end = topLeft + Offset(cellWidth * (1f - impactSweep), cellHeight),
                            ),
                            topLeft = topLeft + Offset(warningInset, warningInset),
                            size = Size(
                                width = cellWidth - (warningInset * 2),
                                height = cellHeight - (warningInset * 2),
                            ),
                            cornerRadius = cornerRadius,
                            style = Stroke(width = 1.8f + impactPulse * 2.2f),
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.40f + impactPulse * 0.34f),
                            start = topLeft + Offset(diagonalInset, diagonalInset),
                            end = topLeft + Offset(cellWidth - diagonalInset, cellHeight - diagonalInset),
                            strokeWidth = crossStroke,
                        )
                        drawLine(
                            color = Color(0xFFFFD166).copy(alpha = 0.42f + impactPulse * 0.32f),
                            start = topLeft + Offset(cellWidth - diagonalInset, diagonalInset),
                            end = topLeft + Offset(diagonalInset, cellHeight - diagonalInset),
                            strokeWidth = crossStroke,
                        )
                    }

                    if (cell.special != SpecialBlockType.None) {
                        drawRoundRect(
                            color = specialColor(cell.special).copy(alpha = 0.36f),
                            topLeft = topLeft + Offset(cellVisual.specialInsetPx, cellVisual.specialInsetPx),
                            size = Size(
                                width = cellWidth - (cellVisual.specialInsetPx * 2),
                                height = cellHeight - (cellVisual.specialInsetPx * 2),
                            ),
                            cornerRadius = CornerRadius(cellVisual.innerCornerRadiusPx, cellVisual.innerCornerRadiusPx),
                            style = Stroke(width = 1.6f),
                        )
                        drawCircle(
                            color = specialColor(cell.special).copy(alpha = 0.8f),
                            radius = minOf(cellWidth, cellHeight) * 0.11f,
                            center = topLeft + Offset(cellWidth * 0.72f, cellHeight * 0.28f),
                        )
                    }
                }
            }
        }

        preview?.let { landing ->
            val previewColor = effectivePreviewColor

            landing.occupiedCells.forEach { point ->
                val topLeft = Offset(
                    x = point.column * cellWidth,
                    y = point.row * cellHeight,
                )
                drawRoundRect(
                    color = previewColor.copy(alpha = PreviewBlockAlpha),
                    topLeft = topLeft + Offset(cellVisual.previewInsetPx, cellVisual.previewInsetPx),
                    size = Size(
                        width = cellWidth - (cellVisual.previewInsetPx * 2),
                        height = cellHeight - (cellVisual.previewInsetPx * 2),
                    ),
                    cornerRadius = cornerRadius,
                )
                drawRoundRect(
                    color = previewColor.copy(alpha = PreviewLineAlpha),
                    topLeft = topLeft + Offset(cellVisual.previewInsetPx, cellVisual.previewInsetPx),
                    size = Size(
                        width = cellWidth - (cellVisual.previewInsetPx * 2),
                        height = cellHeight - (cellVisual.previewInsetPx * 2),
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
                    topLeft = Offset(x = 0f, y = row * cellHeight),
                    size = Size(width = size.width, height = cellHeight),
                    cornerRadius = cornerRadius,
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
        piece.cells.forEach { cell ->
            Box(
                modifier = Modifier
                    .offset(x = cellSize * cell.column, y = cellSize * cell.row)
                    .size(cellSize)
                    .padding(cellInset)
                    .clip(RoundedCornerShape(cellCornerRadius))
                    .border(
                        width = if (piece.special == SpecialBlockType.None) 0.dp else 1.5.dp,
                        color = piece.previewColor().copy(alpha = alpha),
                        shape = RoundedCornerShape(cellCornerRadius),
                    )
                    .background(piece.tone.color().copy(alpha = alpha)),
            )
        }
        blockContent()
    }
}


private fun Piece?.previewColor(): Color = when (this?.special) {
    SpecialBlockType.ColumnClearer -> Color(0xFF57E389)
    SpecialBlockType.RowClearer -> Color(0xFFFFD166)
    SpecialBlockType.Ghost -> Color(0xFF9B8CFF)
    SpecialBlockType.Heavy -> Color(0xFFFF7A90)
    else -> Color(0xFF7CFFB2)
}

private fun specialColor(special: SpecialBlockType): Color = when (special) {
    SpecialBlockType.ColumnClearer -> Color(0xFF57E389)
    SpecialBlockType.RowClearer -> Color(0xFFFFD166)
    SpecialBlockType.Ghost -> Color(0xFF9B8CFF)
    SpecialBlockType.Heavy -> Color(0xFFFF7A90)
    SpecialBlockType.None -> Color.Transparent
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

internal fun CellTone.color(): Color = when (this) {
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
