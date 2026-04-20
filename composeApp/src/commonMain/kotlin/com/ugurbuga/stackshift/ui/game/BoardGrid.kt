package com.ugurbuga.stackshift.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
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

private const val DefaultBoardClearFlashDurationMillis = 420
private const val DefaultBoardShiftDurationMillis = 220
private val BoardFrameInset = 1.dp

@Composable
fun BoardGrid(
    modifier: Modifier = Modifier,
    gameState: GameState,
    preview: PlacementPreview?,
    impactedPreviewCells: Set<GridPoint>,
    guidedColumns: Set<Int> = emptySet(),
    activeColumn: Int?,
    activePiece: Piece?,
    isDragging: Boolean,
    gameOverClearProgressProvider: () -> Float = { 0f },
    showClearFlash: Boolean = true,
    showPreviewClearGuides: Boolean = true,
    stylePulse: Float = 0f,
    clearFlashDurationMillis: Int = DefaultBoardClearFlashDurationMillis,
    boardShiftDurationMillis: Int = DefaultBoardShiftDurationMillis,
) {
    val settings = LocalAppSettings.current
    val colorScheme = MaterialTheme.colorScheme
    val uiColors = StackShiftThemeTokens.uiColors
    val boardBlockStyle = resolveBoardBlockStyle(
        selectedStyle = settings.blockVisualStyle,
        mode = settings.boardBlockStyleMode,
    )
    val isDarkTheme = isStackShiftDarkTheme(settings)
    val showDangerDecorations = gameState.status != GameStatus.GameOver
    val hasDangerPulse = showDangerDecorations && (
        gameState.criticalColumns.isNotEmpty() ||
            gameState.columnPressure.any { pressure ->
                pressure.level == PressureLevel.Critical || pressure.level == PressureLevel.Overflow
            }
        )
    val hasImpactPulse = impactedPreviewCells.isNotEmpty() || (preview?.clearedRows?.isNotEmpty() == true) || (preview?.clearedColumns?.isNotEmpty() == true)
    var clearFlashAlpha by remember { mutableFloatStateOf(0f) }
    var clearFlashSweepProgress by remember { mutableFloatStateOf(1f) }
    var boardTransitionSource by remember { mutableStateOf(gameState.board) }
    var boardTransitionToken by remember { mutableLongStateOf(gameState.clearAnimationToken) }
    val boardShiftProgress = remember { Animatable(1f) }
    val shouldAnimateBoardShift =
        gameState.status == GameStatus.Running && gameState.clearAnimationToken != boardTransitionToken
    val animatedBoardCells = remember(boardTransitionSource, gameState.board, shouldAnimateBoardShift) {
        if (shouldAnimateBoardShift) {
            buildAnimatedBoardCells(boardTransitionSource, gameState.board)
        } else {
            emptyList()
        }
    }

    val boardEffectsTransition = if (hasDangerPulse || hasImpactPulse) {
        rememberInfiniteTransition(label = "boardEffectsTransition")
    } else {
        null
    }
    val dangerPulseState = if (hasDangerPulse) {
        boardEffectsTransition!!.animateFloat(
            initialValue = 0.2f,
            targetValue = 0.55f,
            animationSpec = infiniteRepeatable(animation = tween(durationMillis = 900, easing = FastOutSlowInEasing)),
            label = "dangerPulseAlpha",
        )
    } else {
        remember { mutableFloatStateOf(0.2f) }
    }
    val dangerSweepState = if (hasDangerPulse) {
        boardEffectsTransition!!.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing)),
            label = "dangerSweep",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    val impactPulseState = if (hasImpactPulse) {
        boardEffectsTransition!!.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 620, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "impactPulse",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    val impactSweepState = if (hasImpactPulse) {
        boardEffectsTransition!!.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(animation = tween(durationMillis = 920, easing = FastOutSlowInEasing)),
            label = "impactSweep",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    val shouldAnimateStylePulse = stylePulse == 0f && boardBlockStyle == BlockVisualStyle.DynamicLiquid
    val stylePulseTransition = if (shouldAnimateStylePulse) {
        rememberInfiniteTransition(label = "stylePulse")
    } else {
        null
    }
    val stylePulseState = if (shouldAnimateStylePulse) {
        stylePulseTransition!!.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "stylePulse",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    LaunchedEffect(gameState.clearAnimationToken) {
        if (!showClearFlash) {
            clearFlashAlpha = 0f
            clearFlashSweepProgress = 1f
            return@LaunchedEffect
        }
        if (gameState.recentlyClearedRows.isEmpty() && gameState.recentlyClearedColumns.isEmpty()) return@LaunchedEffect
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(durationMillis = clearFlashDurationMillis, easing = FastOutSlowInEasing),
        ) { value, _ ->
            clearFlashSweepProgress = value
            clearFlashAlpha = 0.9f * (1f - value)
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
            animationSpec = tween(durationMillis = boardShiftDurationMillis, easing = FastOutSlowInEasing),
        )
        boardTransitionSource = gameState.board
        boardTransitionToken = gameState.clearAnimationToken
    }

    BoxWithConstraints(
        modifier = modifier
            .padding(BoardFrameInset)
            .clip(RoundedCornerShape(boardFrameCornerRadiusDp(boardBlockStyle)))
    ) {
        val density = LocalDensity.current
        val cellWidth = maxWidth / gameState.config.columns
        val cellHeight = maxHeight / gameState.config.rows
        val cellWidthPx = with(density) { cellWidth.toPx() }
        val cellHeightPx = with(density) { cellHeight.toPx() }
        val boardCornerRadiusPx = with(density) { boardFrameCornerRadiusDp(boardBlockStyle).toPx() }
        val cellVisual = boardCellVisual(
            cellWidth = cellWidthPx,
            cellHeight = cellHeightPx,
            style = boardBlockStyle,
        )
        val emptySlotVisual = remember(cellWidthPx, cellHeightPx) {
            boardCellVisual(
                cellWidth = cellWidthPx,
                cellHeight = cellHeightPx,
                style = BlockVisualStyle.Flat,
            )
        }
        val hasCriticalColumns = gameState.criticalColumns.isNotEmpty()
        val pieceToneColor = remember(activePiece?.id, settings.blockColorPalette) {
            activePiece?.tone?.paletteColor(settings.blockColorPalette)
        }
        val effectivePreviewColor = pieceToneColor ?: uiColors.success
        val previewCellInsetPx = cellVisual.fillInsetPx
        val previewCellSize = remember(cellWidthPx, cellHeightPx, previewCellInsetPx) {
            Size(
                width = cellWidthPx - (previewCellInsetPx * 2),
                height = cellHeightPx - (previewCellInsetPx * 2),
            )
        }
        val previewCellInsetOffset = remember(previewCellInsetPx) {
            Offset(previewCellInsetPx, previewCellInsetPx)
        }
        val previewIconPadding = boardCellInsetDp(minOf(cellWidth, cellHeight))
        val previewAlpha = remember(isDarkTheme, isDragging) {
            when {
                isDarkTheme && isDragging -> 0.24f
                isDarkTheme -> 0.18f
                isDragging -> 0.28f
                else -> 0.24f
            }
        }
        val previewColumnBounds = remember(preview, cellHeightPx, cellVisual.previewInsetPx) {
            preview?.occupiedCells
                ?.groupBy(GridPoint::column)
                ?.mapValues { (_, points) ->
                    val highestOccupiedRow = points.minOf(GridPoint::row)
                    val lowestOccupiedRow = points.maxOf(GridPoint::row)
                    val top = (highestOccupiedRow * cellHeightPx) + cellVisual.previewInsetPx
                    val bottom = ((lowestOccupiedRow + 1) * cellHeightPx) - cellVisual.previewInsetPx
                    top to bottom
                }
        }
        val highlightedColumns = remember(preview, activePiece?.id, activeColumn, gameState.config.columns) {
            preview?.coveredColumns ?: activePiece?.let { piece ->
                activeColumn?.coerceIn(0, (gameState.config.columns - piece.width).coerceAtLeast(0))?.let { column ->
                    column..<(column + piece.width)
                }
            }
        }
        val guideColumns = remember(guidedColumns, highlightedColumns) {
            guidedColumns.ifEmpty { highlightedColumns?.toSet().orEmpty() }
        }
        val clearProgress = gameOverClearProgressProvider().coerceIn(0f, 1f)
        val useStaticSlotLayer = clearProgress <= 0f && !hasCriticalColumns
        val currentBoardSpecialCells = remember(gameState.board) {
            buildList {
                for (row in 0 until gameState.board.rows) {
                    for (column in 0 until gameState.board.columns) {
                        val cell = gameState.board.cellAt(column, row) ?: continue
                        if (cell.special != SpecialBlockType.None) {
                            add(Triple(column, row, cell))
                        }
                    }
                }
            }
        }
        val animatedSpecialCells = remember(animatedBoardCells) {
            animatedBoardCells.filter { it.cell.special != SpecialBlockType.None }
        }

        BoardGridBackgroundLayer(
            uiColors = uiColors,
            boardDecorAlphaProvider = {
                (1f - clearProgress).coerceIn(0f, 1f)
            },
            boardCornerRadiusPx = boardCornerRadiusPx,
            modifier = Modifier.fillMaxSize(),
        )

        if (useStaticSlotLayer) {
            BoardGridSlotLayer(
                rows = gameState.config.rows,
                columns = gameState.config.columns,
                cellWidthPx = cellWidthPx,
                cellHeightPx = cellHeightPx,
                slotVisual = emptySlotVisual,
                uiColors = uiColors,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val clearRows = clearProgress * gameState.config.rows
            val fullyClearedRows = clearRows.toInt().coerceIn(0, gameState.config.rows)
            val partialClearAlpha = 1f - (clearRows - fullyClearedRows).coerceIn(0f, 1f)
            val boardDecorAlpha = (1f - clearProgress).coerceIn(0f, 1f)

            val dangerPulse = dangerPulseState.value
            val dangerSweep = dangerSweepState.value
            val impactPulse = impactPulseState.value
            val impactSweep = impactSweepState.value
            val stylePulseInternal = stylePulseState.value
            val effectiveStylePulse = if (stylePulse != 0f) stylePulse else stylePulseInternal

            val previewGuideAlpha = when {
                isDarkTheme && isDragging -> 0.12f
                isDarkTheme -> 0.10f
                isDragging -> 0.18f
                else -> 0.15f
            }
            val previewLineColor = effectivePreviewColor.copy(alpha = previewGuideAlpha * boardDecorAlpha)
            val previewTone = activePiece?.tone ?: CellTone.Cyan

            val previewBeamAlpha = when {
                isDarkTheme && isDragging -> 0.24f
                isDarkTheme -> 0.14f
                isDragging -> 0.30f
                else -> 0.22f
            }
            val cornerRadius = cellVisual.cornerRadius
            val boardWidthPx = size.width
            val boardHeightPx = size.height
            val boardFrameCornerRadius = CornerRadius(boardCornerRadiusPx, boardCornerRadiusPx)

            highlightedColumns?.forEach { column ->
                val beamAlpha = previewBeamAlpha * boardDecorAlpha
                if (beamAlpha <= 0f || !showDangerDecorations) return@forEach
                val (barTop, barBottom) = previewColumnBounds?.get(column) ?: return@forEach
                val barHeight = (barBottom - barTop).coerceAtLeast(0f)
                if (barHeight <= 0f) return@forEach

                drawRoundRect(
                    color = previewLineColor,
                    topLeft = Offset(x = column * cellWidthPx, y = barTop),
                    size = Size(width = cellWidthPx, height = barHeight),
                    cornerRadius = cornerRadius,
                )
            }

            guideColumns.forEach { column ->
                if (column !in 0 until gameState.config.columns) return@forEach
                val (barTop, barBottom) = previewColumnBounds?.get(column) ?: return@forEach
                val barHeight = (barBottom - barTop).coerceAtLeast(0f)
                if (barHeight <= 0f) return@forEach
                drawRoundRect(
                    color = effectivePreviewColor.copy(alpha = 0.14f * boardDecorAlpha),
                    topLeft = Offset(x = column * cellWidthPx, y = barTop),
                    size = Size(width = cellWidthPx, height = barHeight),
                    cornerRadius = cornerRadius,
                )
            }

            if (showPreviewClearGuides) {
                preview?.clearedRows?.forEach { row ->
                    drawClearEffectBand(
                        orientation = ClearEffectOrientation.Row,
                        index = row,
                        cellWidthPx = cellWidthPx,
                        cellHeightPx = cellHeightPx,
                        boardWidthPx = boardWidthPx,
                        boardHeightPx = boardHeightPx,
                        cornerRadius = cornerRadius,
                        accentColor = effectivePreviewColor,
                        fillAlpha = (0.08f + 0.12f * impactPulse) * boardDecorAlpha,
                        outlineAlpha = (0.18f + 0.16f * impactPulse) * boardDecorAlpha,
                        sweepProgress = impactSweep,
                        sweepAlpha = (0.16f + 0.26f * impactPulse) * boardDecorAlpha,
                    )
                }

                preview?.clearedColumns?.forEach { column ->
                    drawClearEffectBand(
                        orientation = ClearEffectOrientation.Column,
                        index = column,
                        cellWidthPx = cellWidthPx,
                        cellHeightPx = cellHeightPx,
                        boardWidthPx = boardWidthPx,
                        boardHeightPx = boardHeightPx,
                        cornerRadius = cornerRadius,
                        accentColor = effectivePreviewColor,
                        fillAlpha = (0.08f + 0.12f * impactPulse) * boardDecorAlpha,
                        outlineAlpha = (0.18f + 0.16f * impactPulse) * boardDecorAlpha,
                        sweepProgress = impactSweep,
                        sweepAlpha = (0.16f + 0.26f * impactPulse) * boardDecorAlpha,
                    )
                }
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

            if (hasCriticalColumns) {
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
                    if (!useStaticSlotLayer) {
                        drawFlatBoardEmptySlot(
                            topLeft = topLeft,
                            cellSize = cellSize,
                            slotVisual = emptySlotVisual,
                            uiColors = uiColors,
                            alpha = renderedCellAlpha,
                        )
                        val gridLineColor = if (showDangerDecorations && hasCriticalColumns) {
                            uiColors.danger.copy(alpha = (0.10f + (dangerPulse * 0.26f)) * renderedCellAlpha)
                        } else {
                            uiColors.boardGridLine.copy(alpha = 0.12f * renderedCellAlpha)
                        }
                        drawRoundRect(
                            color = gridLineColor,
                            topLeft = topLeft,
                            size = cellSize,
                            cornerRadius = cornerRadius,
                            style = Stroke(width = 1f),
                        )

                        if (showDangerDecorations && hasCriticalColumns) {
                            drawRoundRect(
                                color = uiColors.danger.copy(alpha = (0.04f + (dangerPulse * 0.18f)) * renderedCellAlpha),
                                topLeft = topLeft,
                                size = cellSize,
                                cornerRadius = cornerRadius,
                                style = Stroke(width = 1.4f),
                            )
                        }
                    }

                    if (!shouldAnimateBoardShift) {
                        gameState.board.cellAt(column, row)?.let { cell ->
                            val isImpactedByPreview = GridPoint(column, row) in impactedPreviewCells
                            drawCellBody(
                                tone = cell.tone,
                                palette = settings.blockColorPalette,
                                style = boardBlockStyle,
                                topLeft = topLeft + Offset(cellVisual.fillInsetPx, cellVisual.fillInsetPx),
                                size = Size(
                                    width = cellWidthPx - (cellVisual.fillInsetPx * 2),
                                    height = cellHeightPx - (cellVisual.fillInsetPx * 2),
                                ),
                                cornerRadius = cornerRadius,
                                alpha = renderedCellAlpha,
                                pulse = effectiveStylePulse,
                            )

                            if (isImpactedByPreview) {
                                val warningInset = (cellVisual.previewInsetPx - (impactPulse * 0.8f)).coerceAtLeast(0.7f)
                                val diagonalInset = warningInset + (minOf(cellWidthPx, cellHeightPx) * 0.18f)
                                val crossStroke = 1.8f + (impactPulse * 1.8f)
                                drawRoundRect(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            uiColors.warning.copy(alpha = 0.22f + (impactPulse * 0.16f)),
                                            uiColors.danger.copy(alpha = 0.16f + (impactPulse * 0.22f)),
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
                                    color = uiColors.danger.copy(alpha = (0.10f + impactPulse * 0.20f) * renderedCellAlpha),
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
                                            uiColors.danger.copy(alpha = 1f * renderedCellAlpha),
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
                landing.occupiedCells.forEach { point ->
                    val topLeft = Offset(
                        x = point.column * cellWidthPx,
                        y = point.row * cellHeightPx,
                    )
                    drawCellBody(
                        tone = previewTone,
                        palette = settings.blockColorPalette,
                        style = boardBlockStyle,
                        topLeft = topLeft + previewCellInsetOffset,
                        size = previewCellSize,
                        cornerRadius = cornerRadius,
                        alpha = previewAlpha * boardDecorAlpha,
                        pulse = effectiveStylePulse,
                    )
                }
            }

            if (showClearFlash && clearFlashAlpha > 0f) {
                gameState.recentlyClearedRows.forEach { row ->
                    drawClearEffectBand(
                        orientation = ClearEffectOrientation.Row,
                        index = row,
                        cellWidthPx = cellWidthPx,
                        cellHeightPx = cellHeightPx,
                        boardWidthPx = boardWidthPx,
                        boardHeightPx = boardHeightPx,
                        cornerRadius = cornerRadius,
                        accentColor = Color.White,
                        fillAlpha = clearFlashAlpha * boardDecorAlpha,
                        outlineAlpha = (clearFlashAlpha * 0.84f) * boardDecorAlpha,
                        sweepProgress = clearFlashSweepProgress,
                        sweepAlpha = (clearFlashAlpha * 0.9f) * boardDecorAlpha,
                    )
                }
                gameState.recentlyClearedColumns.forEach { column ->
                    drawClearEffectBand(
                        orientation = ClearEffectOrientation.Column,
                        index = column,
                        cellWidthPx = cellWidthPx,
                        cellHeightPx = cellHeightPx,
                        boardWidthPx = boardWidthPx,
                        boardHeightPx = boardHeightPx,
                        cornerRadius = cornerRadius,
                        accentColor = Color.White,
                        fillAlpha = clearFlashAlpha * boardDecorAlpha,
                        outlineAlpha = (clearFlashAlpha * 0.84f) * boardDecorAlpha,
                        sweepProgress = clearFlashSweepProgress,
                        sweepAlpha = (clearFlashAlpha * 0.9f) * boardDecorAlpha,
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
                    topLeft = animatedTopLeft + Offset(cellVisual.fillInsetPx, cellVisual.fillInsetPx),
                    size = Size(
                        width = cellWidthPx - (cellVisual.fillInsetPx * 2),
                        height = cellHeightPx - (cellVisual.fillInsetPx * 2),
                    ),
                    cornerRadius = cornerRadius,
                    alpha = boardDecorAlpha,
                    pulse = effectiveStylePulse,
                )
                }
            }
        }

        if (shouldAnimateBoardShift) {
            animatedSpecialCells.forEach { animatedCell ->
                if (animatedCell.cell.special == SpecialBlockType.None) return@forEach
                BoardSpecialIconOverlay(
                    cell = animatedCell.cell,
                    column = animatedCell.point.column,
                    row = animatedCell.point.row,
                    cellWidth = cellWidth,
                    cellHeight = cellHeight,
                    translationYProvider = {
                        val sourceRow = animatedCell.sourceRow.toFloat()
                        val targetRow = animatedCell.point.row.toFloat()
                        val animatedRow =
                            sourceRow + ((targetRow - sourceRow) * boardShiftProgress.value)
                        (animatedRow - animatedCell.point.row) * cellHeightPx
                    },
                    alphaProvider = {
                        val clearProgress = gameOverClearProgressProvider().coerceIn(0f, 1f)
                        (1f - clearProgress).coerceIn(0f, 1f)
                    },
                )
            }
        } else {
            currentBoardSpecialCells.forEach { (column, row, cell) ->
                BoardSpecialIconOverlay(
                    cell = cell,
                    column = column,
                    row = row,
                    cellWidth = cellWidth,
                    cellHeight = cellHeight,
                    alphaProvider = {
                        val clearProgress = gameOverClearProgressProvider().coerceIn(0f, 1f)
                        (1f - clearProgress).coerceIn(0f, 1f)
                    },
                )
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
                    contentPadding = previewIconPadding,
                    alphaProvider = {
                        val clearProgress = gameOverClearProgressProvider().coerceIn(0f, 1f)
                        (1f - clearProgress).coerceIn(0f, 1f)
                    },
                    iconAlpha = previewAlpha,
                    iconSizeRatio = 0.40f,
                )
            }
        }
    }
}

private enum class ClearEffectOrientation {
    Row,
    Column,
}

private fun DrawScope.drawClearEffectBand(
    orientation: ClearEffectOrientation,
    index: Int,
    cellWidthPx: Float,
    cellHeightPx: Float,
    boardWidthPx: Float,
    boardHeightPx: Float,
    cornerRadius: CornerRadius,
    accentColor: Color,
    fillAlpha: Float,
    outlineAlpha: Float,
    sweepProgress: Float,
    sweepAlpha: Float,
) {
    when (orientation) {
        ClearEffectOrientation.Row -> {
            val top = index * cellHeightPx
            val bandSize = Size(width = boardWidthPx, height = cellHeightPx)
            val sweepWidth = maxOf(boardWidthPx * 0.28f, cellHeightPx * 1.8f)
            val sweepLeft = ((boardWidthPx + sweepWidth) * sweepProgress) - sweepWidth
            drawRoundRect(
                color = accentColor.copy(alpha = fillAlpha.coerceIn(0f, 1f)),
                topLeft = Offset(x = 0f, y = top),
                size = bandSize,
                cornerRadius = cornerRadius,
            )
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = (sweepAlpha * 0.84f).coerceAtMost(0.84f)),
                        accentColor.copy(alpha = sweepAlpha.coerceIn(0f, 0.92f)),
                        Color.Transparent,
                    ),
                    startX = sweepLeft,
                    endX = sweepLeft + sweepWidth,
                ),
                topLeft = Offset(x = 0f, y = top),
                size = bandSize,
                cornerRadius = cornerRadius,
            )
            drawRoundRect(
                color = Color.White.copy(alpha = outlineAlpha.coerceIn(0f, 1f)),
                topLeft = Offset(x = 0f, y = top),
                size = bandSize,
                cornerRadius = cornerRadius,
                style = Stroke(width = 1.6f),
            )
        }

        ClearEffectOrientation.Column -> {
            val left = index * cellWidthPx
            val bandSize = Size(width = cellWidthPx, height = boardHeightPx)
            val sweepHeight = maxOf(boardHeightPx * 0.28f, cellWidthPx * 1.8f)
            val sweepTop = ((boardHeightPx + sweepHeight) * sweepProgress) - sweepHeight
            drawRoundRect(
                color = accentColor.copy(alpha = fillAlpha.coerceIn(0f, 1f)),
                topLeft = Offset(x = left, y = 0f),
                size = bandSize,
                cornerRadius = cornerRadius,
            )
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = (sweepAlpha * 0.84f).coerceAtMost(0.84f)),
                        accentColor.copy(alpha = sweepAlpha.coerceIn(0f, 0.92f)),
                        Color.Transparent,
                    ),
                    startY = sweepTop,
                    endY = sweepTop + sweepHeight,
                ),
                topLeft = Offset(x = left, y = 0f),
                size = bandSize,
                cornerRadius = cornerRadius,
            )
            drawRoundRect(
                color = Color.White.copy(alpha = outlineAlpha.coerceIn(0f, 1f)),
                topLeft = Offset(x = left, y = 0f),
                size = bandSize,
                cornerRadius = cornerRadius,
                style = Stroke(width = 1.6f),
            )
        }
    }
}

@Composable
private fun BoardGridBackgroundLayer(
    uiColors: com.ugurbuga.stackshift.ui.theme.StackShiftUiColors,
    boardDecorAlphaProvider: () -> Float,
    boardCornerRadiusPx: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier.graphicsLayer { alpha = boardDecorAlphaProvider() }
    ) {
        val boardWidthPx = size.width
        val boardHeightPx = size.height
        val boardFrameCornerRadius = CornerRadius(boardCornerRadiusPx, boardCornerRadiusPx)

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
                    uiColors.boardSignaturePrimary.copy(alpha = 0.22f),
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
                    uiColors.boardSignatureSecondary.copy(alpha = 0.18f),
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
                    Color.White.copy(alpha = 0.05f),
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.12f),
                ),
            ),
            size = Size(boardWidthPx, boardHeightPx),
            cornerRadius = boardFrameCornerRadius,
        )
    }
}

@Composable
private fun BoardGridSlotLayer(
    rows: Int,
    columns: Int,
    cellWidthPx: Float,
    cellHeightPx: Float,
    slotVisual: BoardCellVisual,
    uiColors: com.ugurbuga.stackshift.ui.theme.StackShiftUiColors,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val cornerRadius = slotVisual.cornerRadius

        for (row in 0 until rows) {
            for (column in 0 until columns) {
                val topLeft = Offset(x = column * cellWidthPx, y = row * cellHeightPx)
                drawFlatBoardEmptySlot(
                    topLeft = topLeft,
                    cellSize = Size(cellWidthPx, cellHeightPx),
                    slotVisual = slotVisual,
                    uiColors = uiColors,
                )
                drawRoundRect(
                    color = uiColors.boardGridLine.copy(alpha = 0.12f),
                    topLeft = topLeft,
                    size = Size(cellWidthPx, cellHeightPx),
                    cornerRadius = cornerRadius,
                    style = Stroke(width = 1f),
                )
            }
        }
    }
}

private fun DrawScope.drawFlatBoardEmptySlot(
    topLeft: Offset,
    cellSize: Size,
    slotVisual: BoardCellVisual,
    uiColors: com.ugurbuga.stackshift.ui.theme.StackShiftUiColors,
    alpha: Float = 1f,
) {
    val slotInset = slotVisual.fillInsetPx * 0.72f
    val slotTopLeft = topLeft + Offset(slotInset, slotInset)
    val slotSize = Size(
        width = cellSize.width - (slotInset * 2),
        height = cellSize.height - (slotInset * 2),
    )
    val slotCornerRadius = CornerRadius(slotVisual.innerCornerRadiusPx, slotVisual.innerCornerRadiusPx)
    drawRoundRect(
        color = uiColors.boardEmptyCell.copy(alpha = 0.44f * alpha),
        topLeft = slotTopLeft,
        size = slotSize,
        cornerRadius = slotCornerRadius,
    )
    drawRoundRect(
        color = uiColors.boardEmptyCellBorder.copy(alpha = 0.16f * alpha),
        topLeft = slotTopLeft,
        size = slotSize,
        cornerRadius = slotCornerRadius,
        style = Stroke(width = 1f),
    )
}

@Composable
private fun BoardSpecialIconOverlay(
    cell: BoardCell,
    column: Int,
    row: Int,
    cellWidth: Dp,
    cellHeight: Dp,
    translationX: Float = 0f,
    translationYProvider: () -> Float = { 0f },
    alphaProvider: () -> Float = { 1f },
    contentPadding: Dp = 2.dp,
    iconAlpha: Float = 1f,
    iconSizeRatio: Float = 0.36f,
) {
    Box(
        modifier = Modifier
            .offset(x = cellWidth * column, y = cellHeight * row)
            .size(cellWidth, cellHeight)
            .padding(contentPadding)
            .graphicsLayer {
                alpha = alphaProvider()
                this.translationX = translationX
                this.translationY = translationYProvider()
            },
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
                palette = settings.blockColorPalette,
            ).copy(alpha = iconAlpha),
            modifier = Modifier.size(minOf(cellWidth, cellHeight) * iconSizeRatio),
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
    val previousByColumnAndKey = buildMap {
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

internal fun specialBlockIconTint(
    style: BlockVisualStyle,
    isDarkTheme: Boolean,
    palette: BlockColorPalette,
): Color {
    if (palette == BlockColorPalette.Monochrome) {
        return Color.Black
    }

    return when (style) {
        BlockVisualStyle.Outline, BlockVisualStyle.PixelArt, BlockVisualStyle.Crystal -> {
            if (isDarkTheme) Color.White else Color.Black
        }

        else -> Color.White
    }
}

@Composable
internal fun blockStyleIconTint(
    style: BlockVisualStyle,
    enabled: Boolean = true,
): Color {
    val settings = LocalAppSettings.current
    return specialBlockIconTint(
        style = style,
        isDarkTheme = isStackShiftDarkTheme(settings),
        palette = settings.blockColorPalette,
    ).copy(alpha = if (enabled) 1f else 0.58f)
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

@Composable
internal fun BlockCellPreview(
    tone: CellTone,
    palette: BlockColorPalette,
    style: BlockVisualStyle,
    size: Dp,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    special: SpecialBlockType = SpecialBlockType.None,
    pulse: Float = 0f,
) {
    val settings = LocalAppSettings.current
    val isDarkTheme = isStackShiftDarkTheme(settings)
    val density = LocalDensity.current
    val cellInset = boardCellInsetDp(size)
    val cellCornerRadius = boardCellCornerRadiusDp(size, style)
    val cellCornerRadiusPx = with(density) { cellCornerRadius.toPx() }
    Box(
        modifier = modifier
            .size(size)
            .padding(cellInset),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCellBody(
                tone = tone,
                palette = palette,
                style = style,
                topLeft = Offset.Zero,
                size = this.size,
                cornerRadius = CornerRadius(cellCornerRadiusPx, cellCornerRadiusPx),
                alpha = alpha,
                pulse = pulse,
            )
        }
        if (special != SpecialBlockType.None) {
            Icon(
                imageVector = boardSpecialIcon(special),
                contentDescription = null,
                tint = specialBlockIconTint(
                    style = style,
                    isDarkTheme = isDarkTheme,
                    palette = palette,
                ),
                modifier = Modifier
                    .size((size.value * 0.36f).dp),
            )
        }
    }
}

@Composable
fun PieceBlocks(
    piece: Piece,
    cellSize: Dp,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    cellInset: Dp? = null,
    cellCornerRadius: Dp? = null,
    pulse: Float = 0f,
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
        val resolvedCellInset = cellInset ?: boardCellInsetDp(cellSize)
        val resolvedCellCornerRadius = cellCornerRadius ?: boardCellCornerRadiusDp(cellSize, visualStyle)
        val cellCornerRadiusPx = with(density) { resolvedCellCornerRadius.toPx() }
        piece.cells.forEach { cell ->
            Box(
                modifier = Modifier
                    .offset(x = cellSize * cell.column, y = cellSize * cell.row)
                    .size(cellSize)
                    .padding(resolvedCellInset),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCellBody(
                        tone = piece.tone,
                        palette = settings.blockColorPalette,
                        style = visualStyle,
                        topLeft = Offset.Zero,
                        size = this.size,
                        cornerRadius = CornerRadius(cellCornerRadiusPx, cellCornerRadiusPx),
                        alpha = alpha,
                        pulse = pulse,
                    )
                }
                if (piece.special != SpecialBlockType.None) {
                    Icon(
                        imageVector = boardSpecialIcon(piece.special),
                        contentDescription = null,
                        tint = specialBlockIconTint(
                            style = visualStyle,
                            isDarkTheme = isDarkTheme,
                            palette = settings.blockColorPalette,
                        ),
                        modifier = Modifier
                            .size((cellSize.value * 0.40f).dp),
                    )
                }
            }
        }
        blockContent()
    }
}


private fun DrawScope.drawCellBody(
    tone: CellTone,
    palette: BlockColorPalette,
    style: BlockVisualStyle,
    topLeft: Offset,
    size: Size,
    cornerRadius: CornerRadius,
    alpha: Float = 1f,
    pulse: Float = 0f,
) {
    val baseColor = tone.paletteColor(palette).copy(alpha = alpha.coerceIn(0f, 1f))
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
                        baseColor.copy(alpha = 1f),
                        baseColor.copy(alpha = 0.88f),
                        baseColor.copy(alpha = 0.62f),
                    ),
                    startY = topLeft.y,
                    endY = topLeft.y + size.height
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
        }

        BlockVisualStyle.Outline -> {
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        baseColor.copy(alpha = 0.28f),
                        baseColor.copy(alpha = 0.14f),
                    ),
                    startY = topLeft.y,
                    endY = topLeft.y + size.height
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
        }

        BlockVisualStyle.Sharp3D -> drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    baseColor.copy(alpha = 1f),
                    baseColor.copy(alpha = 0.86f),
                    baseColor.copy(alpha = 0.54f),
                ),
                startY = topLeft.y,
                endY = topLeft.y + size.height
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
        }

        BlockVisualStyle.Wood -> drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    baseColor.copy(alpha = 0.98f),
                    baseColor.copy(alpha = 0.90f),
                    baseColor.copy(alpha = 0.74f),
                ),
                startY = topLeft.y,
                endY = topLeft.y + size.height
            ),
            topLeft = topLeft,
            size = size,
            cornerRadius = CornerRadius(cornerRadius.x * 0.86f, cornerRadius.y * 0.80f),
        ).also {
            drawWoodGrain(topLeft = topLeft, size = size, alpha = 0.16f)
        }

        BlockVisualStyle.PixelArt -> {
            val pixelSize = size.width / 6.2f
            drawRect(color = baseColor, topLeft = topLeft, size = size)
            drawRect(
                color = Color.Black.copy(alpha = 0.14f * alpha),
                topLeft = topLeft + Offset(0f, size.height - pixelSize),
                size = Size(size.width, pixelSize),
            )
            drawRect(
                color = Color.Black.copy(alpha = 0.14f * alpha),
                topLeft = topLeft + Offset(size.width - pixelSize, 0f),
                size = Size(pixelSize, size.height),
            )
            drawRect(
                color = Color.White.copy(alpha = 0.18f * alpha),
                topLeft = topLeft,
                size = Size(size.width, pixelSize),
            )
            drawRect(
                color = Color.White.copy(alpha = 0.18f * alpha),
                topLeft = topLeft,
                size = Size(pixelSize, size.height),
            )
            drawRect(
                color = Color.White.copy(alpha = 0.28f * alpha),
                topLeft = topLeft + Offset(pixelSize, pixelSize),
                size = Size(pixelSize, pixelSize),
            )
        }

        BlockVisualStyle.Crystal -> {
            drawRect(color = baseColor, topLeft = topLeft, size = size)
            val center = topLeft + Offset(size.width / 2f, size.height / 2f)
            drawRect(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.30f * alpha),
                        Color.Black.copy(alpha = 0.20f * alpha),
                        Color.White.copy(alpha = 0.24f * alpha),
                        Color.Black.copy(alpha = 0.22f * alpha),
                        Color.White.copy(alpha = 0.30f * alpha),
                    ),
                    center = center,
                ),
                topLeft = topLeft,
                size = size,
            )
            drawLine(
                color = Color.White.copy(alpha = 0.32f * alpha),
                start = topLeft,
                end = topLeft + Offset(size.width, size.height),
                strokeWidth = 1.2f,
            )
            drawLine(
                color = Color.White.copy(alpha = 0.32f * alpha),
                start = topLeft + Offset(size.width, 0f),
                end = topLeft + Offset(0f, size.height),
                strokeWidth = 1.2f,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.45f * alpha), Color.Transparent),
                    center = topLeft + Offset(size.width * 0.3f, size.height * 0.3f),
                    radius = size.width * 0.35f,
                ),
                center = topLeft + Offset(size.width * 0.3f, size.height * 0.3f),
                radius = size.width * 0.35f,
            )
            drawRect(
                color = Color.White.copy(alpha = 0.42f * alpha),
                topLeft = topLeft,
                size = size,
                style = Stroke(width = 1.6f),
            )
        }

        BlockVisualStyle.DynamicLiquid -> {
            val constrainedPulse = pulse.coerceIn(0f, 1f)
            val glassColor = Color.White.copy(alpha = 0.16f * alpha)
            val frameWidth = (minOf(size.width, size.height) * 0.08f).coerceIn(1.4f, 3.4f)
            val liquidInset = frameWidth * 0.92f
            val liquidAreaTopLeft = topLeft + Offset(liquidInset, liquidInset)
            val liquidAreaSize = Size(
                width = (size.width - (liquidInset * 2f)).coerceAtLeast(0f),
                height = (size.height - (liquidInset * 2f)).coerceAtLeast(0f),
            )
            val liquidHeightFraction = 0.10f + (0.90f * constrainedPulse)
            val liquidHeight = liquidAreaSize.height * liquidHeightFraction
            val liquidTopY = liquidAreaTopLeft.y + (liquidAreaSize.height - liquidHeight)

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        glassColor,
                        Color.White.copy(alpha = 0.06f * alpha),
                    ),
                    startY = topLeft.y,
                    endY = topLeft.y + size.height,
                ),
                topLeft = topLeft,
                size = size,
                cornerRadius = cornerRadius,
            )
            drawRoundRect(
                color = baseColor.copy(alpha = 0.34f * alpha),
                topLeft = topLeft,
                size = size,
                cornerRadius = cornerRadius,
                style = Stroke(width = frameWidth),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.34f * alpha),
                topLeft = topLeft + Offset(frameWidth * 0.38f, frameWidth * 0.38f),
                size = Size(
                    width = (size.width - (frameWidth * 0.76f)).coerceAtLeast(0f),
                    height = (size.height - (frameWidth * 0.76f)).coerceAtLeast(0f),
                ),
                cornerRadius = CornerRadius(
                    (cornerRadius.x - (frameWidth * 0.38f)).coerceAtLeast(0f),
                    (cornerRadius.y - (frameWidth * 0.38f)).coerceAtLeast(0f),
                ),
                style = Stroke(width = (frameWidth * 0.34f).coerceAtLeast(0.9f)),
            )
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        baseColor.copy(alpha = 0.98f),
                        baseColor.copy(alpha = 0.82f),
                        baseColor.copy(alpha = 0.62f),
                    ),
                    startY = liquidTopY,
                    endY = liquidAreaTopLeft.y + liquidAreaSize.height,
                ),
                topLeft = Offset(liquidAreaTopLeft.x, liquidTopY),
                size = Size(liquidAreaSize.width, liquidHeight),
                cornerRadius = CornerRadius(cornerRadius.x * 0.68f, cornerRadius.y * 0.68f),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.22f * alpha),
                topLeft = Offset(liquidAreaTopLeft.x, liquidTopY),
                size = Size(liquidAreaSize.width, liquidHeight.coerceAtMost(liquidAreaSize.height * 0.20f)),
                cornerRadius = CornerRadius(cornerRadius.x * 0.58f, cornerRadius.y * 0.58f),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.18f * alpha),
                topLeft = topLeft + Offset(size.width * 0.1f, size.height * 0.1f),
                size = Size(size.width * 0.15f, size.height * 0.3f),
                cornerRadius = CornerRadius(4f, 4f),
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
            BlockVisualStyle.PixelArt -> 0f
            BlockVisualStyle.Crystal -> 0f
            BlockVisualStyle.DynamicLiquid -> 0.70f
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

internal fun boardCellCornerRadiusPx(
    cellSizePx: Float,
    style: BlockVisualStyle,
): Float = (cellSizePx * 0.16f * blockStyleCornerScale(style)).coerceAtLeast(0f)

internal fun boardFrameCornerRadiusDp(style: BlockVisualStyle): Dp = when (style) {
    BlockVisualStyle.Flat -> 18.dp
    BlockVisualStyle.Bubble -> 22.dp
    BlockVisualStyle.Outline -> 14.dp
    BlockVisualStyle.Sharp3D -> 6.dp
    BlockVisualStyle.Wood -> 12.dp
    BlockVisualStyle.PixelArt -> 0.dp
    BlockVisualStyle.Crystal -> 0.dp
    BlockVisualStyle.DynamicLiquid -> 18.dp
}

private fun blockStyleCornerScale(style: BlockVisualStyle): Float = when (style) {
    BlockVisualStyle.Flat -> 1.0f
    BlockVisualStyle.Bubble -> 1.20f
    BlockVisualStyle.Outline -> 0.82f
    BlockVisualStyle.Sharp3D -> 0.34f
    BlockVisualStyle.Wood -> 0.76f
    BlockVisualStyle.PixelArt -> 0f
    BlockVisualStyle.Crystal -> 0f
    BlockVisualStyle.DynamicLiquid -> 0.85f
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
