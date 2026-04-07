package com.ugurbuga.stackshift.ui.game

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ugurbuga.stackshift.game.model.AppColorPalette
import com.ugurbuga.stackshift.game.model.AppLanguage
import com.ugurbuga.stackshift.game.model.AppThemeMode
import com.ugurbuga.stackshift.game.model.BlockColorPalette
import com.ugurbuga.stackshift.game.model.BlockVisualStyle
import com.ugurbuga.stackshift.game.model.CellTone
import com.ugurbuga.stackshift.game.model.SpecialBlockType
import com.ugurbuga.stackshift.settings.AppSettings
import com.ugurbuga.stackshift.ui.theme.StackShiftThemeTokens
import com.ugurbuga.stackshift.ui.theme.appBackgroundBrush
import com.ugurbuga.stackshift.ui.theme.isStackShiftDarkTheme
import com.ugurbuga.stackshift.ui.theme.stackShiftThemeSpec
import org.jetbrains.compose.resources.stringResource
import stackshift.composeapp.generated.resources.Res
import stackshift.composeapp.generated.resources.app_language_english
import stackshift.composeapp.generated.resources.app_language_turkish
import stackshift.composeapp.generated.resources.app_theme_dark
import stackshift.composeapp.generated.resources.app_theme_light
import stackshift.composeapp.generated.resources.app_theme_system
import stackshift.composeapp.generated.resources.app_title
import stackshift.composeapp.generated.resources.block_palette_candy
import stackshift.composeapp.generated.resources.block_palette_classic
import stackshift.composeapp.generated.resources.block_palette_earth
import stackshift.composeapp.generated.resources.block_palette_neon
import stackshift.composeapp.generated.resources.block_style_bubble
import stackshift.composeapp.generated.resources.block_style_flat
import stackshift.composeapp.generated.resources.block_style_liquid_glass
import stackshift.composeapp.generated.resources.block_style_neon
import stackshift.composeapp.generated.resources.block_style_outline
import stackshift.composeapp.generated.resources.block_style_sharp_3d
import stackshift.composeapp.generated.resources.block_style_wood
import stackshift.composeapp.generated.resources.settings_block_palette
import stackshift.composeapp.generated.resources.settings_block_style
import stackshift.composeapp.generated.resources.settings_language
import stackshift.composeapp.generated.resources.settings_theme
import stackshift.composeapp.generated.resources.settings_theme_palette
import stackshift.composeapp.generated.resources.settings_title
import stackshift.composeapp.generated.resources.theme_palette_aurora
import stackshift.composeapp.generated.resources.theme_palette_classic
import stackshift.composeapp.generated.resources.theme_palette_sunset

private val ScreenContentMaxWidth = 920.dp
private val ChipShape = RoundedCornerShape(22.dp)
private val SettingsCardShape = RoundedCornerShape(24.dp)
private val PremiumPreviewCardShape = RoundedCornerShape(12.dp)

@Composable
fun AppSettingsScreen(
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val uiColors = StackShiftThemeTokens.uiColors
    val darkTheme = isStackShiftDarkTheme(settings)

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
                    .safeDrawingPadding()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                HeaderCard(onBack = onBack)

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
                        title = stringResource(Res.string.settings_theme_palette),
                        selectedValue = settings.themeColorPalette,
                        options = themePaletteOptions(darkTheme = darkTheme),
                        onSelected = { onSettingsChange(settings.copy(themeColorPalette = it)) },
                    )
                }

                SettingsSectionCard(
                    title = stringResource(Res.string.settings_block_style),
                    icon = Icons.Filled.ViewModule,
                    trailingContent = {
                        LiveBoardMiniPreview(settings = settings)
                    },
                ) {
                    SettingsGroup(
                        title = stringResource(Res.string.settings_block_palette),
                        selectedValue = settings.blockColorPalette,
                        options = blockPaletteOptions(style = settings.blockVisualStyle),
                        onSelected = { onSettingsChange(settings.copy(blockColorPalette = it)) },
                    )
                    SettingsGroup(
                        title = stringResource(Res.string.settings_block_style),
                        selectedValue = settings.blockVisualStyle,
                        options = blockStyleOptions(settings.blockColorPalette),
                        onSelected = { onSettingsChange(settings.copy(blockVisualStyle = it)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderCard(
    onBack: () -> Unit,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = ScreenContentMaxWidth),
        shape = SettingsCardShape,
        colors = CardDefaults.cardColors(containerColor = uiColors.panel),
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
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.size(6.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = stringResource(Res.string.settings_title),
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
                    text = buildString {
                        append(stringResource(Res.string.settings_theme))
                        append(" • ")
                        append(stringResource(Res.string.settings_block_style))
                        append(" • ")
                        append(stringResource(Res.string.settings_language))
                    },
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = ScreenContentMaxWidth),
        shape = SettingsCardShape,
        colors = CardDefaults.cardColors(containerColor = uiColors.panel),
        border = androidx.compose.foundation.BorderStroke(1.dp, uiColors.panelStroke),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(uiColors.panel.copy(alpha = 0.96f))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(14.dp),
            color = uiColors.panelHighlight,
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            options.forEach { option ->
                val selected = option.value == selectedValue
                FilterChip(
                    modifier = Modifier.shadow(
                        elevation = if (selected) 9.dp else 1.dp,
                        shape = ChipShape,
                        ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = if (selected) 0.20f else 0.04f),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = if (selected) 0.20f else 0.04f),
                    ),
                    selected = selected,
                    onClick = { onSelected(option.value) },
                    shape = ChipShape,
                    label = {
                        Row(
                            modifier = Modifier
                                .padding(vertical = 3.dp)
                                .wrapContentWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            option.preview?.invoke()
                            Text(
                                text = option.label,
                                style = if (selected) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = uiColors.chipSelected,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        containerColor = uiColors.chip,
                        labelColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.76f),
                        selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.94f),
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
private fun languageOptions(selected: AppLanguage): List<SettingsOption<AppLanguage>> = AppLanguage.entries.map { language ->
    SettingsOption(
        value = language,
        label = when (language) {
            AppLanguage.English -> stringResource(Res.string.app_language_english)
            AppLanguage.Turkish -> stringResource(Res.string.app_language_turkish)
            AppLanguage.Spanish -> language.endonym
            AppLanguage.French -> language.endonym
            AppLanguage.German -> language.endonym
            AppLanguage.Russian -> language.endonym
        },
        preview = { LanguagePreview(language = language, selected = language == selected) },
    )
}

@Composable
private fun themeModeOptions(): List<SettingsOption<AppThemeMode>> = AppThemeMode.entries.map { mode ->
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
private fun themePaletteOptions(darkTheme: Boolean): List<SettingsOption<AppColorPalette>> = AppColorPalette.entries.map { palette ->
    SettingsOption(
        value = palette,
        label = when (palette) {
            AppColorPalette.Classic -> stringResource(Res.string.theme_palette_classic)
            AppColorPalette.Aurora -> stringResource(Res.string.theme_palette_aurora)
            AppColorPalette.Sunset -> stringResource(Res.string.theme_palette_sunset)
        },
        preview = { ThemePalettePreview(palette = palette, darkTheme = darkTheme) },
    )
}

@Composable
private fun blockPaletteOptions(style: BlockVisualStyle): List<SettingsOption<BlockColorPalette>> = BlockColorPalette.entries.map { palette ->
    SettingsOption(
        value = palette,
        label = when (palette) {
            BlockColorPalette.Classic -> stringResource(Res.string.block_palette_classic)
            BlockColorPalette.Candy -> stringResource(Res.string.block_palette_candy)
            BlockColorPalette.Neon -> stringResource(Res.string.block_palette_neon)
            BlockColorPalette.Earth -> stringResource(Res.string.block_palette_earth)
        },
        preview = { BlockPalettePreview(palette = palette, style = style) },
    )
}

@Composable
private fun blockStyleOptions(palette: BlockColorPalette): List<SettingsOption<BlockVisualStyle>> = BlockVisualStyle.entries.map { style ->
    SettingsOption(
        value = style,
        label = when (style) {
            BlockVisualStyle.Flat -> stringResource(Res.string.block_style_flat)
            BlockVisualStyle.Bubble -> stringResource(Res.string.block_style_bubble)
            BlockVisualStyle.Outline -> stringResource(Res.string.block_style_outline)
            BlockVisualStyle.Sharp3D -> stringResource(Res.string.block_style_sharp_3d)
            BlockVisualStyle.Wood -> stringResource(Res.string.block_style_wood)
            BlockVisualStyle.LiquidGlass -> stringResource(Res.string.block_style_liquid_glass)
            BlockVisualStyle.Neon -> stringResource(Res.string.block_style_neon)
        },
        preview = { BlockStylePreview(style = style, palette = palette) },
    )
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
private fun BlockPalettePreview(
    palette: BlockColorPalette,
    style: BlockVisualStyle,
) {
    PreviewBlockRow(palette = palette, style = style)
}

@Composable
private fun BlockStylePreview(
    style: BlockVisualStyle,
    palette: BlockColorPalette,
) {
    PreviewBlockRow(palette = palette, style = style)
}

@Composable
private fun LanguagePreview(
    language: AppLanguage,
    selected: Boolean,
) {
    Text(
        text = language.localeTag.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun PreviewBlockRow(
    palette: BlockColorPalette,
    style: BlockVisualStyle,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(CellTone.Coral, CellTone.Blue, CellTone.Gold).forEach { tone ->
            BlockCellPreview(
                tone = tone,
                palette = palette,
                style = style,
                size = 18.dp,
            )
        }
    }
}

@Composable
private fun LiveBoardMiniPreview(settings: AppSettings) {
    val uiColors = StackShiftThemeTokens.uiColors
    val boardStyle = settings.blockVisualStyle
    val transition = rememberInfiniteTransition(label = "liveBoardPreview")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 1800)),
        label = "liveBoardPreviewProgress",
    )
    val previewColumn = when {
        progress < 0.33f -> 0
        progress < 0.66f -> 1
        else -> 2
    }
    val previewAlpha = if (progress < 0.5f) 0.80f else 0.60f
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.dp, uiColors.panelStroke),
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            repeat(3) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(4) { column ->
                        val tone = when {
                            row == 0 && column in 1..2 -> CellTone.Cyan
                            row == 1 && column == 1 -> CellTone.Gold
                            row == 2 && column == 2 -> CellTone.Violet
                            else -> null
                        }
                        val isAnimatedPreviewCell = when (row) {
                            0 -> column == previewColumn || column == previewColumn + 1
                            1 -> column == previewColumn + 1
                            else -> false
                        }
                        if (tone == null) {
                            if (isAnimatedPreviewCell) {
                                BlockCellPreview(
                                    tone = CellTone.Emerald,
                                    palette = settings.blockColorPalette,
                                    style = settings.blockVisualStyle,
                                    size = 14.dp,
                                    alpha = previewAlpha,
                                    special = SpecialBlockType.ColumnClearer,
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .border(
                                            width = 1.dp,
                                            color = uiColors.boardEmptyCellBorder,
                                            shape = RoundedCornerShape(6.dp),
                                        ),
                                )
                            }
                        } else {
                            BlockCellPreview(
                                tone = tone,
                                palette = settings.blockColorPalette,
                                style = boardStyle,
                                size = 14.dp,
                                special = if (row == 2 && column == 2) SpecialBlockType.Ghost else SpecialBlockType.None,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxPreview(
    colors: List<Color>,
    icon: ImageVector? = null,
    size: androidx.compose.ui.unit.Dp = 12.dp,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        colors.forEachIndexed { index, color ->
            Card(
                modifier = Modifier.size(size),
                colors = CardDefaults.cardColors(containerColor = color),
                shape = RoundedCornerShape((size.value * 0.35f).dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
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
        settings = AppSettings(),
        onSettingsChange = {},
        onBack = {},
    )
}
