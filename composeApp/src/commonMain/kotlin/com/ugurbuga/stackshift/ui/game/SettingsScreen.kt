package com.ugurbuga.stackshift.ui.game

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ugurbuga.stackshift.ads.GameAdController
import com.ugurbuga.stackshift.game.model.AppColorPalette
import com.ugurbuga.stackshift.game.model.AppLanguage
import com.ugurbuga.stackshift.game.model.AppThemeMode
import com.ugurbuga.stackshift.game.model.BlockColorPalette
import com.ugurbuga.stackshift.game.model.BlockVisualStyle
import com.ugurbuga.stackshift.game.model.CellTone
import com.ugurbuga.stackshift.game.model.paletteColor
import com.ugurbuga.stackshift.settings.AppSettings
import com.ugurbuga.stackshift.settings.DailyChallengeTokenReward
import com.ugurbuga.stackshift.settings.RewardedTokenAdReward
import com.ugurbuga.stackshift.settings.ScorePointsPerToken
import com.ugurbuga.stackshift.settings.isBlockStyleUnlocked
import com.ugurbuga.stackshift.settings.isThemePaletteUnlocked
import com.ugurbuga.stackshift.settings.selectBlockStyle
import com.ugurbuga.stackshift.settings.selectThemeMode
import com.ugurbuga.stackshift.settings.selectThemePalette
import com.ugurbuga.stackshift.settings.tokenCost
import com.ugurbuga.stackshift.settings.unlockBlockStyle
import com.ugurbuga.stackshift.settings.unlockThemePalette
import com.ugurbuga.stackshift.telemetry.AppTelemetry
import com.ugurbuga.stackshift.telemetry.LogScreen
import com.ugurbuga.stackshift.telemetry.NoOpAppTelemetry
import com.ugurbuga.stackshift.telemetry.TelemetryScreenNames
import com.ugurbuga.stackshift.ui.theme.BlockGamesThemeTokens
import com.ugurbuga.stackshift.ui.theme.GameUiShapeTokens
import com.ugurbuga.stackshift.ui.theme.appBackgroundBrush
import com.ugurbuga.stackshift.ui.theme.blockGamesSurfaceShadow
import com.ugurbuga.stackshift.ui.theme.blockGamesThemeSpec
import com.ugurbuga.stackshift.ui.theme.isBlockGamesDarkTheme
import org.jetbrains.compose.resources.stringResource
import stackshift.composeapp.generated.resources.Res
import stackshift.composeapp.generated.resources.app_theme_dark
import stackshift.composeapp.generated.resources.app_theme_light
import stackshift.composeapp.generated.resources.app_theme_system
import stackshift.composeapp.generated.resources.block_style_brick
import stackshift.composeapp.generated.resources.block_style_bubble
import stackshift.composeapp.generated.resources.block_style_cosmic
import stackshift.composeapp.generated.resources.block_style_crystal
import stackshift.composeapp.generated.resources.block_style_dynamic_liquid
import stackshift.composeapp.generated.resources.block_style_flame
import stackshift.composeapp.generated.resources.block_style_flat
import stackshift.composeapp.generated.resources.block_style_gears
import stackshift.composeapp.generated.resources.block_style_grid_split
import stackshift.composeapp.generated.resources.block_style_honeycomb_texture
import stackshift.composeapp.generated.resources.block_style_light_burst
import stackshift.composeapp.generated.resources.block_style_liquid_marble
import stackshift.composeapp.generated.resources.block_style_matte_soft
import stackshift.composeapp.generated.resources.block_style_neon_glow
import stackshift.composeapp.generated.resources.block_style_outline
import stackshift.composeapp.generated.resources.block_style_pixel
import stackshift.composeapp.generated.resources.block_style_prism
import stackshift.composeapp.generated.resources.block_style_sharp_3d
import stackshift.composeapp.generated.resources.block_style_sound_wave
import stackshift.composeapp.generated.resources.block_style_spider_web
import stackshift.composeapp.generated.resources.block_style_stone_texture
import stackshift.composeapp.generated.resources.block_style_tornado
import stackshift.composeapp.generated.resources.block_style_wood
import stackshift.composeapp.generated.resources.cancel
import stackshift.composeapp.generated.resources.rewarded_tokens_button
import stackshift.composeapp.generated.resources.settings_block_style
import stackshift.composeapp.generated.resources.settings_color_palette
import stackshift.composeapp.generated.resources.settings_language
import stackshift.composeapp.generated.resources.settings_theme
import stackshift.composeapp.generated.resources.settings_tokens_balance
import stackshift.composeapp.generated.resources.settings_tokens_earn_challenge
import stackshift.composeapp.generated.resources.settings_tokens_earn_score
import stackshift.composeapp.generated.resources.settings_tokens_title
import stackshift.composeapp.generated.resources.theme_palette_aurora
import stackshift.composeapp.generated.resources.theme_palette_classic
import stackshift.composeapp.generated.resources.theme_palette_minimal_monochrome
import stackshift.composeapp.generated.resources.theme_palette_modern_neon
import stackshift.composeapp.generated.resources.theme_palette_soft_pastel
import stackshift.composeapp.generated.resources.theme_palette_sunset
import stackshift.composeapp.generated.resources.unlock_dialog_confirm
import stackshift.composeapp.generated.resources.unlock_dialog_insufficient_title
import stackshift.composeapp.generated.resources.unlock_dialog_message
import stackshift.composeapp.generated.resources.unlock_dialog_not_enough
import stackshift.composeapp.generated.resources.unlock_dialog_title
import stackshift.composeapp.generated.resources.unlock_dialog_watch_ad

private val ScreenContentMaxWidth = 920.dp

private enum class SettingsTab {
    Theme,
    BlockStyle,
}

@Composable
fun AppSettingsScreen(
    telemetry: AppTelemetry = NoOpAppTelemetry,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    onRewardedTokensRequested: () -> Unit,
    onBack: () -> Unit,
    adController: GameAdController? = null,
    modifier: Modifier = Modifier,
) {
    LogScreen(telemetry, TelemetryScreenNames.Theme)
    val scrollState = rememberScrollState()
    val uiColors = BlockGamesThemeTokens.uiColors
    val darkTheme = isBlockGamesDarkTheme(settings)
    val selectedBlockStyle = settings.blockVisualStyle
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var pendingUnlockRequest by remember { mutableStateOf<UnlockRequest?>(null) }
    val transition = rememberInfiniteTransition(label = "settingsStylePulse")
    val stylePulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "stylePulse",
    )
    val previewToneStep by transition.animateFloat(
        initialValue = 0f,
        targetValue = CellTone.entries.size.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = CellTone.entries.size * 3500,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "previewToneStep",
    )
    val sharedPreviewColor = remember(previewToneStep, settings.blockColorPalette) {
        interpolatedPreviewColor(
            palette = settings.blockColorPalette,
            progress = previewToneStep,
        )
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(appBackgroundBrush(uiColors)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TopBarActionBlockButton(
                        tone = CellTone.Cyan,
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.settings_theme),
                        onClick = onBack,
                        size = 44.dp,
                        pulse = stylePulse,
                    )
                    Text(
                        text = stringResource(Res.string.settings_theme),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.size(44.dp))
                }

                TokenBalanceCard(
                    settings = settings,
                    onRewardedTokensRequested = onRewardedTokensRequested,
                    adController = adController,
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp)
                )

                SecondaryTabRow(
                    selectedTabIndex = selectedTabIndex,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .widthIn(max = ScreenContentMaxWidth),
                    containerColor = uiColors.panel.copy(alpha = 0.90f),
                    contentColor = MaterialTheme.colorScheme.primary,
                ) {
                    listOf(
                        SettingsTab.Theme to stringResource(Res.string.settings_theme),
                        SettingsTab.BlockStyle to stringResource(Res.string.settings_block_style),
                    ).forEachIndexed { index, (_, title) ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.SemiBold else FontWeight.Medium,
                                )
                            },
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    when (selectedTabIndex) {
                        0 -> {
                            SettingsSectionCard(
                                title = stringResource(Res.string.settings_theme),
                                icon = Icons.Filled.Palette,
                            ) {
                                SettingsGroup(
                                    title = stringResource(Res.string.settings_theme),
                                    selectedValue = settings.themeMode,
                                    options = themeModeOptions(),
                                    onSelected = { onSettingsChange(settings.selectThemeMode(it)) },
                                )
                                SettingsGroup(
                                    title = stringResource(Res.string.settings_color_palette),
                                    selectedValue = settings.themeColorPalette,
                                    options = themePaletteOptions(settings = settings, darkTheme = darkTheme),
                                    onSelected = { onSettingsChange(settings.selectThemePalette(it)) },
                                    onLockedSelected = { option ->
                                        pendingUnlockRequest = unlockRequest(
                                            label = option.label,
                                            priceTokens = option.priceTokens,
                                            currentBalance = settings.tokenBalance,
                                        ) { currentSettings ->
                                            currentSettings.unlockThemePalette(option.value)
                                        }
                                    },
                                )
                            }
                        }

                        else -> {
                            SettingsSectionCard(
                                title = stringResource(Res.string.settings_block_style),
                                icon = Icons.Filled.Layers,
                                trailingContent = {
                                    StyleFourBlockPreview(
                                        settings = settings,
                                        pulse = stylePulse,
                                        previewColor = sharedPreviewColor,
                                    )
                                },
                            ) {
                                BlockStyleSettingsGroup(
                                    title = stringResource(Res.string.settings_block_style),
                                    selectedValue = selectedBlockStyle,
                                    options = blockStyleOptions(
                                        settings = settings,
                                        pulse = stylePulse,
                                        previewColor = sharedPreviewColor,
                                    ),
                                    onSelected = { onSettingsChange(settings.selectBlockStyle(it)) },
                                    onLockedSelected = { option ->
                                        pendingUnlockRequest = unlockRequest(
                                            label = option.label,
                                            priceTokens = option.priceTokens,
                                            currentBalance = settings.tokenBalance,
                                        ) { currentSettings ->
                                            currentSettings.unlockBlockStyle(option.value)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }

            }
        }

        pendingUnlockRequest?.let { request ->
            UnlockOptionDialog(
                request = request,
                onDismissRequest = { pendingUnlockRequest = null },
                onConfirm = {
                    request.onUnlock(settings)?.let(onSettingsChange)
                    pendingUnlockRequest = null
                },
                onWatchAd = onRewardedTokensRequested,
                adController = adController,
            )
        }
    }
}

@Composable
fun AppLanguageScreen(
    telemetry: AppTelemetry = NoOpAppTelemetry,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LogScreen(telemetry, TelemetryScreenNames.Language)
    val scrollState = rememberScrollState()
    val uiColors = BlockGamesThemeTokens.uiColors

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(appBackgroundBrush(uiColors)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TopBarActionBlockButton(
                        tone = CellTone.Cyan,
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.settings_language),
                        onClick = onBack,
                        size = 44.dp,
                    )
                    Text(
                        text = stringResource(Res.string.settings_language),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.size(44.dp))
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SettingsSectionCard(
                        title = stringResource(Res.string.settings_language),
                        icon = Icons.Filled.Translate,
                    ) {
                        SettingsGroup(
                            title = stringResource(Res.string.settings_language),
                            selectedValue = settings.language,
                            options = languageOptions(settings.language),
                            onSelected = { onSettingsChange(settings.copy(language = it)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    trailingContent: (@Composable (() -> Unit))? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val optionShape = RoundedCornerShape(GameUiShapeTokens.chipCorner)
    val panelShape = RoundedCornerShape(GameUiShapeTokens.panelCorner)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .blockGamesSurfaceShadow(
                shape = panelShape,
                elevation = 5.dp,
            )
            .widthIn(max = ScreenContentMaxWidth),
        shape = panelShape,
        colors = CardDefaults.cardColors(containerColor = uiColors.panel),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, uiColors.panelStroke),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(uiColors.panel.copy(alpha = 0.96f))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeader(
                title = title,
                icon = icon,
                trailingContent = trailingContent,
            )
            content()
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: ImageVector,
    trailingContent: (@Composable (() -> Unit))? = null,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val surfaceShape = RoundedCornerShape(GameUiShapeTokens.surfaceCorner)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier
                .size(40.dp)
                .blockGamesSurfaceShadow(
                    shape = surfaceShape,
                    elevation = 5.dp,
                ),
            shape = surfaceShape,
            color = uiColors.panelHighlight,
            shadowElevation = 0.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, uiColors.panelStroke),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        trailingContent?.invoke()
    }
}

@Composable
private fun <T> SettingsGroup(
    title: String,
    selectedValue: T,
    options: List<SettingsOption<T>>,
    onSelected: (T) -> Unit,
    onLockedSelected: (SettingsOption<T>) -> Unit = {},
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val optionShape = RoundedCornerShape(GameUiShapeTokens.chipCorner)
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = uiColors.subtitle,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(-(6).dp),
        ) {
            options.forEach { option ->
                val selected = option.value == selectedValue
                FilterChip(
                    modifier = Modifier.blockGamesSurfaceShadow(
                        elevation = if (selected) 5.dp else 0.dp,
                        shape = RoundedCornerShape(GameUiShapeTokens.chipCorner),
                    ),
                    selected = selected,
                    onClick = {
                        if (option.locked) {
                            onLockedSelected(option)
                        } else {
                            onSelected(option.value)
                        }
                    },
                    shape = RoundedCornerShape(GameUiShapeTokens.chipCorner),
                    label = {
                        Row(
                            modifier = Modifier.wrapContentWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            option.preview?.invoke()
                            Text(
                                text = option.label,
                                style = if (selected) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (option.locked) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = uiColors.chipSelected,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        containerColor = uiColors.chip.copy(alpha = 0.62f),
                        labelColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = uiColors.panelStroke.copy(alpha = 0.42f),
                        selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.82f),
                    ),
                    elevation = FilterChipDefaults.filterChipElevation(
                        elevation = 0.dp,
                        pressedElevation = 0.dp,
                        focusedElevation = 0.dp,
                        hoveredElevation = 0.dp,
                        draggedElevation = 0.dp,
                        disabledElevation = 0.dp,
                    ),
                )
            }
        }
    }
}

private data class SettingsOption<T>(
    val value: T,
    val label: String,
    val preview: (@Composable () -> Unit)? = null,
    val locked: Boolean = false,
    val priceTokens: Int = 0,
)

private data class UnlockRequest(
    val label: String,
    val priceTokens: Int,
    val currentBalance: Int,
    val onUnlock: (AppSettings) -> AppSettings?,
) {
    val canAfford: Boolean get() = currentBalance >= priceTokens
}

private fun unlockRequest(
    label: String,
    priceTokens: Int,
    currentBalance: Int,
    onUnlock: (AppSettings) -> AppSettings?,
): UnlockRequest = UnlockRequest(
    label = label,
    priceTokens = priceTokens,
    currentBalance = currentBalance,
    onUnlock = onUnlock,
)

@Composable
private fun BlockStyleSettingsGroup(
    title: String,
    selectedValue: BlockVisualStyle,
    options: List<SettingsOption<BlockVisualStyle>>,
    onSelected: (BlockVisualStyle) -> Unit,
    onLockedSelected: (SettingsOption<BlockVisualStyle>) -> Unit = {},
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val optionShape = RoundedCornerShape(GameUiShapeTokens.chipCorner)
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = uiColors.subtitle,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { option ->
                val selected = option.value == selectedValue
                Card(
                    modifier = Modifier
                        .width(104.dp)
                        .blockGamesSurfaceShadow(
                            elevation = if (selected) 5.dp else 0.dp,
                            shape = optionShape,
                        )
                        .clip(optionShape)
                        .clickable {
                            if (option.locked) onLockedSelected(option) else onSelected(option.value)
                        },
                    shape = optionShape,
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) uiColors.chipSelected else uiColors.chip.copy(alpha = 0.72f),
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.84f)
                        } else {
                            uiColors.panelStroke.copy(alpha = 0.42f)
                        },
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 7.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            option.preview?.invoke()
                            if (option.locked) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f),
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        Box(
                            modifier = Modifier.height(36.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = option.label,
                                style = if (selected) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TokenBalanceCard(
    settings: AppSettings,
    onRewardedTokensRequested: () -> Unit,
    adController: GameAdController? = null,
    modifier: Modifier = Modifier,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    val surfaceShape = RoundedCornerShape(GameUiShapeTokens.panelCorner)
    var adLoading by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .blockGamesSurfaceShadow(
                shape = surfaceShape,
                elevation = 4.dp,
            )
            .widthIn(max = ScreenContentMaxWidth),
        shape = surfaceShape,
        colors = CardDefaults.cardColors(containerColor = uiColors.panelHighlight.copy(alpha = 0.92f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, uiColors.panelStroke),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(Res.string.settings_tokens_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(Res.string.settings_tokens_balance, settings.tokenBalance),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(Res.string.settings_tokens_earn_challenge, DailyChallengeTokenReward),
                    style = MaterialTheme.typography.bodySmall,
                    color = uiColors.subtitle,
                )
                Text(
                    text = stringResource(Res.string.settings_tokens_earn_score, ScorePointsPerToken),
                    style = MaterialTheme.typography.bodySmall,
                    color = uiColors.subtitle,
                )
            }

            if (adController != null) {
                TopBarActionBlockButton(
                    tone = CellTone.Gold,
                    icon = Icons.Filled.Stars,
                    contentDescription = stringResource(Res.string.rewarded_tokens_button, RewardedTokenAdReward),
                    onClick = {
                        if (adLoading) return@TopBarActionBlockButton
                        adLoading = true
                        adController.showRewardedAd { success ->
                            adLoading = false
                            if (success) {
                                onRewardedTokensRequested()
                            }
                        }
                    },
                    enabled = !adLoading,
                    size = 56.dp,
                    showAdIcon = true,
                )
            }
        }
    }
}

@Composable
private fun UnlockOptionDialog(
    request: UnlockRequest,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    onWatchAd: () -> Unit,
    adController: GameAdController? = null,
) {
    if (request.canAfford) {
        ThemedConfirmDialog(
            onDismissRequest = onDismissRequest,
            title = stringResource(Res.string.unlock_dialog_title, request.label),
            message = stringResource(
                Res.string.unlock_dialog_message,
                request.label,
                request.priceTokens,
                request.currentBalance,
            ),
            confirmLabel = stringResource(Res.string.unlock_dialog_confirm),
            dismissLabel = stringResource(Res.string.cancel),
            onConfirm = onConfirm,
            icon = Icons.Filled.Lock,
        )
    } else {
        var adLoading by remember { mutableStateOf(false) }

        ThemedConfirmDialog(
            onDismissRequest = onDismissRequest,
            title = stringResource(Res.string.unlock_dialog_insufficient_title, request.label),
            message = stringResource(
                Res.string.unlock_dialog_not_enough,
                request.priceTokens,
                request.currentBalance,
            ),
            confirmLabel = stringResource(Res.string.unlock_dialog_watch_ad),
            dismissLabel = stringResource(Res.string.cancel),
            onConfirm = {
                if (adLoading) return@ThemedConfirmDialog
                if (adController != null) {
                    adLoading = true
                    adController.showRewardedAd { success ->
                        adLoading = false
                        if (success) {
                            onWatchAd()
                            onDismissRequest()
                        }
                    }
                } else {
                    // Fallback for previews or if adController is missing
                    onWatchAd()
                    onDismissRequest()
                }
            },
            icon = Icons.Outlined.Videocam,
        )
    }
}

@Composable
private fun languageOptions(selected: AppLanguage): List<SettingsOption<AppLanguage>> =
    AppLanguage.entries.map { language ->
        SettingsOption(
            value = language,
            label = stringResource(language.labelRes),
            preview = { LanguagePreview(language = language, selected = language == selected) },
        )
    }

@Composable
private fun themeModeOptions(): List<SettingsOption<AppThemeMode>> =
    AppThemeMode.entries.map { mode ->
        SettingsOption(
            value = mode,
            label = when (mode) {
                AppThemeMode.System -> stringResource(Res.string.app_theme_system)
                AppThemeMode.Light -> stringResource(Res.string.app_theme_light)
                AppThemeMode.Dark -> stringResource(Res.string.app_theme_dark)
            },
            preview = { ThemeModePreview(mode) },
        )
    }

@Composable
private fun themePaletteOptions(
    settings: AppSettings,
    darkTheme: Boolean,
): List<SettingsOption<AppColorPalette>> =
    AppColorPalette.entries.map { palette ->
        SettingsOption(
            value = palette,
            label = when (palette) {
                AppColorPalette.Classic -> stringResource(Res.string.theme_palette_classic)
                AppColorPalette.Aurora -> stringResource(Res.string.theme_palette_aurora)
                AppColorPalette.Sunset -> stringResource(Res.string.theme_palette_sunset)
                AppColorPalette.ModernNeon -> stringResource(Res.string.theme_palette_modern_neon)
                AppColorPalette.SoftPastel -> stringResource(Res.string.theme_palette_soft_pastel)
                AppColorPalette.MinimalMonochrome -> stringResource(Res.string.theme_palette_minimal_monochrome)
            },
            preview = { ThemePalettePreview(palette = palette, darkTheme = darkTheme) },
            locked = !settings.isThemePaletteUnlocked(palette),
            priceTokens = palette.tokenCost(),
        )
    }

@Composable
private fun blockStyleOptions(
    settings: AppSettings,
    pulse: Float,
    previewColor: Color,
): List<SettingsOption<BlockVisualStyle>> {
    val visibleStyles = listOf(
        BlockVisualStyle.Flat,
        BlockVisualStyle.Bubble,
        BlockVisualStyle.Outline,
        BlockVisualStyle.Sharp3D,
        BlockVisualStyle.Wood,
        BlockVisualStyle.GridSplit,
        BlockVisualStyle.Crystal,
        BlockVisualStyle.DynamicLiquid,
        BlockVisualStyle.Tornado,
        BlockVisualStyle.HoneycombTexture,
        BlockVisualStyle.SpiderWeb,
        BlockVisualStyle.Cosmic,
        BlockVisualStyle.Brick,
        BlockVisualStyle.SoundWave,
        BlockVisualStyle.Prism,
        BlockVisualStyle.Flame,
        BlockVisualStyle.Gears,
        BlockVisualStyle.Pixel,
    )

    return visibleStyles.map { style ->
        SettingsOption(
            value = style,
            label = when (style) {
                BlockVisualStyle.Flat -> stringResource(Res.string.block_style_flat)
                BlockVisualStyle.Bubble -> stringResource(Res.string.block_style_bubble)
                BlockVisualStyle.Outline -> stringResource(Res.string.block_style_outline)
                BlockVisualStyle.Sharp3D -> stringResource(Res.string.block_style_sharp_3d)
                BlockVisualStyle.Wood -> stringResource(Res.string.block_style_wood)
                BlockVisualStyle.GridSplit -> stringResource(Res.string.block_style_grid_split)
                BlockVisualStyle.Crystal -> stringResource(Res.string.block_style_crystal)
                BlockVisualStyle.DynamicLiquid -> stringResource(Res.string.block_style_dynamic_liquid)
                BlockVisualStyle.MatteSoft -> stringResource(Res.string.block_style_matte_soft)
                BlockVisualStyle.NeonGlow -> stringResource(Res.string.block_style_neon_glow)
                BlockVisualStyle.Tornado -> stringResource(Res.string.block_style_tornado)
                BlockVisualStyle.StoneTexture -> stringResource(Res.string.block_style_stone_texture)
                BlockVisualStyle.HoneycombTexture -> stringResource(Res.string.block_style_honeycomb_texture)
                BlockVisualStyle.LightBurst -> stringResource(Res.string.block_style_light_burst)
                BlockVisualStyle.LiquidMarble -> stringResource(Res.string.block_style_liquid_marble)
                BlockVisualStyle.SpiderWeb -> stringResource(Res.string.block_style_spider_web)
                BlockVisualStyle.Cosmic -> stringResource(Res.string.block_style_cosmic)
                BlockVisualStyle.Brick -> stringResource(Res.string.block_style_brick)
                BlockVisualStyle.SoundWave -> stringResource(Res.string.block_style_sound_wave)
                BlockVisualStyle.Prism -> stringResource(Res.string.block_style_prism)
                BlockVisualStyle.Electric -> stringResource(Res.string.block_style_flat)
                BlockVisualStyle.Flame -> stringResource(Res.string.block_style_flame)
                BlockVisualStyle.Gears -> stringResource(Res.string.block_style_gears)
                BlockVisualStyle.Pixel -> stringResource(Res.string.block_style_pixel)
            },
            preview = {
                BlockStylePreview(
                    style = style,
                    pulse = pulse,
                    previewColor = previewColor,
                    previewPalette = settings.blockColorPalette,
                )
            },
            locked = !settings.isBlockStyleUnlocked(style),
            priceTokens = style.tokenCost(),
        )
    }
}

@Composable
private fun ThemeModePreview(mode: AppThemeMode) {
    val colors = when (mode) {
        AppThemeMode.System -> listOf(Color(0xFF7AA8FF), Color(0xFFE7EEF8))
        AppThemeMode.Light -> listOf(Color(0xFFFFD76A), Color(0xFFFFF7D8))
        AppThemeMode.Dark -> listOf(Color(0xFF8F83FF), Color(0xFF1D2233))
    }
    BoxPreview(colors = colors, size = 16.dp)
}

@Composable
private fun ThemePalettePreview(
    palette: AppColorPalette,
    darkTheme: Boolean,
) {
    val scheme = blockGamesThemeSpec(palette = palette, darkTheme = darkTheme).colorScheme
    val colors = listOf(
        scheme.primary,
        scheme.secondary,
        scheme.tertiary,
        scheme.surfaceVariant,
    )
    BoxPreview(colors = colors, size = 16.dp)
}


@Composable
private fun BlockStylePreview(
    style: BlockVisualStyle,
    pulse: Float,
    previewColor: Color,
    previewPalette: BlockColorPalette,
) {
    BlockCellPreview(
        baseColor = settingsPreviewColor(
            style = style,
            animatedPreviewColor = previewColor,
            palette = previewPalette,
        ),
        style = style,
        size = 54.dp,
        pulse = settingsPreviewPulse(style = style, pulse = pulse),
    )
}

private fun settingsPreviewPulse(
    style: BlockVisualStyle,
    pulse: Float,
): Float = when (style) {
    BlockVisualStyle.Pixel,
        -> 0f

    else -> pulse
}

private fun settingsPreviewColor(
    style: BlockVisualStyle,
    animatedPreviewColor: Color,
    palette: BlockColorPalette,
): Color = animatedPreviewColor

private fun interpolatedPreviewColor(
    palette: BlockColorPalette,
    progress: Float,
): Color {
    val tones = CellTone.entries
    val normalized = ((progress % tones.size) + tones.size) % tones.size
    val startIndex = normalized.toInt().coerceIn(0, tones.lastIndex)
    val endIndex = (startIndex + 1) % tones.size
    val blend = normalized - startIndex
    return lerp(
        tones[startIndex].paletteColor(palette),
        tones[endIndex].paletteColor(palette),
        blend,
    )
}

@Composable
private fun LanguagePreview(
    language: AppLanguage,
    selected: Boolean,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    Box(
        modifier = Modifier
            .background(
                color = if (selected) {
                    uiColors.panelHighlight.copy(alpha = 0.96f)
                } else {
                    uiColors.panelMuted.copy(alpha = 0.96f)
                },
                shape = RoundedCornerShape(GameUiShapeTokens.chipCorner)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = language.flag,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun StyleFourBlockPreview(settings: AppSettings, pulse: Float, previewColor: Color) {
    BlockCellPreview(
        baseColor = settingsPreviewColor(
            style = settings.blockVisualStyle,
            animatedPreviewColor = previewColor,
            palette = settings.blockColorPalette,
        ),
        style = settings.blockVisualStyle,
        size = 44.dp,
        pulse = settingsPreviewPulse(style = settings.blockVisualStyle, pulse = pulse),
    )
}

@Composable
private fun BoxPreview(
    colors: List<Color>,
    icon: ImageVector? = null,
    size: Dp = 10.dp,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        colors.forEachIndexed { index, color ->
            Box(
                modifier = Modifier
                    .size(size)
                    .background(color, shape = RoundedCornerShape((size.value * 0.35f).dp))
                    .border(
                        width = 1.dp,
                        color = uiColors.panelStroke.copy(alpha = 0.32f),
                        shape = RoundedCornerShape((size.value * 0.35f).dp),
                    ),
            ) {
                if (icon != null && index == colors.lastIndex) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(size * 0.52f),
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun AppSettingsScreenPreview() {
    AppSettingsScreen(
        settings = AppSettings(themeMode = AppThemeMode.Light),
        onSettingsChange = {},
        onRewardedTokensRequested = {},
        onBack = {},
    )
}

@Preview
@Composable
private fun AppLanguageScreenPreview() {
    AppLanguageScreen(
        settings = AppSettings(themeMode = AppThemeMode.Light),
        onSettingsChange = {},
        onBack = {},
    )
}

