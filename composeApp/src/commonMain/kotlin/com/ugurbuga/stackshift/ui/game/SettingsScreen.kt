package com.ugurbuga.stackshift.ui.game

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.ViewModule
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ugurbuga.stackshift.game.model.AppColorPalette
import com.ugurbuga.stackshift.game.model.AppLanguage
import com.ugurbuga.stackshift.game.model.AppThemeMode
import com.ugurbuga.stackshift.game.model.BlockColorPalette
import com.ugurbuga.stackshift.game.model.BlockVisualStyle
import com.ugurbuga.stackshift.game.model.CellTone
import com.ugurbuga.stackshift.game.model.normalizeBlockVisualStyle
import com.ugurbuga.stackshift.settings.AppSettings
import com.ugurbuga.stackshift.telemetry.AppTelemetry
import com.ugurbuga.stackshift.telemetry.LogScreen
import com.ugurbuga.stackshift.telemetry.NoOpAppTelemetry
import com.ugurbuga.stackshift.telemetry.TelemetryScreenNames
import com.ugurbuga.stackshift.ui.theme.GameUiShapeTokens
import com.ugurbuga.stackshift.ui.theme.StackShiftThemeTokens
import com.ugurbuga.stackshift.ui.theme.appBackgroundBrush
import com.ugurbuga.stackshift.ui.theme.isStackShiftDarkTheme
import com.ugurbuga.stackshift.ui.theme.stackShiftSurfaceShadow
import com.ugurbuga.stackshift.ui.theme.stackShiftThemeSpec
import org.jetbrains.compose.resources.stringResource
import stackshift.composeapp.generated.resources.Res
import stackshift.composeapp.generated.resources.app_theme_dark
import stackshift.composeapp.generated.resources.app_theme_light
import stackshift.composeapp.generated.resources.app_theme_system
import stackshift.composeapp.generated.resources.app_title
import stackshift.composeapp.generated.resources.block_palette_monochrome
import stackshift.composeapp.generated.resources.block_palette_neon
import stackshift.composeapp.generated.resources.block_style_bubble
import stackshift.composeapp.generated.resources.block_style_crystal
import stackshift.composeapp.generated.resources.block_style_dynamic_liquid
import stackshift.composeapp.generated.resources.block_style_flat
import stackshift.composeapp.generated.resources.block_style_outline
import stackshift.composeapp.generated.resources.block_style_wood
import stackshift.composeapp.generated.resources.settings_block_style
import stackshift.composeapp.generated.resources.settings_language
import stackshift.composeapp.generated.resources.settings_theme
import stackshift.composeapp.generated.resources.theme_palette_aurora
import stackshift.composeapp.generated.resources.theme_palette_classic
import stackshift.composeapp.generated.resources.theme_palette_soft_pastel
import stackshift.composeapp.generated.resources.theme_palette_sunset

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
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LogScreen(telemetry, TelemetryScreenNames.Theme)
    val scrollState = rememberScrollState()
    val uiColors = StackShiftThemeTokens.uiColors
    val darkTheme = isStackShiftDarkTheme(settings)
    val normalizedBlockStyle = normalizeBlockVisualStyle(settings.blockVisualStyle)
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val transition = rememberInfiniteTransition(label = "settingsStylePulse")
    val stylePulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
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
                .background(appBackgroundBrush(uiColors)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    HeaderCard(
                        onBack = onBack,
                        title = stringResource(Res.string.settings_theme),
                        summary = buildString {
                            append(stringResource(Res.string.settings_theme))
                            append(" • ")
                            append(stringResource(Res.string.settings_block_style))
                        },
                    )
                }

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
                                    onSelected = { onSettingsChange(settings.copy(themeMode = it)) },
                                )
                                SettingsGroup(
                                    title = if (settings.language == AppLanguage.Turkish) "Renk Paleti" else "Color Palette",
                                    selectedValue = settings.themeColorPalette,
                                    options = themePaletteOptions(darkTheme = darkTheme),
                                    onSelected = { onSettingsChange(settings.copy(themeColorPalette = it)) },
                                )
                            }
                        }

                        else -> {
                            SettingsSectionCard(
                                title = stringResource(Res.string.settings_block_style),
                                icon = Icons.Filled.ViewModule,
                                trailingContent = {
                                    StyleFourBlockPreview(settings = settings, pulse = stylePulse)
                                },
                            ) {
                                SettingsGroup(
                                    title = stringResource(Res.string.settings_block_style),
                                    selectedValue = normalizedBlockStyle,
                                    options = blockStyleOptions(
                                        settings.blockColorPalette,
                                        pulse = stylePulse
                                    ),
                                    onSelected = { onSettingsChange(settings.copy(blockVisualStyle = it)) },
                                )
                            }
                        }
                    }
                }

            }
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
    val uiColors = StackShiftThemeTokens.uiColors

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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    HeaderCard(
                        onBack = onBack,
                        title = stringResource(Res.string.settings_language),
                        summary = stringResource(Res.string.app_title),
                    )
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
private fun HeaderCard(
    onBack: () -> Unit,
    title: String,
    summary: String,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    val panelShape = RoundedCornerShape(GameUiShapeTokens.panelCorner)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .stackShiftSurfaceShadow(
                shape = panelShape,
                elevation = 10.dp,
            )
            .widthIn(max = ScreenContentMaxWidth),
        shape = panelShape,
        colors = CardDefaults.cardColors(containerColor = uiColors.panel),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, uiColors.panelStroke),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            uiColors.settingsHeroStart.copy(alpha = 0.88f),
                            uiColors.settingsHeroEnd.copy(alpha = 0.72f),
                        ),
                    ),
                )
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TopBarActionBlockButton(
                tone = CellTone.Cyan,
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = title,
                onClick = onBack,
                size = 40.dp,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(Res.string.app_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
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
    val uiColors = StackShiftThemeTokens.uiColors
    val panelShape = RoundedCornerShape(GameUiShapeTokens.panelCorner)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .stackShiftSurfaceShadow(
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
    val uiColors = StackShiftThemeTokens.uiColors
    val surfaceShape = RoundedCornerShape(GameUiShapeTokens.surfaceCorner)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier
                .size(40.dp)
                .stackShiftSurfaceShadow(
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
) {
    val uiColors = StackShiftThemeTokens.uiColors
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
                    modifier = Modifier.stackShiftSurfaceShadow(
                        elevation = if (selected) 5.dp else 0.dp,
                        shape = RoundedCornerShape(GameUiShapeTokens.chipCorner),
                    ),
                    selected = selected,
                    onClick = { onSelected(option.value) },
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
)

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
private fun themePaletteOptions(darkTheme: Boolean): List<SettingsOption<AppColorPalette>> =
    AppColorPalette.entries.map { palette ->
        SettingsOption(
            value = palette,
            label = when (palette) {
                AppColorPalette.Classic -> stringResource(Res.string.theme_palette_classic)
                AppColorPalette.Aurora -> stringResource(Res.string.theme_palette_aurora)
                AppColorPalette.Sunset -> stringResource(Res.string.theme_palette_sunset)
                AppColorPalette.ModernNeon -> stringResource(Res.string.block_palette_neon)
                AppColorPalette.SoftPastel -> stringResource(Res.string.theme_palette_soft_pastel)
                AppColorPalette.MinimalMonochrome -> stringResource(Res.string.block_palette_monochrome)
            },
            preview = { ThemePalettePreview(palette = palette, darkTheme = darkTheme) },
        )
    }

@Composable
private fun blockStyleOptions(
    palette: BlockColorPalette,
    pulse: Float
): List<SettingsOption<BlockVisualStyle>> {
    val settings = com.ugurbuga.stackshift.localization.LocalAppSettings.current
    val visibleStyles = listOf(
        BlockVisualStyle.Flat,
        BlockVisualStyle.Bubble,
        BlockVisualStyle.Outline,
        BlockVisualStyle.Sharp3D,
        BlockVisualStyle.Wood,
        BlockVisualStyle.PixelArt,
        BlockVisualStyle.Crystal,
        BlockVisualStyle.DynamicLiquid,
        BlockVisualStyle.Metallic,
        BlockVisualStyle.HoneycombTexture,
        BlockVisualStyle.Lava,
        BlockVisualStyle.SpiderWeb,
        BlockVisualStyle.Cosmic,
        BlockVisualStyle.Bamboo,
    )

    return visibleStyles.map { style ->
        SettingsOption(
            value = style,
            label = when (style) {
                BlockVisualStyle.Flat -> stringResource(Res.string.block_style_flat)
                BlockVisualStyle.Bubble -> stringResource(Res.string.block_style_bubble)
                BlockVisualStyle.Outline -> stringResource(Res.string.block_style_outline)
                BlockVisualStyle.Sharp3D -> if (settings.language == AppLanguage.Turkish) "Keskin Modern" else "Sharp Modern"
                BlockVisualStyle.Wood -> stringResource(Res.string.block_style_wood)
                BlockVisualStyle.PixelArt -> if (settings.language == AppLanguage.Turkish) "Rubik Küp" else "Rubik Cube"
                BlockVisualStyle.Crystal -> stringResource(Res.string.block_style_crystal)
                BlockVisualStyle.DynamicLiquid -> stringResource(Res.string.block_style_dynamic_liquid)
                BlockVisualStyle.MatteSoft -> if (settings.language == AppLanguage.Turkish) "Mat Soft" else "Matte Soft"
                BlockVisualStyle.NeonGlow -> "Neon Glow"
                BlockVisualStyle.Metallic -> if (settings.language == AppLanguage.Turkish) "Rüzgar İzi" else "Wind Trail"
                BlockVisualStyle.StoneTexture -> if (settings.language == AppLanguage.Turkish) "Taş Doku" else "Stone Texture"
                BlockVisualStyle.HoneycombTexture -> if (settings.language == AppLanguage.Turkish) "Petek Doku" else "Honeycomb Texture"
                BlockVisualStyle.LightBurst -> if (settings.language == AppLanguage.Turkish) "Işık Hüzmesi" else "Light Burst"
                BlockVisualStyle.LiquidMarble -> if (settings.language == AppLanguage.Turkish) "Sıvı Mermer" else "Liquid Marble"
                BlockVisualStyle.Lava -> if (settings.language == AppLanguage.Turkish) "Vitray" else "Stained Glass"
                BlockVisualStyle.SpiderWeb -> if (settings.language == AppLanguage.Turkish) "Örümcek Ağı" else "Spider Web"
                BlockVisualStyle.Cosmic -> if (settings.language == AppLanguage.Turkish) "Kozmik" else "Cosmic"
                BlockVisualStyle.Bamboo -> if (settings.language == AppLanguage.Turkish) "Tuğla" else "Brick"
            },
            preview = { BlockStylePreview(style = style, palette = palette, pulse = pulse) },
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
    val scheme = stackShiftThemeSpec(palette = palette, darkTheme = darkTheme).colorScheme
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
    palette: BlockColorPalette,
    pulse: Float,
) {
    PreviewBlockRow(palette = palette, style = style, pulse = pulse)
}

@Composable
private fun LanguagePreview(
    language: AppLanguage,
    selected: Boolean,
) {
    val uiColors = StackShiftThemeTokens.uiColors
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
            .border(
                width = 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.48f) else uiColors.panelStroke,
                shape = RoundedCornerShape(GameUiShapeTokens.chipCorner)
            )
    ) {
        Text(
            text = language.localeTag.uppercase(),
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
private fun PreviewBlockRow(
    palette: BlockColorPalette,
    style: BlockVisualStyle,
    pulse: Float,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(CellTone.Coral, CellTone.Blue, CellTone.Gold).forEach { tone ->
            BlockCellPreview(
                tone = tone,
                palette = palette,
                style = style,
                size = 15.dp,
                pulse = pulse,
            )
        }
    }
}

@Composable
private fun StyleFourBlockPreview(settings: AppSettings, pulse: Float) {
    val tones = listOf(CellTone.Lime, CellTone.Violet, CellTone.Rose, CellTone.Amber)
    Row(
        modifier = Modifier.padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tones.take(4).forEach { tone ->
            BlockCellPreview(
                tone = tone,
                palette = settings.blockColorPalette,
                style = settings.blockVisualStyle,
                size = 36.dp,
                pulse = pulse,
            )
        }
    }
}

@Composable
private fun BoxPreview(
    colors: List<Color>,
    icon: ImageVector? = null,
    size: Dp = 10.dp,
) {
    val uiColors = StackShiftThemeTokens.uiColors
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

