package com.ugurbuga.stackshift.ui.game

import androidx.compose.animation.core.Animatable
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ugurbuga.stackshift.game.model.BlockColorPalette
import com.ugurbuga.stackshift.game.model.BlockVisualStyle
import com.ugurbuga.stackshift.game.model.BoardBlockStyleMode
import com.ugurbuga.stackshift.game.model.BoardCell
import com.ugurbuga.stackshift.game.model.BoardMatrix
import com.ugurbuga.stackshift.game.model.CellTone
import com.ugurbuga.stackshift.game.model.GameState
import com.ugurbuga.stackshift.game.model.GameStatus
import com.ugurbuga.stackshift.game.model.GridPoint
import com.ugurbuga.stackshift.game.model.Piece
import com.ugurbuga.stackshift.game.model.PlacementPreview
import com.ugurbuga.stackshift.game.model.PressureLevel
import com.ugurbuga.stackshift.game.model.SpecialBlockType
import com.ugurbuga.stackshift.localization.LocalAppSettings
import com.ugurbuga.stackshift.ui.theme.StackShiftThemeTokens
import com.ugurbuga.stackshift.ui.theme.isStackShiftDarkTheme

private const val PreviewLineAlpha = 0.12f
private val BoardFrameInset = 1.dp

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
    gameOverClearProgress: Float = 0f,
) {
    val settings = LocalAppSettings.current
    val colorScheme = MaterialTheme.colorScheme
    val uiColors = StackShiftThemeTokens.uiColors
    val boardBlockStyle = resolveBoardBlockStyle(
        selectedStyle = settings.blockVisualStyle,
        mode = settings.boardBlockStyleMode,
    )
    val isDarkTheme = isStackShiftDarkTheme(settings)
    val clearProgress = gameOverClearProgress.coerceIn(0f, 1f)
    val clearRows = clearProgress * gameState.config.rows
    val fullyClearedRows = clearRows.toInt().coerceIn(0, gameState.config.rows)
    val partialClearAlpha = 1f - (clearRows - fullyClearedRows).coerceIn(0f, 1f)
    val boardDecorAlpha = (1f - clearProgress).coerceIn(0f, 1f)
    val showDangerDecorations = gameState.status != GameStatus.GameOver
    var clearFlashAlpha by remember { mutableFloatStateOf(0f) }
    var boardTransitionSource by remember { mutableStateOf(gameState.board) }
    var boardTransitionToken by remember { mutableLongStateOf(gameState.clearAnimationToken) }
    val boardShiftProgress = remember { Animatable(1f) }
    val shouldAnimateBoardShift =
        gameState.status == GameStatus.Running && gameState.clearAnimationToken != boardTransitionToken
    val animatedBoardCells = if (shouldAnimateBoardShift) {
        buildAnimatedBoardCells(boardTransitionSource, gameState.board)
    } else {
        emptyList()
    }

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

    LaunchedEffect(gameState.clearAnimationToken, gameState.status) {
        if (gameState.status != GameStatus.Running) {
            boardShiftProgress.snapTo(1f)
            boardTransitionSource = gameState.board
            boardTransitionToken = gameState.clearAnimationToken
            return@LaunchedEffect
        }

        if (!shouldAnimateBoardShift) {
            boardShiftProgress.snapTo(1f)
            boardTransitionSource = gameState.board
            boardTransitionToken = gameState.clearAnimationToken
            return@LaunchedEffect
        }

        boardShiftProgress.snapTo(0f)
        boardShiftProgress.animateTo(
            1f,
            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        )
        boardTransitionSource = gameState.board
        boardTransitionToken = gameState.clearAnimationToken
    }

    BoxWithConstraints(
        modifier = modifier
            .padding(BoardFrameInset)
            .clip(RoundedCornerShape(boardFrameCornerRadiusDp(boardBlockStyle)))
    ) {
        val cellWidth = maxWidth / gameState.config.columns
        val cellHeight = maxHeight / gameState.config.rows
        val cellWidthPx = with(LocalDensity.current) { cellWidth.toPx() }
        val cellHeightPx = with(LocalDensity.current) { cellHeight.toPx() }
        val boardCornerRadiusPx = with(LocalDensity.current) { boardFrameCornerRadiusDp(boardBlockStyle).toPx() }
        val cellVisual = boardCellVisual(
            cellWidth = cellWidthPx,
            cellHeight = cellHeightPx,
            style = boardBlockStyle,
        )
        val previewFillAlpha = if (isDarkTheme) 0.22f else 0.30f
        val cornerRadius = cellVisual.cornerRadius

        Canvas(modifier = Modifier.fillMaxSize()) {
            val boardWidthPx = size.width
            val boardHeightPx = size.height
            val boardFrameCornerRadius = CornerRadius(boardCornerRadiusPx, boardCornerRadiusPx)
            val scanlineStep = (cellHeightPx * 1.65f).coerceAtLeast(10f)

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        uiColors.boardGradientTop.copy(alpha = 0.98f),
                        uiColors.gameSurface.copy(alpha = 0.94f),
                        uiColors.boardGradientBottom.copy(alpha = 0.98f),
                    ),
                ),
                size = Size(boardWidthPx, boardHeightPx),
                cornerRadius = boardFrameCornerRadius,
            )
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        uiColors.boardSignaturePrimary.copy(alpha = 0.22f * boardDecorAlpha),
                        Color.Transparent,
                    ),
                    center = Offset(boardWidthPx * 0.22f, boardHeightPx * 0.14f),
                    radius = boardWidthPx * 0.72f,
                ),
                size = Size(boardWidthPx, boardHeightPx),
                cornerRadius = boardFrameCornerRadius,
            )
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        uiColors.boardSignatureSecondary.copy(alpha = 0.18f * boardDecorAlpha),
                        Color.Transparent,
                    ),
                    center = Offset(boardWidthPx * 0.78f, boardHeightPx * 0.88f),
                    radius = boardWidthPx * 0.84f,
                ),
                size = Size(boardWidthPx, boardHeightPx),
                cornerRadius = boardFrameCornerRadius,
            )
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.05f * boardDecorAlpha),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.12f * boardDecorAlpha),
                    ),
                ),
                size = Size(boardWidthPx, boardHeightPx),
                cornerRadius = boardFrameCornerRadius,
            )

            var scanlineY = scanlineStep * 0.5f
            while (scanlineY < boardHeightPx) {
                drawLine(
                    color = uiColors.boardGridLine.copy(alpha = 0.08f * boardDecorAlpha),
                    start = Offset(0f, scanlineY),
                    end = Offset(boardWidthPx, scanlineY),
                    strokeWidth = 1f,
                )
                scanlineY += scanlineStep
            }

            val pieceToneColor = activePiece?.tone?.paletteColor(settings.blockColorPalette)
            val effectivePreviewColor = pieceToneColor ?: uiColors.success
            val previewLineColor = effectivePreviewColor.copy(alpha = PreviewLineAlpha * boardDecorAlpha)
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
                val beamAlpha = (if (isDragging) 0.22f else 0.12f) * boardDecorAlpha
                if (beamAlpha <= 0f || !showDangerDecorations) return@forEach
                val (barTop, barBottom) = previewColumnBounds?.get(column) ?: (0f to boardHeightPx)
                val barHeight = (barBottom - barTop).coerceAtLeast(0f)
                if (barHeight <= 0f) return@forEach

                drawRoundRect(
                    color = previewLineColor,
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
                if (alpha <= 0f || !showDangerDecorations) return@forEach
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
                            uiColors.danger.copy(alpha = dangerPulse * boardDecorAlpha),
                            uiColors.warning.copy(alpha = (dangerPulse * 0.9f).coerceAtMost(0.65f) * boardDecorAlpha),
                            colorScheme.secondary.copy(alpha = (dangerPulse * 0.8f).coerceAtMost(0.55f) * boardDecorAlpha),
                            uiColors.danger.copy(alpha = dangerPulse * boardDecorAlpha),
                        ),
                        start = Offset(x = boardWidthPx * dangerSweep, y = 0f),
                        end = Offset(x = boardWidthPx * (1f - dangerSweep), y = boardHeightPx),
                    ),
                    cornerRadius = CornerRadius(boardCornerRadiusPx, boardCornerRadiusPx),
                    style = Stroke(width = 2.6f),
                )
            } else {
                drawRoundRect(
                    color = uiColors.boardOutlineGlow.copy(alpha = 0.48f * boardDecorAlpha),
                    cornerRadius = boardFrameCornerRadius,
                    style = Stroke(width = 4f),
                )
                drawRoundRect(
                    color = uiColors.boardOutline.copy(alpha = 0.44f * boardDecorAlpha),
                    cornerRadius = boardFrameCornerRadius,
                    style = Stroke(width = 2f),
                )
            }

            for (row in 0 until gameState.config.rows) {
                for (column in 0 until gameState.config.columns) {
                    val topLeft = Offset(x = column * cellWidthPx, y = row * cellHeightPx)
                    val cellSize = Size(width = cellWidthPx, height = cellHeightPx)
                    val rowFromBottom = (gameState.config.rows - 1 - row).coerceAtLeast(0)
                    val cellClearAlpha = when {
                        clearProgress <= 0f -> 1f
                        rowFromBottom < fullyClearedRows -> 0f
                        rowFromBottom == fullyClearedRows -> partialClearAlpha
                        else -> 1f
                    }
                    val renderedCellAlpha = cellClearAlpha * boardDecorAlpha
                    if (renderedCellAlpha <= 0f) continue
                    val slotInset = cellVisual.fillInsetPx * 0.72f
                    val slotTopLeft = topLeft + Offset(slotInset, slotInset)
                    val slotSize = Size(
                        width = cellWidthPx - (slotInset * 2),
                        height = cellHeightPx - (slotInset * 2),
                    )
                    val slotCornerRadius = CornerRadius(cellVisual.innerCornerRadiusPx, cellVisual.innerCornerRadiusPx)

                    drawRoundRect(
                        color = uiColors.boardEmptyCell.copy(alpha = 0.40f * renderedCellAlpha),
                        topLeft = slotTopLeft,
                        size = slotSize,
                        cornerRadius = slotCornerRadius,
                    )
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.06f * renderedCellAlpha),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.10f * renderedCellAlpha),
                            ),
                        ),
                        topLeft = slotTopLeft,
                        size = slotSize,
                        cornerRadius = slotCornerRadius,
                    )
                    val gridLineColor = if (showDangerDecorations && gameState.criticalColumns.isNotEmpty()) {
                        uiColors.danger.copy(alpha = (0.10f + (dangerPulse * 0.26f)) * renderedCellAlpha)
                    } else {
                        uiColors.boardGridLine.copy(alpha = 0.52f * renderedCellAlpha)
                    }
                    drawRoundRect(
                        color = gridLineColor,
                        topLeft = topLeft,
                        size = cellSize,
                        cornerRadius = cornerRadius,
                        style = Stroke(width = 1f),
                    )

                    if (showDangerDecorations && gameState.criticalColumns.isNotEmpty()) {
                        drawRoundRect(
                            color = uiColors.danger.copy(alpha = (0.04f + (dangerPulse * 0.18f)) * renderedCellAlpha),
                            topLeft = topLeft,
                            size = cellSize,
                            cornerRadius = cornerRadius,
                            style = Stroke(width = 1.4f),
                        )
                    }

                    if (!shouldAnimateBoardShift) {
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
                                alpha = renderedCellAlpha,
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
                                    color = warningColor.copy(alpha = (0.10f + impactPulse * 0.20f) * renderedCellAlpha),
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
                                            uiColors.warning.copy(alpha = 0.95f * renderedCellAlpha),
                                            warningColor.copy(alpha = 1f * renderedCellAlpha),
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
            }

            preview?.let { landing ->
                val previewColor = effectivePreviewColor.copy(alpha = previewFillAlpha)
                val previewStyle = boardBlockStyle

                landing.occupiedCells.forEach { point ->
                    val topLeft = Offset(
                        x = point.column * cellWidthPx,
                        y = point.row * cellHeightPx,
                    )
                    drawPreviewCellBody(
                        baseColor = previewColor.copy(alpha = previewColor.alpha * boardDecorAlpha),
                        style = previewStyle,
                        topLeft = topLeft + Offset(cellVisual.previewInsetPx, cellVisual.previewInsetPx),
                        size = Size(
                            width = cellWidthPx - (cellVisual.previewInsetPx * 2),
                            height = cellHeightPx - (cellVisual.previewInsetPx * 2),
                        ),
                        cornerRadius = cornerRadius,
                    )
                    drawRoundRect(
                        color = previewLineColor,
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
                        color = Color.White.copy(alpha = clearFlashAlpha * boardDecorAlpha),
                        topLeft = Offset(x = 0f, y = row * cellHeightPx),
                        size = Size(width = boardWidthPx, height = cellHeightPx),
                        cornerRadius = cornerRadius,
                    )
                }
            }

            if (shouldAnimateBoardShift) {
                animatedBoardCells.forEach { animatedCell ->
                    val sourceRow = animatedCell.sourceRow.toFloat()
                    val targetRow = animatedCell.point.row.toFloat()
                    val animatedRow = sourceRow + ((targetRow - sourceRow) * boardShiftProgress.value)
                    val animatedTopLeft = Offset(
                        x = animatedCell.point.column * cellWidthPx,
                        y = animatedRow * cellHeightPx,
                    )
                    drawCellBody(
                        tone = animatedCell.cell.tone,
                        palette = settings.blockColorPalette,
                        style = boardBlockStyle,
                        special = animatedCell.cell.special,
                        topLeft = animatedTopLeft + Offset(cellVisual.fillInsetPx, cellVisual.fillInsetPx),
                        size = Size(
                            width = cellWidthPx - (cellVisual.fillInsetPx * 2),
                            height = cellHeightPx - (cellVisual.fillInsetPx * 2),
                        ),
                        cornerRadius = cornerRadius,
                        alpha = boardDecorAlpha,
                    )
                }
            }
        }

        if (shouldAnimateBoardShift) {
            animatedBoardCells.forEach { animatedCell ->
                if (animatedCell.cell.special == SpecialBlockType.None) return@forEach
                val sourceRow = animatedCell.sourceRow.toFloat()
                val targetRow = animatedCell.point.row.toFloat()
                val animatedRow = sourceRow + ((targetRow - sourceRow) * boardShiftProgress.value)
                BoardSpecialIconOverlay(
                    cell = animatedCell.cell,
                    column = animatedCell.point.column,
                    row = animatedCell.point.row,
                    cellWidth = cellWidth,
                    cellHeight = cellHeight,
                    translationY = (animatedRow - animatedCell.point.row) * cellHeightPx,
                    alpha = boardDecorAlpha,
                )
            }
        } else {
            for (row in 0 until gameState.config.rows) {
                for (column in 0 until gameState.config.columns) {
                    val cell = gameState.board.cellAt(column, row) ?: continue
                    if (cell.special == SpecialBlockType.None) continue

                    BoardSpecialIconOverlay(
                        cell = cell,
                        column = column,
                        row = row,
                        cellWidth = cellWidth,
                        cellHeight = cellHeight,
                        alpha = boardDecorAlpha,
                    )
                }
            }
        }

        val previewSpecial = activePiece?.special
        if (previewSpecial != null && previewSpecial != SpecialBlockType.None) {
            preview?.occupiedCells?.forEach { point ->
                BoardSpecialIconOverlay(
                    cell = BoardCell(tone = CellTone.Blue, special = previewSpecial),
                    column = point.column,
                    row = point.row,
                    cellWidth = cellWidth,
                    cellHeight = cellHeight,
                    alpha = boardDecorAlpha,
                    iconAlpha = 0.92f,
                )
            }
        }
    }
}

@Composable
private fun BoardSpecialIconOverlay(
    cell: BoardCell,
    column: Int,
    row: Int,
    cellWidth: Dp,
    cellHeight: Dp,
    translationX: Float = 0f,
    translationY: Float = 0f,
    alpha: Float = 1f,
    iconAlpha: Float = 1f,
) {
    Box(
        modifier = Modifier
            .offset(x = cellWidth * column, y = cellHeight * row)
            .size(cellWidth, cellHeight)
            .padding(2.dp)
            .graphicsLayer(alpha = alpha, translationX = translationX, translationY = translationY),
        contentAlignment = Alignment.Center,
    ) {
        val settings = LocalAppSettings.current
        val isDarkTheme = isStackShiftDarkTheme(settings)
        val resolvedBoardStyle = resolveBoardBlockStyle(
            selectedStyle = settings.blockVisualStyle,
            mode = settings.boardBlockStyleMode,
        )
        Icon(
            imageVector = boardSpecialIcon(cell.special),
            contentDescription = null,
            tint = specialBlockIconTint(
                style = resolvedBoardStyle,
                isDarkTheme = isDarkTheme,
            ).copy(alpha = iconAlpha),
            modifier = Modifier.size(minOf(cellWidth, cellHeight) * 0.36f),
        )
    }
}

private data class AnimatedBoardCell(
    val point: GridPoint,
    val cell: BoardCell,
    val sourceRow: Int,
)

private fun buildAnimatedBoardCells(
    previousBoard: BoardMatrix,
    currentBoard: BoardMatrix,
): List<AnimatedBoardCell> {
    val previousByColumnAndKey = buildMap<Int, MutableMap<Int, ArrayDeque<Int>>> {
        for (column in 0 until previousBoard.columns) {
            val grouped = mutableMapOf<Int, ArrayDeque<Int>>()
            for (row in 0 until previousBoard.rows) {
                val cell = previousBoard.cellAt(column, row) ?: continue
                val key = boardCellKey(cell)
                grouped.getOrPut(key) { ArrayDeque() }.addLast(row)
            }
            if (grouped.isNotEmpty()) put(column, grouped)
        }
    }

    return buildList {
        for (column in 0 until currentBoard.columns) {
            for (row in 0 until currentBoard.rows) {
                val cell = currentBoard.cellAt(column, row) ?: continue
                val key = boardCellKey(cell)
                val sourceRow = previousByColumnAndKey[column]?.get(key)?.removeFirstOrNull() ?: row
                add(AnimatedBoardCell(point = GridPoint(column = column, row = row), cell = cell, sourceRow = sourceRow))
            }
        }
    }
}

private fun boardCellKey(cell: BoardCell): Int = (cell.tone.ordinal shl 8) or cell.special.ordinal

private fun <T> ArrayDeque<T>.removeFirstOrNull(): T? = if (isEmpty()) null else removeFirst()

internal fun specialBlockIconTint(
    style: BlockVisualStyle,
    isDarkTheme: Boolean,
): Color = when (style) {
    BlockVisualStyle.Outline, BlockVisualStyle.LiquidGlass -> {
        if (isDarkTheme) Color.White else Color.Black
    }
    else -> Color.White
}

private fun DrawScope.drawWoodGrain(
    topLeft: Offset,
    size: Size,
    alpha: Float,
) {
    val lineWidth = size.height * 0.028f
    drawLine(
        color = Color.White.copy(alpha = alpha),
        start = topLeft + Offset(size.width * 0.08f, size.height * 0.22f),
        end = topLeft + Offset(size.width * 0.92f, size.height * 0.38f),
        strokeWidth = lineWidth,
    )
    drawLine(
        color = Color.Black.copy(alpha = alpha * 0.72f),
        start = topLeft + Offset(size.width * 0.08f, size.height * 0.42f),
        end = topLeft + Offset(size.width * 0.92f, size.height * 0.58f),
        strokeWidth = lineWidth,
    )
    drawLine(
        color = Color.White.copy(alpha = alpha * 0.70f),
        start = topLeft + Offset(size.width * 0.08f, size.height * 0.62f),
        end = topLeft + Offset(size.width * 0.92f, size.height * 0.78f),
        strokeWidth = lineWidth,
    )
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
        .background(baseColor, shape = shape)

    BlockVisualStyle.Bubble -> Modifier
        .border(
            width = if (hasSpecial) 1.6.dp else 0.9.dp,
            color = if (hasSpecial) specialColor else baseColor.copy(alpha = 0.64f),
            shape = shape,
        )
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    baseColor.copy(alpha = 1f),
                    baseColor.copy(alpha = 0.84f),
                    baseColor.copy(alpha = 0.64f),
                ),
            ),
            shape = shape,
        )

    BlockVisualStyle.Outline -> Modifier
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    baseColor.copy(alpha = if (hasSpecial) 0.28f else 0.18f),
                    baseColor.copy(alpha = if (hasSpecial) 0.18f else 0.10f),
                ),
            ),
            shape = shape,
        )
        .border(
            width = if (hasSpecial) 2.2.dp else 1.8.dp,
            color = baseColor.copy(alpha = 0.98f),
            shape = shape,
        )

    BlockVisualStyle.Sharp3D -> Modifier
        .border(
            width = if (hasSpecial) 1.4.dp else 1.dp,
            color = if (hasSpecial) specialColor else baseColor.copy(alpha = 0.48f),
            shape = shape,
        )
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    baseColor.copy(alpha = 1f),
                    baseColor.copy(alpha = 0.84f),
                    baseColor.copy(alpha = 0.52f),
                ),
            ),
            shape = shape,
        )

    BlockVisualStyle.Wood -> Modifier
        .border(
            width = if (hasSpecial) 1.2.dp else 0.9.dp,
            color = if (hasSpecial) specialColor else baseColor.copy(alpha = 0.58f),
            shape = shape,
        )
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    baseColor.copy(alpha = 0.98f),
                    baseColor.copy(alpha = 0.88f),
                    baseColor.copy(alpha = 0.72f),
                ),
            ),
            shape = shape,
        )

    BlockVisualStyle.LiquidGlass -> Modifier
        .border(
            width = if (hasSpecial) 1.7.dp else 1.dp,
            color = if (hasSpecial) specialColor else Color.White.copy(alpha = 0.54f),
            shape = shape,
        )
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.16f),
                    baseColor.copy(alpha = 0.34f),
                    baseColor.copy(alpha = 0.18f),
                ),
            ),
            shape = shape,
        )

    BlockVisualStyle.Neon -> Modifier
        .border(
            width = if (hasSpecial) 1.7.dp else 1.2.dp,
            color = if (hasSpecial) specialColor else baseColor.copy(alpha = 0.76f),
            shape = shape,
        )
        .background(
            brush = Brush.radialGradient(
                colors = listOf(
                    baseColor.copy(alpha = 1f),
                    baseColor.copy(alpha = 0.78f),
                    baseColor.copy(alpha = 0.18f),
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
    val settings = LocalAppSettings.current
    val isDarkTheme = isStackShiftDarkTheme(settings)
    val cellShape = RoundedCornerShape(boardCellCornerRadiusDp(size, style))
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
        contentAlignment = Alignment.Center,
    ) {
        if (special != SpecialBlockType.None) {
            Icon(
                imageVector = boardSpecialIcon(special),
                contentDescription = null,
                tint = specialBlockIconTint(style = style, isDarkTheme = isDarkTheme),
                modifier = Modifier
                    .size((size.value * 0.36f).dp),
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
    cellCornerRadius: Dp? = null,
    blockContent: @Composable BoxScope.() -> Unit = {},
) {
    Box(
        modifier = modifier.size(
            width = cellSize * piece.width,
            height = cellSize * piece.height,
        ),
    ) {
        val settings = LocalAppSettings.current
        val density = LocalDensity.current
        val visualStyle = resolveBoardBlockStyle(
            selectedStyle = settings.blockVisualStyle,
            mode = settings.boardBlockStyleMode,
        )
        val isDarkTheme = isStackShiftDarkTheme(settings)
        val resolvedCellCornerRadius = cellCornerRadius ?: boardCellCornerRadiusDp(cellSize, visualStyle)
        val cellCornerRadiusPx = with(density) { resolvedCellCornerRadius.toPx() }
        piece.cells.forEach { cell ->
            Box(
                modifier = Modifier
                    .offset(x = cellSize * cell.column, y = cellSize * cell.row)
                    .size(cellSize)
                    .padding(cellInset),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCellBody(
                        tone = piece.tone,
                        palette = settings.blockColorPalette,
                        style = visualStyle,
                        special = piece.special,
                        topLeft = Offset.Zero,
                        size = size,
                        cornerRadius = CornerRadius(cellCornerRadiusPx, cellCornerRadiusPx),
                        alpha = alpha,
                    )
                }
                if (piece.special != SpecialBlockType.None) {
                    Icon(
                        imageVector = boardSpecialIcon(piece.special),
                        contentDescription = null,
                        tint = specialBlockIconTint(style = visualStyle, isDarkTheme = isDarkTheme),
                        modifier = Modifier
                            .size((cellSize.value * 0.40f).dp),
                    )
                }
            }
        }
        blockContent()
    }
}


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
    alpha: Float = 1f,
) {
    val baseColor = tone.paletteColor(palette).copy(alpha = alpha.coerceIn(0f, 1f))
    val specialColor = specialAccentColor(special).copy(alpha = alpha.coerceIn(0f, 1f))
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
                        baseColor.copy(alpha = 0.88f),
                        baseColor.copy(alpha = 0.62f),
                    ),
                ),
                topLeft = topLeft,
                size = size,
                cornerRadius = CornerRadius(cornerRadius.x * 1.22f, cornerRadius.y * 1.22f),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.22f),
                topLeft = topLeft + Offset(size.width * 0.10f, size.height * 0.10f),
                size = Size(size.width * 0.34f, size.height * 0.24f),
                cornerRadius = CornerRadius(cornerRadius.x * 0.94f, cornerRadius.y * 0.94f),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.10f),
                topLeft = topLeft + Offset(size.width * 0.12f, size.height * 0.24f),
                size = Size(size.width * 0.60f, size.height * 0.10f),
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
                brush = Brush.verticalGradient(
                    colors = listOf(
                        baseColor.copy(alpha = 0.28f),
                        baseColor.copy(alpha = 0.14f),
                    ),
                ),
                topLeft = topLeft,
                size = size,
                cornerRadius = cornerRadius,
            )
            drawRoundRect(
                color = baseColor.copy(alpha = 0.96f),
                topLeft = topLeft,
                size = size,
                cornerRadius = cornerRadius,
                style = Stroke(width = 2.2f),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.10f),
                topLeft = topLeft + Offset(size.width * 0.06f, size.height * 0.06f),
                size = Size(size.width * 0.88f, size.height * 0.88f),
                cornerRadius = CornerRadius(cornerRadius.x * 0.86f, cornerRadius.y * 0.86f),
                style = Stroke(width = 0.9f),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.10f),
                topLeft = topLeft + Offset(size.width * 0.10f, size.height * 0.10f),
                size = Size(size.width * 0.30f, size.height * 0.16f),
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
                    baseColor.copy(alpha = 0.86f),
                    baseColor.copy(alpha = 0.54f),
                ),
            ),
            topLeft = topLeft,
            size = size,
            cornerRadius = CornerRadius(cornerRadius.x * 0.58f, cornerRadius.y * 0.58f),
        ).also {
            drawRoundRect(
                color = Color.White.copy(alpha = 0.18f),
                topLeft = topLeft + Offset(size.width * 0.06f, size.height * 0.08f),
                size = Size(size.width * 0.84f, size.height * 0.18f),
                cornerRadius = CornerRadius(cornerRadius.x * 0.34f, cornerRadius.y * 0.34f),
            )
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.14f),
                topLeft = topLeft + Offset(size.width * 0.06f, size.height * 0.66f),
                size = Size(size.width * 0.88f, size.height * 0.22f),
                cornerRadius = CornerRadius(cornerRadius.x * 0.30f, cornerRadius.y * 0.30f),
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
            brush = Brush.verticalGradient(
                colors = listOf(
                    baseColor.copy(alpha = 0.98f),
                    baseColor.copy(alpha = 0.90f),
                    baseColor.copy(alpha = 0.74f),
                ),
            ),
            topLeft = topLeft,
            size = size,
            cornerRadius = CornerRadius(cornerRadius.x * 0.86f, cornerRadius.y * 0.80f),
        ).also {
            drawWoodGrain(topLeft = topLeft, size = size, alpha = 0.16f)
            if (special != SpecialBlockType.None) {
                drawRoundRect(
                    color = specialColor,
                    topLeft = topLeft,
                    size = size,
                    cornerRadius = CornerRadius(cornerRadius.x * 0.72f, cornerRadius.y * 0.72f),
                    style = Stroke(width = 1.4f),
                )
            }
        }
        BlockVisualStyle.LiquidGlass -> {
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.14f),
                        baseColor.copy(alpha = 0.34f),
                        baseColor.copy(alpha = 0.18f),
                    ),
                ),
                topLeft = topLeft,
                size = size,
                cornerRadius = CornerRadius(cornerRadius.x * 1.12f, cornerRadius.y * 1.12f),
            )
            drawRoundRect(
                color = baseColor.copy(alpha = 0.18f),
                topLeft = topLeft + Offset(size.width * 0.02f, size.height * 0.02f),
                size = Size(size.width * 0.96f, size.height * 0.96f),
                cornerRadius = CornerRadius(cornerRadius.x, cornerRadius.y),
                style = Stroke(width = 1f),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.22f),
                topLeft = topLeft + Offset(size.width * 0.06f, size.height * 0.06f),
                size = Size(size.width * 0.78f, size.height * 0.36f),
                cornerRadius = CornerRadius(cornerRadius.x * 0.8f, cornerRadius.y * 0.8f),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.08f),
                topLeft = topLeft + Offset(size.width * 0.12f, size.height * 0.50f),
                size = Size(size.width * 0.52f, size.height * 0.14f),
                cornerRadius = CornerRadius(cornerRadius.x * 0.64f, cornerRadius.y * 0.64f),
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
                color = baseColor.copy(alpha = 0.16f),
                topLeft = topLeft - Offset(size.width * 0.06f, size.height * 0.06f),
                size = Size(size.width * 1.12f, size.height * 1.12f),
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
                color = baseColor.copy(alpha = 0.58f),
                topLeft = topLeft,
                size = size,
                cornerRadius = cornerRadius,
                style = Stroke(width = 1.7f),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.14f),
                topLeft = topLeft + Offset(size.width * 0.12f, size.height * 0.14f),
                size = Size(size.width * 0.44f, size.height * 0.14f),
                cornerRadius = CornerRadius(cornerRadius.x * 0.42f, cornerRadius.y * 0.42f),
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
            color = baseColor,
            topLeft = topLeft,
            size = size,
            cornerRadius = cornerRadius,
        )
        BlockVisualStyle.Bubble -> {
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        baseColor.copy(alpha = 0.34f),
                        baseColor.copy(alpha = 0.24f),
                        baseColor.copy(alpha = 0.16f),
                    ),
                ),
                topLeft = topLeft,
                size = size,
                cornerRadius = CornerRadius(cornerRadius.x * 1.22f, cornerRadius.y * 1.22f),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.08f),
                topLeft = topLeft + Offset(size.width * 0.10f, size.height * 0.10f),
                size = Size(size.width * 0.34f, size.height * 0.26f),
                cornerRadius = CornerRadius(cornerRadius.x * 0.94f, cornerRadius.y * 0.94f),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.04f),
                topLeft = topLeft + Offset(size.width * 0.14f, size.height * 0.22f),
                size = Size(size.width * 0.64f, size.height * 0.12f),
                cornerRadius = CornerRadius(cornerRadius.x * 0.42f, cornerRadius.y * 0.42f),
            )
        }
        BlockVisualStyle.Outline -> {
            drawRoundRect(
                color = baseColor.copy(alpha = 0.10f),
                topLeft = topLeft,
                size = size,
                cornerRadius = cornerRadius,
            )
            drawRoundRect(
                color = baseColor.copy(alpha = 0.28f),
                topLeft = topLeft,
                size = size,
                cornerRadius = cornerRadius,
                style = Stroke(width = 1.4f),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.05f),
                topLeft = topLeft + Offset(size.width * 0.08f, size.height * 0.08f),
                size = Size(size.width * 0.84f, size.height * 0.84f),
                cornerRadius = CornerRadius(cornerRadius.x * 0.82f, cornerRadius.y * 0.82f),
                style = Stroke(width = 0.8f),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.05f),
                topLeft = topLeft + Offset(size.width * 0.10f, size.height * 0.10f),
                size = Size(size.width * 0.30f, size.height * 0.16f),
                cornerRadius = CornerRadius(cornerRadius.x * 0.7f, cornerRadius.y * 0.7f),
            )
        }
        BlockVisualStyle.Sharp3D -> drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    baseColor.copy(alpha = 0.38f),
                    baseColor.copy(alpha = 0.26f),
                    baseColor.copy(alpha = 0.16f),
                ),
            ),
            topLeft = topLeft,
            size = size,
            cornerRadius = CornerRadius(cornerRadius.x * 0.72f, cornerRadius.y * 0.72f),
        ).also {
            drawRoundRect(
                color = Color.White.copy(alpha = 0.08f),
                topLeft = topLeft + Offset(size.width * 0.06f, size.height * 0.08f),
                size = Size(size.width * 0.86f, size.height * 0.16f),
                cornerRadius = CornerRadius(cornerRadius.x * 0.42f, cornerRadius.y * 0.42f),
            )
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.04f),
                topLeft = topLeft + Offset(size.width * 0.08f, size.height * 0.70f),
                size = Size(size.width * 0.84f, size.height * 0.18f),
                cornerRadius = CornerRadius(cornerRadius.x * 0.34f, cornerRadius.y * 0.34f),
            )
        }
        BlockVisualStyle.Wood -> drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    baseColor.copy(alpha = 0.36f),
                    baseColor.copy(alpha = 0.26f),
                    baseColor.copy(alpha = 0.18f),
                ),
            ),
            topLeft = topLeft,
            size = size,
            cornerRadius = CornerRadius(cornerRadius.x * 0.90f, cornerRadius.y * 0.84f),
        ).also {
            drawWoodGrain(topLeft = topLeft, size = size, alpha = 0.06f)
        }
        BlockVisualStyle.LiquidGlass -> {
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.08f),
                        baseColor.copy(alpha = 0.18f),
                        baseColor.copy(alpha = 0.10f),
                    ),
                ),
                topLeft = topLeft,
                size = size,
                cornerRadius = CornerRadius(cornerRadius.x * 1.16f, cornerRadius.y * 1.16f),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.06f),
                topLeft = topLeft + Offset(size.width * 0.04f, size.height * 0.05f),
                size = Size(size.width * 0.86f, size.height * 0.28f),
                cornerRadius = CornerRadius(cornerRadius.x, cornerRadius.y),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.05f),
                topLeft = topLeft + Offset(size.width * 0.10f, size.height * 0.50f),
                size = Size(size.width * 0.56f, size.height * 0.18f),
                cornerRadius = CornerRadius(cornerRadius.x * 0.72f, cornerRadius.y * 0.72f),
                style = Stroke(width = 1f),
            )
        }
        BlockVisualStyle.Neon -> {
            drawRoundRect(
                color = baseColor.copy(alpha = 0.12f),
                topLeft = topLeft - Offset(size.width * 0.06f, size.height * 0.06f),
                size = Size(size.width * 1.12f, size.height * 1.12f),
                cornerRadius = CornerRadius(cornerRadius.x * 1.15f, cornerRadius.y * 1.15f),
            )
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        baseColor.copy(alpha = 0.32f),
                        baseColor.copy(alpha = 0.18f),
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
                color = baseColor.copy(alpha = 0.24f),
                topLeft = topLeft,
                size = size,
                cornerRadius = cornerRadius,
                style = Stroke(width = 1.2f),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.06f),
                topLeft = topLeft + Offset(size.width * 0.14f, size.height * 0.16f),
                size = Size(size.width * 0.36f, size.height * 0.10f),
                cornerRadius = CornerRadius(cornerRadius.x * 0.40f, cornerRadius.y * 0.40f),
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
    style: BlockVisualStyle,
): BoardCellVisual {
    val minSize = minOf(cellWidth, cellHeight)
    val fillInset = (minSize * 0.085f).coerceIn(1.5f, 3.2f)
    val previewInset = (minSize * 0.06f).coerceIn(1.4f, 2.4f)
    val specialInset = (minSize * 0.14f).coerceIn(4f, 6f)
    val outerRadius = minSize * 0.16f * blockStyleCornerScale(style)
    return BoardCellVisual(
        fillInsetPx = fillInset,
        previewInsetPx = previewInset,
        specialInsetPx = specialInset,
        cornerRadius = CornerRadius(outerRadius, outerRadius),
        innerCornerRadiusPx = outerRadius * when (style) {
            BlockVisualStyle.Flat -> 0.78f
            BlockVisualStyle.Bubble -> 0.92f
            BlockVisualStyle.Outline -> 0.70f
            BlockVisualStyle.Sharp3D -> 0.52f
            BlockVisualStyle.Wood -> 0.66f
            BlockVisualStyle.LiquidGlass -> 0.84f
            BlockVisualStyle.Neon -> 0.86f
        },
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
internal fun boardCellCornerRadiusDp(
    cellSize: Dp,
    style: BlockVisualStyle,
): Dp {
    val density = LocalDensity.current
    return with(density) {
        (cellSize.toPx() * 0.16f * blockStyleCornerScale(style)).toDp()
    }
}

internal fun boardFrameCornerRadiusDp(style: BlockVisualStyle): Dp = when (style) {
    BlockVisualStyle.Flat -> 18.dp
    BlockVisualStyle.Bubble -> 20.dp
    BlockVisualStyle.Outline -> 16.dp
    BlockVisualStyle.Sharp3D -> 10.dp
    BlockVisualStyle.Wood -> 15.dp
    BlockVisualStyle.LiquidGlass -> 18.dp
    BlockVisualStyle.Neon -> 18.dp
}

private fun blockStyleCornerScale(style: BlockVisualStyle): Float = when (style) {
    BlockVisualStyle.Flat -> 1.0f
    BlockVisualStyle.Bubble -> 1.20f
    BlockVisualStyle.Outline -> 0.88f
    BlockVisualStyle.Sharp3D -> 0.58f
    BlockVisualStyle.Wood -> 0.90f
    BlockVisualStyle.LiquidGlass -> 1.08f
    BlockVisualStyle.Neon -> 1.12f
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
