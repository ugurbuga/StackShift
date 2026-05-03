package com.ugurbuga.blockgames

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.platform.GlobalPlatformConfig
import com.ugurbuga.blockgames.ui.theme.blockGamesThemeSpec
import com.ugurbuga.blockgames.ui.theme.isBlockGamesDarkTheme

@Composable
fun AndroidApp() {
    val gameplayStyle = resolveGameplayStyle()
    GlobalPlatformConfig.gameplayStyle = gameplayStyle
    BlockGamesAppHost(
        bootstrapLogSource = "android_app_${BuildConfig.APP_VARIANT_NAME}",
        gameplayStyle = gameplayStyle,
    ) { settings, canNavigateBack, onRequestBack ->
        val darkTheme = isBlockGamesDarkTheme(settings)
        val themeSpec = blockGamesThemeSpec(
            settings = settings,
            darkTheme = darkTheme,
        )
        AndroidSystemBarsEffect(
            darkTheme = darkTheme,
            navigationBarColor = themeSpec.uiColors.screenGradientBottom,
            windowBackgroundColor = themeSpec.uiColors.screenGradientBottom,
        )
        BackHandler(enabled = canNavigateBack, onBack = onRequestBack)
    }
}

private fun resolveGameplayStyle(): GameplayStyle = when (BuildConfig.GAMEPLAY_STYLE) {
    GameplayStyle.BlockWise.name -> GameplayStyle.BlockWise
    else -> GameplayStyle.StackShift
}


