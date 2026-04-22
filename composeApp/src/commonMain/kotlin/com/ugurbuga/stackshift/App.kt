package com.ugurbuga.stackshift

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ugurbuga.stackshift.ads.AppFooterAdSlot
import com.ugurbuga.stackshift.ads.rememberPlatformGameAdController
import com.ugurbuga.stackshift.game.model.GameState
import com.ugurbuga.stackshift.localization.AppEnvironment
import com.ugurbuga.stackshift.localization.currentDeviceLocaleTag
import com.ugurbuga.stackshift.platform.rememberNotificationManager
import com.ugurbuga.stackshift.presentation.game.GameViewModel
import com.ugurbuga.stackshift.settings.AppSettings
import com.ugurbuga.stackshift.settings.AppSettingsStorage
import com.ugurbuga.stackshift.settings.FirstRunGameOnboardingStateFactory
import com.ugurbuga.stackshift.settings.GameSessionStorage
import com.ugurbuga.stackshift.settings.HighScoreStorage
import com.ugurbuga.stackshift.settings.initializeAppSettingsForFirstLaunch
import com.ugurbuga.stackshift.settings.logLanguageBootstrapDecision
import com.ugurbuga.stackshift.telemetry.AppTelemetry
import com.ugurbuga.stackshift.telemetry.TelemetryActionNames
import com.ugurbuga.stackshift.telemetry.TelemetryUserPropertyNames
import com.ugurbuga.stackshift.telemetry.rememberAppTelemetry
import com.ugurbuga.stackshift.ui.game.AppLanguageScreen
import com.ugurbuga.stackshift.ui.game.AppSettingsScreen
import com.ugurbuga.stackshift.ui.game.GameTutorialScreen
import com.ugurbuga.stackshift.ui.game.HomeScreen
import com.ugurbuga.stackshift.ui.game.StackShiftGameApp
import com.ugurbuga.stackshift.ui.game.ThemedConfirmDialog
import com.ugurbuga.stackshift.ui.theme.LocalStackShiftUiColors
import com.ugurbuga.stackshift.ui.theme.isStackShiftDarkTheme
import com.ugurbuga.stackshift.ui.theme.stackShiftThemeSpec
import com.ugurbuga.stackshift.ui.theme.stackShiftTypography
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import stackshift.composeapp.generated.resources.Res
import stackshift.composeapp.generated.resources.cancel
import stackshift.composeapp.generated.resources.leave_session_confirm
import stackshift.composeapp.generated.resources.leave_session_confirm_body
import stackshift.composeapp.generated.resources.leave_session_confirm_title
import kotlin.time.Duration.Companion.milliseconds

enum class AppRoute {
    Home,
    Game,
    InteractiveOnboarding,
    Settings,
    Language,
    Tutorial,
}

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
    currentRoute: AppRoute,
    highestScore: Int,
    onPlayRequested: () -> Unit,
    onNavigateToInteractiveOnboarding: () -> Unit,
    onNavigateToTheme: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToTutorial: () -> Unit,
    onNavigateBack: () -> Unit,
    onSettingsChange: (AppSettings) -> Unit,
    onTutorialFinished: () -> Unit,
    onInteractiveOnboardingFinished: (GameState) -> Unit,
    onInteractiveOnboardingReturnHome: (GameState) -> Unit,
    showLeaveSessionDialog: Boolean = false,
    onDismissLeaveSessionDialog: () -> Unit = {},
    onConfirmLeaveSessionDialog: () -> Unit = {},
) {
    val adController = rememberPlatformGameAdController()
    val notificationManager = rememberNotificationManager()

    AppEnvironment(settings = settings) {
        LaunchedEffect(Unit) {
            notificationManager.scheduleMissYouNotification()
        }
        notificationManager.RequestPermission()
        StackShiftTheme(settings = settings) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) {
                        when (currentRoute) {
                            AppRoute.Home -> {
                                HomeScreen(
                                    modifier = Modifier.fillMaxSize(),
                                    settings = settings,
                                    highestScore = highestScore,
                                    telemetry = telemetry,
                                    onPlay = onPlayRequested,
                                    onOpenInteractiveGuide = onNavigateToInteractiveOnboarding,
                                    onOpenTutorial = {
                                        telemetry.logUserAction(TelemetryActionNames.OpenTutorial)
                                        onNavigateToTutorial()
                                    },
                                    onOpenTheme = {
                                        telemetry.logUserAction(TelemetryActionNames.OpenTheme)
                                        onNavigateToTheme()
                                    },
                                    onOpenLanguage = {
                                        telemetry.logUserAction(TelemetryActionNames.OpenLanguage)
                                        onNavigateToLanguage()
                                    },
                                    notificationManager = notificationManager,
                                )
                            }

                            AppRoute.Settings -> {
                                AppSettingsScreen(
                                    modifier = Modifier.fillMaxSize(),
                                    telemetry = telemetry,
                                    settings = settings,
                                    onSettingsChange = onSettingsChange,
                                    onBack = onNavigateBack,
                                )
                            }

                            AppRoute.Language -> {
                                AppLanguageScreen(
                                    modifier = Modifier.fillMaxSize(),
                                    telemetry = telemetry,
                                    settings = settings,
                                    onSettingsChange = onSettingsChange,
                                    onBack = onNavigateBack,
                                )
                            }

                            AppRoute.Tutorial -> {
                                GameTutorialScreen(
                                    modifier = Modifier.fillMaxSize(),
                                    telemetry = telemetry,
                                    onFinish = {
                                        onTutorialFinished()
                                        onNavigateBack()
                                    },
                                    adController = adController,
                                )
                            }

                            AppRoute.Game,
                            AppRoute.InteractiveOnboarding,
                            -> {
                                StackShiftGameApp(
                                    modifier = Modifier.fillMaxSize(),
                                    telemetry = telemetry,
                                    viewModel = gameViewModel,
                                    interactiveOnboardingEnabled = currentRoute == AppRoute.InteractiveOnboarding,
                                    onInteractiveOnboardingFinished = { finalState ->
                                        onInteractiveOnboardingFinished(finalState)
                                    },
                                        onInteractiveOnboardingReturnHome = onInteractiveOnboardingReturnHome,
                                    onBack = onNavigateBack,
                                    adController = adController,
                                )
                            }
                        }
                    }

                    AppFooterAdSlot(adController = adController)
                }

                if (showLeaveSessionDialog) {
                    LeaveSessionConfirmDialog(
                        onDismissRequest = onDismissLeaveSessionDialog,
                        onConfirm = onConfirmLeaveSessionDialog,
                    )
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
                GameSessionStorage.load(),
            )
        )
    }
    var gameViewModel by gameViewModelState
    val currentRoute = routeStack.lastOrNull() ?: AppRoute.Home
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
            gameViewModel = createGameViewModel(GameSessionStorage.load())
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
        gameViewModel = createGameViewModel(FirstRunGameOnboardingStateFactory.initialState())
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
    LaunchedEffect(persistActiveSession.value, pendingSessionState) {
        val state = pendingSessionState ?: return@LaunchedEffect
        if (!persistActiveSession.value) return@LaunchedEffect
        delay(350.milliseconds)
        if (persistActiveSession.value && (pendingSessionState == state)) {
            GameSessionStorage.save(state)
        }
    }
    androidx.compose.runtime.DisposableEffect(gameViewModel) {
        onDispose(gameViewModel::dispose)
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
    LaunchedEffect(settings) {
        telemetry.logUserProperty(TelemetryUserPropertyNames.Language, settings.language.localeTag)
        telemetry.logUserProperty(TelemetryUserPropertyNames.ThemeMode, settings.themeMode.name)
        telemetry.logUserProperty(TelemetryUserPropertyNames.ThemePalette, settings.themeColorPalette.name)
        telemetry.logUserProperty(TelemetryUserPropertyNames.BlockPalette, settings.blockColorPalette.name)
        telemetry.logUserProperty(TelemetryUserPropertyNames.BlockStyle, settings.blockVisualStyle.name)
        telemetry.logUserProperty(TelemetryUserPropertyNames.BoardBlockStyleMode, settings.boardBlockStyleMode.name)
    }
}

@Composable
fun LeaveSessionConfirmDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    ThemedConfirmDialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(Res.string.leave_session_confirm_title),
        message = stringResource(Res.string.leave_session_confirm_body),
        confirmLabel = stringResource(Res.string.leave_session_confirm),
        dismissLabel = stringResource(Res.string.cancel),
        onConfirm = onConfirm,
        icon = Icons.AutoMirrored.Filled.ArrowBack,
        dismissButtonIcon = Icons.Filled.Close,
    )
}


