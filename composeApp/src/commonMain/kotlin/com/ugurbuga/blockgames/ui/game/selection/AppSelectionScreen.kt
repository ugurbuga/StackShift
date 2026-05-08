package com.ugurbuga.blockgames.ui.game.selection

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Gamepad
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import blockgames.composeapp.generated.resources.Res
import blockgames.composeapp.generated.resources.app_title_blockwise
import blockgames.composeapp.generated.resources.app_title_boomblocks
import blockgames.composeapp.generated.resources.app_title_mergeshift
import blockgames.composeapp.generated.resources.app_title_stackshift
import blockgames.composeapp.generated.resources.selection_blockwise_desc
import blockgames.composeapp.generated.resources.selection_boomblocks_desc
import blockgames.composeapp.generated.resources.selection_mergeshift_desc
import blockgames.composeapp.generated.resources.selection_stackshift_desc
import blockgames.composeapp.generated.resources.selection_title
import com.ugurbuga.blockgames.BlockGamesTheme
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.paletteColor
import com.ugurbuga.blockgames.settings.AppSettings
import com.ugurbuga.blockgames.telemetry.AppTelemetry
import com.ugurbuga.blockgames.telemetry.LogScreen
import com.ugurbuga.blockgames.telemetry.NoOpAppTelemetry
import com.ugurbuga.blockgames.telemetry.TelemetryScreenNames
import com.ugurbuga.blockgames.ui.game.TopBarActionBlockButton
import com.ugurbuga.blockgames.ui.theme.BlockGamesThemeTokens
import com.ugurbuga.blockgames.ui.theme.GameUiShapeTokens
import com.ugurbuga.blockgames.ui.theme.blockGamesSurfaceShadow
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun AppSelectionScreen(
    onGameplayStyleSelected: (GameplayStyle) -> Unit,
    telemetry: AppTelemetry,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    LogScreen(telemetry, TelemetryScreenNames.Selection)

    val transition = rememberInfiniteTransition(label = "selectionStylePulse")
    val stylePulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "stylePulse",
    )

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
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TopBarActionBlockButton(
                        tone = CellTone.Cyan,
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.selection_title),
                        onClick = onBack,
                        size = 44.dp,
                        pulse = stylePulse,
                    )
                    Text(
                        text = stringResource(Res.string.selection_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.size(44.dp))
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SelectionItem(
                        title = Res.string.app_title_stackshift,
                        description = Res.string.selection_stackshift_desc,
                        tone = CellTone.Cyan,
                        onClick = { onGameplayStyleSelected(GameplayStyle.StackShift) }
                    )
                    SelectionItem(
                        title = Res.string.app_title_blockwise,
                        description = Res.string.selection_blockwise_desc,
                        tone = CellTone.Amber,
                        onClick = { onGameplayStyleSelected(GameplayStyle.BlockWise) }
                    )
                    SelectionItem(
                        title = Res.string.app_title_mergeshift,
                        description = Res.string.selection_mergeshift_desc,
                        tone = CellTone.Violet,
                        onClick = { onGameplayStyleSelected(GameplayStyle.MergeShift) }
                    )
                    SelectionItem(
                        title = Res.string.app_title_boomblocks,
                        description = Res.string.selection_boomblocks_desc,
                        tone = CellTone.Coral,
                        onClick = { onGameplayStyleSelected(GameplayStyle.BoomBlocks) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectionItem(
    title: StringResource,
    description: StringResource,
    tone: CellTone,
    onClick: () -> Unit,
) {
    val uiColors = BlockGamesThemeTokens.uiColors

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .blockGamesSurfaceShadow(
                shape = RoundedCornerShape(GameUiShapeTokens.panelCorner),
                elevation = 8.dp,
            )
            .clip(RoundedCornerShape(GameUiShapeTokens.panelCorner))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(GameUiShapeTokens.panelCorner),
        colors = CardDefaults.cardColors(containerColor = uiColors.gameSurface.copy(alpha = 0.90f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tone.paletteColor(com.ugurbuga.blockgames.game.model.BlockColorPalette.Classic).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Gamepad,
                    contentDescription = null,
                    tint = tone.paletteColor(com.ugurbuga.blockgames.game.model.BlockColorPalette.Classic),
                    modifier = Modifier.size(32.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = uiColors.subtitle,
                )
            }
        }
    }
}

@Preview
@Composable
fun AppSelectionScreenPreview() {
    BlockGamesTheme(settings = AppSettings()) {
        AppSelectionScreen(
            onGameplayStyleSelected = {},
            telemetry = NoOpAppTelemetry,
            onBack = {},
        )
    }
}
