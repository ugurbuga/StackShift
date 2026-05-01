package com.ugurbuga.blockgames

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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import blockgames.composeapp.generated.resources.Res
import blockgames.composeapp.generated.resources.cancel
import blockgames.composeapp.generated.resources.leave_session_confirm
import blockgames.composeapp.generated.resources.leave_session_confirm_body
import blockgames.composeapp.generated.resources.leave_session_confirm_title
import blockgames.composeapp.generated.resources.reward_earned_message
import blockgames.composeapp.generated.resources.reward_piece_column_message
import blockgames.composeapp.generated.resources.reward_piece_row_message
import com.ugurbuga.blockgames.ads.AppFooterAdSlot
import com.ugurbuga.blockgames.ads.rememberPlatformGameAdController
import com.ugurbuga.blockgames.game.model.DailyChallenge
import com.ugurbuga.blockgames.game.model.GameConfig
import com.ugurbuga.blockgames.game.model.GameMode
import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.game.model.GameStatus
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.SpecialBlockType
import com.ugurbuga.blockgames.localization.AppEnvironment
import com.ugurbuga.blockgames.localization.appStringResource
import com.ugurbuga.blockgames.localization.currentDeviceLocaleTag
import com.ugurbuga.blockgames.platform.GlobalPlatformConfig
import com.ugurbuga.blockgames.platform.currentEpochMillis
import com.ugurbuga.blockgames.platform.feedback.GameSound
import com.ugurbuga.blockgames.platform.feedback.rememberSoundEffectPlayer
import com.ugurbuga.blockgames.platform.getCurrentDate
import com.ugurbuga.blockgames.platform.rememberNotificationManager
import com.ugurbuga.blockgames.presentation.game.GameViewModel
import com.ugurbuga.blockgames.settings.AppSettings
import com.ugurbuga.blockgames.settings.AppSettingsStorage
import com.ugurbuga.blockgames.settings.BlockWiseOnboardingStateFactory
import com.ugurbuga.blockgames.settings.GameSessionSlot
import com.ugurbuga.blockgames.settings.GameSessionStorage
import com.ugurbuga.blockgames.settings.HighScoreStorage
import com.ugurbuga.blockgames.settings.RewardedTokenAdReward
import com.ugurbuga.blockgames.settings.StackShiftGameOnboardingStateFactory
import com.ugurbuga.blockgames.settings.awardBonusTokens
import com.ugurbuga.blockgames.settings.awardCompletedChallenge
import com.ugurbuga.blockgames.settings.awardScoreTokens
import com.ugurbuga.blockgames.settings.initializeAppSettingsForFirstLaunch
import com.ugurbuga.blockgames.settings.logLanguageBootstrapDecision
import com.ugurbuga.blockgames.settings.recordAppOpened
import com.ugurbuga.blockgames.settings.sanitized
import com.ugurbuga.blockgames.settings.sessionSlot
import com.ugurbuga.blockgames.settings.sessionSlotFor
import com.ugurbuga.blockgames.telemetry.AppTelemetry
import com.ugurbuga.blockgames.telemetry.TelemetryActionNames
import com.ugurbuga.blockgames.telemetry.TelemetryUserPropertyNames
import com.ugurbuga.blockgames.telemetry.rememberAppTelemetry
import com.ugurbuga.blockgames.ui.game.settings.AppSettingsScreen
import com.ugurbuga.blockgames.ui.game.game.BlockGamesGameApp
import com.ugurbuga.blockgames.ui.game.gametutorial.GameTutorialScreen
import com.ugurbuga.blockgames.ui.game.home.HomeScreen
import com.ugurbuga.blockgames.ui.game.RewardFeedbackCard
import com.ugurbuga.blockgames.ui.game.ThemedConfirmDialog
import com.ugurbuga.blockgames.ui.game.dailychallenge.DailyChallengeScreen
import com.ugurbuga.blockgames.ui.game.settings.AppLanguageScreen
import com.ugurbuga.blockgames.ui.theme.LocalBlockGamesUiColors
import com.ugurbuga.blockgames.ui.theme.blockGamesThemeSpec
import com.ugurbuga.blockgames.ui.theme.blockGamesTypography
import com.ugurbuga.blockgames.ui.theme.isBlockGamesDarkTheme
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
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
fun BlockGamesTheme(
    settings: AppSettings,
    content: @Composable () -> Unit,
) {
    val darkTheme = isBlockGamesDarkTheme(settings)
    val themeSpec = blockGamesThemeSpec(
        settings = settings,
        darkTheme = darkTheme,
    )
    PlatformSystemBarsEffect(darkTheme = darkTheme)
    CompositionLocalProvider(LocalBlockGamesUiColors provides themeSpec.uiColors) {
        val typography = blockGamesTypography()
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
fun BlockGamesRoot(
    settings: AppSettings,
    telemetry: AppTelemetry,
    gameViewModel: GameViewModel,
    classicHighScore: Int,
    timeAttackHighScore: Int,
    rewardFeedback: RewardFeedbackState,
    onRewardFeedbackDismiss: () -> Unit,
    onPlayRequested: () -> Unit,
    onTimeAttackRequested: () -> Unit,
    onPlayChallengeRequested: (DailyChallenge) -> Unit,
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
    currentRoute: AppRoute = AppRoute.Home,
    gameplayStyle: GameplayStyle = GlobalPlatformConfig.gameplayStyle,
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
            onSettingsChange(settings.recordAppOpened(currentEpochMillis()))
        }
        LaunchedEffect(settings.challengeProgress, settings.lastAppOpenedAtEpochMillis) {
            notificationManager.cancelDailyChallengeNotification()
            notificationManager.scheduleDailyChallengeNotification()
            notificationManager.cancelMissYouNotification()
            notificationManager.scheduleMissYouNotification()
        }
        notificationManager.RequestPermission()
        BlockGamesTheme(settings = settings) {
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
                                    classicHighScore = classicHighScore,
                                    timeAttackHighScore = timeAttackHighScore,
                                    gameplayStyle = gameplayStyle,
                                    telemetry = telemetry,
                                    onPlay = onPlayRequested,
                                    onPlayTimeAttack = onTimeAttackRequested,
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
                                    gameplayStyle = gameplayStyle,
                                    progress = settings.challengeProgress,
                                    onBack = onNavigateBack,
                                    onPlayChallenge = onPlayChallengeRequested,
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
                                    gameplayStyle = gameplayStyle,
                                    onBack = onNavigateBack,
                                    onFinish = onTutorialFinishRequested,
                                    adController = adController,
                                )
                            }

                            AppRoute.Game,
                            AppRoute.InteractiveOnboarding,
                                -> {
                                BlockGamesGameApp(
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
                        appStringResource(
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
    BlockGamesAppHost(bootstrapLogSource = "app_common")
}

@Composable
fun BlockGamesAppHost(
    bootstrapLogSource: String,
    gameplayStyle: GameplayStyle = GlobalPlatformConfig.gameplayStyle,
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

    fun isUsableSavedSession(
        state: GameState,
        slot: GameSessionSlot,
    ): Boolean {
        if (state.config.columns <= 0 || state.config.rows <= 0) return false
        if (slot == GameSessionSlot.DailyChallenge && state.activeChallenge == null) return false
        if (state.status != GameStatus.Running) return true
        return when (state.gameplayStyle) {
            GameplayStyle.BlockWise -> state.trayPieces.isNotEmpty()
            GameplayStyle.StackShift -> state.activePiece != null
        }
    }

    fun loadSavedSession(slot: GameSessionSlot): GameState? {
        val savedState = GameSessionStorage.load(slot) ?: return null
        if (savedState.gameplayStyle != gameplayStyle || !isUsableSavedSession(savedState, slot)) {
            GameSessionStorage.clear(slot)
            return null
        }
        return savedState
    }

    val gameViewModelState = remember { mutableStateOf(createGameViewModel(initialState = null)) }
    var gameViewModel by gameViewModelState

    fun restoreOrRestartSession(
        slot: GameSessionSlot,
        fallback: () -> Unit,
    ) {
        persistActiveSession.value = true
        val savedState = loadSavedSession(slot)
        if (savedState != null) {
            gameViewModel.replaceState(savedState)
        } else {
            fallback()
        }
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
            gameViewModel = createGameViewModel(loadSavedSession(GameSessionSlot.Classic))
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
        val initialState = if (gameplayStyle == GameplayStyle.BlockWise) {
            BlockWiseOnboardingStateFactory.initialState()
        } else {
            StackShiftGameOnboardingStateFactory.initialState(gameplayStyle)
        }
        gameViewModel = createGameViewModel(initialState)
    }

    fun startPlayFlow(
        mode: GameMode = GameMode.Classic,
    ) {
        val sessionSlot = sessionSlotFor(mode = mode)
        if (mode == GameMode.Classic && !settings.hasSeenTutorial) {
            navigateTo(AppRoute.Tutorial)
        } else if (mode == GameMode.Classic && !settings.hasShownInteractiveOnboarding) {
            prepareInteractiveOnboarding()
            navigateTo(AppRoute.InteractiveOnboarding)
        } else {
            restoreOrRestartSession(slot = sessionSlot) {
                gameViewModel.restart(mode = mode, gameplayStyle = gameplayStyle)
            }
            navigateTo(AppRoute.Game)
        }
    }


    fun completeInteractiveOnboarding(finalState: GameState, returnHome: Boolean) {
        persistActiveSession.value = true
        if (!settings.hasShownInteractiveOnboarding) {
            persistSettings(settings.copy(hasShownInteractiveOnboarding = true))
        }
        if (returnHome) {
            navigateHome()
        } else {
            telemetry.logUserAction(TelemetryActionNames.StartGameFromHome)
            restoreOrRestartSession(slot = GameSessionSlot.Classic) {
                gameViewModel.restart(mode = GameMode.Classic, gameplayStyle = gameplayStyle)
            }
            replaceTop(AppRoute.Game)
        }
    }

    var isResetReady by remember(initialBootstrapResult) {
        mutableStateOf(initialBootstrapResult.settings.isHighScoresClearedOnce)
    }

    LaunchedEffect(initialBootstrapResult, telemetry) {
        if (!isResetReady) {
            GameplayStyle.entries.forEach { gs ->
                HighScoreStorage.save(0, GameMode.Classic, gs)
                HighScoreStorage.save(0, GameMode.TimeAttack, gs)
            }
            GameSessionStorage.clear()
            persistSettings(settings.copy(isHighScoresClearedOnce = true))
            isResetReady = true
        }

        logLanguageBootstrapDecision(
            source = bootstrapLogSource,
            result = initialBootstrapResult,
            telemetry = telemetry,
        )
        if (initialBootstrapResult.shouldPersist) {
            AppSettingsStorage.save(settings)
        }
    }
    LaunchedEffect(persistActiveSession.value, pendingSessionState) {
        val state = pendingSessionState ?: return@LaunchedEffect
        if (!persistActiveSession.value) return@LaunchedEffect
        delay(350.milliseconds)
        if (persistActiveSession.value && (pendingSessionState == state)) {
            GameSessionStorage.save(state.sessionSlot(), state)
        }
    }
    DisposableEffect(gameViewModel) {
        onDispose(gameViewModel::dispose)
    }

    if (!isResetReady) return

    beforeRoot(settings, canNavigateBack, ::requestBackNavigation)

    BlockGamesRoot(
        settings = settings,
        telemetry = telemetry,
        gameViewModel = gameViewModel,
        classicHighScore = HighScoreStorage.load(GameMode.Classic, gameplayStyle),
        timeAttackHighScore = HighScoreStorage.load(GameMode.TimeAttack, gameplayStyle),
        rewardFeedback = rewardFeedback,
        onRewardFeedbackDismiss = { rewardFeedback = rewardFeedback.copy(visible = false) },
        onPlayRequested = {
            telemetry.logUserAction(TelemetryActionNames.StartGameFromHome)
            startPlayFlow(GameMode.Classic)
        },
        onTimeAttackRequested = {
            telemetry.logUserAction(TelemetryActionNames.StartTimeAttackFromHome)
            startPlayFlow(GameMode.TimeAttack)
        },
        onNavigateToTheme = { navigateTo(AppRoute.Settings) },
        onPlayChallengeRequested = { challenge ->
            restoreOrRestartSession(slot = GameSessionSlot.DailyChallenge) {
                gameViewModel.restart(
                    config = GameConfig(),
                    challenge = challenge,
                    mode = GameMode.Classic,
                    gameplayStyle = gameplayStyle,
                )
            }
            val restoredChallenge = gameViewModel.snapshotState().activeChallenge
            val isMatchingChallenge = restoredChallenge?.let {
                it.year == challenge.year && it.month == challenge.month && it.day == challenge.day
            } == true
            if (!isMatchingChallenge) {
                gameViewModel.restart(
                    config = GameConfig(),
                    challenge = challenge,
                    mode = GameMode.Classic,
                    gameplayStyle = gameplayStyle,
                )
            }
            navigateTo(AppRoute.Game)
        },
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
        currentRoute = currentRoute,
        gameplayStyle = gameplayStyle,
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
        title = appStringResource(Res.string.leave_session_confirm_title),
        message = appStringResource(Res.string.leave_session_confirm_body),
        confirmLabel = appStringResource(Res.string.leave_session_confirm),
        dismissLabel = appStringResource(Res.string.cancel),
        onConfirm = onConfirm,
        icon = Icons.AutoMirrored.Filled.ArrowBack,
        dismissButtonIcon = Icons.Filled.Close,
    )
}


