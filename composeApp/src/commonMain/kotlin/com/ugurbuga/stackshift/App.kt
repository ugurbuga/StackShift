package com.ugurbuga.stackshift

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.tooling.preview.Preview
import com.ugurbuga.stackshift.ads.NoOpGameAdController
import com.ugurbuga.stackshift.ads.rememberPlatformGameAdController
import com.ugurbuga.stackshift.settings.AppSettings
import com.ugurbuga.stackshift.settings.AppSettingsStorage
import com.ugurbuga.stackshift.settings.GameSessionStorage
import com.ugurbuga.stackshift.localization.AppEnvironment
import com.ugurbuga.stackshift.telemetry.AppTelemetry
import com.ugurbuga.stackshift.ui.game.AppSettingsScreen
import com.ugurbuga.stackshift.ui.game.GameTutorialScreen
import com.ugurbuga.stackshift.ui.game.StackShiftGameApp
import com.ugurbuga.stackshift.presentation.game.GameViewModel
import com.ugurbuga.stackshift.telemetry.TelemetryActionNames
import com.ugurbuga.stackshift.telemetry.TelemetryUserPropertyNames
import com.ugurbuga.stackshift.telemetry.rememberAppTelemetry
import com.ugurbuga.stackshift.ui.theme.LocalStackShiftUiColors
import com.ugurbuga.stackshift.ui.theme.isStackShiftDarkTheme
import com.ugurbuga.stackshift.ui.theme.stackShiftThemeSpec

@Composable
fun StackShiftTheme(
    settings: AppSettings,
    content: @Composable () -> Unit,
) {
    val darkTheme = isStackShiftDarkTheme(settings)
    val themeSpec = stackShiftThemeSpec(
        settings = settings,
        darkTheme = darkTheme,
    )
    PlatformSystemBarsEffect(darkTheme = darkTheme)
    CompositionLocalProvider(LocalStackShiftUiColors provides themeSpec.uiColors) {
        MaterialTheme(
            colorScheme = themeSpec.colorScheme,
            content = content,
        )
    }
}

@Composable
fun StackShiftRoot(
    settings: AppSettings,
    telemetry: AppTelemetry,
    gameViewModel: GameViewModel,
    showSettings: Boolean,
    showTutorial: Boolean,
    onShowSettingsChange: (Boolean) -> Unit,
    onShowTutorialChange: (Boolean) -> Unit,
    onSettingsChange: (AppSettings) -> Unit,
) {
    val adController = rememberPlatformGameAdController()

    AppEnvironment(settings = settings) {
        StackShiftTheme(settings = settings) {
            if (showSettings) {
                AppSettingsScreen(
                    telemetry = telemetry,
                    settings = settings,
                    onSettingsChange = onSettingsChange,
                    onReplayTutorial = {
                        onShowSettingsChange(false)
                        onShowTutorialChange(true)
                    },
                    onBack = { onShowSettingsChange(false) },
                )
            } else if (showTutorial || !settings.hasSeenTutorial) {
                GameTutorialScreen(
                    telemetry = telemetry,
                    onFinish = {
                        onShowTutorialChange(false)
                        if (!settings.hasSeenTutorial) {
                            onSettingsChange(settings.copy(hasSeenTutorial = true))
                        }
                    },
                )
            } else {
                Column(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
                    StackShiftGameApp(
                        modifier = androidx.compose.ui.Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        telemetry = telemetry,
                        viewModel = gameViewModel,
                        adController = adController,
                        onOpenSettings = {
                            telemetry.logUserAction(TelemetryActionNames.OpenSettings)
                            onShowSettingsChange(true)
                        },
                    )
                    if (adController !== NoOpGameAdController) {
                        adController.Banner(
                            modifier = androidx.compose.ui.Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                        )
                    }
                }
            }
        }
    }
}

@Composable
@Preview
fun App() {
    var settings by remember { mutableStateOf(AppSettings()) }
    var showSettings by remember { mutableStateOf(false) }
    var showTutorial by remember { mutableStateOf(false) }
    val telemetry = rememberAppTelemetry()
    val gameViewModel = remember {
        GameViewModel(
            initialState = GameSessionStorage.load(),
            onStateChanged = GameSessionStorage::save,
        )
    }
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
    androidx.compose.runtime.DisposableEffect(gameViewModel) {
        onDispose(gameViewModel::dispose)
    }
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
        },
    )
}
