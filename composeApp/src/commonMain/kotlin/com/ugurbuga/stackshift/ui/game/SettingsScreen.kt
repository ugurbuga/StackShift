package com.ugurbuga.stackshift.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Icon
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import com.ugurbuga.stackshift.game.model.AppLanguage
import com.ugurbuga.stackshift.game.model.AppColorPalette
import com.ugurbuga.stackshift.game.model.AppThemeMode
import com.ugurbuga.stackshift.game.model.BlockColorPalette
import com.ugurbuga.stackshift.game.model.BlockVisualStyle
import com.ugurbuga.stackshift.game.model.BoardBlockStyleMode
import com.ugurbuga.stackshift.settings.AppSettings
import com.ugurbuga.stackshift.settings.AppSettingsStorage
import org.jetbrains.compose.resources.stringResource
import stackshift.composeapp.generated.resources.Res
import stackshift.composeapp.generated.resources.app_language_english
import stackshift.composeapp.generated.resources.app_language_turkish
import stackshift.composeapp.generated.resources.app_theme_dark
import stackshift.composeapp.generated.resources.app_theme_light
import stackshift.composeapp.generated.resources.app_theme_system
import stackshift.composeapp.generated.resources.theme_palette_aurora
import stackshift.composeapp.generated.resources.theme_palette_classic
import stackshift.composeapp.generated.resources.theme_palette_sunset
import stackshift.composeapp.generated.resources.block_palette_classic
import stackshift.composeapp.generated.resources.block_palette_candy
import stackshift.composeapp.generated.resources.block_palette_earth
import stackshift.composeapp.generated.resources.block_palette_neon
import stackshift.composeapp.generated.resources.block_style_bubble
import stackshift.composeapp.generated.resources.block_style_flat
import stackshift.composeapp.generated.resources.block_style_liquid_glass
import stackshift.composeapp.generated.resources.block_style_neon
import stackshift.composeapp.generated.resources.block_style_outline
import stackshift.composeapp.generated.resources.block_style_sharp_3d
import stackshift.composeapp.generated.resources.block_style_wood
import stackshift.composeapp.generated.resources.board_block_style_always_flat
import stackshift.composeapp.generated.resources.board_block_style_match_selected
import stackshift.composeapp.generated.resources.settings_block_palette
import stackshift.composeapp.generated.resources.settings_block_style
import stackshift.composeapp.generated.resources.settings_board_block_style
import stackshift.composeapp.generated.resources.settings_language
import stackshift.composeapp.generated.resources.settings_theme
import stackshift.composeapp.generated.resources.settings_theme_palette
import stackshift.composeapp.generated.resources.settings_title

private val ScreenContentMaxWidth = 920.dp
private val ChipShape = RoundedCornerShape(22.dp)

@Composable
fun AppSettingsScreen(
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = Color(0xFF070B14)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().width(ScreenContentMaxWidth),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                }
                Spacer(Modifier.size(8.dp))
                Text(text = stringResource(Res.string.settings_title), style = MaterialTheme.typography.titleLarge, color = Color.White)
            }
            SettingsGroup(
                title = stringResource(Res.string.settings_language),
                selectedValue = settings.language,
                options = languageOptions(settings.language),
                onSelected = { language ->
                    val updated = settings.copy(language = language)
                    onSettingsChange(updated)
                    AppSettingsStorage.save(updated)
                },
            )
            SettingsGroup(
                title = stringResource(Res.string.settings_theme),
                selectedValue = settings.themeMode,
                options = themeModeOptions(),
                onSelected = { themeMode ->
                    val updated = settings.copy(themeMode = themeMode)
                    onSettingsChange(updated)
                    AppSettingsStorage.save(updated)
                },
            )
            SettingsGroup(
                title = stringResource(Res.string.settings_theme_palette),
                selectedValue = settings.themeColorPalette,
                options = themePaletteOptions(),
                onSelected = { palette ->
                    val updated = settings.copy(themeColorPalette = palette)
                    onSettingsChange(updated)
                    AppSettingsStorage.save(updated)
                },
            )
            SettingsGroup(
                title = stringResource(Res.string.settings_block_palette),
                selectedValue = settings.blockColorPalette,
                options = blockPaletteOptions(),
                onSelected = { palette ->
                    val updated = settings.copy(blockColorPalette = palette)
                    onSettingsChange(updated)
                    AppSettingsStorage.save(updated)
                },
            )
            SettingsGroup(
                title = stringResource(Res.string.settings_block_style),
                selectedValue = settings.blockVisualStyle,
                options = blockStyleOptions(),
                onSelected = { style ->
                    val updated = settings.copy(blockVisualStyle = style)
                    onSettingsChange(updated)
                    AppSettingsStorage.save(updated)
                },
            )
            SettingsGroup(
                title = stringResource(Res.string.settings_board_block_style),
                selectedValue = settings.boardBlockStyleMode,
                options = boardBlockStyleOptions(),
                onSelected = { mode ->
                    val updated = settings.copy(boardBlockStyleMode = mode)
                    onSettingsChange(updated)
                    AppSettingsStorage.save(updated)
                },
            )
        }
    }
}

@Composable
private fun <T> SettingsGroup(
    title: String,
    selectedValue: T,
    options: List<SettingsOption<T>>,
    onSelected: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = Color.White)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            options.forEach { option ->
                val selected = option.value == selectedValue
                FilterChip(
                    selected = selected,
                    onClick = { onSelected(option.value) },
                    shape = ChipShape,
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            option.preview?.invoke()
                            Text(text = option.label)
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF22305A),
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF12182A),
                        labelColor = Color.White.copy(alpha = 0.88f),
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
        preview = { LanguagePreview(selected = language == selected) },
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
private fun themePaletteOptions(): List<SettingsOption<AppColorPalette>> = AppColorPalette.entries.map { palette ->
    SettingsOption(
        value = palette,
        label = when (palette) {
            AppColorPalette.Classic -> stringResource(Res.string.theme_palette_classic)
            AppColorPalette.Aurora -> stringResource(Res.string.theme_palette_aurora)
            AppColorPalette.Sunset -> stringResource(Res.string.theme_palette_sunset)
        },
        preview = { ThemePalettePreview(palette) },
    )
}

@Composable
private fun blockPaletteOptions(): List<SettingsOption<BlockColorPalette>> = BlockColorPalette.entries.map { palette ->
    SettingsOption(
        value = palette,
        label = when (palette) {
            BlockColorPalette.Classic -> stringResource(Res.string.block_palette_classic)
            BlockColorPalette.Candy -> stringResource(Res.string.block_palette_candy)
            BlockColorPalette.Neon -> stringResource(Res.string.block_palette_neon)
            BlockColorPalette.Earth -> stringResource(Res.string.block_palette_earth)
        },
        preview = { BlockPalettePreview(palette) },
    )
}

@Composable
private fun blockStyleOptions(): List<SettingsOption<BlockVisualStyle>> = BlockVisualStyle.entries.map { style ->
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
        preview = { BlockStylePreview(style) },
    )
}

@Composable
private fun boardBlockStyleOptions(): List<SettingsOption<BoardBlockStyleMode>> = BoardBlockStyleMode.entries.map { mode ->
    SettingsOption(
        value = mode,
        label = when (mode) {
            BoardBlockStyleMode.AlwaysFlat -> stringResource(Res.string.board_block_style_always_flat)
            BoardBlockStyleMode.MatchSelectedBlockStyle -> stringResource(Res.string.board_block_style_match_selected)
        },
        preview = { BoardStylePreview(mode) },
    )
}

@Composable
private fun ThemeModePreview(mode: AppThemeMode) {
    val color = when (mode) {
        AppThemeMode.System -> Color(0xFF6AA7FF)
        AppThemeMode.Light -> Color(0xFFFFD166)
        AppThemeMode.Dark -> Color(0xFF9B8CFF)
    }
    BoxPreview(colors = listOf(color))
}

@Composable
private fun ThemePalettePreview(palette: AppColorPalette) {
    val colors = when (palette) {
        AppColorPalette.Classic -> listOf(Color(0xFF4F6DF5), Color(0xFF20B4A7), Color(0xFFC57E1B), Color(0xFFE3EAF7))
        AppColorPalette.Aurora -> listOf(Color(0xFF14A3B8), Color(0xFF6C63FF), Color(0xFF33A884), Color(0xFFD4F1FF))
        AppColorPalette.Sunset -> listOf(Color(0xFFE16D4A), Color(0xFF8B5CF6), Color(0xFFF0A72E), Color(0xFFF7E1D4))
    }
    BoxPreview(colors = colors)
}

@Composable
private fun BlockPalettePreview(palette: BlockColorPalette) {
    val colors = when (palette) {
        BlockColorPalette.Classic -> listOf(Color(0xFFFF6B81), Color(0xFF4DD599), Color(0xFF5B8FF9), Color(0xFFF7C948))
        BlockColorPalette.Candy -> listOf(Color(0xFFF77ACF), Color(0xFF68E3D1), Color(0xFF79C7FF), Color(0xFFF5B27A))
        BlockColorPalette.Neon -> listOf(Color(0xFFFF4FA3), Color(0xFF39FF88), Color(0xFF27C7FF), Color(0xFFFFF24D))
        BlockColorPalette.Earth -> listOf(Color(0xFFD86D45), Color(0xFF7BA24B), Color(0xFF4A8A8E), Color(0xFFDAA53A))
    }
    BoxPreview(colors = colors, icon = Icons.Filled.FitnessCenter)
}

@Composable
private fun BlockStylePreview(style: BlockVisualStyle) {
    val colors = when (style) {
        BlockVisualStyle.Flat -> listOf(Color(0xFFFF6B81), Color(0xFF5B8FF9), Color(0xFFF7C948))
        BlockVisualStyle.Bubble -> listOf(Color(0xFFFF7A90), Color(0xFF6AA7FF), Color(0xFFFFD166))
        BlockVisualStyle.Outline -> listOf(Color(0xFFFF6B81), Color(0xFF5B8FF9), Color(0xFFF7C948))
        BlockVisualStyle.Sharp3D -> listOf(Color(0xFFFF718B), Color(0xFF5B8FF9), Color(0xFFF1C84A))
        BlockVisualStyle.Wood -> listOf(Color(0xFFD86D45), Color(0xFF7BA24B), Color(0xFFDAA53A))
        BlockVisualStyle.LiquidGlass -> listOf(Color(0xFFFFA4B2), Color(0xFF8CB6FF), Color(0xFFF8E08A))
        BlockVisualStyle.Neon -> listOf(Color(0xFFFF4FA3), Color(0xFF39FF88), Color(0xFFFFF24D))
    }
    BoxPreview(colors = colors, icon = Icons.Filled.FitnessCenter)
}

@Composable
private fun BoardStylePreview(mode: BoardBlockStyleMode) {
    val colors = when (mode) {
        BoardBlockStyleMode.AlwaysFlat -> listOf(Color(0xFFE3EAF7), Color(0xFF5B8FF9))
        BoardBlockStyleMode.MatchSelectedBlockStyle -> listOf(Color(0xFFE3EAF7), Color(0xFF5B8FF9))
    }
    BoxPreview(colors = colors, icon = Icons.Filled.ViewModule)
}

@Composable
private fun LanguagePreview(selected: Boolean) {
    val color = if (selected) Color(0xFF9B8CFF) else Color(0xFF4F6DF5)
    BoxPreview(colors = listOf(color))
}

@Composable
private fun BoxPreview(
    colors: List<Color>,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        colors.forEachIndexed { index, color ->
            Card(
                modifier = Modifier.size(12.dp),
                colors = CardDefaults.cardColors(containerColor = color),
                shape = RoundedCornerShape(4.dp),
            ) {
                if (icon != null && index == colors.lastIndex) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(8.dp),
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
    AppSettingsScreen(settings = AppSettings(), onSettingsChange = {}, onBack = {})
}
