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
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
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
import com.ugurbuga.stackshift.game.model.boardSpecialIcon
import com.ugurbuga.stackshift.game.model.normalizeBlockVisualStyle
import com.ugurbuga.stackshift.game.model.paletteColor
import com.ugurbuga.stackshift.game.model.resolveBoardBlockStyle
import com.ugurbuga.stackshift.localization.LocalAppSettings
import com.ugurbuga.stackshift.ui.theme.StackShiftThemeTokens
import com.ugurbuga.stackshift.ui.theme.isStackShiftDarkTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val DefaultBoardClearFlashDurationMillis = 420
private const val DefaultBoardShiftDurationMillis = 220
private const val CosmicStarCount = 20
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
    val hasImpactPulse =
        impactedPreviewCells.isNotEmpty() || (preview?.clearedRows?.isNotEmpty() == true) || (preview?.clearedColumns?.isNotEmpty() == true)
    var clearFlashAlpha by remember { mutableFloatStateOf(0f) }
    var clearFlashSweepProgress by remember { mutableFloatStateOf(1f) }
    var boardTransitionSource by remember { mutableStateOf(gameState.board) }
    var boardTransitionToken by remember { mutableLongStateOf(gameState.clearAnimationToken) }
    val boardShiftProgress = remember { Animatable(1f) }
    val shouldAnimateBoardShift =
        gameState.status == GameStatus.Running && gameState.clearAnimationToken != boardTransitionToken
    val animatedBoardCells =
        remember(boardTransitionSource, gameState.board, shouldAnimateBoardShift) {
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
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 900,
                    easing = FastOutSlowInEasing
                )
            ),
            label = "dangerPulseAlpha",
        )
    } else {
        remember { mutableFloatStateOf(0.2f) }
    }
    val dangerSweepState = if (hasDangerPulse) {
        boardEffectsTransition!!.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 1600,
                    easing = FastOutSlowInEasing
                )
            ),
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
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 920,
                    easing = FastOutSlowInEasing
                )
            ),
            label = "impactSweep",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    val shouldAnimateStylePulse = stylePulse == 0f && (
        boardBlockStyle == BlockVisualStyle.DynamicLiquid || boardBlockStyle == BlockVisualStyle.Tornado || boardBlockStyle == BlockVisualStyle.Prism
    )
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
            animationSpec = tween(
                durationMillis = clearFlashDurationMillis,
                easing = FastOutSlowInEasing
            ),
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
            animationSpec = tween(
                durationMillis = boardShiftDurationMillis,
                easing = FastOutSlowInEasing
            ),
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
                    val bottom =
                        ((lowestOccupiedRow + 1) * cellHeightPx) - cellVisual.previewInsetPx
                    top to bottom
                }
        }
        val highlightedColumns =
            remember(preview, activePiece?.id, activeColumn, gameState.config.columns) {
                preview?.coveredColumns ?: activePiece?.let { piece ->
                    activeColumn?.coerceIn(
                        0,
                        (gameState.config.columns - piece.width).coerceAtLeast(0)
                    )?.let { column ->
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
            val previewLineColor =
                effectivePreviewColor.copy(alpha = previewGuideAlpha * boardDecorAlpha)
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
                        topLeft = Offset(
                            x = pressure.column * cellWidthPx,
                            y = sweepTop.coerceIn(0f, boardHeightPx - cellHeightPx)
                        ),
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
                            colorScheme.secondary.copy(
                                alpha = (dangerPulse * 0.8f).coerceAtMost(
                                    0.55f
                                ) * boardDecorAlpha
                            ),
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
                                topLeft = topLeft + Offset(
                                    cellVisual.fillInsetPx,
                                    cellVisual.fillInsetPx
                                ),
                                size = Size(
                                    width = cellWidthPx - (cellVisual.fillInsetPx * 2),
                                    height = cellHeightPx - (cellVisual.fillInsetPx * 2),
                                ),
                                cornerRadius = cornerRadius,
                                alpha = renderedCellAlpha,
                                pulse = effectiveStylePulse,
                            )

                            if (isImpactedByPreview) {
                                val warningInset =
                                    (cellVisual.previewInsetPx - (impactPulse * 0.8f)).coerceAtLeast(
                                        0.7f
                                    )
                                val diagonalInset =
                                    warningInset + (minOf(cellWidthPx, cellHeightPx) * 0.18f)
                                val crossStroke = 1.8f + (impactPulse * 1.8f)
                                drawRoundRect(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            uiColors.warning.copy(alpha = 0.22f + (impactPulse * 0.16f)),
                                            uiColors.danger.copy(alpha = 0.16f + (impactPulse * 0.22f)),
                                            Color.Transparent,
                                        ),
                                        center = topLeft + Offset(
                                            cellWidthPx / 2f,
                                            cellHeightPx / 2f
                                        ),
                                        radius = minOf(
                                            cellWidthPx,
                                            cellHeightPx
                                        ) * (0.98f + impactPulse * 0.18f),
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
                                        end = topLeft + Offset(
                                            cellWidthPx * (1f - impactSweep),
                                            cellHeightPx
                                        ),
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
                                    end = topLeft + Offset(
                                        cellWidthPx - diagonalInset,
                                        cellHeightPx - diagonalInset
                                    ),
                                    strokeWidth = crossStroke,
                                )
                                drawLine(
                                    color = uiColors.warning.copy(alpha = 0.42f + impactPulse * 0.32f),
                                    start = topLeft + Offset(
                                        cellWidthPx - diagonalInset,
                                        diagonalInset
                                    ),
                                    end = topLeft + Offset(
                                        diagonalInset,
                                        cellHeightPx - diagonalInset
                                    ),
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
                    val animatedRow =
                        sourceRow + ((targetRow - sourceRow) * boardShiftProgress.value)
                    val animatedTopLeft = Offset(
                        x = animatedCell.point.column * cellWidthPx,
                        y = animatedRow * cellHeightPx,
                    )
                    drawCellBody(
                        tone = animatedCell.cell.tone,
                        palette = settings.blockColorPalette,
                        style = boardBlockStyle,
                        topLeft = animatedTopLeft + Offset(
                            cellVisual.fillInsetPx,
                            cellVisual.fillInsetPx
                        ),
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
    val slotCornerRadius =
        CornerRadius(slotVisual.innerCornerRadiusPx, slotVisual.innerCornerRadiusPx)
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
                add(
                    AnimatedBoardCell(
                        point = GridPoint(column = column, row = row),
                        cell = cell,
                        sourceRow = sourceRow
                    )
                )
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
        BlockVisualStyle.Outline,
        BlockVisualStyle.GridSplit,
        BlockVisualStyle.Crystal,
        BlockVisualStyle.DynamicLiquid,
        BlockVisualStyle.HoneycombTexture,
        BlockVisualStyle.LightBurst,
        BlockVisualStyle.LiquidMarble,
        BlockVisualStyle.Brick,
        BlockVisualStyle.SoundWave,
        BlockVisualStyle.Prism -> {
            if (isDarkTheme) Color.White else Color.Black
        }

        BlockVisualStyle.SpiderWeb -> {
            Color.Black
        }

        BlockVisualStyle.Tornado,
        BlockVisualStyle.Cosmic -> Color.White

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

internal fun DrawScope.drawWoodGrain(
    topLeft: Offset,
    size: Size,
    alpha: Float,
) {
    val minDim = minOf(size.width, size.height)
    val colorAlpha = 0.35f * alpha // Increased visibility as requested
    val lineCount = 8
    val verticalStep = size.height / (lineCount + 1)

    repeat(lineCount) { i ->
        val yBase = topLeft.y + verticalStep * (i + 1)
        val path = Path().apply {
            moveTo(topLeft.x, yBase)
            val segments = 4
            val segmentWidth = size.width / segments
            for (j in 1..segments) {
                val x = topLeft.x + j * segmentWidth
                val waveOffset = (if (j % 2 == 0) -1f else 1f) * minDim * 0.02f
                val cpX = x - segmentWidth / 2f
                quadraticTo(cpX, yBase + waveOffset, x, yBase)
            }
        }
        
        val strokeWidth = (minDim * 0.012f).coerceAtLeast(1.0f) // Thicker as requested
        
        // Shadow line
        drawPath(
            path = path,
            color = Color.Black.copy(alpha = colorAlpha * 0.5f),
            style = Stroke(width = strokeWidth),
        )
        
        // Highlight line with slight offset
        clipRect(topLeft.x, topLeft.y, topLeft.x + size.width, topLeft.y + size.height) {
            withTransform({
                translate(0f, -1.2f) // Slightly more offset for thicker lines
            }) {
                drawPath(
                    path = path,
                    color = Color.White.copy(alpha = colorAlpha * 0.8f),
                    style = Stroke(width = strokeWidth)
                )
            }
        }
    }
}

private fun DrawScope.drawStonePattern(
    topLeft: Offset,
    size: Size,
    toneColor: Color,
    alpha: Float,
    cornerRadius: CornerRadius,
) {
    val minDimension = minOf(size.width, size.height)
    
    // Base stone body with more rocky gradient
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                tintColor(toneColor, 0.15f).copy(alpha = alpha),
                toneColor.copy(alpha = alpha),
                shadeColor(toneColor, 0.22f).copy(alpha = alpha),
            ),
            start = topLeft,
            end = topLeft + Offset(size.width, size.height)
        ),
        topLeft = topLeft,
        size = size,
        cornerRadius = cornerRadius,
    )

    // Irregular polygonal cracks matching the visual reference
    val strokeColor = Color.Black.copy(alpha = 0.28f * alpha)
    val highlightColor = Color.White.copy(alpha = 0.22f * alpha)
    val strokeWidth = (minDimension * 0.038f).coerceAtLeast(1.2f)
    
    val crackPaths = listOf(
        // Major structural cracks
        listOf(Offset(0.25f, 0f), Offset(0.35f, 0.3f), Offset(0f, 0.45f)),
        listOf(Offset(0.35f, 0.3f), Offset(0.65f, 0.35f), Offset(0.75f, 0f)),
        listOf(Offset(0.65f, 0.35f), Offset(0.85f, 0.6f), Offset(1f, 0.5f)),
        listOf(Offset(0f, 0.75f), Offset(0.3f, 0.7f), Offset(0.45f, 1f)),
        listOf(Offset(0.3f, 0.7f), Offset(0.6f, 0.65f), Offset(0.85f, 0.6f)),
        listOf(Offset(0.6f, 0.65f), Offset(0.75f, 1f)),
        // Minor detail cracks
        listOf(Offset(0.35f, 0.3f), Offset(0.3f, 0.7f)),
        listOf(Offset(0.65f, 0.35f), Offset(0.6f, 0.65f))
    )

    crackPaths.forEach { points ->
        val path = Path().apply {
            moveTo(topLeft.x + size.width * points[0].x, topLeft.y + size.height * points[0].y)
            for (i in 1 until points.size) {
                lineTo(topLeft.x + size.width * points[i].x, topLeft.y + size.height * points[i].y)
            }
        }
        drawPath(path = path, color = strokeColor, style = Stroke(width = strokeWidth))
        
        // Depth highlight on one side of the crack
        val highlightPath = Path().apply {
            moveTo(topLeft.x + size.width * points[0].x + 0.8f, topLeft.y + size.height * points[0].y + 0.8f)
            for (i in 1 until points.size) {
                lineTo(topLeft.x + size.width * points[i].x + 0.8f, topLeft.y + size.height * points[i].y + 0.8f)
            }
        }
        drawPath(path = highlightPath, color = highlightColor, style = Stroke(width = strokeWidth * 0.4f))
    }

    // Outer rock bevel/border
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.15f * alpha),
        topLeft = topLeft,
        size = size,
        cornerRadius = cornerRadius,
        style = Stroke(width = strokeWidth * 1.2f)
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
        val resolvedCellCornerRadius =
            cellCornerRadius ?: boardCellCornerRadiusDp(cellSize, visualStyle)
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


internal fun DrawScope.drawCellBody(
    tone: CellTone,
    palette: BlockColorPalette,
    style: BlockVisualStyle,
    topLeft: Offset,
    size: Size,
    cornerRadius: CornerRadius,
    alpha: Float = 1f,
    pulse: Float = 0f,
) {
    drawCellBody(
        baseColor = tone.paletteColor(palette),
        palette = palette,
        style = style,
        topLeft = topLeft,
        size = size,
        cornerRadius = cornerRadius,
        alpha = alpha,
        pulse = pulse,
    )
}

internal fun DrawScope.drawCellBody(
    baseColor: Color,
    palette: BlockColorPalette,
    style: BlockVisualStyle,
    topLeft: Offset,
    size: Size,
    cornerRadius: CornerRadius,
    alpha: Float = 1f,
    pulse: Float = 0f,
) {
    val constrainedAlpha = alpha.coerceIn(0f, 1f)
    if (constrainedAlpha <= 0f) return

    val rawBaseColor = baseColor
    val baseColor = rawBaseColor.copy(alpha = constrainedAlpha)
    val minDimension = minOf(size.width, size.height)

    when (style) {
        BlockVisualStyle.Flat,
        BlockVisualStyle.MatteSoft
            -> drawRoundRect(
            color = baseColor,
            topLeft = topLeft,
            size = size,
            cornerRadius = cornerRadius,
        )

        BlockVisualStyle.Bubble,
        BlockVisualStyle.NeonGlow
            -> {
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

        BlockVisualStyle.Sharp3D -> {
            val bottomLayer = angularPanelPath(
                topLeft = topLeft,
                size = size,
                insetFraction = 0.03f,
                notchFraction = 0.08f,
            )
            val topInset = minDimension * 0.11f
            val topLayerTopLeft = topLeft + Offset(topInset, topInset)
            val topLayerSize = Size(
                width = (size.width - (topInset * 2f)).coerceAtLeast(0f),
                height = (size.height - (topInset * 2f)).coerceAtLeast(0f),
            )
            val topLayer = angularPanelPath(
                topLeft = topLayerTopLeft,
                size = topLayerSize,
                insetFraction = 0.04f,
                notchFraction = 0.09f,
            )

            drawPath(
                path = bottomLayer,
                brush = Brush.linearGradient(
                    colors = listOf(
                        shadeColor(rawBaseColor, 0.14f).copy(alpha = constrainedAlpha),
                        rawBaseColor.copy(alpha = constrainedAlpha),
                        shadeColor(rawBaseColor, 0.30f).copy(alpha = constrainedAlpha),
                    ),
                    start = topLeft,
                    end = topLeft + Offset(size.width, size.height),
                ),
            )
            drawPath(
                path = bottomLayer,
                color = Color.Black.copy(alpha = 0.18f * constrainedAlpha),
                style = Stroke(width = (minDimension * 0.038f).coerceAtLeast(1.2f)),
            )

            drawPath(
                path = topLayer,
                brush = Brush.linearGradient(
                    colors = listOf(
                        tintColor(rawBaseColor, 0.22f).copy(alpha = constrainedAlpha),
                        tintColor(rawBaseColor, 0.08f).copy(alpha = constrainedAlpha),
                        shadeColor(rawBaseColor, 0.22f).copy(alpha = constrainedAlpha),
                    ),
                    start = topLayerTopLeft,
                    end = topLayerTopLeft + Offset(topLayerSize.width, topLayerSize.height),
                ),
            )
            drawPath(
                path = topLayer,
                color = Color.White.copy(alpha = 0.16f * constrainedAlpha),
                style = Stroke(width = (minDimension * 0.024f).coerceAtLeast(1f)),
            )

            drawPath(
                path = Path().apply {
                    moveTo(topLeft.x + size.width * 0.14f, topLeft.y + size.height * 0.18f)
                    lineTo(topLeft.x + size.width * 0.56f, topLeft.y + size.height * 0.18f)
                    lineTo(topLeft.x + size.width * 0.38f, topLeft.y + size.height * 0.36f)
                    lineTo(topLeft.x + size.width * 0.14f, topLeft.y + size.height * 0.36f)
                    close()
                },
                color = Color.White.copy(alpha = 0.10f * constrainedAlpha),
            )
            drawPath(
                path = Path().apply {
                    moveTo(topLeft.x + size.width * 0.24f, topLeft.y + size.height * 0.74f)
                    lineTo(topLeft.x + size.width * 0.84f, topLeft.y + size.height * 0.74f)
                    lineTo(topLeft.x + size.width * 0.72f, topLeft.y + size.height * 0.86f)
                    lineTo(topLeft.x + size.width * 0.14f, topLeft.y + size.height * 0.86f)
                    close()
                },
                color = Color.Black.copy(alpha = 0.16f * constrainedAlpha),
            )
        }

        BlockVisualStyle.Wood -> {
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        tintColor(rawBaseColor, 0.12f).copy(alpha = constrainedAlpha),
                        baseColor,
                        shadeColor(rawBaseColor, 0.10f).copy(alpha = constrainedAlpha),
                    ),
                    startY = topLeft.y,
                    endY = topLeft.y + size.height
                ),
                topLeft = topLeft,
                size = size,
                cornerRadius = CornerRadius(cornerRadius.x * 0.86f, cornerRadius.y * 0.80f),
            )
            drawWoodGrain(topLeft = topLeft, size = size, alpha = constrainedAlpha)
        }

        BlockVisualStyle.StoneTexture -> {
            drawStonePattern(
                topLeft = topLeft,
                size = size,
                toneColor = rawBaseColor,
                alpha = constrainedAlpha,
                cornerRadius = cornerRadius
            )
        }

        BlockVisualStyle.GridSplit -> {
            drawGridSplitPattern(
                topLeft = topLeft,
                size = size,
                toneColor = rawBaseColor,
                alpha = constrainedAlpha,
                cornerRadius = cornerRadius,
            )
        }

        BlockVisualStyle.Crystal -> {
            drawRect(color = baseColor, topLeft = topLeft, size = size)
            val center = topLeft + Offset(size.width / 2f, size.height / 2f)
            drawRect(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.30f * constrainedAlpha),
                        Color.Black.copy(alpha = 0.20f * constrainedAlpha),
                        Color.White.copy(alpha = 0.24f * constrainedAlpha),
                        Color.Black.copy(alpha = 0.22f * constrainedAlpha),
                        Color.White.copy(alpha = 0.30f * constrainedAlpha),
                    ),
                    center = center,
                ),
                topLeft = topLeft,
                size = size,
            )
            drawLine(
                color = Color.White.copy(alpha = 0.32f * constrainedAlpha),
                start = topLeft,
                end = topLeft + Offset(size.width, size.height),
                strokeWidth = 1.2f,
            )
            drawLine(
                color = Color.White.copy(alpha = 0.32f * constrainedAlpha),
                start = topLeft + Offset(size.width, 0f),
                end = topLeft + Offset(0f, size.height),
                strokeWidth = 1.2f,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.45f * constrainedAlpha),
                        Color.Transparent
                    ),
                    center = topLeft + Offset(size.width * 0.3f, size.height * 0.3f),
                    radius = size.width * 0.35f,
                ),
                center = topLeft + Offset(size.width * 0.3f, size.height * 0.3f),
                radius = size.width * 0.35f,
            )
            drawRect(
                color = Color.White.copy(alpha = 0.42f * constrainedAlpha),
                topLeft = topLeft,
                size = size,
                style = Stroke(width = 1.6f),
            )
        }

        BlockVisualStyle.DynamicLiquid -> {
            val constrainedPulse = pulse.coerceIn(0f, 1f)
            val glassColor = Color.White.copy(alpha = 0.16f * constrainedAlpha)
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
                        Color.White.copy(alpha = 0.06f * constrainedAlpha),
                    ),
                    startY = topLeft.y,
                    endY = topLeft.y + size.height,
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
                style = Stroke(width = frameWidth),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.34f * constrainedAlpha),
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
                color = Color.White.copy(alpha = 0.22f * constrainedAlpha),
                topLeft = Offset(liquidAreaTopLeft.x, liquidTopY),
                size = Size(
                    liquidAreaSize.width,
                    liquidHeight.coerceAtMost(liquidAreaSize.height * 0.20f)
                ),
                cornerRadius = CornerRadius(cornerRadius.x * 0.58f, cornerRadius.y * 0.58f),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.18f * constrainedAlpha),
                topLeft = topLeft + Offset(size.width * 0.1f, size.height * 0.1f),
                size = Size(size.width * 0.15f, size.height * 0.3f),
                cornerRadius = CornerRadius(4f, 4f),
            )
        }

        BlockVisualStyle.Tornado -> {
            drawTornadoPattern(
                topLeft = topLeft,
                size = size,
                toneColor = rawBaseColor,
                alpha = constrainedAlpha,
                pulse = pulse,
            )
        }

        BlockVisualStyle.HoneycombTexture -> {
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        tintColor(rawBaseColor, 0.16f).copy(alpha = constrainedAlpha),
                        baseColor,
                        shadeColor(rawBaseColor, 0.12f).copy(alpha = constrainedAlpha),
                    ),
                    startY = topLeft.y,
                    endY = topLeft.y + size.height,
                ),
                topLeft = topLeft,
                size = size,
                cornerRadius = CornerRadius(cornerRadius.x * 0.92f, cornerRadius.y * 0.92f),
            )
            drawHoneycombPattern(
                topLeft = topLeft,
                size = size,
                alpha = constrainedAlpha,
                toneColor = rawBaseColor
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.10f * constrainedAlpha),
                topLeft = topLeft,
                size = size,
                cornerRadius = CornerRadius(cornerRadius.x * 0.92f, cornerRadius.y * 0.92f),
                style = Stroke(width = (minDimension * 0.025f).coerceAtLeast(1f)),
            )
        }

        BlockVisualStyle.LightBurst -> {
            val center = topLeft + Offset(size.width * 0.5f, size.height * 0.5f)
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        tintColor(rawBaseColor, 0.26f).copy(alpha = constrainedAlpha),
                        tintColor(rawBaseColor, 0.12f).copy(alpha = constrainedAlpha),
                    ),
                    startY = topLeft.y,
                    endY = topLeft.y + size.height,
                ),
                topLeft = topLeft,
                size = size,
                cornerRadius = CornerRadius(cornerRadius.x * 1.08f, cornerRadius.y * 1.08f),
            )
            drawBurstRays(
                center = center,
                radius = minDimension * 0.66f,
                rayCount = 16,
                color = Color.White,
                alpha = 0.20f * constrainedAlpha,
                strokeWidth = (minDimension * 0.020f).coerceAtLeast(0.9f),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.72f * constrainedAlpha),
                        Color.White.copy(alpha = 0.18f * constrainedAlpha),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = minDimension * 0.58f,
                ),
                center = center,
                radius = minDimension * 0.58f,
            )
        }

        BlockVisualStyle.LiquidMarble -> {
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        tintColor(rawBaseColor, 0.18f).copy(alpha = constrainedAlpha),
                        baseColor,
                        tintColor(rawBaseColor, 0.08f).copy(alpha = constrainedAlpha),
                    ),
                    start = topLeft,
                    end = topLeft + Offset(size.width, size.height),
                ),
                topLeft = topLeft,
                size = size,
                cornerRadius = CornerRadius(cornerRadius.x * 1.02f, cornerRadius.y * 1.02f),
            )
            drawMarbleVeins(
                topLeft = topLeft,
                size = size,
                primary = Color.White.copy(alpha = 0.42f * constrainedAlpha),
                secondary = shadeColor(rawBaseColor, 0.14f).copy(alpha = 0.18f * constrainedAlpha),
                strokeWidth = (minDimension * 0.075f).coerceAtLeast(1.6f),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.12f * constrainedAlpha),
                topLeft = topLeft + Offset(size.width * 0.08f, size.height * 0.08f),
                size = Size(size.width * 0.70f, size.height * 0.14f),
                cornerRadius = CornerRadius(cornerRadius.x * 0.72f, cornerRadius.y * 0.72f),
            )
        }

        BlockVisualStyle.SpiderWeb -> {
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        tintColor(rawBaseColor, 0.18f).copy(alpha = constrainedAlpha),
                        baseColor,
                        shadeColor(rawBaseColor, 0.10f).copy(alpha = constrainedAlpha),
                    ),
                    startY = topLeft.y,
                    endY = topLeft.y + size.height,
                ),
                topLeft = topLeft,
                size = size,
                cornerRadius = CornerRadius(cornerRadius.x * 0.72f, cornerRadius.y * 0.72f),
            )
            drawSpiderWebPattern(
                topLeft = topLeft,
                size = size,
                alpha = constrainedAlpha,
                lineColor = Color.White.copy(alpha = 0.44f * constrainedAlpha),
            )
        }

        BlockVisualStyle.Cosmic -> {
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        tintColor(rawBaseColor, 0.14f).copy(alpha = constrainedAlpha),
                        shadeColor(rawBaseColor, 0.42f).copy(alpha = constrainedAlpha),
                        Color(0xFF140F1E).copy(alpha = constrainedAlpha),
                    ),
                    center = topLeft + Offset(size.width * 0.48f, size.height * 0.46f),
                    radius = minDimension * 0.86f,
                ),
                topLeft = topLeft,
                size = size,
                cornerRadius = CornerRadius(cornerRadius.x * 0.72f, cornerRadius.y * 0.72f),
            )
            drawCosmicField(
                topLeft = topLeft,
                size = size,
                baseGlow = tintColor(rawBaseColor, 0.22f),
                alpha = constrainedAlpha,
            )
        }

        BlockVisualStyle.Brick -> {
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        tintColor(rawBaseColor, 0.14f).copy(alpha = constrainedAlpha),
                        baseColor.copy(alpha = constrainedAlpha),
                        shadeColor(rawBaseColor, 0.18f).copy(alpha = constrainedAlpha),
                    ),
                    startY = topLeft.y,
                    endY = topLeft.y + size.height,
                ),
                topLeft = topLeft,
                size = size,
                cornerRadius = CornerRadius(cornerRadius.x * 0.52f, cornerRadius.y * 0.52f),
            )
            drawBrickPattern(
                topLeft = topLeft,
                size = size,
                baseTone = rawBaseColor,
                alpha = constrainedAlpha,
                cornerRadius = cornerRadius,
            )
        }

        BlockVisualStyle.SoundWave -> {
            drawSoundWavePattern(
                topLeft = topLeft,
                size = size,
                toneColor = rawBaseColor,
                alpha = constrainedAlpha,
                pulse = pulse,
            )
        }

        BlockVisualStyle.Prism -> {
            drawPrismRefraction(
                topLeft = topLeft,
                size = size,
                toneColor = rawBaseColor,
                alpha = constrainedAlpha,
                cornerRadius = cornerRadius,
                pulse = pulse,
            )
        }
    }
}

private fun tintColor(color: Color, amount: Float): Color =
    lerp(color, Color.White, amount.coerceIn(0f, 1f))

private fun shadeColor(color: Color, amount: Float): Color =
    lerp(color, Color.Black, amount.coerceIn(0f, 1f))

private fun DrawScope.drawMarbleVeins(
    topLeft: Offset,
    size: Size,
    primary: Color,
    secondary: Color,
    strokeWidth: Float,
) {
    val paths = listOf(
        marbleVeinPath(
            topLeft = topLeft,
            size = size,
            points = listOf(
                Offset(0.10f, 0.08f),
                Offset(0.34f, 0.22f),
                Offset(0.22f, 0.48f),
                Offset(0.42f, 0.72f),
                Offset(0.22f, 0.96f),
            ),
        ),
        marbleVeinPath(
            topLeft = topLeft,
            size = size,
            points = listOf(
                Offset(0.62f, 0.02f),
                Offset(0.84f, 0.18f),
                Offset(0.58f, 0.40f),
                Offset(0.84f, 0.66f),
                Offset(0.66f, 0.96f),
            ),
        ),
        marbleVeinPath(
            topLeft = topLeft,
            size = size,
            points = listOf(
                Offset(0.00f, 0.28f),
                Offset(0.22f, 0.38f),
                Offset(0.08f, 0.62f),
                Offset(0.28f, 0.82f),
                Offset(0.12f, 1.00f),
            ),
        ),
    )

    paths.forEachIndexed { index, path ->
        drawPath(
            path = path,
            color = if (index == 1) secondary else primary,
            style = Stroke(width = strokeWidth * if (index == 1) 0.72f else 1f),
        )
    }
}

private fun DrawScope.drawBurstRays(
    center: Offset,
    radius: Float,
    rayCount: Int,
    color: Color,
    alpha: Float,
    strokeWidth: Float,
) {
    repeat(rayCount) { index ->
        val angle = ((2.0 * PI * index) / rayCount).toFloat()
        val rayRadius = if (index % 2 == 0) radius else radius * 0.72f
        val end = center + Offset(cos(angle) * rayRadius, sin(angle) * rayRadius)
        drawLine(
            color = color.copy(alpha = alpha * if (index % 2 == 0) 1f else 0.62f),
            start = center,
            end = end,
            strokeWidth = strokeWidth,
        )
    }
}


private fun DrawScope.drawHoneycombPattern(
    topLeft: Offset,
    size: Size,
    alpha: Float,
    toneColor: Color,
) {
    val minSize = minOf(size.width, size.height)
    val radius = minSize * 0.115f
    val horizontalStep = radius * 1.74f
    val verticalStep = radius * 1.52f
    val strokeColor = shadeColor(toneColor, 0.28f).copy(alpha = 0.36f * alpha)
    val highlightColor = tintColor(toneColor, 0.30f).copy(alpha = 0.18f * alpha)
    val inset = radius * 0.36f
    clipRect(
        left = topLeft.x + inset,
        top = topLeft.y + inset,
        right = topLeft.x + size.width - inset,
        bottom = topLeft.y + size.height - inset,
    ) {
        var rowIndex = 0
        var y = topLeft.y + radius
        while (y <= topLeft.y + size.height) {
            val offset = if (rowIndex % 2 == 0) 0f else horizontalStep / 2f
            var x = topLeft.x + radius + offset
            while (x <= topLeft.x + size.width) {
                val center = Offset(x, y)
                val hexagon = hexagonPath(center = center, radius = radius)
                drawPath(
                    path = hexagon,
                    color = strokeColor,
                    style = Stroke(width = (minSize * 0.023f).coerceAtLeast(1.1f)),
                )
                drawPath(
                    path = hexagon,
                    color = highlightColor,
                    style = Stroke(width = (minSize * 0.010f).coerceAtLeast(0.6f)),
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.12f * alpha),
                    center = center,
                    radius = radius * 0.16f,
                )
                x += horizontalStep
            }
            y += verticalStep
            rowIndex += 1
        }
    }
}

private fun DrawScope.drawSpiderWebPattern(
    topLeft: Offset,
    size: Size,
    alpha: Float,
    lineColor: Color,
) {
    val center = topLeft + Offset(size.width / 2f, size.height / 2f)
    val anchors = listOf(
        Offset(0.00f, 0.00f),
        Offset(0.22f, 0.04f),
        Offset(0.50f, 0.00f),
        Offset(0.78f, 0.04f),
        Offset(1.00f, 0.00f),
        Offset(0.96f, 0.22f),
        Offset(1.00f, 0.50f),
        Offset(0.96f, 0.78f),
        Offset(1.00f, 1.00f),
        Offset(0.78f, 0.96f),
        Offset(0.50f, 1.00f),
        Offset(0.22f, 0.96f),
        Offset(0.00f, 1.00f),
        Offset(0.04f, 0.78f),
        Offset(0.00f, 0.50f),
        Offset(0.04f, 0.22f),
    )

    anchors.forEach { anchorFraction ->
        val anchor = topLeft + Offset(size.width * anchorFraction.x, size.height * anchorFraction.y)
        drawLine(
            color = lineColor,
            start = center,
            end = anchor,
            strokeWidth = (minOf(size.width, size.height) * 0.016f).coerceAtLeast(0.7f),
        )
    }

    listOf(0.14f, 0.28f, 0.44f, 0.62f, 0.82f).forEachIndexed { index, factor ->
        val ringPath = Path().apply {
            anchors.forEachIndexed { anchorIndex, anchorFraction ->
                val point = center + Offset(
                    x = (topLeft.x + (size.width * anchorFraction.x) - center.x) * factor,
                    y = (topLeft.y + (size.height * anchorFraction.y) - center.y) * factor,
                )
                if (anchorIndex == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
            }
            close()
        }
        drawPath(
            path = ringPath,
            color = lineColor.copy(alpha = (0.60f - (index * 0.07f)).coerceAtLeast(0.24f) * alpha),
            style = Stroke(width = (minOf(size.width, size.height) * 0.014f).coerceAtLeast(0.7f)),
        )
    }

    anchors.windowed(size = 2, step = 2, partialWindows = false)
        .forEach { (startFraction, endFraction) ->
            val start =
                topLeft + Offset(size.width * startFraction.x, size.height * startFraction.y)
            val end = topLeft + Offset(size.width * endFraction.x, size.height * endFraction.y)
            drawLine(
                    color = lineColor.copy(alpha = 0.18f * alpha),
                start = start,
                end = end,
                strokeWidth = (minOf(size.width, size.height) * 0.010f).coerceAtLeast(0.5f),
            )
        }

    drawCircle(
                color = Color.White.copy(alpha = 0.22f * alpha),
        center = center,
        radius = minOf(size.width, size.height) * 0.05f,
    )
}

private fun DrawScope.drawCosmicField(
    topLeft: Offset,
    size: Size,
    baseGlow: Color,
    alpha: Float,
) {
    val minDimension = minOf(size.width, size.height)
    listOf(
        Triple(Offset(0.26f, 0.32f), 0.34f, baseGlow.copy(alpha = 0.34f * alpha)),
        Triple(Offset(0.64f, 0.44f), 0.28f, Color.White.copy(alpha = 0.18f * alpha)),
        Triple(Offset(0.76f, 0.24f), 0.20f, tintColor(baseGlow, 0.32f).copy(alpha = 0.22f * alpha)),
        Triple(Offset(0.44f, 0.74f), 0.24f, Color(0xFF6FC3FF).copy(alpha = 0.16f * alpha)),
    ).forEach { (fraction, radiusFactor, color) ->
        val center = topLeft + Offset(size.width * fraction.x, size.height * fraction.y)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color, Color.Transparent),
                center = center,
                radius = minDimension * radiusFactor,
            ),
            center = center,
            radius = minDimension * radiusFactor,
        )
    }

    drawArc(
        color = Color.White.copy(alpha = 0.10f * alpha),
        startAngle = -24f,
        sweepAngle = 208f,
        useCenter = false,
        topLeft = topLeft + Offset(size.width * 0.14f, size.height * 0.22f),
        size = Size(size.width * 0.68f, size.height * 0.42f),
        style = Stroke(width = (minDimension * 0.022f).coerceAtLeast(0.9f)),
    )
    drawArc(
        color = tintColor(baseGlow, 0.42f).copy(alpha = 0.12f * alpha),
        startAngle = 116f,
        sweepAngle = 164f,
        useCenter = false,
        topLeft = topLeft + Offset(size.width * 0.06f, size.height * 0.10f),
        size = Size(size.width * 0.84f, size.height * 0.70f),
        style = Stroke(width = (minDimension * 0.016f).coerceAtLeast(0.7f)),
    )

    stableStarField(
        seed = cosmicSeed(
            topLeft = topLeft,
            size = size,
            colorSeed = baseGlow.hashCode()
        )
    ).forEachIndexed { index, star ->
        val fraction = Offset(star.x, star.y)
        val center = topLeft + Offset(size.width * fraction.x, size.height * fraction.y)
        val radius = minDimension * star.radiusFactor
        val starColor = when (index % 4) {
            0 -> Color.White
            1 -> Color(0xFFBFE7FF)
            2 -> Color(0xFFFFE5A8)
            else -> tintColor(baseGlow, 0.50f)
        }
        drawCircle(
            color = starColor.copy(alpha = star.alpha * alpha),
            center = center,
            radius = radius
        )
        if (star.twinkle) {
            drawBurstRays(
                center = center,
                radius = radius * 3.1f,
                rayCount = 4,
                color = starColor,
                alpha = 0.24f * alpha,
                strokeWidth = radius * 0.42f,
            )
        }
    }
}

private fun DrawScope.drawRubikCubeFace(
    topLeft: Offset,
    size: Size,
    toneColor: Color,
    alpha: Float,
    cornerRadius: CornerRadius,
    palette: BlockColorPalette,
) {
    val minDimension = minOf(size.width, size.height)
    val shellCorner = CornerRadius(
        x = (cornerRadius.x * 0.34f).coerceAtLeast(minDimension * 0.06f),
        y = (cornerRadius.y * 0.34f).coerceAtLeast(minDimension * 0.06f),
    )
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                tintColor(toneColor, 0.18f).copy(alpha = alpha),
                toneColor.copy(alpha = alpha),
                shadeColor(toneColor, 0.22f).copy(alpha = alpha),
            ),
            start = topLeft,
            end = topLeft + Offset(size.width, size.height),
        ),
        topLeft = topLeft,
        size = size,
        cornerRadius = shellCorner,
    )
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.18f * alpha),
        topLeft = topLeft,
        size = size,
        cornerRadius = shellCorner,
        style = Stroke(width = (minDimension * 0.040f).coerceAtLeast(1.1f)),
    )

    val gridCount = 4
    val shellInset = (minDimension * 0.08f).coerceIn(1.2f, 3.6f)
    val stickerGap = (minDimension * 0.022f).coerceIn(0.6f, 1.4f)
    val faceSize = (minDimension - (shellInset * 2f)).coerceAtLeast(minDimension * 0.54f)
    val stickerSize = ((faceSize - (stickerGap * (gridCount - 1))) / gridCount).coerceAtLeast(0f)
    val faceTopLeft = topLeft + Offset((size.width - faceSize) / 2f, (size.height - faceSize) / 2f)
    val stickerCorner = CornerRadius(
        x = (stickerSize * 0.16f).coerceAtLeast(0.6f),
        y = (stickerSize * 0.16f).coerceAtLeast(0.6f),
    )
    val alphaBoost = if (palette == BlockColorPalette.Monochrome) 0.10f else 0f

    drawRoundRect(
        color = Color.Black.copy(alpha = 0.16f * alpha),
        topLeft = faceTopLeft - Offset(stickerGap, stickerGap),
        size = Size(faceSize + (stickerGap * 2f), faceSize + (stickerGap * 2f)),
        cornerRadius = CornerRadius(shellCorner.x * 0.72f, shellCorner.y * 0.72f),
    )

    repeat(gridCount) { row ->
        repeat(gridCount) { column ->
            val stickerTopLeft = faceTopLeft + Offset(
                x = column * (stickerSize + stickerGap),
                y = row * (stickerSize + stickerGap),
            )
            val intensity = ((gridCount * 2) - row - column).toFloat() / (gridCount * 2)
            val stickerAlpha = (0.42f + (intensity * 0.34f) + alphaBoost).coerceIn(0.34f, 0.92f) * alpha
            val stickerColor = if ((row + column) % 2 == 0) {
                tintColor(toneColor, 0.10f + (intensity * 0.12f)).copy(alpha = stickerAlpha)
            } else {
                shadeColor(toneColor, 0.04f + ((1f - intensity) * 0.14f)).copy(alpha = stickerAlpha)
            }
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        tintColor(toneColor, 0.12f).copy(alpha = (stickerAlpha * 0.96f).coerceAtMost(alpha)),
                        stickerColor,
                        shadeColor(toneColor, 0.18f).copy(alpha = (stickerAlpha * 0.94f).coerceAtMost(alpha)),
                    ),
                    startY = stickerTopLeft.y,
                    endY = stickerTopLeft.y + stickerSize,
                ),
                topLeft = stickerTopLeft,
                size = Size(stickerSize, stickerSize),
                cornerRadius = stickerCorner,
            )
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.16f * alpha),
                topLeft = stickerTopLeft,
                size = Size(stickerSize, stickerSize),
                cornerRadius = stickerCorner,
                style = Stroke(width = (stickerSize * 0.08f).coerceAtLeast(0.8f)),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.14f * alpha),
                topLeft = stickerTopLeft + Offset(stickerSize * 0.10f, stickerSize * 0.08f),
                size = Size(stickerSize * 0.62f, stickerSize * 0.20f),
                cornerRadius = CornerRadius(stickerCorner.x * 0.7f, stickerCorner.y * 0.7f),
            )
        }
    }
}

private fun DrawScope.drawTornadoPattern(
    topLeft: Offset,
    size: Size,
    toneColor: Color,
    alpha: Float,
    pulse: Float,
) {
    val minDimension = minOf(size.width, size.height)
    val center = topLeft + Offset(size.width / 2f, size.height / 2f)
    val rotationOffset = pulse * 2f * PI.toFloat()

    // Background less faded as requested
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                toneColor.copy(alpha = 0.68f * alpha),
                Color.Transparent,
            ),
            center = center,
            radius = minDimension * 0.54f,
        ),
        center = center,
        radius = minDimension * 0.54f,
    )

    // Swirling tornado effect
    val tornadoPath = Path()
    val segments = 40
    val rotations = 3f
    for (i in 0..segments) {
        val t = i.toFloat() / segments
        val radius = (0.05f + t * 0.35f) * minDimension
        val angle = t * rotations * 2f * PI.toFloat() + rotationOffset
        val x = center.x + cos(angle) * radius
        val y = (center.y + (minDimension * 0.35f)) - (t * minDimension * 0.7f)
        if (i == 0) tornadoPath.moveTo(x, y) else tornadoPath.lineTo(x, y)
    }

    drawPath(
        path = tornadoPath,
        color = toneColor.copy(alpha = alpha),
        style = Stroke(
            width = (minDimension * 0.06f).coerceAtLeast(1.8f),
            cap = StrokeCap.Round,
        ),
    )

    repeat(3) { index ->
        val arcRotation = (rotationOffset * (1.2f + index * 0.4f)) % (2f * PI.toFloat())
        drawArc(
            color = toneColor.copy(alpha = (0.25f - index * 0.05f).coerceAtLeast(0.05f) * alpha),
            startAngle = arcRotation * (180f / PI.toFloat()),
            sweepAngle = 120f,
            useCenter = false,
            topLeft = topLeft + Offset(size.width * (0.1f + index * 0.08f), size.height * (0.1f + index * 0.08f)),
            size = Size(size.width * (0.8f - index * 0.16f), size.height * (0.8f - index * 0.16f)),
            style = Stroke(width = (minDimension * 0.02f).coerceAtLeast(1f), cap = StrokeCap.Round),
        )
    }

    // Border in main color as requested
    drawRoundRect(
        color = toneColor.copy(alpha = 0.8f * alpha),
        topLeft = topLeft,
        size = size,
        cornerRadius = CornerRadius(minDimension * 0.16f, minDimension * 0.16f),
        style = Stroke(width = (minDimension * 0.05f).coerceAtLeast(1.5f))
    )
}

private fun angularPanelPath(
    topLeft: Offset,
    size: Size,
    insetFraction: Float,
    notchFraction: Float,
): Path {
    val insetX = size.width * insetFraction
    val insetY = size.height * insetFraction
    val notchX = size.width * notchFraction
    val notchY = size.height * notchFraction
    return Path().apply {
        moveTo(topLeft.x + insetX + notchX, topLeft.y + insetY)
        lineTo(topLeft.x + size.width - insetX, topLeft.y + insetY)
        lineTo(topLeft.x + size.width - insetX, topLeft.y + size.height - insetY - notchY)
        lineTo(topLeft.x + size.width - insetX - notchX, topLeft.y + size.height - insetY)
        lineTo(topLeft.x + insetX, topLeft.y + size.height - insetY)
        lineTo(topLeft.x + insetX, topLeft.y + insetY + notchY)
        close()
    }
}

private fun DrawScope.drawBrickPattern(
    topLeft: Offset,
    size: Size,
    baseTone: Color,
    alpha: Float,
    cornerRadius: CornerRadius,
) {
    val mortarColor = shadeColor(baseTone, 0.56f).copy(alpha = 0.38f * alpha)
    val brickStroke = Color.White.copy(alpha = 0.12f * alpha)
    val mortarWidth = (minOf(size.width, size.height) * 0.050f).coerceIn(1.2f, 2.8f)
    val rowBands = listOf(
        0.00f to 0.36f,
        0.36f to 0.68f,
        0.68f to 1.00f,
    )
    val rowJointFractions = listOf(
        listOf(0.50f),
        listOf(0.26f, 0.74f),
        listOf(0.50f),
    )

    listOf(0.36f, 0.68f).forEach { rowFraction ->
        val y = topLeft.y + size.height * rowFraction
        drawLine(
            color = mortarColor,
            start = Offset(topLeft.x, y),
            end = Offset(topLeft.x + size.width, y),
            strokeWidth = mortarWidth,
        )
        drawLine(
            color = Color.White.copy(alpha = 0.08f * alpha),
            start = Offset(topLeft.x, y - mortarWidth * 0.34f),
            end = Offset(topLeft.x + size.width, y - mortarWidth * 0.34f),
            strokeWidth = mortarWidth * 0.26f,
        )
    }

    rowBands.forEachIndexed { index, (rowStartFraction, rowEndFraction) ->
        val rowStart = topLeft.y + size.height * rowStartFraction
        val rowEnd = topLeft.y + size.height * rowEndFraction
        rowJointFractions[index].forEach { jointFraction ->
            val x = topLeft.x + size.width * jointFraction
            drawLine(
                color = mortarColor,
                start = Offset(x, rowStart),
                end = Offset(x, rowEnd),
                strokeWidth = mortarWidth,
            )
        }
    }

    listOf(
        Pair(Offset(0.10f, 0.14f), Size(0.28f, 0.10f)),
        Pair(Offset(0.60f, 0.14f), Size(0.22f, 0.10f)),
        Pair(Offset(0.08f, 0.48f), Size(0.18f, 0.08f)),
        Pair(Offset(0.40f, 0.46f), Size(0.18f, 0.08f)),
        Pair(Offset(0.70f, 0.80f), Size(0.18f, 0.07f)),
    ).forEach { (offsetFraction, sizeFraction) ->
        val accentTopLeft =
            topLeft + Offset(size.width * offsetFraction.x, size.height * offsetFraction.y)
        drawRoundRect(
            color = brickStroke,
            topLeft = accentTopLeft,
            size = Size(size.width * sizeFraction.width, size.height * sizeFraction.height),
            cornerRadius = CornerRadius(cornerRadius.x * 0.20f, cornerRadius.y * 0.20f),
        )
    }

    drawRoundRect(
        color = Color.Black.copy(alpha = 0.16f * alpha),
        topLeft = topLeft,
        size = size,
        cornerRadius = CornerRadius(cornerRadius.x * 0.52f, cornerRadius.y * 0.52f),
        style = Stroke(width = (minOf(size.width, size.height) * 0.028f).coerceAtLeast(1f)),
    )
}

private fun DrawScope.drawSoundWavePattern(
    topLeft: Offset,
    size: Size,
    toneColor: Color,
    alpha: Float,
    pulse: Float,
) {
    val minDim = minOf(size.width, size.height)
    val center = topLeft + Offset(size.width / 2f, size.height / 2f)
    val strokeWidth = (minDim * 0.072f).coerceIn(2.4f, 6.0f)

    // Base container: faded version of the tone color
    drawRoundRect(
        color = toneColor.copy(alpha = 0.25f * alpha),
        topLeft = topLeft,
        size = size,
        cornerRadius = CornerRadius(minDim * 0.22f, minDim * 0.22f),
    )

    // Horizontal baseline (also faded tone color)
    drawLine(
        color = toneColor.copy(alpha = 0.40f * alpha),
        start = Offset(topLeft.x + size.width * 0.12f, center.y),
        end = Offset(topLeft.x + size.width * 0.88f, center.y),
        strokeWidth = strokeWidth * 0.30f,
    )

    // spikes
    val spikeCount = 9
    val spikeGap = (size.width * 0.72f) / (spikeCount - 1)
    val startX = topLeft.x + size.width * 0.14f
    
    val seed = (topLeft.x.toInt() * 37 + topLeft.y.toInt())

    for (i in 0 until spikeCount) {
        val x = startX + i * spikeGap
        val distFromCenter = kotlin.math.abs(i - (spikeCount - 1) / 2f) / ((spikeCount - 1) / 2f)
        val centerWeight = 1.0f - (distFromCenter * 0.6f)
        
        val spikeSeed = seed + i * 997
        val uniqueFactor = 0.4f + (spikeSeed % 60) / 100f
        
        val baseHeight = minDim * 0.75f * centerWeight * uniqueFactor
        val pulseEffect = 0.12f * minDim * sin(pulse * 6.8f + i * 0.8f + seed * 0.15f)
        val finalHeight = (baseHeight + pulseEffect).coerceIn(minDim * 0.10f, minDim * 0.90f)

        // Main spike (Solid tone color)
        drawLine(
            color = toneColor.copy(alpha = alpha),
            start = Offset(x, center.y - finalHeight / 2f),
            end = Offset(x, center.y + finalHeight / 2f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawPrismRefraction(
    topLeft: Offset,
    size: Size,
    toneColor: Color,
    alpha: Float,
    cornerRadius: CornerRadius,
    pulse: Float,
) {
    // Base background layer
    val prismBg = lerp(Color.White, toneColor, 0.20f).copy(alpha = alpha)
    drawRoundRect(
        color = prismBg,
        topLeft = topLeft,
        size = size,
        cornerRadius = cornerRadius,
    )

    // CORE PEAK POINT ANIMATED
    val peakMotionRadius = size.width * 0.12f // Slightly smaller radius
    val peakMotionAngle = pulse * PI.toFloat() // Half speed rotation (0 to 180 deg and back)
    val peakOffsetX = cos(peakMotionAngle) * peakMotionRadius
    val peakOffsetY = sin(peakMotionAngle) * peakMotionRadius

    val peak = topLeft + Offset(
        size.width * 0.5f + peakOffsetX,
        size.height * 0.5f + peakOffsetY
    )

    // Facet drawing with tiling logic to cover whole area
    fun drawSolidFacet(points: List<Offset>, facetAlpha: Float, isHighlight: Boolean) {
        val path = Path().apply {
            if (points.isNotEmpty()) {
                moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) {
                    lineTo(points[i].x, points[i].y)
                }
                close()
            }
        }
        
        val displayColor = if (isHighlight) {
            tintColor(toneColor, facetAlpha * 0.7f)
        } else {
            shadeColor(toneColor, facetAlpha * 0.7f)
        }.copy(alpha = alpha * 0.95f)

        drawPath(path = path, color = displayColor)
        
        drawPath(
            path = path,
            color = Color.White.copy(alpha = 0.22f * alpha),
            style = Stroke(width = 0.9f)
        )
    }

    val corners = listOf(
        topLeft,
        topLeft + Offset(size.width, 0f),
        topLeft + Offset(size.width, size.height),
        topLeft + Offset(0f, size.height)
    )

    // Midpoints on each edge to create 8 non-overlapping segments
    val tMid = topLeft + Offset(size.width * 0.5f, 0f)
    val rMid = topLeft + Offset(size.width, size.height * 0.5f)
    val bMid = topLeft + Offset(size.width * 0.5f, size.height)
    val lMid = topLeft + Offset(0f, size.height * 0.5f)

    // Draw 8 non-overlapping facets (triangles) that meet at the peak
    drawSolidFacet(listOf(corners[0], tMid, peak), 0.08f, true)
    drawSolidFacet(listOf(tMid, corners[1], peak), 0.25f, false)
    drawSolidFacet(listOf(corners[1], rMid, peak), 0.05f, true)
    drawSolidFacet(listOf(rMid, corners[2], peak), 0.42f, false)
    drawSolidFacet(listOf(corners[2], bMid, peak), 0.12f, true)
    drawSolidFacet(listOf(bMid, corners[3], peak), 0.38f, false)
    drawSolidFacet(listOf(corners[3], lMid, peak), 0.15f, true)
    drawSolidFacet(listOf(lMid, corners[0], peak), 0.52f, false)

    // Sharp light streaks
    drawLine(
        color = Color.White.copy(alpha = 0.35f * alpha),
        start = corners[0],
        end = peak,
        strokeWidth = 1.2f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color.White.copy(alpha = 0.25f * alpha),
        start = corners[2],
        end = peak,
        strokeWidth = 1.2f,
        cap = StrokeCap.Round
    )

    // Shimmer at center
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.25f * alpha), Color.Transparent),
            center = peak,
            radius = size.width * 0.20f
        ),
        center = peak,
        radius = size.width * 0.20f
    )

    // Outer frame
    drawRoundRect(
        color = Color.White.copy(alpha = 0.25f * alpha),
        topLeft = topLeft,
        size = size,
        cornerRadius = cornerRadius,
        style = Stroke(width = 1.8f)
    )
}

private fun DrawScope.drawGridSplitPattern(
    topLeft: Offset,
    size: Size,
    toneColor: Color,
    alpha: Float,
    cornerRadius: CornerRadius,
) {
    val minDim = minOf(size.width, size.height)
    val gridCount = 3
    val gapPx = (minDim * 0.05f).coerceIn(1.5f, 4f)
    val cellSize = (minDim - (gapPx * (gridCount - 1))) / gridCount
    
    val innerCornerRadius = CornerRadius(cornerRadius.x * 0.4f, cornerRadius.y * 0.4f)

    for (row in 0 until gridCount) {
        for (col in 0 until gridCount) {
            val cellTopLeft = topLeft + Offset(
                col * (cellSize + gapPx),
                row * (cellSize + gapPx)
            )
            
            // Base cell body
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        tintColor(toneColor, 0.10f).copy(alpha = alpha),
                        shadeColor(toneColor, 0.15f).copy(alpha = alpha),
                    ),
                    startY = cellTopLeft.y,
                    endY = cellTopLeft.y + cellSize
                ),
                topLeft = cellTopLeft,
                size = Size(cellSize, cellSize),
                cornerRadius = innerCornerRadius,
            )
            
            // Cell highlight
            drawRoundRect(
                color = Color.White.copy(alpha = 0.12f * alpha),
                topLeft = cellTopLeft,
                size = Size(cellSize, cellSize),
                cornerRadius = innerCornerRadius,
                style = Stroke(width = 1.0f)
            )
        }
    }
}

private data class StableStar(
    val x: Float,
    val y: Float,
    val radiusFactor: Float,
    val alpha: Float,
    val twinkle: Boolean,
)

private fun cosmicSeed(topLeft: Offset, size: Size, colorSeed: Int): Int {
    val columnBucket = if (size.width > 0f) (topLeft.x / size.width).toInt() else 0
    val rowBucket = if (size.height > 0f) (topLeft.y / size.height).toInt() else 0
    return (columnBucket * 31) xor (rowBucket * 17) xor (size.width.toBits() * 13) xor size.height.toBits() xor colorSeed
}

private fun stableStarField(seed: Int): List<StableStar> {
    var state = seed.toLong() and 0xFFFF_FFFFL

    fun nextFloat(): Float {
        state = ((state * 1_664_525L) + 1_013_904_223L) and 0xFFFF_FFFFL
        return (((state ushr 8) and 0x00FF_FFFFL).toFloat() / 0x00FF_FFFF.toFloat()).coerceIn(
            0f,
            1f
        )
    }

    return List(CosmicStarCount) {
        StableStar(
            x = 0.08f + (nextFloat() * 0.84f),
            y = 0.08f + (nextFloat() * 0.84f),
            radiusFactor = 0.010f + (nextFloat() * 0.026f),
            alpha = 0.52f + (nextFloat() * 0.44f),
            twinkle = nextFloat() > 0.56f,
        )
    }
}

private fun marbleVeinPath(
    topLeft: Offset,
    size: Size,
    points: List<Offset>,
): Path = Path().apply {
    val resolved = points.map { fraction ->
        Offset(topLeft.x + size.width * fraction.x, topLeft.y + size.height * fraction.y)
    }
    if (resolved.isEmpty()) return@apply
    moveTo(resolved.first().x, resolved.first().y)
    resolved.windowed(size = 2).forEach { (start, end) ->
        val controlX = (start.x + end.x) / 2f
        cubicTo(
            controlX,
            start.y,
            controlX,
            end.y,
            end.x,
            end.y,
        )
    }
}

private fun hexagonPath(center: Offset, radius: Float): Path = Path().apply {
    repeat(6) { index ->
        val angle = ((PI / 180.0) * ((60 * index) - 30)).toFloat()
        val point = center + Offset(cos(angle) * radius, sin(angle) * radius)
        if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
    }
    close()
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
            BlockVisualStyle.Sharp3D -> 0.22f
            BlockVisualStyle.Wood -> 0.66f
            BlockVisualStyle.GridSplit -> 0.28f
            BlockVisualStyle.Crystal -> 0f
            BlockVisualStyle.DynamicLiquid -> 0.70f
            BlockVisualStyle.MatteSoft -> 0.78f
            BlockVisualStyle.NeonGlow -> 0.92f
            BlockVisualStyle.Tornado -> 0.88f
            BlockVisualStyle.StoneTexture -> 0.66f
            BlockVisualStyle.HoneycombTexture -> 0.72f
            BlockVisualStyle.LightBurst -> 0.96f
            BlockVisualStyle.LiquidMarble -> 0.78f
            BlockVisualStyle.SpiderWeb -> 0.44f
            BlockVisualStyle.Cosmic -> 0.56f
            BlockVisualStyle.Brick -> 0.46f
            BlockVisualStyle.SoundWave -> 0.82f
            BlockVisualStyle.Prism -> 0.68f
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
    BlockVisualStyle.GridSplit -> 10.dp
    BlockVisualStyle.Crystal -> 0.dp
    BlockVisualStyle.DynamicLiquid -> 18.dp
    BlockVisualStyle.MatteSoft -> 18.dp
    BlockVisualStyle.NeonGlow -> 22.dp
    BlockVisualStyle.Tornado -> 18.dp
    BlockVisualStyle.StoneTexture -> 12.dp
    BlockVisualStyle.HoneycombTexture -> 12.dp
    BlockVisualStyle.LightBurst -> 20.dp
    BlockVisualStyle.LiquidMarble -> 18.dp
    BlockVisualStyle.SpiderWeb -> 6.dp
    BlockVisualStyle.Cosmic -> 10.dp
    BlockVisualStyle.Brick -> 8.dp
    BlockVisualStyle.SoundWave -> 16.dp
    BlockVisualStyle.Prism -> 12.dp
}

private fun blockStyleCornerScale(style: BlockVisualStyle): Float = when (style) {
    BlockVisualStyle.Flat -> 1.0f
    BlockVisualStyle.Bubble -> 1.20f
    BlockVisualStyle.Outline -> 0.82f
    BlockVisualStyle.Sharp3D -> 0.30f
    BlockVisualStyle.Wood -> 0.76f
    BlockVisualStyle.GridSplit -> 0.54f
    BlockVisualStyle.Crystal -> 0f
    BlockVisualStyle.DynamicLiquid -> 0.85f
    BlockVisualStyle.MatteSoft -> 1.0f
    BlockVisualStyle.NeonGlow -> 1.20f
    BlockVisualStyle.Tornado -> 1.00f
    BlockVisualStyle.StoneTexture -> 0.76f
    BlockVisualStyle.HoneycombTexture -> 0.78f
    BlockVisualStyle.LightBurst -> 1.04f
    BlockVisualStyle.LiquidMarble -> 0.98f
    BlockVisualStyle.SpiderWeb -> 0.34f
    BlockVisualStyle.Cosmic -> 0.54f
    BlockVisualStyle.Brick -> 0.46f
    BlockVisualStyle.SoundWave -> 0.88f
    BlockVisualStyle.Prism -> 0.72f
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

    BlockColorPalette.Aurora -> when (this) {
        CellTone.Cyan -> Color(0xFF69DCF6)
        CellTone.Gold -> Color(0xFF9FE7FF)
        CellTone.Violet -> Color(0xFFAB9BFF)
        CellTone.Emerald -> Color(0xFF63E1BE)
        CellTone.Coral -> Color(0xFFFF88B4)
        CellTone.Blue -> Color(0xFF61A8FF)
        CellTone.Rose -> Color(0xFFD7A6FF)
        CellTone.Lime -> Color(0xFF8BF0D0)
        CellTone.Amber -> Color(0xFFFFD57D)
    }

    BlockColorPalette.Sunset -> when (this) {
        CellTone.Cyan -> Color(0xFFFFA77B)
        CellTone.Gold -> Color(0xFFFFC95C)
        CellTone.Violet -> Color(0xFFC79BFF)
        CellTone.Emerald -> Color(0xFFFF9B72)
        CellTone.Coral -> Color(0xFFFF6E7C)
        CellTone.Blue -> Color(0xFF8E7CFF)
        CellTone.Rose -> Color(0xFFFF93B5)
        CellTone.Lime -> Color(0xFFFFB85F)
        CellTone.Amber -> Color(0xFFFF8C42)
    }

    BlockColorPalette.SoftPastel -> when (this) {
        CellTone.Cyan -> Color(0xFF9EDFF2)
        CellTone.Gold -> Color(0xFFF6D8A8)
        CellTone.Violet -> Color(0xFFCABCF7)
        CellTone.Emerald -> Color(0xFFB7E6D2)
        CellTone.Coral -> Color(0xFFF2AFC0)
        CellTone.Blue -> Color(0xFFAFC8F4)
        CellTone.Rose -> Color(0xFFF7C6D9)
        CellTone.Lime -> Color(0xFFD6EDB5)
        CellTone.Amber -> Color(0xFFF3C9A6)
    }
}

internal fun resolveBoardBlockStyle(
    selectedStyle: BlockVisualStyle,
    mode: BoardBlockStyleMode,
): BlockVisualStyle = when (mode) {
    BoardBlockStyleMode.AlwaysFlat -> BlockVisualStyle.Flat
    BoardBlockStyleMode.MatchSelectedBlockStyle -> normalizeBlockVisualStyle(selectedStyle)
}

private fun boardSpecialIcon(type: SpecialBlockType) = when (type) {
    SpecialBlockType.ColumnClearer -> Icons.Filled.SwapVert
    SpecialBlockType.RowClearer -> Icons.Filled.SwapHoriz
    SpecialBlockType.Ghost -> Icons.Filled.Layers
    SpecialBlockType.Heavy -> Icons.Filled.Hub
    SpecialBlockType.None -> Icons.AutoMirrored.Filled.HelpOutline
}
