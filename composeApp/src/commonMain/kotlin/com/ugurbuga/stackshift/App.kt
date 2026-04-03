package com.ugurbuga.stackshift

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.ugurbuga.stackshift.game.model.AppColorPalette
import com.ugurbuga.stackshift.settings.AppSettings
import com.ugurbuga.stackshift.settings.AppSettingsStorage
import com.ugurbuga.stackshift.localization.AppEnvironment
import com.ugurbuga.stackshift.ui.game.AppSettingsScreen
import com.ugurbuga.stackshift.ui.game.StackShiftGameApp

@Composable
fun StackShiftTheme(
    settings: AppSettings,
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = stackShiftColorScheme(settings), content = content)
}

@Composable
@Preview
fun App() {
    var settings by remember { mutableStateOf(AppSettings()) }
    var showSettings by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        settings = AppSettingsStorage.load()
    }
    AppEnvironment(settings = settings) {
        StackShiftTheme(settings = settings) {
            if (showSettings) {
                AppSettingsScreen(
                    settings = settings,
                    onSettingsChange = { updated ->
                        settings = updated
                        AppSettingsStorage.save(updated)
                    },
                    onBack = { showSettings = false },
                )
            } else {
                StackShiftGameApp(onOpenSettings = { showSettings = true })
            }
        }
    }
}

@Composable
private fun stackShiftColorScheme(settings: AppSettings) = when (settings.themeMode.isDark ?: isSystemInDarkTheme()) {
    true -> darkColorScheme(
        primary = palettePrimary(settings.themeColorPalette, darkTheme = true),
        secondary = paletteSecondary(settings.themeColorPalette, darkTheme = true),
        tertiary = paletteTertiary(settings.themeColorPalette, darkTheme = true),
        background = Color(0xFF070B14),
        surface = Color(0xFF12182A),
        onPrimary = Color(0xFF04110A),
        onBackground = Color(0xFFF3F6FF),
        onSurface = Color(0xFFF3F6FF),
    )
    false -> lightColorScheme(
        primary = palettePrimary(settings.themeColorPalette, darkTheme = false),
        secondary = paletteSecondary(settings.themeColorPalette, darkTheme = false),
        tertiary = paletteTertiary(settings.themeColorPalette, darkTheme = false),
        background = Color(0xFFF7F9FF),
        surface = Color(0xFFFFFFFF),
        onPrimary = Color(0xFF0F172A),
        onBackground = Color(0xFF182033),
        onSurface = Color(0xFF182033),
    )
}

private fun palettePrimary(palette: AppColorPalette, darkTheme: Boolean): Color = when (palette) {
    AppColorPalette.Classic -> if (darkTheme) Color(0xFF7CFFB2) else Color(0xFFA8F0CF)
    AppColorPalette.Aurora -> if (darkTheme) Color(0xFF14A3B8) else Color(0xFF8FDDE8)
    AppColorPalette.Sunset -> if (darkTheme) Color(0xFFE16D4A) else Color(0xFFF1B08C)
}

private fun paletteSecondary(palette: AppColorPalette, darkTheme: Boolean): Color = when (palette) {
    AppColorPalette.Classic -> if (darkTheme) Color(0xFF9B8CFF) else Color(0xFFC0B5FF)
    AppColorPalette.Aurora -> if (darkTheme) Color(0xFF6C63FF) else Color(0xFFA3A0FF)
    AppColorPalette.Sunset -> if (darkTheme) Color(0xFF8B5CF6) else Color(0xFFC8A4FF)
}

private fun paletteTertiary(palette: AppColorPalette, darkTheme: Boolean): Color = when (palette) {
    AppColorPalette.Classic -> if (darkTheme) Color(0xFFFFD166) else Color(0xFFFFE0A3)
    AppColorPalette.Aurora -> if (darkTheme) Color(0xFF33A884) else Color(0xFF8CD9BC)
    AppColorPalette.Sunset -> if (darkTheme) Color(0xFFF0A72E) else Color(0xFFF6C56B)
}