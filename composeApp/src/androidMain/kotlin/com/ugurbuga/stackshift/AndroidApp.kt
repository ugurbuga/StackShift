package com.ugurbuga.stackshift

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.ugurbuga.stackshift.localization.AppEnvironment
import com.ugurbuga.stackshift.presentation.game.GameViewModel
import com.ugurbuga.stackshift.settings.AppSettings
import com.ugurbuga.stackshift.settings.AppSettingsStorage
import com.ugurbuga.stackshift.settings.GameSessionStorage
import com.ugurbuga.stackshift.ui.game.AppSettingsScreen
import com.ugurbuga.stackshift.ui.game.StackShiftGameApp
import com.ugurbuga.stackshift.ui.theme.isStackShiftDarkTheme

@Composable
fun AndroidApp() {
    var settings by remember { mutableStateOf(AppSettings()) }
    var showSettings by remember { mutableStateOf(false) }
    val gameViewModel = remember {
        GameViewModel(
            initialState = GameSessionStorage.load(),
            onStateChanged = GameSessionStorage::save,
        )
    }

    LaunchedEffect(Unit) {
        settings = AppSettingsStorage.load()
    }

    DisposableEffect(gameViewModel) {
        onDispose(gameViewModel::dispose)
    }

    AndroidSystemBarsEffect(darkTheme = isStackShiftDarkTheme(settings))

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
                StackShiftGameApp(
                    viewModel = gameViewModel,
                    onOpenSettings = { showSettings = true },
                )
            }
        }
    }
}

