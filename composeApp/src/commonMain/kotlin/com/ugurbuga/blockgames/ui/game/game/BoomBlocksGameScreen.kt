package com.ugurbuga.blockgames.ui.game.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import blockgames.composeapp.generated.resources.Res
import blockgames.composeapp.generated.resources.time_remaining
import com.ugurbuga.blockgames.game.model.BoardMatrix
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.game.model.GameStatus
import com.ugurbuga.blockgames.game.model.GridPoint
import com.ugurbuga.blockgames.game.model.paletteColor
import com.ugurbuga.blockgames.game.model.resolveBoardBlockStyle
import com.ugurbuga.blockgames.localization.LocalAppSettings
import com.ugurbuga.blockgames.platform.currentEpochMillis
import com.ugurbuga.blockgames.ui.game.GameOverDialog
import com.ugurbuga.blockgames.ui.game.MinimalTopBar
import com.ugurbuga.blockgames.ui.game.boardCellCornerRadiusPx
import com.ugurbuga.blockgames.ui.game.drawCellBody
import com.ugurbuga.blockgames.ui.theme.BlockGamesThemeTokens
import com.ugurbuga.blockgames.ui.theme.GameUiShapeTokens
import com.ugurbuga.blockgames.ui.theme.blockGamesSurfaceShadow
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val BOOM_BLOCKS_HINT_CYCLE_MILLIS = 1_100
private const val BOOM_BLOCKS_EXPLOSION_DURATION_MILLIS = 350
private const val BOOM_BLOCKS_FALL_DELAY_MILLIS = 220L
private const val BOOM_BLOCKS_FALL_DURATION_MILLIS = 450

@Composable
fun BoomBlocksGameScreen(
    gameState: GameState,
    onTapCell: (GridPoint) -> Unit,
    onRestart: () -> Unit,
    onBack: () -> Unit,
    highestScore: Int,
    modifier: Modifier = Modifier,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    var cellSize by remember { mutableStateOf(0f) }

    var currentTime by remember { mutableStateOf(currentEpochMillis()) }
    LaunchedEffect(gameState.status) {
        while (gameState.status == GameStatus.Running) {
            kotlinx.coroutines.delay(500)
            currentTime = currentEpochMillis()
        }
    }

    val isIdle = gameState.status == GameStatus.Running && (currentTime - gameState.lastActionTime > 5000)
    
    val infiniteTransition = rememberInfiniteTransition(label = "boomBlocks")
    val hintPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(BOOM_BLOCKS_HINT_CYCLE_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hintPhase",
    )

    val stylePulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "stylePulse",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        uiColors.screenGradientTop,
                        uiColors.screenGradientBottom,
                    ),
                ),
            )
            .safeDrawingPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MinimalTopBar(
                gameState = gameState,
                scoreHighlightStrengthProvider = { 0f },
                scoreHighlightScaleProvider = { 1f },
                remainingTimeLabel = stringResource(Res.string.time_remaining),
                onBack = onBack,
                onRestart = onRestart,
            )

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(gameState.config.columns.toFloat() / gameState.config.rows.toFloat())
                    .blockGamesSurfaceShadow(
                        shape = RoundedCornerShape(GameUiShapeTokens.panelCorner),
                        elevation = 8.dp,
                    )
                    .clip(RoundedCornerShape(GameUiShapeTokens.panelCorner))
                    .background(uiColors.gameSurface.copy(alpha = 0.8f))
                    .onGloballyPositioned { coordinates ->
                        cellSize = coordinates.size.width.toFloat() / gameState.config.columns
                    }
                    .pointerInput(gameState.status) {
                        if (gameState.status == GameStatus.Running) {
                            detectTapGestures { offset ->
                                val col = (offset.x / cellSize).toInt()
                                val row = (offset.y / cellSize).toInt()
                                if (col in 0 until gameState.config.columns && row in 0 until gameState.config.rows) {
                                    onTapCell(GridPoint(col, row))
                                }
                            }
                        }
                    },
            ) {
                GameGrid(
                    gameState = gameState,
                    modifier = Modifier.fillMaxSize(),
                    hintEnabled = isIdle,
                    hintPhase = hintPhase,
                    stylePulse = stylePulse,
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(64.dp))
        }

        if (gameState.status == GameStatus.GameOver) {
            GameOverDialog(
                gameState = gameState,
                highestScore = highestScore,
                showNewHighScoreMessage = gameState.score > highestScore,
                revealProgressProvider = { 1f },
                canUseExtraLife = false,
                isExtraLifeLoading = false,
                showExtraLifeButton = false,
                onPlayAgain = onRestart,
                onUseExtraLife = {},
            )
        }
    }
}

private enum class GravityDir { Up, Down, Left, Right }

private data class VisualBlock(
    val id: Int,
    val tone: CellTone,
    val sourceRow: Float,
    val targetRow: Int,
    val sourceCol: Float,
    val targetCol: Int
)

@Composable
private fun GameGrid(
    gameState: GameState,
    modifier: Modifier = Modifier,
    hintEnabled: Boolean = false,
    hintPhase: Float = 0f,
    stylePulse: Float = 0f,
) {
    val settings = LocalAppSettings.current
    val resolvedStyle = resolveBoardBlockStyle(
        selectedStyle = settings.blockVisualStyle,
        mode = settings.boardBlockStyleMode,
    )
    val board = gameState.board
    
    var previousBoard by remember { mutableStateOf<BoardMatrix?>(null) }
    var visualBlocks by remember { mutableStateOf<List<VisualBlock>>(emptyList()) }
    val animationProgress = remember { Animatable(0f) }
    
    val explosionProgress = remember { Animatable(0f) }
    var explodedPoints by remember { mutableStateOf(emptySet<GridPoint>()) }
    var explosionTones by remember { mutableStateOf(mapOf<GridPoint, CellTone>()) }

    LaunchedEffect(board, gameState.clearAnimationToken) {
        val prev = previousBoard
        
        // Reset previous board on restart or significant change to prevent sliding from old game sessions
        val isRestart = gameState.status == GameStatus.Running && gameState.score == 0
        if (isRestart || prev == null || prev.columns != board.columns || prev.rows != board.rows) {
            val initialBlocks = mutableListOf<VisualBlock>()
            for (c in 0 until board.columns) {
                for (r in 0 until board.rows) {
                    val cell = board.cellAt(c, r) ?: continue
                    initialBlocks.add(VisualBlock(
                        id = cell.value,
                        tone = cell.tone,
                        sourceRow = r.toFloat(),
                        targetRow = r,
                        sourceCol = c.toFloat(),
                        targetCol = c,
                    ))
                }
            }
            visualBlocks = initialBlocks
            previousBoard = board
            animationProgress.snapTo(1f)
            return@LaunchedEffect
        }

        // Determine gravity direction based on explosion center
        val group = gameState.recentlyExplodedPoints
        val gravityDirection = if (group.isNotEmpty()) {
            val avgRow = group.map { it.row }.average().toFloat()
            val avgCol = group.map { it.column }.average().toFloat()
            val rows = board.rows.toFloat()
            val cols = board.columns.toFloat()

            val relDistToTop = avgRow / rows
            val relDistToBottom = (rows - 1 - avgRow) / rows
            val relDistToLeft = avgCol / cols
            val relDistToRight = (cols - 1 - avgCol) / cols

            val minDist = minOf(relDistToTop, relDistToBottom, relDistToLeft, relDistToRight)
            when {
                minDist == relDistToTop -> GravityDir.Up
                minDist == relDistToBottom -> GravityDir.Down
                minDist == relDistToLeft -> GravityDir.Left
                else -> GravityDir.Right
            }
        } else GravityDir.Down

        // Map blocks to their previous positions
        val prevBlockPositions = mutableMapOf<Int, GridPoint>()
        for (c in 0 until prev.columns) {
            for (r in 0 until prev.rows) {
                prev.cellAt(c, r)?.let { prevBlockPositions[it.value] = GridPoint(c, r) }
            }
        }

        val newBlocks = mutableListOf<VisualBlock>()
        val columnRefillOffsets = IntArray(board.columns) { 0 }
        val rowRefillOffsets = IntArray(board.rows) { 0 }

        for (c in 0 until board.columns) {
            for (r in 0 until board.rows) {
                val cell = board.cellAt(c, r) ?: continue
                val prevPos = prevBlockPositions[cell.value]

                if (prevPos != null) {
                    newBlocks.add(VisualBlock(
                        id = cell.value,
                        tone = cell.tone,
                        sourceRow = prevPos.row.toFloat(),
                        targetRow = r,
                        sourceCol = prevPos.column.toFloat(),
                        targetCol = c,
                    ))
                } else {
                    // New block logic
                    val (sourceCol, sourceRow) = when (gravityDirection) {
                        GravityDir.Up -> {
                            val offset = board.rows + columnRefillOffsets[c]
                            columnRefillOffsets[c]++
                            c.toFloat() to offset.toFloat()
                        }
                        GravityDir.Down -> {
                            val offset = -1 - columnRefillOffsets[c]
                            columnRefillOffsets[c]++
                            c.toFloat() to offset.toFloat()
                        }
                        GravityDir.Left -> {
                            val offset = board.columns + rowRefillOffsets[r]
                            rowRefillOffsets[r]++
                            offset.toFloat() to r.toFloat()
                        }
                        GravityDir.Right -> {
                            val offset = -1 - rowRefillOffsets[r]
                            rowRefillOffsets[r]++
                            offset.toFloat() to r.toFloat()
                        }
                    }
                    newBlocks.add(VisualBlock(
                        id = cell.value,
                        tone = cell.tone,
                        sourceRow = sourceRow,
                        targetRow = r,
                        sourceCol = sourceCol,
                        targetCol = c,
                    ))
                }
            }
        }

        visualBlocks = newBlocks
        previousBoard = board
        animationProgress.stop()
        animationProgress.snapTo(0f)
        
        if (gameState.recentlyExplodedPoints.isNotEmpty()) {
            kotlinx.coroutines.delay(BOOM_BLOCKS_FALL_DELAY_MILLIS)
        }
        
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(BOOM_BLOCKS_FALL_DURATION_MILLIS, easing = FastOutSlowInEasing)
        )
    }
    
    LaunchedEffect(gameState.clearAnimationToken) {
        if (gameState.recentlyExplodedPoints.isNotEmpty()) {
            explosionTones = gameState.recentlyExplodedTones
            explodedPoints = gameState.recentlyExplodedPoints
            explosionProgress.stop()
            explosionProgress.snapTo(1f)
            explosionProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(BOOM_BLOCKS_EXPLOSION_DURATION_MILLIS, easing = LinearEasing)
            )
            explodedPoints = emptySet()
            explosionTones = emptyMap()
        }
    }

    val explodableCells = remember(board) {
        val visited = mutableSetOf<GridPoint>()
        val result = mutableSetOf<GridPoint>()
        for (row in 0 until board.rows) {
            for (col in 0 until board.columns) {
                val point = GridPoint(col, row)
                if (point !in visited) {
                    val group = findConnectedGroup(board, point)
                    if (group.size >= 3) {
                        result.addAll(group)
                    }
                    visited.addAll(group)
                }
            }
        }
        result
    }

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val cellWidth = size.width / board.columns
        val cellHeight = size.height / board.rows
        
        visualBlocks.forEach { block ->
            val progress = animationProgress.value
            val currentRow = block.sourceRow + (block.targetRow - block.sourceRow) * progress
            val currentCol = block.sourceCol + (block.targetCol - block.sourceCol) * progress

            val isExplodable = GridPoint(block.targetCol, block.targetRow) in explodableCells
            val hintOffset = ((block.targetCol * 0.13f) + (block.targetRow * 0.07f)) % 1f
            val cellHintPhase = (hintPhase + hintOffset) % 1f
            val shimmer = ((sin(cellHintPhase * 2.0 * PI).toFloat()) + 1f) / 2f
            val scale = if (hintEnabled && isExplodable) 1.02f + (0.04f * shimmer) else 1f
            val drawSize = cellWidth * 0.92f * scale
            val offsetX = (cellWidth - drawSize) / 2
            val offsetY = (cellHeight - drawSize) / 2
            val blockTopLeft = Offset(
                x = currentCol * cellWidth + offsetX,
                y = currentRow * cellHeight + offsetY,
            )

            if (hintEnabled && isExplodable) {
                val haloInset = drawSize * 0.08f
                val haloSize = Size(
                    width = drawSize + (haloInset * (1.2f + shimmer)),
                    height = drawSize + (haloInset * (1.2f + shimmer)),
                )
                val haloTopLeft = Offset(
                    x = blockTopLeft.x - ((haloSize.width - drawSize) / 2f),
                    y = blockTopLeft.y - ((haloSize.height - drawSize) / 2f),
                )
                drawRect(
                    color = Color.White.copy(alpha = 0.10f + (0.14f * shimmer)),
                    topLeft = haloTopLeft,
                    size = haloSize,
                )
            }

            val cornerRadiusPx = boardCellCornerRadiusPx(drawSize, resolvedStyle)
            drawCellBody(
                tone = block.tone,
                palette = settings.blockColorPalette,
                style = resolvedStyle,
                topLeft = blockTopLeft,
                size = Size(drawSize, drawSize),
                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                pulse = stylePulse,
            )
        }
        
        if (explosionProgress.value > 0) {
            val alpha = explosionProgress.value
            explodedPoints.forEach { point ->
                val tone = explosionTones[point] ?: CellTone.Cyan
                val toneColor = tone.paletteColor(settings.blockColorPalette)
                val centerX = point.column * cellWidth + cellWidth / 2
                val centerY = point.row * cellHeight + cellHeight / 2
                
                val radius = cellWidth * (1f - alpha) * 2.5f
                drawCircle(
                    color = toneColor.copy(alpha = alpha * 0.4f),
                    radius = radius,
                    center = Offset(centerX, centerY)
                )
                
                repeat(12) { i ->
                    val angle = (i * 30f) * (PI / 180f).toFloat()
                    val dist = cellWidth * (1f - alpha) * 3f
                    val sparkleX = centerX + cos(angle.toDouble()).toFloat() * dist
                    val sparkleY = centerY + sin(angle.toDouble()).toFloat() * dist
                    drawCircle(
                        color = toneColor.copy(alpha = alpha),
                        radius = 5.dp.toPx() * alpha,
                        center = Offset(sparkleX, sparkleY)
                    )
                }
            }
        }
    }
}

private fun findConnectedGroup(
    board: BoardMatrix,
    start: GridPoint
): Set<GridPoint> {
    val tone = board.toneAt(start.column, start.row) ?: return emptySet()
    val group = mutableSetOf<GridPoint>()
    val queue = mutableListOf(start)

    while (queue.isNotEmpty()) {
        val point = queue.removeAt(0)
        if (point in group) continue
        if (board.toneAt(point.column, point.row) == tone) {
            group.add(point)
            val neighbors = listOf(
                GridPoint(point.column + 1, point.row),
                GridPoint(point.column - 1, point.row),
                GridPoint(point.column, point.row + 1),
                GridPoint(point.column, point.row - 1)
            )
            for (neighbor in neighbors) {
                if (neighbor.column in 0 until board.columns &&
                    neighbor.row in 0 until board.rows &&
                    neighbor !in group
                ) {
                    queue.add(neighbor)
                }
            }
        }
    }
    return group
}
