package com.ugurbuga.blockgames.ui.game.home

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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import blockgames.composeapp.generated.resources.Res
import blockgames.composeapp.generated.resources.app_title_banner_blockwise_bottom
import blockgames.composeapp.generated.resources.app_title_banner_blockwise_top
import blockgames.composeapp.generated.resources.app_title_banner_mergeshift_bottom
import blockgames.composeapp.generated.resources.app_title_banner_mergeshift_top
import blockgames.composeapp.generated.resources.app_title_banner_stackshift_bottom
import blockgames.composeapp.generated.resources.app_title_banner_stackshift_top
import blockgames.composeapp.generated.resources.high_score
import blockgames.composeapp.generated.resources.home_classic_cta
import blockgames.composeapp.generated.resources.home_time_attack_cta
import blockgames.composeapp.generated.resources.settings_challenges
import blockgames.composeapp.generated.resources.settings_language
import blockgames.composeapp.generated.resources.settings_theme
import blockgames.composeapp.generated.resources.settings_tutorial
import com.ugurbuga.blockgames.BlockGamesTheme
import com.ugurbuga.blockgames.game.model.AppColorPalette
import com.ugurbuga.blockgames.game.model.AppThemeMode
import com.ugurbuga.blockgames.game.model.BlockVisualStyle
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.localization.appNameStringResource
import com.ugurbuga.blockgames.localization.appStringResource
import com.ugurbuga.blockgames.platform.GlobalPlatformConfig
import com.ugurbuga.blockgames.platform.NotificationManager
import com.ugurbuga.blockgames.platform.isDebugBuild
import com.ugurbuga.blockgames.platform.rememberNotificationManager
import com.ugurbuga.blockgames.settings.AppSettings
import com.ugurbuga.blockgames.telemetry.AppTelemetry
import com.ugurbuga.blockgames.telemetry.LogScreen
import com.ugurbuga.blockgames.telemetry.NoOpAppTelemetry
import com.ugurbuga.blockgames.telemetry.TelemetryScreenNames
import com.ugurbuga.blockgames.ui.game.BlockCellPreview
import com.ugurbuga.blockgames.ui.game.blockStyleIconTint
import com.ugurbuga.blockgames.ui.game.boardCellCornerRadiusDp
import com.ugurbuga.blockgames.ui.game.boardCellCornerRadiusPx
import com.ugurbuga.blockgames.ui.game.boardCellInsetDp
import com.ugurbuga.blockgames.ui.game.drawCellBody
import com.ugurbuga.blockgames.ui.game.rememberBlockStylePulse
import com.ugurbuga.blockgames.ui.game.specialBlockIconTint
import com.ugurbuga.blockgames.ui.theme.BlockGamesThemeTokens
import com.ugurbuga.blockgames.ui.theme.GameUiShapeTokens
import com.ugurbuga.blockgames.ui.theme.blockGamesSurfaceShadow
import com.ugurbuga.blockgames.ui.theme.isBlockGamesDarkTheme
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs

private const val HomeTitleBannerColumns = 6
private const val HomeTitleBannerRows = 2

@Composable
fun HomeScreen(
    settings: AppSettings,
    classicHighScore: Int,
    timeAttackHighScore: Int,
    telemetry: AppTelemetry,
    onPlay: () -> Unit,
    onPlayTimeAttack: () -> Unit,
    onOpenTutorial: () -> Unit,
    onOpenTheme: () -> Unit,
    onOpenLanguage: () -> Unit,
    onOpenChallenges: () -> Unit,
    notificationManager: NotificationManager,
    modifier: Modifier = Modifier,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
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

    notificationManager.RequestPermission()

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
                    text = appNameStringResource(),
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

            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.78f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        HomeQuickActionButton(
                            text = appStringResource(Res.string.home_classic_cta),
                            icon = Icons.Filled.PlayArrow,
                            tone = CellTone.Cyan,
                            settings = settings,
                            pulse = stylePulse,
                            modifier = Modifier.weight(1f),
                            onClick = onPlay,
                            iconSize = 44.dp,
                            textStyle = MaterialTheme.typography.labelLarge,
                        )

                        HomeQuickActionButton(
                            text = appStringResource(Res.string.home_time_attack_cta),
                            icon = Icons.Filled.Timer,
                            tone = CellTone.Amber,
                            settings = settings,
                            pulse = stylePulse,
                            modifier = Modifier.weight(1f),
                            onClick = onPlayTimeAttack,
                            iconSize = 44.dp,
                            textStyle = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                HomeHighScoreCard(
                    classicHighScore = classicHighScore,
                    timeAttackHighScore = timeAttackHighScore,
                    onClick = {
                        if (isDebugBuild()) {
                            notificationManager.sendTestNotification()
                        }
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    HomeQuickActionButton(
                        text = appStringResource(Res.string.settings_challenges),
                        icon = Icons.Default.EmojiEvents,
                        tone = CellTone.Emerald,
                        settings = settings,
                        pulse = stylePulse,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenChallenges,
                    )
                    HomeQuickActionButton(
                        text = appStringResource(Res.string.settings_tutorial),
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        tone = CellTone.Gold,
                        settings = settings,
                        pulse = stylePulse,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenTutorial,
                    )
                    HomeQuickActionButton(
                        text = appStringResource(Res.string.settings_theme),
                        icon = Icons.Filled.Palette,
                        tone = CellTone.Violet,
                        settings = settings,
                        pulse = stylePulse,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenTheme,
                    )
                    HomeQuickActionButton(
                        text = appStringResource(Res.string.settings_language),
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
    val gameplayStyle = GlobalPlatformConfig.gameplayStyle
    val uiColors = BlockGamesThemeTokens.uiColors
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
    val isBlockWiseBanner = gameplayStyle == GameplayStyle.BlockWise

    // Lower piece cycle
    val lowerDrift = segmentProgress(sequencePhase, 0.00f, 0.10f)
    val lowerAlign = segmentProgress(sequencePhase, 0.10f, 0.15f)
    val lowerLaunch = segmentProgress(sequencePhase, 0.15f, 0.20f)
    val lowerExplode = segmentProgress(
        sequencePhase,
        0.25f,
        0.32f
    ) // Landed at 0.20, wait until 0.25, then explode

    // Upper piece cycle
    val upperDrift = segmentProgress(sequencePhase, 0.34f, 0.44f)
    val upperAlign = segmentProgress(sequencePhase, 0.44f, 0.49f)
    val upperLaunch = segmentProgress(sequencePhase, 0.49f, 0.54f)
    val upperExplode = segmentProgress(
        sequencePhase,
        0.59f,
        0.66f
    ) // Landed at 0.54, wait until 0.59, then explode

    // STACK launch
    val stackAlign = segmentProgress(sequencePhase, 0.68f, 0.74f)
    val stackLaunch = segmentProgress(sequencePhase, 0.74f, 0.82f)
    val blockWordTravel = segmentProgress(sequencePhase, 0.68f, 0.82f)

    // SHIFT launch
    val shiftAlign = segmentProgress(sequencePhase, 0.82f, 0.88f)
    val shiftLaunch = segmentProgress(sequencePhase, 0.88f, 0.96f)
    val wiseWordTravel = segmentProgress(sequencePhase, 0.82f, 0.96f)

    val topRow = rememberHomeTitleRow(word = homeTitleBannerTopWord(), startColumn = 0)
    val bottomRow = rememberHomeTitleRow(
        word = homeTitleBannerBottomWord(),
        startColumn = if (isBlockWiseBanner) 2 else 1,
    )
    val animatedTopWordCells = if (isBlockWiseBanner) topRow else topRow.filterNotNull()
    val animatedBottomWordCells = if (isBlockWiseBanner) bottomRow else bottomRow.filterNotNull()
    val blockWiseBottomGapCells = remember {
        listOf(
            HomeTitleCell(tone = CellTone.Blue),
            HomeTitleCell(tone = CellTone.Lime),
        )
    }
    val lowerLaunchTone = CellTone.Gold
    val upperLaunchTone = CellTone.Violet
    Card(
        modifier = modifier
            .fillMaxWidth()
            .blockGamesSurfaceShadow(
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
            val dockDoubleStartX = (maxWidth - (dockCellSize * 2f)) / 2f
            val dockSingleLeftX = maxWidth * 0.12f
            val dockSingleRightX = maxWidth - dockCellSize - (maxWidth * 0.12f)
            val dockWordStartX = (maxWidth - (dockCellSize * 5f)) / 2f
            val dockBlockwiseBottomWordStartX =
                (maxWidth - (dockCellSize * HomeTitleBannerColumns)) / 2f
            val dockPieceY = dockTop + ((dockHeight - dockCellSize) / 2f)
            val topTargetX = 0.dp
            val topGapTargetX = cellWidth * 5f
            val bottomTargetX = if (isBlockWiseBanner) 0.dp else cellWidth
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
                            topRow.forEach { cell ->
                                if (cell == null) {
                                    HomeTitleEmptyCell(
                                        settings = settings,
                                        pulse = pulse,
                                        modifier = Modifier.weight(1f).fillMaxHeight(),
                                    )
                                } else {
                                    HomeTitleAnimatedCell(
                                        letter = cell.letter,
                                        tone = cell.tone,
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
                            bottomRow.forEach { cell ->
                                if (cell == null) {
                                    HomeTitleEmptyCell(
                                        settings = settings,
                                        pulse = pulse,
                                        modifier = Modifier.weight(1f).fillMaxHeight(),
                                    )
                                } else {
                                    HomeTitleAnimatedCell(
                                        letter = cell.letter,
                                        tone = cell.tone,
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
                isBlockWiseBanner && sequencePhase < 0.20f -> HomeTitleAnimatedPiece(
                    cells = blockWiseBottomGapCells,
                    settings = settings,
                    pulse = pulse,
                    cellSize = dockCellSize,
                    modifier = Modifier.offset(
                        x = lerpDp(
                            dockDoubleStartX,
                            bottomGapTargetX,
                            segmentProgress(sequencePhase, 0.00f, 0.20f),
                        ),
                        y = lerpDp(
                            dockPieceY,
                            boardCellHeight,
                            segmentProgress(sequencePhase, 0.00f, 0.20f),
                        ),
                    ),
                )

                !isBlockWiseBanner && sequencePhase < 0.15f -> HomeTitleAnimatedPiece(
                    cells = listOf(HomeTitleCell(tone = lowerLaunchTone)),
                    settings = settings,
                    pulse = pulse,
                    cellSize = dockCellSize,
                    modifier = Modifier.offset(
                        x = lerpDp(dockSingleStartX, dockSingleLeftX, lowerDrift).let { x ->
                            if (sequencePhase > 0.10f) lerpDp(
                                x,
                                bottomGapTargetX,
                                lowerAlign
                            ) else x
                        },
                        y = dockPieceY,
                    ),
                )

                sequencePhase < 0.25f -> HomeTitleAnimatedPiece(
                    cells = if (isBlockWiseBanner) blockWiseBottomGapCells else listOf(
                        HomeTitleCell(
                            tone = lowerLaunchTone
                        )
                    ),
                    settings = settings,
                    pulse = pulse,
                    cellSize = dockCellSize,
                    modifier = Modifier.offset(
                        x = bottomGapTargetX,
                        y = if (isBlockWiseBanner) {
                            boardCellHeight
                        } else {
                            lerpDp(dockPieceY, boardCellHeight, lowerLaunch)
                        },
                    ),
                )

                sequencePhase < 0.32f -> HomeTitleAnimatedPiece(
                    cells = if (isBlockWiseBanner) blockWiseBottomGapCells else listOf(
                        HomeTitleCell(
                            tone = lowerLaunchTone
                        )
                    ),
                    settings = settings,
                    pulse = pulse,
                    cellSize = dockCellSize,
                    modifier = Modifier
                        .offset(x = bottomGapTargetX, y = boardCellHeight)
                        .graphicsLayer { alpha = 1f - lowerExplode },
                )

                isBlockWiseBanner && sequencePhase < 0.54f -> HomeTitleAnimatedPiece(
                    cells = listOf(HomeTitleCell(tone = upperLaunchTone)),
                    settings = settings,
                    pulse = pulse,
                    cellSize = dockCellSize,
                    modifier = Modifier.offset(
                        x = lerpDp(
                            dockSingleStartX,
                            topGapTargetX,
                            segmentProgress(sequencePhase, 0.34f, 0.54f),
                        ),
                        y = lerpDp(
                            dockPieceY,
                            0.dp,
                            segmentProgress(sequencePhase, 0.34f, 0.54f),
                        ),
                    ),
                )

                !isBlockWiseBanner && sequencePhase < 0.49f -> HomeTitleAnimatedPiece(
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
                        y = if (isBlockWiseBanner) {
                            0.dp
                        } else {
                            lerpDp(dockPieceY, 0.dp, upperLaunch)
                        },
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
                    cells = if (isBlockWiseBanner) animatedTopWordCells else topRow.filterNotNull(),
                    settings = settings,
                    pulse = pulse,
                    cellSize = dockCellSize,
                    modifier = Modifier.offset(
                        x = lerpDp(
                            dockWordStartX,
                            topTargetX,
                            if (isBlockWiseBanner) blockWordTravel else stackAlign,
                        ),
                        y = lerpDp(
                            dockPieceY,
                            0.dp,
                            if (isBlockWiseBanner) blockWordTravel else stackLaunch,
                        ),
                    ),
                )

                sequencePhase < 0.96f -> HomeTitleAnimatedPiece(
                    cells = if (isBlockWiseBanner) animatedBottomWordCells else bottomRow.filterNotNull(),
                    settings = settings,
                    pulse = pulse,
                    cellSize = dockCellSize,
                    modifier = Modifier.offset(
                        x = lerpDp(
                            if (isBlockWiseBanner) dockBlockwiseBottomWordStartX else dockWordStartX,
                            bottomTargetX,
                            if (isBlockWiseBanner) wiseWordTravel else shiftAlign,
                        ),
                        y = lerpDp(
                            dockPieceY,
                            boardCellHeight,
                            if (isBlockWiseBanner) wiseWordTravel else shiftLaunch,
                        ),
                    ),
                )
            }

            val handAlpha = when {
                isBlockWiseBanner && sequencePhase < 0.20f -> 1f
                isBlockWiseBanner && sequencePhase >= 0.34f && sequencePhase < 0.54f -> 1f
                isBlockWiseBanner && sequencePhase >= 0.68f && sequencePhase < 0.96f -> 1f
                !isBlockWiseBanner && sequencePhase < 0.15f -> 1f
                !isBlockWiseBanner && sequencePhase in 0.34f..0.49f -> 1f
                sequencePhase in 0.68f..0.74f -> 1f
                sequencePhase in 0.82f..0.88f -> 1f
                else -> 0f
            }

            if (handAlpha > 0f) {
                val isDark = isBlockGamesDarkTheme(settings)
                val handColor = if (isDark) Color.White else Color.Black.copy(alpha = 0.85f)
                val handX = when {
                    isBlockWiseBanner && sequencePhase < 0.20f -> lerpDp(
                        dockDoubleStartX,
                        bottomGapTargetX,
                        segmentProgress(sequencePhase, 0.00f, 0.20f),
                    ) + homeTitleOccupiedCenterOffset(blockWiseBottomGapCells, dockCellSize)

                    isBlockWiseBanner && sequencePhase < 0.54f -> lerpDp(
                        dockSingleStartX,
                        topGapTargetX,
                        segmentProgress(sequencePhase, 0.34f, 0.54f),
                    )

                    isBlockWiseBanner && sequencePhase < 0.82f -> lerpDp(
                        dockWordStartX,
                        topTargetX,
                        blockWordTravel,
                    ) + homeTitleOccupiedCenterOffset(animatedTopWordCells, dockCellSize)

                    isBlockWiseBanner && sequencePhase < 0.96f -> lerpDp(
                        dockBlockwiseBottomWordStartX,
                        bottomTargetX,
                        wiseWordTravel,
                    ) + homeTitleOccupiedCenterOffset(animatedBottomWordCells, dockCellSize)

                    sequencePhase < 0.15f -> lerpDp(
                        dockSingleStartX,
                        dockSingleLeftX,
                        lowerDrift
                    ).let { x ->
                        if (sequencePhase > 0.10f) lerpDp(x, bottomGapTargetX, lowerAlign) else x
                    }

                    sequencePhase < 0.49f -> lerpDp(
                        dockSingleStartX,
                        dockSingleRightX,
                        upperDrift
                    ).let { x ->
                        if (sequencePhase > 0.44f) lerpDp(x, topGapTargetX, upperAlign) else x
                    }
                    // For 5-block pieces, offset by 2 cells to center under the 3rd block
                    sequencePhase < 0.74f -> lerpDp(
                        dockWordStartX,
                        topTargetX,
                        stackAlign
                    ) + (dockCellSize * 2f)

                    else -> lerpDp(dockWordStartX, bottomTargetX, shiftAlign) + (dockCellSize * 2f)
                }
                val handY = when {
                    isBlockWiseBanner && sequencePhase < 0.20f -> lerpDp(
                        dockPieceY,
                        boardCellHeight,
                        segmentProgress(sequencePhase, 0.00f, 0.20f),
                    )

                    isBlockWiseBanner && sequencePhase < 0.54f -> lerpDp(
                        dockPieceY,
                        0.dp,
                        segmentProgress(sequencePhase, 0.34f, 0.54f),
                    )

                    isBlockWiseBanner && sequencePhase < 0.82f -> lerpDp(
                        dockPieceY,
                        0.dp,
                        blockWordTravel,
                    )

                    isBlockWiseBanner && sequencePhase < 0.96f -> lerpDp(
                        dockPieceY,
                        boardCellHeight,
                        wiseWordTravel,
                    )

                    else -> dockPieceY
                }

                HomeTitleDemoHand(
                    x = handX,
                    y = handY,
                    size = dockCellSize,
                    alpha = handAlpha,
                    color = handColor,
                )
            }
        }
    }
}

@Composable
private fun homeTitleBannerTopWord(): String {
    val gameplayStyle = GlobalPlatformConfig.gameplayStyle
    return stringResource(
        when (gameplayStyle) {
            GameplayStyle.StackShift -> Res.string.app_title_banner_stackshift_top
            GameplayStyle.BlockWise -> Res.string.app_title_banner_blockwise_top
            GameplayStyle.MergeShift -> Res.string.app_title_banner_mergeshift_top
            else -> Res.string.app_title_banner_stackshift_top
        }
    )
}

@Composable
private fun homeTitleBannerBottomWord(): String {
    val gameplayStyle = GlobalPlatformConfig.gameplayStyle
    return stringResource(
        when (gameplayStyle) {
            GameplayStyle.StackShift -> Res.string.app_title_banner_stackshift_bottom
            GameplayStyle.BlockWise -> Res.string.app_title_banner_blockwise_bottom
            GameplayStyle.MergeShift -> Res.string.app_title_banner_mergeshift_bottom
            else -> Res.string.app_title_banner_stackshift_bottom
        }
    )
}

@Composable
private fun HomeTitleDemoHand(
    x: Dp,
    y: Dp,
    size: Dp,
    alpha: Float,
    color: Color,
) {
    Icon(
        imageVector = Icons.Default.TouchApp,
        contentDescription = null,
        tint = color.copy(alpha = 0.72f * alpha),
        modifier = Modifier
            .offset(x = x + (size * 0.4f), y = y + (size * 0.6f))
            .size(size * 0.8f)
            .graphicsLayer {
                rotationZ = -15f
            },
    )
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
    start: Dp,
    end: Dp,
    progress: Float,
): Dp = start + ((end - start) * progress.coerceIn(0f, 1f))

@Composable
private fun HomeTitleAnimatedCell(
    letter: String?,
    tone: CellTone,
    settings: AppSettings,
    pulse: Float,
    alpha: Float,
    modifier: Modifier = Modifier,
    offsetX: Dp = 0.dp,
    offsetY: Dp = 0.dp,
) {
    if (alpha <= 0f) return
    val effectivePulse = rememberBlockStylePulse(
        style = settings.blockVisualStyle,
        pulse = pulse,
    )
    val letterTint = specialBlockIconTint(
        style = settings.blockVisualStyle,
        isDarkTheme = isBlockGamesDarkTheme(settings),
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
    pulse: Float = 0f,
    modifier: Modifier = Modifier,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        val cellSize = maxWidth
        val corner =
            RoundedCornerShape(boardCellCornerRadiusDp(cellSize, settings.blockVisualStyle))
        val inset = boardCellInsetDp(cellSize) * 0.72f
        val pulseAlpha = if (settings.blockVisualStyle == BlockVisualStyle.DynamicLiquid) {
            0.10f + (0.15f * pulse)
        } else {
            0.20f
        }
        Box(
            modifier = Modifier
                .size(cellSize)
                .clip(corner)
                .background(uiColors.boardEmptyCellBorder.copy(alpha = pulseAlpha))
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
    val uiColors = BlockGamesThemeTokens.uiColors
    Card(
        modifier = modifier.blockGamesSurfaceShadow(
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
    cells: List<HomeTitleCell?>,
    settings: AppSettings,
    pulse: Float,
    cellSize: Dp,
    modifier: Modifier = Modifier,
) {
    if (cells.isEmpty()) return
    Row(modifier = modifier) {
        cells.forEach { cell ->
            if (cell == null) {
                Spacer(modifier = Modifier.size(cellSize))
            } else {
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
}

private fun homeTitleOccupiedCenterOffset(
    cells: List<HomeTitleCell?>,
    cellSize: Dp,
): Dp {
    val occupiedIndices = cells.mapIndexedNotNull { index, cell -> index.takeIf { cell != null } }
    if (occupiedIndices.isEmpty()) return 0.dp
    val centerIndex = occupiedIndices.average().toFloat()
    return cellSize * centerIndex
}

@Composable
private fun HomeTitleRowClearOverlay(
    alpha: Float,
    rowIndex: Int,
    cellHeight: Dp,
    modifier: Modifier = Modifier,
) {
    if (alpha <= 0f) return
    val uiColors = BlockGamesThemeTokens.uiColors
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
    classicHighScore: Int,
    timeAttackHighScore: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    Surface(
        modifier = modifier
            .blockGamesSurfaceShadow(
                shape = RoundedCornerShape(GameUiShapeTokens.surfaceCorner),
                elevation = 5.dp,
            )
            .clip(RoundedCornerShape(GameUiShapeTokens.surfaceCorner))
            .clickable { onClick() },
        shape = RoundedCornerShape(GameUiShapeTokens.surfaceCorner),
        color = uiColors.panelMuted.copy(alpha = 0.96f),
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, uiColors.panelStroke.copy(alpha = 0.76f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = appStringResource(Res.string.high_score),
                style = MaterialTheme.typography.labelMedium,
                color = uiColors.subtitle,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HomeHighScoreMetric(
                    title = appStringResource(Res.string.home_classic_cta),
                    value = classicHighScore.toString(),
                )
                Box(
                    modifier = Modifier
                        .size(width = 1.dp, height = 32.dp)
                        .height(32.dp)
                        .background(uiColors.panelStroke.copy(alpha = 0.48f)),
                )
                HomeHighScoreMetric(
                    title = appStringResource(Res.string.home_time_attack_cta),
                    value = timeAttackHighScore.toString(),
                )
            }
        }
    }
}

@Composable
private fun HomeHighScoreMetric(
    title: String,
    value: String,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = uiColors.subtitle,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
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
    iconSize: Dp = 24.dp,
    textStyle: TextStyle = MaterialTheme.typography.labelMedium,
) {
    val resolvedStyle = settings.blockVisualStyle
    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier.aspectRatio(1f)) {
        val cellSize = maxWidth
        val cellInset = boardCellInsetDp(cellSize)
        val buttonShape = RoundedCornerShape(boardCellCornerRadiusDp(cellSize, resolvedStyle))
        val effectivePulse = rememberBlockStylePulse(
            style = resolvedStyle,
            pulse = pulse,
        )
        val contentColor = blockStyleIconTint(style = resolvedStyle)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(cellInset),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blockGamesSurfaceShadow(
                        shape = buttonShape,
                        elevation = 0.dp,
                    )
                    .clip(buttonShape)
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cornerRadiusPx = boardCellCornerRadiusPx(
                        with(density) { cellSize.toPx() },
                        resolvedStyle
                    )
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

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BlockGamesThemeTokens.uiColors.gameSurface.copy(alpha = 0.35f))
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(iconSize),
                    )
                    Text(
                        text = text,
                        style = textStyle,
                        color = contentColor,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        minLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}


@Preview(name = "Light Mode", showBackground = true)
@Composable
fun HomeScreenLightPreview() {
    val settings = AppSettings(
        themeMode = AppThemeMode.Light,
        blockVisualStyle = BlockVisualStyle.Prism,
        themeColorPalette = AppColorPalette.ModernNeon
    )
    BlockGamesTheme(settings = settings) {
        HomeScreen(
            settings = settings,
            classicHighScore = 1250,
            timeAttackHighScore = 860,
            telemetry = NoOpAppTelemetry,
            onPlay = {},
            onPlayTimeAttack = {},
            onOpenTutorial = {},
            onOpenTheme = {},
            onOpenLanguage = {},
            onOpenChallenges = {},
            notificationManager = rememberNotificationManager(),
        )
    }
}

@Preview(name = "Dark Mode", showBackground = true)
@Composable
fun HomeScreenDarkPreview() {
    val settings = AppSettings(
        themeMode = AppThemeMode.Dark,
        blockVisualStyle = BlockVisualStyle.Flat,
        themeColorPalette = AppColorPalette.Classic
    )
    BlockGamesTheme(settings = settings) {
        HomeScreen(
            settings = settings,
            classicHighScore = 1250,
            timeAttackHighScore = 860,
            telemetry = NoOpAppTelemetry,
            onPlay = {},
            onPlayTimeAttack = {},
            onOpenTutorial = {},
            onOpenTheme = {},
            onOpenLanguage = {},
            onOpenChallenges = {},
            notificationManager = rememberNotificationManager(),
        )
    }
}
