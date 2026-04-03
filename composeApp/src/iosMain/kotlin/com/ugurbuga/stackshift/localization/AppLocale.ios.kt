package com.ugurbuga.stackshift.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults

actual object LocalAppLocale {
    private const val LanguageKey = "AppleLanguages"
    private const val defaultLocale = "en"
    private val LocalLocale = staticCompositionLocalOf { defaultLocale }

    actual val current: String
        @Composable get() = LocalLocale.current

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        val resolved = value ?: defaultLocale
        if (value == null) {
            NSUserDefaults.standardUserDefaults.removeObjectForKey(LanguageKey)
        } else {
            NSUserDefaults.standardUserDefaults.setObject(listOf(resolved), forKey = LanguageKey)
        }
        return LocalLocale.provides(resolved)
    }
}
