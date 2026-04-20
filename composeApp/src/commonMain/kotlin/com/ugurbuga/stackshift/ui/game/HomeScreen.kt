package com.ugurbuga.stackshift.ui.game

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ugurbuga.stackshift.game.model.CellTone
import com.ugurbuga.stackshift.game.model.SpecialBlockType
import com.ugurbuga.stackshift.settings.AppSettings
import com.ugurbuga.stackshift.telemetry.AppTelemetry
import com.ugurbuga.stackshift.telemetry.LogScreen
import com.ugurbuga.stackshift.telemetry.NoOpAppTelemetry
import com.ugurbuga.stackshift.telemetry.TelemetryScreenNames
import com.ugurbuga.stackshift.ui.theme.GameUiShapeTokens
import com.ugurbuga.stackshift.ui.theme.StackShiftThemeTokens
import com.ugurbuga.stackshift.ui.theme.stackShiftSurfaceShadow
import org.jetbrains.compose.resources.stringResource
import stackshift.composeapp.generated.resources.Res
import stackshift.composeapp.generated.resources.high_score
import stackshift.composeapp.generated.resources.settings_language
import stackshift.composeapp.generated.resources.settings_theme
import stackshift.composeapp.generated.resources.settings_tutorial
import stackshift.composeapp.generated.resources.settings_tutorial_body
import stackshift.composeapp.generated.resources.settings_tutorial_replay
import stackshift.composeapp.generated.resources.tutorial_finish

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
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
        ) {
            var maxActionButtonHeightPx by remember(settings.language, maxWidth) { mutableIntStateOf(0) }
            val uniformActionButtonHeight = with(LocalDensity.current) {
                maxActionButtonHeightPx.toDp()
            }
            val reportActionButtonHeight: (Int) -> Unit =
                { measuredHeight ->
                    if (measuredHeight > maxActionButtonHeightPx) {
                        maxActionButtonHeightPx = measuredHeight
                    }
                }

            Column(
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Card(
                    modifier = Modifier.stackShiftSurfaceShadow(
                        shape = RoundedCornerShape(GameUiShapeTokens.panelCorner),
                        elevation = 10.dp,
                    ),
                    shape = RoundedCornerShape(GameUiShapeTokens.panelCorner),
                    colors = CardDefaults.cardColors(containerColor = uiColors.gameSurface.copy(alpha = 0.94f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    border = BorderStroke(1.dp, uiColors.panelStroke.copy(alpha = 0.82f)),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        uiColors.panelHighlight.copy(alpha = 0.20f),
                                        uiColors.launchGlow.copy(alpha = 0.10f),
                                        uiColors.gameSurface.copy(alpha = 0.02f),
                                    ),
                                ),
                            )
                            .padding(horizontal = 18.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = "StackShift",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.ExtraBold,
                                )
                                Text(
                                    text = stringResource(Res.string.settings_tutorial_body),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Surface(
                                modifier = Modifier.stackShiftSurfaceShadow(
                                    shape = RoundedCornerShape(GameUiShapeTokens.surfaceCorner),
                                    elevation = 5.dp,
                                ),
                                shape = RoundedCornerShape(GameUiShapeTokens.surfaceCorner),
                                color = uiColors.panelMuted.copy(alpha = 0.96f),
                                shadowElevation = 5.dp,
                                border = BorderStroke(1.dp, uiColors.panelStroke.copy(alpha = 0.76f)),
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
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
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                listOf(
                                    CellTone.Cyan,
                                    CellTone.Gold,
                                    CellTone.Violet,
                                    CellTone.Emerald
                                ).forEachIndexed { index, tone ->
                                    BlockCellPreview(
                                        tone = tone,
                                        palette = settings.blockColorPalette,
                                        style = settings.blockVisualStyle,
                                        size = 18.dp,
                                        special = if (index == 2) SpecialBlockType.Ghost else SpecialBlockType.None,
                                        pulse = stylePulse,
                                    )
                                }
                            }
                        }
                    }
                }

                HomeActionButton(
                    text = stringResource(Res.string.tutorial_finish),
                    icon = Icons.Filled.PlayArrow,
                    tone = CellTone.Cyan,
                    settings = settings,
                    pulse = stylePulse,
                    minHeight = uniformActionButtonHeight,
                    onMeasured = reportActionButtonHeight,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onPlay,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HomeActionButton(
                        text = stringResource(Res.string.settings_tutorial_replay),
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        tone = CellTone.Emerald,
                        settings = settings,
                        pulse = stylePulse,
                        minHeight = uniformActionButtonHeight,
                        onMeasured = reportActionButtonHeight,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenInteractiveGuide,
                    )
                    HomeActionButton(
                        text = stringResource(Res.string.settings_tutorial),
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        tone = CellTone.Gold,
                        settings = settings,
                        pulse = stylePulse,
                        minHeight = uniformActionButtonHeight,
                        onMeasured = reportActionButtonHeight,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenTutorial,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HomeActionButton(
                        text = stringResource(Res.string.settings_theme),
                        icon = Icons.Filled.Palette,
                        tone = CellTone.Violet,
                        settings = settings,
                        pulse = stylePulse,
                        minHeight = uniformActionButtonHeight,
                        onMeasured = reportActionButtonHeight,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenTheme,
                    )
                    HomeActionButton(
                        text = stringResource(Res.string.settings_language),
                        icon = Icons.Filled.Translate,
                        tone = CellTone.Coral,
                        settings = settings,
                        pulse = stylePulse,
                        minHeight = uniformActionButtonHeight,
                        onMeasured = reportActionButtonHeight,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenLanguage,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeActionButton(
    text: String,
    icon: ImageVector,
    tone: CellTone,
    settings: AppSettings,
    pulse: Float,
    minHeight: Dp,
    onMeasured: (Int) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    val buttonShape = RoundedCornerShape(GameUiShapeTokens.buttonCorner)
    val effectivePulse = rememberBlockStylePulse(
        style = settings.blockVisualStyle,
        pulse = pulse,
    )
    val iconTint = blockStyleIconTint(
        style = settings.blockVisualStyle,
    )
    Card(
        modifier = modifier
            .heightIn(min = minHeight)
            .onSizeChanged { onMeasured(it.height) }
            .stackShiftSurfaceShadow(
                shape = buttonShape,
                elevation = 8.dp,
            )
            .clip(buttonShape)
            .clickable(onClick = onClick),
        shape = buttonShape,
        colors = CardDefaults.cardColors(containerColor = uiColors.gameSurface.copy(alpha = 0.94f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
        border = BorderStroke(1.dp, uiColors.panelStroke.copy(alpha = 0.76f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(buttonShape)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            uiColors.panelHighlight.copy(alpha = 0.18f),
                            Color.Transparent,
                        ),
                    ),
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                BlockCellPreview(
                    tone = tone,
                    palette = settings.blockColorPalette,
                    style = settings.blockVisualStyle,
                    size = 32.dp,
                    pulse = effectivePulse,
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}


@Preview
@Composable
fun HomeScreenPreview() {
    HomeScreen(
        settings = AppSettings(),
        highestScore = 1,
        telemetry = NoOpAppTelemetry,
        onPlay = {},
        onOpenInteractiveGuide = {},
        onOpenTutorial = {},
        onOpenTheme = {},
        onOpenLanguage = {},
    )
}
