package com.ugurbuga.stackshift.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ugurbuga.stackshift.game.model.BlockVisualStyle
import com.ugurbuga.stackshift.game.model.normalizeBlockVisualStyle
import com.ugurbuga.stackshift.localization.LocalAppSettings

@Immutable
data class GameUiShapeSpec(
    val panelCorner: Dp,
    val surfaceCorner: Dp,
    val chipCorner: Dp,
    val buttonCorner: Dp,
    val dockCorner: Dp,
    val previewSlotCorner: Dp,
    val hintCorner: Dp,
    val badgeCorner: Dp,
    val targetCorner: Dp,
)

object GameUiShapeTokens {
    val panelCorner: Dp
        @Composable get() = rememberGameUiShapeSpec().panelCorner
    val surfaceCorner: Dp
        @Composable get() = rememberGameUiShapeSpec().surfaceCorner
    val chipCorner: Dp
        @Composable get() = rememberGameUiShapeSpec().chipCorner
    val buttonCorner: Dp
        @Composable get() = rememberGameUiShapeSpec().buttonCorner
    val dockCorner: Dp
        @Composable get() = rememberGameUiShapeSpec().dockCorner
    val previewSlotCorner: Dp
        @Composable get() = rememberGameUiShapeSpec().previewSlotCorner
    val hintCorner: Dp
        @Composable get() = rememberGameUiShapeSpec().hintCorner
    val badgeCorner: Dp
        @Composable get() = rememberGameUiShapeSpec().badgeCorner
    val targetCorner: Dp
        @Composable get() = rememberGameUiShapeSpec().targetCorner
}

@Composable
fun rememberGameUiShapeSpec(): GameUiShapeSpec {
    val settings = LocalAppSettings.current
    val resolvedStyle = normalizeBlockVisualStyle(settings.blockVisualStyle)
    return remember(resolvedStyle) {
        gameUiShapeSpec(resolvedStyle)
    }
}

fun gameUiShapeSpec(style: BlockVisualStyle): GameUiShapeSpec {
    val base = styleFrameCorner(style)
    return GameUiShapeSpec(
        panelCorner = base,
        surfaceCorner = base,
        chipCorner = base,
        buttonCorner = base,
        dockCorner = base,
        previewSlotCorner = base,
        hintCorner = base,
        badgeCorner = base,
        targetCorner = base,
    )
}

private fun styleFrameCorner(style: BlockVisualStyle): Dp = when (style) {
    BlockVisualStyle.Flat -> 18.dp
    BlockVisualStyle.Bubble -> 22.dp
    BlockVisualStyle.Outline -> 14.dp
    BlockVisualStyle.Sharp3D -> 6.dp
    BlockVisualStyle.Wood -> 12.dp
    BlockVisualStyle.GridSplit -> 4.dp
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

