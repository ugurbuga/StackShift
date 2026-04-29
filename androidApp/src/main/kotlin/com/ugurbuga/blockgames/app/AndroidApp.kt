package com.ugurbuga.blockgames.app

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import com.ugurbuga.stackshift.AndroidSystemBarsEffect
import com.ugurbuga.stackshift.StackShiftAppHost
import com.ugurbuga.stackshift.game.model.GameplayStyle
import com.ugurbuga.stackshift.platform.GlobalPlatformConfig
import com.ugurbuga.stackshift.ui.theme.blockGamesThemeSpec
import com.ugurbuga.stackshift.ui.theme.isBlockGamesDarkTheme

@Composable
fun AndroidApp() {
    val gameplayStyle = resolveGameplayStyle()
    GlobalPlatformConfig.gameplayStyle = gameplayStyle
    StackShiftAppHost(
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


