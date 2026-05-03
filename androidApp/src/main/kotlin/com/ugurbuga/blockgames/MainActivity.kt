package com.ugurbuga.blockgames

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.drawable.toDrawable
import com.ugurbuga.blockgames.ads.AndroidAdMobIds
import com.ugurbuga.blockgames.platform.GlobalPlatformConfig
import com.ugurbuga.blockgames.settings.AppContextHolder
import com.ugurbuga.blockgames.settings.AppSettingsStorage
import com.ugurbuga.blockgames.ui.theme.blockGamesThemeSpec

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        GlobalPlatformConfig.isDebug = BuildConfig.DEBUG
        AppContextHolder.context = applicationContext
        AndroidAdMobIds.BannerAdUnitId = BuildConfig.ADS_BANNER_UNIT_ID
        AndroidAdMobIds.InterstitialAdUnitId = BuildConfig.ADS_INTERSTITIAL_UNIT_ID
        AndroidAdMobIds.RewardedReviveAdUnitId = BuildConfig.ADS_REWARDED_UNIT_ID
        AndroidAdMobIds.RewardedSpecialAdUnitId = BuildConfig.ADS_REWARDED_SPECIAL_UNIT_ID

        val initialSettings = AppSettingsStorage.load()
        val initialDarkTheme = initialSettings.themeMode.isDark ?: isSystemDarkTheme()
        val initialThemeSpec = blockGamesThemeSpec(
            settings = initialSettings,
            darkTheme = initialDarkTheme,
        )

        setTheme(R.style.Theme_BlockGames)
        enableEdgeToEdge(
            statusBarStyle = if (initialDarkTheme) {
                SystemBarStyle.dark(Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(
                    Color.TRANSPARENT,
                    Color.TRANSPARENT,
                )
            },
            navigationBarStyle = if (initialDarkTheme) {
                SystemBarStyle.dark(Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(
                    Color.TRANSPARENT,
                    Color.TRANSPARENT,
                )
            },
        )
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(
            initialThemeSpec.uiColors.screenGradientBottom.toArgb().toDrawable()
        )

        setContent {
            AndroidApp()
        }
    }

    private fun isSystemDarkTheme(): Boolean {
        val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return nightMode == Configuration.UI_MODE_NIGHT_YES
    }
}

@Composable
fun AppAndroidPreview() {
    AndroidApp()
}
