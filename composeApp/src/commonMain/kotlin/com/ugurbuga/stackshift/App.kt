package com.ugurbuga.stackshift

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.ugurbuga.stackshift.ads.AppFooterAdSlot
import com.ugurbuga.stackshift.ads.rememberPlatformGameAdController
import com.ugurbuga.stackshift.game.model.GameConfig
import com.ugurbuga.stackshift.game.model.GameState
import com.ugurbuga.stackshift.game.model.GameStatus
import com.ugurbuga.stackshift.game.model.SpecialBlockType
import com.ugurbuga.stackshift.localization.AppEnvironment
import com.ugurbuga.stackshift.localization.currentDeviceLocaleTag
import com.ugurbuga.stackshift.platform.feedback.GameSound
import com.ugurbuga.stackshift.platform.feedback.rememberSoundEffectPlayer
import com.ugurbuga.stackshift.platform.getCurrentDate
import com.ugurbuga.stackshift.platform.rememberNotificationManager
import com.ugurbuga.stackshift.presentation.game.GameViewModel
import com.ugurbuga.stackshift.settings.AppSettings
import com.ugurbuga.stackshift.settings.AppSettingsStorage
import com.ugurbuga.stackshift.settings.FirstRunGameOnboardingStateFactory
import com.ugurbuga.stackshift.settings.GameSessionStorage
import com.ugurbuga.stackshift.settings.HighScoreStorage
import com.ugurbuga.stackshift.settings.RewardedTokenAdReward
import com.ugurbuga.stackshift.settings.awardBonusTokens
import com.ugurbuga.stackshift.settings.awardCompletedChallenge
import com.ugurbuga.stackshift.settings.awardScoreTokens
import com.ugurbuga.stackshift.settings.initializeAppSettingsForFirstLaunch
import com.ugurbuga.stackshift.settings.logLanguageBootstrapDecision
import com.ugurbuga.stackshift.settings.sanitized
import com.ugurbuga.stackshift.telemetry.AppTelemetry
import com.ugurbuga.stackshift.telemetry.TelemetryActionNames
import com.ugurbuga.stackshift.telemetry.TelemetryUserPropertyNames
import com.ugurbuga.stackshift.telemetry.rememberAppTelemetry
import com.ugurbuga.stackshift.ui.game.AppLanguageScreen
import com.ugurbuga.stackshift.ui.game.AppSettingsScreen
import com.ugurbuga.stackshift.ui.game.DailyChallengeScreen
import com.ugurbuga.stackshift.ui.game.GameTutorialScreen
import com.ugurbuga.stackshift.ui.game.HomeScreen
import com.ugurbuga.stackshift.ui.game.RewardFeedbackCard
import com.ugurbuga.stackshift.ui.game.StackShiftGameApp
import com.ugurbuga.stackshift.ui.game.ThemedConfirmDialog
import com.ugurbuga.stackshift.ui.theme.LocalStackShiftUiColors
import com.ugurbuga.stackshift.ui.theme.isStackShiftDarkTheme
import com.ugurbuga.stackshift.ui.theme.stackShiftThemeSpec
import com.ugurbuga.stackshift.ui.theme.stackShiftTypography
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import stackshift.composeapp.generated.resources.Res
import stackshift.composeapp.generated.resources.cancel
import stackshift.composeapp.generated.resources.leave_session_confirm
import stackshift.composeapp.generated.resources.leave_session_confirm_body
import stackshift.composeapp.generated.resources.leave_session_confirm_title
import stackshift.composeapp.generated.resources.reward_earned_message
import stackshift.composeapp.generated.resources.reward_piece_column_message
import stackshift.composeapp.generated.resources.reward_piece_row_message
import kotlin.time.Duration.Companion.milliseconds

enum class AppRoute {
    Home,
    Game,
    InteractiveOnboarding,
    Settings,
    Language,
    Tutorial,
    DailyChallenges,
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

data class RewardFeedbackState(
    val messageRes: StringResource? = null,
    val messageArgs: List<Any> = emptyList(),
    val icon: ImageVector = Icons.Filled.Stars,
    val visible: Boolean = false
)

@Composable
fun StackShiftRoot(
    settings: AppSettings,
    telemetry: AppTelemetry,
    gameViewModel: GameViewModel,
    currentRoute: AppRoute,
    highestScore: Int,
    rewardFeedback: RewardFeedbackState,
    onRewardFeedbackDismiss: () -> Unit,
    onPlayRequested: () -> Unit,
    onNavigateToInteractiveOnboarding: () -> Unit,
    onNavigateToTheme: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToTutorial: () -> Unit,
    onNavigateToChallenges: () -> Unit,
    onNavigateBack: () -> Unit,
    onSettingsChange: (AppSettings) -> Unit,
    onRewardedTokensEarned: () -> Unit,
    onReplaceActivePieceRewarded: (SpecialBlockType) -> Unit,
    onTutorialFinishRequested: () -> Unit,
    onInteractiveOnboardingFinished: (GameState) -> Unit,
    onInteractiveOnboardingReturnHome: (GameState) -> Unit,
    showLeaveSessionDialog: Boolean = false,
    onDismissLeaveSessionDialog: () -> Unit = {},
    onConfirmLeaveSessionDialog: () -> Unit = {},
) {
    val adController = rememberPlatformGameAdController()
    val notificationManager = rememberNotificationManager()
    val soundPlayer = rememberSoundEffectPlayer(settings.soundEnabled)

    LaunchedEffect(currentRoute, settings.soundEnabled) {
        if (!settings.soundEnabled || currentRoute == AppRoute.Game || currentRoute == AppRoute.InteractiveOnboarding) {
            soundPlayer.stop(GameSound.Bgm)
        } else {
            soundPlayer.play(GameSound.Bgm)
        }
    }

    AppEnvironment(settings = settings) {
        LaunchedEffect(Unit) {
            notificationManager.scheduleMissYouNotification()
            notificationManager.scheduleDailyChallengeNotification()
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
                                    onOpenChallenges = {
                                        onNavigateToChallenges()
                                    },
                                    notificationManager = notificationManager,
                                )
                            }

                            AppRoute.DailyChallenges -> {
                                val date = remember { getCurrentDate() }
                                DailyChallengeScreen(
                                    currentYear = date.year,
                                    currentMonth = date.month,
                                    currentDay = date.day,
                                    progress = settings.challengeProgress,
                                    onBack = onNavigateBack,
                                    onPlayChallenge = { challenge ->
                                        gameViewModel.restart(GameConfig(), challenge)
                                        onPlayRequested()
                                    }
                                )
                            }

                            AppRoute.Settings -> {
                                AppSettingsScreen(
                                    modifier = Modifier.fillMaxSize(),
                                    telemetry = telemetry,
                                    settings = settings,
                                    onSettingsChange = onSettingsChange,
                                    onRewardedTokensRequested = onRewardedTokensEarned,
                                    onBack = onNavigateBack,
                                    adController = adController,
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
                                    onBack = onNavigateBack,
                                    onFinish = onTutorialFinishRequested,
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
                                    onReplaceActivePieceRewarded = onReplaceActivePieceRewarded,
                                    onBack = onNavigateBack,
                                    adController = adController,
                                    soundPlayer = soundPlayer,
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

                RewardFeedbackCard(
                    message = if (rewardFeedback.messageRes != null) {
                        stringResource(
                            rewardFeedback.messageRes,
                            *rewardFeedback.messageArgs.toTypedArray()
                        )
                    } else "",
                    icon = rewardFeedback.icon,
                    visible = rewardFeedback.visible,
                    onDismiss = onRewardFeedbackDismiss
                )
            }
        }
    }
}

@Composable
@Preview
fun App() {
    StackShiftAppHost(bootstrapLogSource = "app_common")
}

@Composable
fun StackShiftAppHost(
    bootstrapLogSource: String,
    beforeRoot: @Composable (settings: AppSettings, canNavigateBack: Boolean, onRequestBack: () -> Unit) -> Unit = { _, _, _ -> },
) {
    val telemetry = rememberAppTelemetry()
    val initialBootstrapResult = remember {
        initializeAppSettingsForFirstLaunch(
            settings = AppSettingsStorage.load(),
            deviceLocaleTag = currentDeviceLocaleTag(),
        )
    }
    var settings by remember(initialBootstrapResult) { mutableStateOf(initialBootstrapResult.settings.sanitized()) }

    var rewardFeedback by remember { mutableStateOf(RewardFeedbackState()) }

    var routeStack by remember { mutableStateOf(listOf(AppRoute.Home)) }
    var showLeaveSessionDialog by remember { mutableStateOf(false) }
    val persistActiveSession = remember { mutableStateOf(true) }
    var pendingSessionState by remember { mutableStateOf<GameState?>(null) }
    fun persistSettings(updated: AppSettings) {
        val sanitizedSettings = updated.sanitized()
        settings = sanitizedSettings
        AppSettingsStorage.save(sanitizedSettings)
    }

    fun createGameViewModel(initialState: GameState?) = GameViewModel(
        initialState = initialState,
        onStateChanged = { state ->
            if (persistActiveSession.value) {
                pendingSessionState = state
            }
        },
        onChallengeCompleted = { completedChallenge ->
            persistSettings(settings.awardCompletedChallenge(completedChallenge))
        },
        onGameOver = { finalState ->
            persistSettings(settings.awardScoreTokens(finalState.score))
        },
    )

    val gameViewModelState =
        remember { mutableStateOf(createGameViewModel(GameSessionStorage.load())) }
    var gameViewModel by gameViewModelState

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
            gameViewModel = createGameViewModel(GameSessionStorage.load())
        }
    }

    fun navigateHome() {
        showLeaveSessionDialog = false
        routeStack = listOf(AppRoute.Home)
    }

    fun requestBackNavigation() {
        val isGameOver = gameViewModel.snapshotState().status == GameStatus.GameOver
        if ((currentRoute == AppRoute.Game || currentRoute == AppRoute.InteractiveOnboarding) && !isGameOver) {
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

    fun completeInteractiveOnboarding(finalState: GameState, returnHome: Boolean) {
        persistActiveSession.value = true
        pendingSessionState = finalState
        GameSessionStorage.save(finalState)
        if (!settings.hasShownInteractiveOnboarding) {
            persistSettings(settings.copy(hasShownInteractiveOnboarding = true))
        }
        if (returnHome) {
            navigateHome()
        } else {
            replaceTop(AppRoute.Game)
        }
    }

    LaunchedEffect(initialBootstrapResult, telemetry) {
        logLanguageBootstrapDecision(
            source = bootstrapLogSource,
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

    beforeRoot(settings, canNavigateBack, ::requestBackNavigation)

    StackShiftRoot(
        settings = settings,
        telemetry = telemetry,
        gameViewModel = gameViewModel,
        currentRoute = currentRoute,
        highestScore = HighScoreStorage.load(),
        rewardFeedback = rewardFeedback,
        onRewardFeedbackDismiss = { rewardFeedback = rewardFeedback.copy(visible = false) },
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
        onNavigateToChallenges = { navigateTo(AppRoute.DailyChallenges) },
        onNavigateBack = ::requestBackNavigation,
        onSettingsChange = { updated ->
            val shouldLogSettingsChange = updated.copy(
                hasSeenTutorial = settings.hasSeenTutorial,
                hasShownInteractiveOnboarding = settings.hasShownInteractiveOnboarding,
                hasInitializedLanguage = settings.hasInitializedLanguage,
            ) != settings
            persistSettings(updated)
            if (shouldLogSettingsChange) {
                telemetry.logUserAction(TelemetryActionNames.SettingsChanged)
            }
        },
        onRewardedTokensEarned = {
            persistSettings(settings.awardBonusTokens(RewardedTokenAdReward))
            rewardFeedback = RewardFeedbackState(
                messageRes = Res.string.reward_earned_message,
                messageArgs = listOf(RewardedTokenAdReward),
                icon = Icons.Filled.Stars,
                visible = true
            )
        },
        onReplaceActivePieceRewarded = { specialType ->
            telemetry.logUserAction("replace_active_piece_${specialType.name.lowercase()}")
            gameViewModel.replaceActivePiece(specialType)
            rewardFeedback = RewardFeedbackState(
                messageRes = if (specialType == SpecialBlockType.RowClearer) Res.string.reward_piece_row_message
                else Res.string.reward_piece_column_message,
                icon = if (specialType == SpecialBlockType.RowClearer) Icons.Filled.SwapHoriz else Icons.Filled.SwapVert,
                visible = true
            )
        },
        onTutorialFinishRequested = {
            if (!settings.hasSeenTutorial) {
                persistSettings(settings.copy(hasSeenTutorial = true))
            }
            telemetry.logUserAction(TelemetryActionNames.OpenInteractiveOnboarding)
            prepareInteractiveOnboarding()
            replaceTop(AppRoute.InteractiveOnboarding)
        },
        onInteractiveOnboardingFinished = { finalState ->
            completeInteractiveOnboarding(finalState = finalState, returnHome = false)
        },
        onInteractiveOnboardingReturnHome = { finalState ->
            completeInteractiveOnboarding(finalState = finalState, returnHome = true)
        },
        showLeaveSessionDialog = showLeaveSessionDialog,
        onDismissLeaveSessionDialog = { showLeaveSessionDialog = false },
        onConfirmLeaveSessionDialog = ::navigateBack,
    )
    LaunchedEffect(settings) {
        telemetry.logUserProperty(TelemetryUserPropertyNames.Language, settings.language.localeTag)
        telemetry.logUserProperty(TelemetryUserPropertyNames.ThemeMode, settings.themeMode.name)
        telemetry.logUserProperty(
            TelemetryUserPropertyNames.ThemePalette,
            settings.themeColorPalette.name
        )
        telemetry.logUserProperty(
            TelemetryUserPropertyNames.BlockPalette,
            settings.blockColorPalette.name
        )
        telemetry.logUserProperty(
            TelemetryUserPropertyNames.BlockStyle,
            settings.blockVisualStyle.name
        )
        telemetry.logUserProperty(
            TelemetryUserPropertyNames.BoardBlockStyleMode,
            settings.boardBlockStyleMode.name
        )
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


