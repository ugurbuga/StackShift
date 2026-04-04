package com.ugurbuga.stackshift

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.ugurbuga.stackshift.settings.AppSettings
import com.ugurbuga.stackshift.settings.AppSettingsStorage
import com.ugurbuga.stackshift.localization.AppEnvironment
import com.ugurbuga.stackshift.ui.game.AppSettingsScreen
import com.ugurbuga.stackshift.ui.game.StackShiftGameApp
import com.ugurbuga.stackshift.ui.theme.LocalStackShiftUiColors
import com.ugurbuga.stackshift.ui.theme.isStackShiftDarkTheme
import com.ugurbuga.stackshift.ui.theme.stackShiftThemeSpec

@Composable
fun StackShiftTheme(
    settings: AppSettings,
    content: @Composable () -> Unit,
) {
    val themeSpec = stackShiftThemeSpec(
        settings = settings,
        darkTheme = isStackShiftDarkTheme(settings),
    )
    CompositionLocalProvider(LocalStackShiftUiColors provides themeSpec.uiColors) {
        MaterialTheme(
            colorScheme = themeSpec.colorScheme,
            content = content,
        )
    }
}

@Composable
@Preview
fun App() {
    var settings by remember { mutableStateOf(AppSettings()) }
    var showSettings by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        settings = AppSettingsStorage.load()
    }
    AppEnvironment(settings = settings) {
        StackShiftTheme(settings = settings) {
            if (showSettings) {
                AppSettingsScreen(
                    settings = settings,
                    onSettingsChange = { updated ->
                        settings = updated
                        AppSettingsStorage.save(updated)
                    },
                    onBack = { showSettings = false },
                )
            } else {
                StackShiftGameApp(onOpenSettings = { showSettings = true })
            }
        }
    }
}
