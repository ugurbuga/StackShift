package com.ugurbuga.stackshift.app

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import com.ugurbuga.stackshift.AndroidSystemBarsEffect
import com.ugurbuga.stackshift.StackShiftAppHost
import com.ugurbuga.stackshift.ui.theme.isStackShiftDarkTheme
import com.ugurbuga.stackshift.ui.theme.stackShiftThemeSpec

@Composable
fun AndroidApp() {
    StackShiftAppHost(bootstrapLogSource = "android_app") { settings, canNavigateBack, onRequestBack ->
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
        BackHandler(enabled = canNavigateBack, onBack = onRequestBack)
    }
}

