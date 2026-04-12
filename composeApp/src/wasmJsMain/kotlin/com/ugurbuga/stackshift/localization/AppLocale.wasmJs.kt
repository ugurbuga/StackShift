package com.ugurbuga.stackshift.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.browser.window

actual object LocalAppLocale {
    private val LocalLocale = staticCompositionLocalOf(::browserDefaultLocale)

    actual val current: String
        @Composable get() = LocalLocale.current

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        return LocalLocale.provides(value?.takeIf(String::isNotBlank) ?: browserDefaultLocale())
    }

    private fun browserDefaultLocale(): String = runCatching {
        window.navigator.language
    }.getOrNull()?.takeIf(String::isNotBlank) ?: "en"
}

