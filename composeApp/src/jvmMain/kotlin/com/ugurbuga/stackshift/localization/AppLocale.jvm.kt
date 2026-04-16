package com.ugurbuga.stackshift.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

actual object LocalAppLocale {
    private var defaultLocale: Locale? = null
    private var lastAppliedLocaleTag: String? = null
    private val LocalLocale = staticCompositionLocalOf { Locale.getDefault().toLanguageTag() }

    actual val current: String
        @Composable get() = LocalLocale.current

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        if (defaultLocale == null) {
            defaultLocale = Locale.getDefault()
        }
        val newLocale = value?.let(Locale::forLanguageTag) ?: defaultLocale!!
        val newLocaleTag = newLocale.toLanguageTag()
        if (lastAppliedLocaleTag != newLocaleTag) {
            println(
                "[LanguageBootstrap][Apply] platform=jvm previous=${lastAppliedLocaleTag ?: "<none>"} next=$newLocaleTag requested=${value ?: "<device-default>"}"
            )
            lastAppliedLocaleTag = newLocaleTag
        }
        Locale.setDefault(newLocale)
        return LocalLocale.provides(newLocaleTag)
    }
}

actual fun currentDeviceLocaleTag(): String = Locale.getDefault().toLanguageTag()

