package com.ugurbuga.stackshift.app

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.ugurbuga.stackshift.AndroidSystemBarsEffect
import com.ugurbuga.stackshift.AppRoute
import com.ugurbuga.stackshift.StackShiftRoot
import com.ugurbuga.stackshift.game.model.GameState
import com.ugurbuga.stackshift.localization.currentDeviceLocaleTag
import com.ugurbuga.stackshift.settings.FirstRunGameOnboardingStateFactory
import com.ugurbuga.stackshift.presentation.game.GameViewModel
import com.ugurbuga.stackshift.settings.AppSettingsStorage
import com.ugurbuga.stackshift.settings.GameSessionStorage
import com.ugurbuga.stackshift.settings.HighScoreStorage
import com.ugurbuga.stackshift.settings.initializeAppSettingsForFirstLaunch
import com.ugurbuga.stackshift.settings.logLanguageBootstrapDecision
import com.ugurbuga.stackshift.telemetry.TelemetryActionNames
import com.ugurbuga.stackshift.telemetry.TelemetryUserPropertyNames
import com.ugurbuga.stackshift.telemetry.rememberAppTelemetry
import com.ugurbuga.stackshift.ui.theme.isStackShiftDarkTheme
import com.ugurbuga.stackshift.ui.theme.stackShiftThemeSpec
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun AndroidApp() {
    val telemetry = rememberAppTelemetry()
    val initialBootstrapResult = remember {
        initializeAppSettingsForFirstLaunch(
            settings = AppSettingsStorage.load(),
            deviceLocaleTag = currentDeviceLocaleTag(),
        )
    }
    var settings by remember(initialBootstrapResult) { mutableStateOf(initialBootstrapResult.settings) }
    var routeStack by remember { mutableStateOf(listOf(AppRoute.Home)) }
    var showLeaveSessionDialog by remember { mutableStateOf(false) }
    val persistActiveSession = remember { mutableStateOf(true) }
    var pendingSessionState by remember { mutableStateOf<GameState?>(null) }
    fun createGameViewModel(initialState: GameState?) = GameViewModel(
        initialState = initialState,
        onStateChanged = { state ->
            if (persistActiveSession.value) {
                pendingSessionState = state
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
    val currentRoute = routeStack.lastOrNull() ?: AppRoute.Home
    val canNavigateBack = routeStack.size > 1
    fun navigateTo(route: AppRoute) {
        if (routeStack.lastOrNull() == route) return
        routeStack = routeStack + route
    }
    fun replaceTop(route: AppRoute) {
        routeStack = if (routeStack.isEmpty()) {
            listOf(route)
        } else {
            routeStack.dropLast(1) + route
        }
    }
    fun navigateBack() {
        if (routeStack.size <= 1) return
        showLeaveSessionDialog = false
        val leavingRoute = routeStack.lastOrNull()
        routeStack = routeStack.dropLast(1)
        if (leavingRoute == AppRoute.InteractiveOnboarding) {
            persistActiveSession.value = true
            pendingSessionState = null
            gameViewModelState.value = createGameViewModel(GameSessionStorage.load())
        }
    }
    fun navigateHome() {
        showLeaveSessionDialog = false
        routeStack = listOf(AppRoute.Home)
    }
    fun requestBackNavigation() {
        if (currentRoute == AppRoute.Game || currentRoute == AppRoute.InteractiveOnboarding) {
            showLeaveSessionDialog = true
        } else {
            navigateBack()
        }
    }
    fun prepareInteractiveOnboarding() {
        persistActiveSession.value = false
        pendingSessionState = null
        gameViewModelState.value = createGameViewModel(FirstRunGameOnboardingStateFactory.initialState())
    }
    fun startPlayFlow() {
        if (!settings.hasShownInteractiveOnboarding) {
            prepareInteractiveOnboarding()
            navigateTo(AppRoute.InteractiveOnboarding)
        } else {
            persistActiveSession.value = true
            navigateTo(AppRoute.Game)
        }
    }
    fun openInteractiveOnboarding() {
        prepareInteractiveOnboarding()
        navigateTo(AppRoute.InteractiveOnboarding)
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

    LaunchedEffect(persistActiveSession.value, pendingSessionState) {
        val state = pendingSessionState ?: return@LaunchedEffect
        if (!persistActiveSession.value) return@LaunchedEffect
        delay(350.milliseconds)
        if (persistActiveSession.value && pendingSessionState == state) {
            GameSessionStorage.save(state)
        }
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
    val themeSpec = stackShiftThemeSpec(
        settings = settings,
        darkTheme = darkTheme,
    )
    AndroidSystemBarsEffect(
        darkTheme = darkTheme,
        navigationBarColor = themeSpec.uiColors.screenGradientBottom,
        windowBackgroundColor = themeSpec.uiColors.screenGradientBottom,
    )
    BackHandler(enabled = canNavigateBack) {
        requestBackNavigation()
    }

    StackShiftRoot(
        settings = settings,
        telemetry = telemetry,
        gameViewModel = gameViewModel,
        currentRoute = currentRoute,
        highestScore = HighScoreStorage.load(),
        onPlayRequested = {
            telemetry.logUserAction(TelemetryActionNames.StartGameFromHome)
            startPlayFlow()
        },
        onNavigateToInteractiveOnboarding = {
            telemetry.logUserAction(TelemetryActionNames.OpenInteractiveOnboarding)
            openInteractiveOnboarding()
        },
        onNavigateToTheme = { navigateTo(AppRoute.Settings) },
        onNavigateToLanguage = { navigateTo(AppRoute.Language) },
        onNavigateToTutorial = { navigateTo(AppRoute.Tutorial) },
        onNavigateBack = ::requestBackNavigation,
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
            pendingSessionState = finalState
            GameSessionStorage.save(finalState)
            if (!settings.hasShownInteractiveOnboarding) {
                val updatedSettings = settings.copy(hasShownInteractiveOnboarding = true)
                settings = updatedSettings
                AppSettingsStorage.save(updatedSettings)
            }
            replaceTop(AppRoute.Game)
        },
        onInteractiveOnboardingReturnHome = { finalState ->
            persistActiveSession.value = true
            pendingSessionState = finalState
            GameSessionStorage.save(finalState)
            if (!settings.hasShownInteractiveOnboarding) {
                val updatedSettings = settings.copy(hasShownInteractiveOnboarding = true)
                settings = updatedSettings
                AppSettingsStorage.save(updatedSettings)
            }
            navigateHome()
        },
        showLeaveSessionDialog = showLeaveSessionDialog,
        onDismissLeaveSessionDialog = { showLeaveSessionDialog = false },
        onConfirmLeaveSessionDialog = ::navigateBack,
    )
}

