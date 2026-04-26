package com.ugurbuga.stackshift.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.preferredLanguages

actual object LocalAppLocale {
    private const val LanguageKey = "AppleLanguages"
    private const val defaultLocale = "en"
    private var lastAppliedLocaleTag: String? = null
    private val LocalLocale = staticCompositionLocalOf { defaultLocale }

    actual val current: String
        @Composable get() = LocalLocale.current

    @Composable
    actual infix fun provides(value: String?): Array<ProvidedValue<*>> {
        val resolved = value ?: defaultLocale
        if (lastAppliedLocaleTag != resolved) {
            println(
                "[LanguageBootstrap][Apply] platform=ios previous=${lastAppliedLocaleTag ?: "<none>"} next=$resolved requested=${value ?: "<device-default>"}"
            )
            lastAppliedLocaleTag = resolved
        }
        if (value == null) {
            NSUserDefaults.standardUserDefaults.removeObjectForKey(LanguageKey)
        } else {
            NSUserDefaults.standardUserDefaults.setObject(listOf(resolved), forKey = LanguageKey)
        }
        return arrayOf(LocalLocale.provides(resolved))
    }
}

actual fun currentDeviceLocaleTag(): String =
    (NSLocale.preferredLanguages.firstOrNull() as? String)
        ?.takeIf { it.isNotBlank() }
        ?: "en"

