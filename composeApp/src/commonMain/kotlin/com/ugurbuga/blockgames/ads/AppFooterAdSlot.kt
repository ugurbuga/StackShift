package com.ugurbuga.blockgames.ads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.SpecialBlockType
import com.ugurbuga.blockgames.localization.LocalAppSettings
import com.ugurbuga.blockgames.localization.appStringResource
import com.ugurbuga.blockgames.ui.game.BlockCellPreview
import com.ugurbuga.blockgames.ui.theme.BlockGamesThemeTokens
import blockgames.composeapp.generated.resources.Res
import blockgames.composeapp.generated.resources.app_title_stackshift

val AppFooterBannerHeight = 50.dp

@Composable
fun AppFooterAdSlot(
    adController: GameAdController,
    modifier: Modifier = Modifier,
) {
    if (adController.bannerPresentationMode != BannerPresentationMode.Inline) return

    var bannerLoaded by remember { mutableStateOf(false) }
    val uiColors = BlockGamesThemeTokens.uiColors

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .background(uiColors.panel.copy(alpha = 0.96f)),
        color = uiColors.panel.copy(alpha = 0.96f),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppFooterBannerHeight),
        ) {
            if (!bannerLoaded) {
                AppBrandedBannerPlaceholder(modifier = Modifier.matchParentSize())
            }
            adController.Banner(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = if (bannerLoaded) 1f else 0f },
                onLoadStateChanged = { bannerLoaded = it },
            )
        }
    }
}

@Composable
private fun AppBrandedBannerPlaceholder(modifier: Modifier = Modifier) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val settings = LocalAppSettings.current
    Surface(
        modifier = modifier,
        color = uiColors.panel.copy(alpha = 0.96f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            uiColors.panelHighlight.copy(alpha = 0.26f),
                            uiColors.launchGlow.copy(alpha = 0.14f),
                            uiColors.gameSurface.copy(alpha = 0.18f),
                        ),
                    ),
                )
                .padding(horizontal = 12.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.fillMaxHeight(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    listOf(
                        CellTone.Cyan to SpecialBlockType.ColumnClearer,
                        CellTone.Gold to SpecialBlockType.RowClearer,
                        CellTone.Violet to SpecialBlockType.Ghost,
                        CellTone.Coral to SpecialBlockType.Heavy,
                    ).forEachIndexed { index, (tone, special) ->
                        BlockCellPreview(
                            tone = tone,
                            palette = settings.blockColorPalette,
                            style = settings.blockVisualStyle,
                            size = if (index in 1..2) 17.dp else 18.dp,
                            special = special,
                        )
                    }
                }
                Text(
                    text = appStringResource(Res.string.app_title_stackshift),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

