package com.ugurbuga.stackshift

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.ugurbuga.stackshift.game.model.GameState
import com.ugurbuga.stackshift.localization.currentDeviceLocaleTag
import com.ugurbuga.stackshift.settings.FirstRunGameOnboardingStateFactory
import com.ugurbuga.stackshift.presentation.game.GameViewModel
import com.ugurbuga.stackshift.settings.AppSettingsStorage
import com.ugurbuga.stackshift.settings.GameSessionStorage
import com.ugurbuga.stackshift.settings.initializeAppSettingsForFirstLaunch
import com.ugurbuga.stackshift.settings.logLanguageBootstrapDecision
import com.ugurbuga.stackshift.telemetry.TelemetryActionNames
import com.ugurbuga.stackshift.telemetry.TelemetryUserPropertyNames
import com.ugurbuga.stackshift.telemetry.rememberAppTelemetry
import com.ugurbuga.stackshift.ui.theme.isStackShiftDarkTheme
import com.ugurbuga.stackshift.ui.theme.stackShiftThemeSpec

@Composable
fun AndroidApp() {
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
    val gameViewModelState = remember {
        mutableStateOf(
            createGameViewModel(
                GameSessionStorage.load()
            )
        )
    }
    fun startInteractiveOnboarding() {
        persistActiveSession.value = false
        gameViewModelState.value = createGameViewModel(FirstRunGameOnboardingStateFactory.initialState())
        showInteractiveOnboarding = true
    }
    val gameViewModel = gameViewModelState.value

    LaunchedEffect(initialBootstrapResult, telemetry) {
        logLanguageBootstrapDecision(
            source = "android_app",
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
        showInteractiveOnboarding = showInteractiveOnboarding,
        onShowSettingsChange = { showSettings = it },
        onShowTutorialChange = { showTutorial = it },
        onShowInteractiveOnboardingChange = { shouldShow ->
            if (shouldShow && !showInteractiveOnboarding) {
                startInteractiveOnboarding()
            } else {
                showInteractiveOnboarding = shouldShow
            }
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
}

