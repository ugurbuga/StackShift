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
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ugurbuga.stackshift.ads.NoOpGameAdController
import com.ugurbuga.stackshift.ads.rememberPlatformGameAdController
import com.ugurbuga.stackshift.settings.AppSettings
import com.ugurbuga.stackshift.settings.AppSettingsStorage
import com.ugurbuga.stackshift.settings.FirstRunGameOnboardingStateFactory
import com.ugurbuga.stackshift.settings.GameSessionStorage
import com.ugurbuga.stackshift.game.model.GameState
import com.ugurbuga.stackshift.localization.AppEnvironment
import com.ugurbuga.stackshift.localization.currentDeviceLocaleTag
import com.ugurbuga.stackshift.telemetry.AppTelemetry
import com.ugurbuga.stackshift.ui.game.AppSettingsScreen
import com.ugurbuga.stackshift.ui.game.GameTutorialScreen
import com.ugurbuga.stackshift.ui.game.StackShiftGameApp
import com.ugurbuga.stackshift.presentation.game.GameViewModel
import com.ugurbuga.stackshift.settings.initializeAppSettingsForFirstLaunch
import com.ugurbuga.stackshift.settings.logLanguageBootstrapDecision
import com.ugurbuga.stackshift.telemetry.TelemetryActionNames
import com.ugurbuga.stackshift.telemetry.TelemetryUserPropertyNames
import com.ugurbuga.stackshift.telemetry.rememberAppTelemetry
import com.ugurbuga.stackshift.ui.theme.LocalStackShiftUiColors
import com.ugurbuga.stackshift.ui.theme.isStackShiftDarkTheme
import com.ugurbuga.stackshift.ui.theme.stackShiftTypography
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
        val typography = stackShiftTypography()
        MaterialTheme(
            colorScheme = themeSpec.colorScheme,
            typography = typography,
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
    showInteractiveOnboarding: Boolean,
    onShowSettingsChange: (Boolean) -> Unit,
    onShowTutorialChange: (Boolean) -> Unit,
    onShowInteractiveOnboardingChange: (Boolean) -> Unit,
    onSettingsChange: (AppSettings) -> Unit,
    onTutorialFinished: () -> Unit,
    onInteractiveOnboardingFinished: (GameState) -> Unit,
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
            } else if (showTutorial) {
                GameTutorialScreen(
                    telemetry = telemetry,
                    onFinish = {
                        onTutorialFinished()
                        onShowTutorialChange(false)
                    },
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    StackShiftGameApp(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        telemetry = telemetry,
                        viewModel = gameViewModel,
                        interactiveOnboardingEnabled = showInteractiveOnboarding,
                        onInteractiveOnboardingFinished = { finalState ->
                            onInteractiveOnboardingFinished(finalState)
                            onShowInteractiveOnboardingChange(false)
                        },
                        adController = adController,
                        onOpenSettings = {
                            telemetry.logUserAction(TelemetryActionNames.OpenSettings)
                            onShowSettingsChange(true)
                        },
                        onOpenTutorial = {
                            telemetry.logUserAction(TelemetryActionNames.OpenTutorial)
                            onShowTutorialChange(true)
                        },
                    )
                    if (adController !== NoOpGameAdController) {
                        adController.Banner(
                            modifier = Modifier
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
    val telemetry = rememberAppTelemetry()
    val initialBootstrapResult = remember {
        initializeAppSettingsForFirstLaunch(
            settings = AppSettingsStorage.load(),
            deviceLocaleTag = currentDeviceLocaleTag(),
        )
    }
    val initialShowInteractiveOnboarding = remember(initialBootstrapResult) {
        !initialBootstrapResult.settings.hasShownInteractiveOnboarding
    }
    var settings by remember(initialBootstrapResult) { mutableStateOf(initialBootstrapResult.settings) }
    var showSettings by remember { mutableStateOf(false) }
    var showTutorial by remember { mutableStateOf(false) }
    var showInteractiveOnboarding by remember { mutableStateOf(false) }
    val persistActiveSession = remember { mutableStateOf(!initialShowInteractiveOnboarding) }
    fun createGameViewModel(initialState: GameState?) = GameViewModel(
        initialState = initialState,
        onStateChanged = { state ->
            if (persistActiveSession.value) {
                GameSessionStorage.save(state)
            }
        },
    )
    var gameViewModel by remember {
        mutableStateOf(
            createGameViewModel(
                GameSessionStorage.load(),
            )
        )
    }
    fun startInteractiveOnboarding() {
        persistActiveSession.value = false
        gameViewModel = createGameViewModel(FirstRunGameOnboardingStateFactory.initialState())
        showInteractiveOnboarding = true
    }
    LaunchedEffect(initialBootstrapResult, telemetry) {
        logLanguageBootstrapDecision(
            source = "app_common",
            result = initialBootstrapResult,
            telemetry = telemetry,
        )
        if (initialBootstrapResult.shouldPersist) {
            AppSettingsStorage.save(initialBootstrapResult.settings)
        }
    }
    LaunchedEffect(initialShowInteractiveOnboarding) {
        if (!initialShowInteractiveOnboarding || showInteractiveOnboarding) return@LaunchedEffect
        startInteractiveOnboarding()
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
        showInteractiveOnboarding = showInteractiveOnboarding,
        onShowSettingsChange = { showSettings = it },
        onShowTutorialChange = { showTutorial = it },
        onShowInteractiveOnboardingChange = { shouldShow ->
            if (shouldShow && !showInteractiveOnboarding) {
                startInteractiveOnboarding()
                return@StackShiftRoot
            }
            showInteractiveOnboarding = shouldShow
        },
        onSettingsChange = { updated ->
            val shouldLogSettingsChange = updated.copy(
                hasSeenTutorial = settings.hasSeenTutorial,
                hasShownInteractiveOnboarding = settings.hasShownInteractiveOnboarding,
                hasInitializedLanguage = settings.hasInitializedLanguage,
            ) != settings
            settings = updated
            AppSettingsStorage.save(updated)
            if (shouldLogSettingsChange) {
                telemetry.logUserAction(TelemetryActionNames.SettingsChanged)
            }
        },
        onTutorialFinished = {
            if (!settings.hasSeenTutorial) {
                val updatedSettings = settings.copy(hasSeenTutorial = true)
                settings = updatedSettings
                AppSettingsStorage.save(updatedSettings)
            }
        },
        onInteractiveOnboardingFinished = { finalState ->
            persistActiveSession.value = true
            GameSessionStorage.save(finalState)
            if (!settings.hasShownInteractiveOnboarding) {
                val updatedSettings = settings.copy(hasShownInteractiveOnboarding = true)
                settings = updatedSettings
                AppSettingsStorage.save(updatedSettings)
            }
        },
    )
    LaunchedEffect(settings) {
        telemetry.logUserProperty(TelemetryUserPropertyNames.Language, settings.language.localeTag)
        telemetry.logUserProperty(TelemetryUserPropertyNames.ThemeMode, settings.themeMode.name)
        telemetry.logUserProperty(TelemetryUserPropertyNames.ThemePalette, settings.themeColorPalette.name)
        telemetry.logUserProperty(TelemetryUserPropertyNames.BlockPalette, settings.blockColorPalette.name)
        telemetry.logUserProperty(TelemetryUserPropertyNames.BlockStyle, settings.blockVisualStyle.name)
        telemetry.logUserProperty(TelemetryUserPropertyNames.BoardBlockStyleMode, settings.boardBlockStyleMode.name)
    }
}


