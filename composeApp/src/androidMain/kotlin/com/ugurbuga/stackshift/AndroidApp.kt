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

@Composable
fun AndroidApp() {
    var settings by remember { mutableStateOf(AppSettings()) }
    var showSettings by remember { mutableStateOf(false) }
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

    AndroidSystemBarsEffect(darkTheme = isStackShiftDarkTheme(settings))

    StackShiftRoot(
        settings = settings,
        telemetry = telemetry,
        gameViewModel = gameViewModel,
        showSettings = showSettings,
        onShowSettingsChange = { showSettings = it },
        onSettingsChange = { updated ->
            settings = updated
            AppSettingsStorage.save(updated)
            telemetry.logUserAction(TelemetryActionNames.SettingsChanged)
        }
    )
}

