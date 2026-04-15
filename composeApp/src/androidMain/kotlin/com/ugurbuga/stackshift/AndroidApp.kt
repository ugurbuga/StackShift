package com.ugurbuga.stackshift

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.ugurbuga.stackshift.presentation.game.GameViewModel
import com.ugurbuga.stackshift.settings.AppSettings
import com.ugurbuga.stackshift.settings.AppSettingsStorage
import com.ugurbuga.stackshift.settings.GameSessionStorage
import com.ugurbuga.stackshift.telemetry.TelemetryActionNames
import com.ugurbuga.stackshift.telemetry.TelemetryUserPropertyNames
import com.ugurbuga.stackshift.telemetry.rememberAppTelemetry
import com.ugurbuga.stackshift.ui.theme.isStackShiftDarkTheme
import com.ugurbuga.stackshift.ui.theme.stackShiftThemeSpec

@Composable
fun AndroidApp() {
    var settings by remember { mutableStateOf(AppSettings()) }
    var showSettings by remember { mutableStateOf(false) }
    var showTutorial by remember { mutableStateOf(false) }
    val telemetry = rememberAppTelemetry()
    val gameViewModelState = remember {
        mutableStateOf(
            GameViewModel(
                initialState = GameSessionStorage.load(),
                onStateChanged = GameSessionStorage::save,
            ),
        )
    }
    val gameViewModel = gameViewModelState.value

    LaunchedEffect(Unit) {
        settings = AppSettingsStorage.load()
    }

    LaunchedEffect(settings) {
        telemetry.logUserProperty(TelemetryUserPropertyNames.Language, settings.language.localeTag)
        telemetry.logUserProperty(TelemetryUserPropertyNames.ThemeMode, settings.themeMode.name)
        telemetry.logUserProperty(TelemetryUserPropertyNames.ThemePalette, settings.themeColorPalette.name)
        telemetry.logUserProperty(TelemetryUserPropertyNames.BlockPalette, settings.blockColorPalette.name)
        telemetry.logUserProperty(TelemetryUserPropertyNames.BlockStyle, settings.blockVisualStyle.name)
        telemetry.logUserProperty(TelemetryUserPropertyNames.BoardBlockStyleMode, settings.boardBlockStyleMode.name)
    }

    DisposableEffect(gameViewModel) {
        onDispose(gameViewModel::dispose)
    }

    val darkTheme = isStackShiftDarkTheme(settings)
    val systemBarColor = stackShiftThemeSpec(settings = settings, darkTheme = darkTheme).uiColors.screenGradientBottom
    AndroidSystemBarsEffect(
        darkTheme = darkTheme,
        navigationBarColor = systemBarColor,
    )

    StackShiftRoot(
        settings = settings,
        telemetry = telemetry,
        gameViewModel = gameViewModel,
        showSettings = showSettings,
        showTutorial = showTutorial,
        onShowSettingsChange = { showSettings = it },
        onShowTutorialChange = { showTutorial = it },
        onSettingsChange = { updated ->
            val shouldLogSettingsChange = updated.copy(hasSeenTutorial = settings.hasSeenTutorial) != settings
            settings = updated
            AppSettingsStorage.save(updated)
            if (shouldLogSettingsChange) {
                telemetry.logUserAction(TelemetryActionNames.SettingsChanged)
            }
        }
    )
}

