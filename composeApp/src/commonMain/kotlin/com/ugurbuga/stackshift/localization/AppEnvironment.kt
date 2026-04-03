package com.ugurbuga.stackshift.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.staticCompositionLocalOf
import com.ugurbuga.stackshift.settings.AppSettings

val LocalAppSettings = staticCompositionLocalOf { AppSettings() }

expect object LocalAppLocale {
    val current: String
        @Composable get

    @Composable
    infix fun provides(value: String?): ProvidedValue<*>
}

@Composable
fun AppEnvironment(
    settings: AppSettings,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalAppSettings provides settings,
        LocalAppLocale provides settings.language.localeTag,
    ) {
        key(settings.language.localeTag) {
            content()
        }
    }
}
