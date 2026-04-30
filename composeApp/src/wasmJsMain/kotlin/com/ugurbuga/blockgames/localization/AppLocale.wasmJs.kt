package com.ugurbuga.blockgames.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.browser.window

actual object LocalAppLocale {
    private var lastAppliedLocaleTag: String? = null
    private val LocalLocale = staticCompositionLocalOf(::browserDefaultLocale)

    actual val current: String
        @Composable get() = LocalLocale.current

    @Composable
    actual infix fun provides(value: String?): Array<ProvidedValue<*>> {
        val resolved = value?.takeIf(String::isNotBlank) ?: browserDefaultLocale()
        if (lastAppliedLocaleTag != resolved) {
            println(
                "[LanguageBootstrap][Apply] platform=wasm previous=${lastAppliedLocaleTag ?: "<none>"} next=$resolved requested=${value ?: "<device-default>"}"
            )
            lastAppliedLocaleTag = resolved
        }
        return arrayOf(LocalLocale.provides(resolved))
    }

    private fun browserDefaultLocale(): String = runCatching {
        window.navigator.language
    }.getOrNull()?.takeIf(String::isNotBlank) ?: "en"
}

actual fun currentDeviceLocaleTag(): String = runCatching {
    window.navigator.language
}.getOrNull()?.takeIf(String::isNotBlank) ?: "en"

