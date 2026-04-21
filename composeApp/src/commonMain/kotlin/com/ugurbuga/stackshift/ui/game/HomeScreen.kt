package com.ugurbuga.stackshift.ui.game

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ugurbuga.stackshift.StackShiftTheme
import com.ugurbuga.stackshift.game.model.AppThemeMode
import com.ugurbuga.stackshift.game.model.BlockColorPalette
import com.ugurbuga.stackshift.game.model.BlockVisualStyle
import com.ugurbuga.stackshift.game.model.CellTone
import com.ugurbuga.stackshift.settings.AppSettings
import com.ugurbuga.stackshift.telemetry.AppTelemetry
import com.ugurbuga.stackshift.telemetry.LogScreen
import com.ugurbuga.stackshift.telemetry.NoOpAppTelemetry
import com.ugurbuga.stackshift.telemetry.TelemetryScreenNames
import com.ugurbuga.stackshift.ui.theme.GameUiShapeTokens
import com.ugurbuga.stackshift.ui.theme.StackShiftThemeTokens
import com.ugurbuga.stackshift.ui.theme.isStackShiftDarkTheme
import com.ugurbuga.stackshift.ui.theme.stackShiftSurfaceShadow
import org.jetbrains.compose.resources.stringResource
import stackshift.composeapp.generated.resources.Res
import stackshift.composeapp.generated.resources.app_title
import stackshift.composeapp.generated.resources.high_score
import stackshift.composeapp.generated.resources.home_play_cta
import stackshift.composeapp.generated.resources.settings_language
import stackshift.composeapp.generated.resources.settings_theme
import stackshift.composeapp.generated.resources.settings_tutorial
import stackshift.composeapp.generated.resources.settings_tutorial_replay
import kotlin.math.abs

private const val HomeTitleBannerColumns = 6
private const val HomeTitleBannerRows = 2

@Composable
fun HomeScreen(
    settings: AppSettings,
    highestScore: Int,
    telemetry: AppTelemetry,
    onPlay: () -> Unit,
    onOpenInteractiveGuide: () -> Unit,
    onOpenTutorial: () -> Unit,
    onOpenTheme: () -> Unit,
    onOpenLanguage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    val stylePulseTransition = rememberInfiniteTransition(label = "homeStylePulse")
    val stylePulse by stylePulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "homeStylePulseValue",
    )

    LogScreen(telemetry, TelemetryScreenNames.Home)

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            uiColors.screenGradientTop,
                            uiColors.screenGradientBottom,
                        ),
                    ),
                )
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(Res.string.app_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
                HomeTitleBanner(
                    settings = settings,
                    pulse = stylePulse,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            HomePrimaryPlayButton(
                text = stringResource(Res.string.home_play_cta),
                settings = settings,
                pulse = stylePulse,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(100.dp),
                onClick = onPlay,
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                HomeHighScoreCard(
                    highestScore = highestScore,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    HomeQuickActionButton(
                        text = stringResource(Res.string.settings_tutorial_replay),
                        icon = Icons.Filled.Refresh,
                        tone = CellTone.Emerald,
                        settings = settings,
                        pulse = stylePulse,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenInteractiveGuide,
                    )
                    HomeQuickActionButton(
                        text = stringResource(Res.string.settings_tutorial),
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        tone = CellTone.Gold,
                        settings = settings,
                        pulse = stylePulse,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenTutorial,
                    )
                    HomeQuickActionButton(
                        text = stringResource(Res.string.settings_theme),
                        icon = Icons.Filled.Palette,
                        tone = CellTone.Violet,
                        settings = settings,
                        pulse = stylePulse,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenTheme,
                    )
                    HomeQuickActionButton(
                        text = stringResource(Res.string.settings_language),
                        icon = Icons.Filled.Translate,
                        tone = CellTone.Coral,
                        settings = settings,
                        pulse = stylePulse,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenLanguage,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeTitleBanner(
    settings: AppSettings,
    pulse: Float,
    modifier: Modifier = Modifier,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    val bannerMotionTransition = rememberInfiniteTransition(label = "homeTitleBannerMotion")
    val sequenceClock by bannerMotionTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 15_200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "homeTitleBannerSequenceClock",
    )
    val sequencePhase = normalizedPhase(sequenceClock)
    
    // Lower piece cycle
    val lowerDrift = segmentProgress(sequencePhase, 0.00f, 0.10f)
    val lowerAlign = segmentProgress(sequencePhase, 0.10f, 0.15f)
    val lowerLaunch = segmentProgress(sequencePhase, 0.15f, 0.20f)
    val lowerExplode = segmentProgress(sequencePhase, 0.25f, 0.32f) // Landed at 0.20, wait until 0.25, then explode
    
    // Upper piece cycle
    val upperDrift = segmentProgress(sequencePhase, 0.34f, 0.44f)
    val upperAlign = segmentProgress(sequencePhase, 0.44f, 0.49f)
    val upperLaunch = segmentProgress(sequencePhase, 0.49f, 0.54f)
    val upperExplode = segmentProgress(sequencePhase, 0.59f, 0.66f) // Landed at 0.54, wait until 0.59, then explode

    // STACK launch
    val stackAlign = segmentProgress(sequencePhase, 0.68f, 0.74f)
    val stackLaunch = segmentProgress(sequencePhase, 0.74f, 0.82f)
    
    // SHIFT launch
    val shiftAlign = segmentProgress(sequencePhase, 0.82f, 0.88f)
    val shiftLaunch = segmentProgress(sequencePhase, 0.88f, 0.96f)

    val topRow = rememberHomeTitleRow(word = "STACK", startColumn = 0)
    val bottomRow = rememberHomeTitleRow(word = "SHIFT", startColumn = 1)
    val lowerLaunchTone = CellTone.Gold
    val upperLaunchTone = CellTone.Violet
    Card(
        modifier = modifier
            .fillMaxWidth()
            .stackShiftSurfaceShadow(
                shape = RoundedCornerShape(GameUiShapeTokens.panelCorner),
                elevation = 10.dp,
            ),
        shape = RoundedCornerShape(GameUiShapeTokens.panelCorner),
        colors = CardDefaults.cardColors(containerColor = uiColors.gameSurface.copy(alpha = 0.94f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, uiColors.panelStroke.copy(alpha = 0.82f)),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(6f / 3.8f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            uiColors.panelHighlight.copy(alpha = 0.22f),
                            uiColors.launchGlow.copy(alpha = 0.10f),
                            Color.Transparent,
                        ),
                    ),
                ),
        ) {
            val cellWidth = maxWidth / HomeTitleBannerColumns
            val boardHeight = cellWidth * HomeTitleBannerRows
            val boardCellHeight = cellWidth
            val dockTop = boardHeight + 16.dp
            val dockHeight = maxHeight - dockTop
            val dockCellSize = boardCellHeight
            val dockSingleStartX = (maxWidth - dockCellSize) / 2f
            val dockSingleLeftX = maxWidth * 0.12f
            val dockSingleRightX = maxWidth - dockCellSize - (maxWidth * 0.12f)
            val dockWordStartX = (maxWidth - (dockCellSize * 5f)) / 2f
            val dockPieceY = dockTop + ((dockHeight - dockCellSize) / 2f)
            val topTargetX = 0.dp
            val topGapTargetX = cellWidth * 5f
            val bottomTargetX = cellWidth
            val bottomGapTargetX = 0.dp
            val topRowAlpha = titleTopRowAlpha(
                phase = sequencePhase,
                upperExplode = upperExplode,
                stackLaunch = stackLaunch,
            )
            val bottomRowAlpha = titleBottomRowAlpha(
                phase = sequencePhase,
                lowerExplode = lowerExplode,
                shiftLaunch = shiftLaunch,
            )

            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .height(boardHeight)
                        .fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.matchParentSize()) {
                        Row(
                            modifier = Modifier
                                .height(boardCellHeight)
                                .fillMaxWidth(),
                        ) {
                            topRow.forEachIndexed { column, cell ->
                                if (column == 5) {
                                    HomeTitleEmptyCell(
                                        settings = settings,
                                        modifier = Modifier.weight(1f).fillMaxHeight(),
                                    )
                                } else {
                                    val resolvedCell = cell ?: HomeTitleCell(tone = CellTone.Cyan)
                                    HomeTitleAnimatedCell(
                                        letter = resolvedCell.letter,
                                        tone = resolvedCell.tone,
                                        settings = settings,
                                        pulse = pulse,
                                        alpha = topRowAlpha,
                                        modifier = Modifier.weight(1f).fillMaxHeight(),
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier
                                .height(boardCellHeight)
                                .fillMaxWidth(),
                        ) {
                            bottomRow.forEachIndexed { column, cell ->
                                if (column == 0) {
                                    HomeTitleEmptyCell(
                                        settings = settings,
                                        modifier = Modifier.weight(1f).fillMaxHeight(),
                                    )
                                } else {
                                    val resolvedCell = cell ?: HomeTitleCell(tone = CellTone.Cyan)
                                    HomeTitleAnimatedCell(
                                        letter = resolvedCell.letter,
                                        tone = resolvedCell.tone,
                                        settings = settings,
                                        pulse = pulse,
                                        alpha = bottomRowAlpha,
                                        modifier = Modifier.weight(1f).fillMaxHeight(),
                                    )
                                }
                            }
                        }
                    }

                    HomeTitleRowClearOverlay(
                        alpha = clearFlashAlpha(lowerExplode) * if (sequencePhase in 0.25f..0.32f) 1f else 0f,
                        rowIndex = 1,
                        cellHeight = boardCellHeight,
                        modifier = Modifier.matchParentSize(),
                    )
                    HomeTitleRowClearOverlay(
                        alpha = clearFlashAlpha(upperExplode) * if (sequencePhase in 0.59f..0.66f) 1f else 0f,
                        rowIndex = 0,
                        cellHeight = boardCellHeight,
                        modifier = Modifier.matchParentSize(),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dockHeight),
                ) {
                    HomeTitleMiniDock(
                        modifier = Modifier.matchParentSize(),
                    )
                }
            }

            when {
                sequencePhase < 0.15f -> HomeTitleAnimatedPiece(
                    cells = listOf(HomeTitleCell(tone = lowerLaunchTone)),
                    settings = settings,
                    pulse = pulse,
                    cellSize = dockCellSize,
                    modifier = Modifier.offset(
                        x = lerpDp(dockSingleStartX, dockSingleLeftX, lowerDrift).let { x ->
                            if (sequencePhase > 0.10f) lerpDp(x, bottomGapTargetX, lowerAlign) else x
                        },
                        y = dockPieceY,
                    ),
                )

                sequencePhase < 0.25f -> HomeTitleAnimatedPiece(
                    cells = listOf(HomeTitleCell(tone = lowerLaunchTone)),
                    settings = settings,
                    pulse = pulse,
                    cellSize = dockCellSize,
                    modifier = Modifier.offset(
                        x = bottomGapTargetX,
                        y = lerpDp(dockPieceY, boardCellHeight, lowerLaunch),
                    ),
                )

                sequencePhase < 0.32f -> HomeTitleAnimatedPiece(
                    cells = listOf(HomeTitleCell(tone = lowerLaunchTone)),
                    settings = settings,
                    pulse = pulse,
                    cellSize = dockCellSize,
                    modifier = Modifier
                        .offset(x = bottomGapTargetX, y = boardCellHeight)
                        .graphicsLayer { alpha = 1f - lowerExplode },
                )

                sequencePhase < 0.49f -> HomeTitleAnimatedPiece(
                    cells = listOf(HomeTitleCell(tone = upperLaunchTone)),
                    settings = settings,
                    pulse = pulse,
                    cellSize = dockCellSize,
                    modifier = Modifier.offset(
                        x = lerpDp(dockSingleStartX, dockSingleRightX, upperDrift).let { x ->
                            if (sequencePhase > 0.44f) lerpDp(x, topGapTargetX, upperAlign) else x
                        },
                        y = dockPieceY,
                    ),
                )

                sequencePhase < 0.59f -> HomeTitleAnimatedPiece(
                    cells = listOf(HomeTitleCell(tone = upperLaunchTone)),
                    settings = settings,
                    pulse = pulse,
                    cellSize = dockCellSize,
                    modifier = Modifier.offset(
                        x = topGapTargetX,
                        y = lerpDp(dockPieceY, 0.dp, upperLaunch),
                    ),
                )

                sequencePhase < 0.66f -> HomeTitleAnimatedPiece(
                    cells = listOf(HomeTitleCell(tone = upperLaunchTone)),
                    settings = settings,
                    pulse = pulse,
                    cellSize = dockCellSize,
                    modifier = Modifier
                        .offset(x = topGapTargetX, y = 0.dp)
                        .graphicsLayer { alpha = 1f - upperExplode },
                )

                sequencePhase < 0.82f -> HomeTitleAnimatedPiece(
                    cells = topRow.filterNotNull(),
                    settings = settings,
                    pulse = pulse,
                    cellSize = dockCellSize,
                    modifier = Modifier.offset(
                        x = lerpDp(dockWordStartX, topTargetX, stackAlign),
                        y = lerpDp(dockPieceY, 0.dp, stackLaunch),
                    ),
                )

                sequencePhase < 0.96f -> HomeTitleAnimatedPiece(
                    cells = bottomRow.filterNotNull(),
                    settings = settings,
                    pulse = pulse,
                    cellSize = dockCellSize,
                    modifier = Modifier.offset(
                        x = lerpDp(dockWordStartX, bottomTargetX, shiftAlign),
                        y = lerpDp(dockPieceY, boardCellHeight, shiftLaunch),
                    ),
                )
            }
        }
    }
}

private data class HomeTitleCell(
    val letter: String? = null,
    val tone: CellTone,
)

@Composable
private fun rememberHomeTitleRow(
    word: String,
    startColumn: Int,
): List<HomeTitleCell?> {
    val tones = if (startColumn == 0) {
        listOf(CellTone.Cyan, CellTone.Gold, CellTone.Violet, CellTone.Emerald, CellTone.Coral)
    } else {
        listOf(CellTone.Blue, CellTone.Lime, CellTone.Amber, CellTone.Rose, CellTone.Cyan)
    }
    return List(HomeTitleBannerColumns) { index ->
        val letterIndex = index - startColumn
        if (letterIndex in word.indices) {
            HomeTitleCell(
                letter = word[letterIndex].toString(),
                tone = tones[letterIndex],
            )
        } else {
            null
        }
    }
}

private fun segmentProgress(
    phase: Float,
    start: Float,
    end: Float,
): Float = ((phase - start) / (end - start)).coerceIn(0f, 1f)

private fun clearFlashAlpha(progress: Float): Float = 1f - abs((progress * 2f) - 1f)

private fun titleTopRowAlpha(
    phase: Float,
    upperExplode: Float,
    stackLaunch: Float,
): Float = when {
    phase < 0.59f -> 1f
    phase < 0.66f -> 1f - upperExplode
    phase < 0.74f -> 0f
    phase < 0.82f -> stackLaunch
    else -> 1f
}

private fun titleBottomRowAlpha(
    phase: Float,
    lowerExplode: Float,
    shiftLaunch: Float,
): Float = when {
    phase < 0.25f -> 1f
    phase < 0.32f -> 1f - lowerExplode
    phase < 0.88f -> 0f
    phase < 0.96f -> shiftLaunch
    else -> 1f
}

private fun normalizedPhase(value: Float): Float {
    val remainder = value % 1f
    return if (remainder < 0f) remainder + 1f else remainder
}

private fun lerpDp(
    start: androidx.compose.ui.unit.Dp,
    end: androidx.compose.ui.unit.Dp,
    progress: Float,
): androidx.compose.ui.unit.Dp = start + ((end - start) * progress.coerceIn(0f, 1f))

@Composable
private fun HomeTitleAnimatedCell(
    letter: String?,
    tone: CellTone,
    settings: AppSettings,
    pulse: Float,
    alpha: Float,
    modifier: Modifier = Modifier,
    offsetX: androidx.compose.ui.unit.Dp = 0.dp,
    offsetY: androidx.compose.ui.unit.Dp = 0.dp,
) {
    if (alpha <= 0f) return
    val effectivePulse = rememberBlockStylePulse(
        style = settings.blockVisualStyle,
        pulse = pulse,
    )
    val letterTint = specialBlockIconTint(
        style = settings.blockVisualStyle,
        isDarkTheme = isStackShiftDarkTheme(settings),
        palette = settings.blockColorPalette,
    )

    BoxWithConstraints(
        modifier = modifier.offset(x = offsetX, y = offsetY),
        contentAlignment = Alignment.Center,
    ) {
        val cellSize = maxWidth
        val letterFontSize = (cellSize.value * 0.40f).coerceIn(16f, 34f).sp
        BlockCellPreview(
            tone = tone,
            palette = settings.blockColorPalette,
            style = settings.blockVisualStyle,
            size = cellSize,
            alpha = alpha,
            pulse = effectivePulse,
        )
        if (letter != null) {
            Text(
                text = letter,
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = letterFontSize),
                color = letterTint.copy(alpha = alpha),
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun HomeTitleEmptyCell(
    settings: AppSettings,
    modifier: Modifier = Modifier,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        val cellSize = maxWidth
        val corner =
            RoundedCornerShape(boardCellCornerRadiusDp(cellSize, settings.blockVisualStyle))
        val inset = boardCellInsetDp(cellSize) * 0.72f
        Box(
            modifier = Modifier
                .size(cellSize)
                .clip(corner)
                .background(uiColors.boardEmptyCellBorder.copy(alpha = 0.20f))
                .padding(inset),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(corner)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                uiColors.boardEmptyCell.copy(alpha = 0.40f),
                                uiColors.boardEmptyCell.copy(alpha = 0.22f),
                            ),
                        ),
                    ),
            )
        }
    }
}

@Composable
private fun HomeTitleMiniDock(
    modifier: Modifier = Modifier,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    Card(
        modifier = modifier.stackShiftSurfaceShadow(
            shape = RoundedCornerShape(GameUiShapeTokens.dockCorner),
            elevation = 8.dp,
        ),
        shape = RoundedCornerShape(GameUiShapeTokens.dockCorner),
        colors = CardDefaults.cardColors(containerColor = uiColors.gameSurface.copy(alpha = 0.90f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, uiColors.boardEmptyCellBorder.copy(alpha = 0.68f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            uiColors.launchGlow.copy(alpha = 0.16f),
                            Color.Transparent,
                        ),
                    ),
                )
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(GameUiShapeTokens.surfaceCorner))
                    .background(uiColors.panelMuted.copy(alpha = 0.48f))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                uiColors.panelHighlight.copy(alpha = 0.10f),
                                Color.Transparent,
                            ),
                        ),
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun HomeTitleAnimatedPiece(
    cells: List<HomeTitleCell>,
    settings: AppSettings,
    pulse: Float,
    cellSize: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    if (cells.isEmpty()) return
    Row(modifier = modifier) {
        cells.forEach { cell ->
            HomeTitleAnimatedCell(
                letter = cell.letter,
                tone = cell.tone,
                settings = settings,
                pulse = pulse,
                alpha = 1f,
                modifier = Modifier.size(cellSize),
            )
        }
    }
}

@Composable
private fun HomeTitleRowClearOverlay(
    alpha: Float,
    rowIndex: Int,
    cellHeight: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    if (alpha <= 0f) return
    val uiColors = StackShiftThemeTokens.uiColors
    Box(
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(cellHeight)
                .offset(y = cellHeight * rowIndex)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            uiColors.warning.copy(alpha = 0.12f * alpha),
                            Color.White.copy(alpha = 0.36f * alpha),
                            uiColors.launchGlow.copy(alpha = 0.20f * alpha),
                        ),
                    ),
                ),
        )
    }
}

@Composable
private fun HomeHighScoreCard(
    highestScore: Int,
    modifier: Modifier = Modifier,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    Surface(
        modifier = modifier.stackShiftSurfaceShadow(
            shape = RoundedCornerShape(GameUiShapeTokens.surfaceCorner),
            elevation = 5.dp,
        ),
        shape = RoundedCornerShape(GameUiShapeTokens.surfaceCorner),
        color = uiColors.panelMuted.copy(alpha = 0.96f),
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, uiColors.panelStroke.copy(alpha = 0.76f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(Res.string.high_score),
                style = MaterialTheme.typography.labelMedium,
                color = uiColors.subtitle,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = highestScore.toString(),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun HomePrimaryPlayButton(
    text: String,
    settings: AppSettings,
    pulse: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resolvedStyle = settings.blockVisualStyle
    val buttonShape = RoundedCornerShape(GameUiShapeTokens.buttonCorner)
    val effectivePulse = rememberBlockStylePulse(
        style = resolvedStyle,
        pulse = pulse,
    )
    val contentColor = blockStyleIconTint(style = resolvedStyle)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .stackShiftSurfaceShadow(
                shape = buttonShape,
                elevation = 12.dp,
            )
            .graphicsLayer {
                val scale = 1f + (pulse * 0.05f)
                scaleX = scale
                scaleY = scale
            }
            .clip(buttonShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val minDim = minOf(size.width, size.height)
            val cornerRadiusPx = boardCellCornerRadiusPx(minDim, resolvedStyle)
            drawCellBody(
                tone = CellTone.Cyan,
                palette = settings.blockColorPalette,
                style = resolvedStyle,
                topLeft = Offset.Zero,
                size = this.size,
                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                pulse = effectivePulse,
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(42.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.headlineSmall,
                color = contentColor,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun HomeQuickActionButton(
    text: String,
    icon: ImageVector,
    tone: CellTone,
    settings: AppSettings,
    pulse: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resolvedStyle = settings.blockVisualStyle
    val buttonShape = RoundedCornerShape(GameUiShapeTokens.buttonCorner)
    val effectivePulse = rememberBlockStylePulse(
        style = resolvedStyle,
        pulse = pulse,
    )
    val contentColor = blockStyleIconTint(style = resolvedStyle)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .stackShiftSurfaceShadow(
                shape = buttonShape,
                elevation = 5.dp,
            )
            .clip(buttonShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val minDim = minOf(size.width, size.height)
            val cornerRadiusPx = boardCellCornerRadiusPx(minDim, resolvedStyle)
            drawCellBody(
                tone = tone,
                palette = settings.blockColorPalette,
                style = resolvedStyle,
                topLeft = Offset.Zero,
                size = this.size,
                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                pulse = effectivePulse,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}


@Preview(name = "Light Mode", showBackground = true)
@Composable
fun HomeScreenLightPreview() {
    val settings = AppSettings(
        themeMode = AppThemeMode.Light,
        blockVisualStyle = BlockVisualStyle.DynamicLiquid,
        blockColorPalette = BlockColorPalette.Neon
    )
    StackShiftTheme(settings = settings) {
        HomeScreen(
            settings = settings,
            highestScore = 1250,
            telemetry = NoOpAppTelemetry,
            onPlay = {},
            onOpenInteractiveGuide = {},
            onOpenTutorial = {},
            onOpenTheme = {},
            onOpenLanguage = {},
        )
    }
}

@Preview(name = "Dark Mode", showBackground = true)
@Composable
fun HomeScreenDarkPreview() {
    val settings = AppSettings(
        themeMode = AppThemeMode.Dark,
        blockVisualStyle = BlockVisualStyle.Flat,
        blockColorPalette = BlockColorPalette.Classic
    )
    StackShiftTheme(settings = settings) {
        HomeScreen(
            settings = settings,
            highestScore = 1250,
            telemetry = NoOpAppTelemetry,
            onPlay = {},
            onOpenInteractiveGuide = {},
            onOpenTutorial = {},
            onOpenTheme = {},
            onOpenLanguage = {},
        )
    }
}
