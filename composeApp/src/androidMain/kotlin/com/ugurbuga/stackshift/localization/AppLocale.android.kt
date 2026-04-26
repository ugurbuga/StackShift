package com.ugurbuga.stackshift.localization

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

actual object LocalAppLocale {
    private var defaultLocale: Locale? = null
    private var lastAppliedLocaleTag: String? = null
    private val LocalLocale = staticCompositionLocalOf { Locale.getDefault().toLanguageTag() }

    actual val current: String
        @Composable get() = LocalLocale.current

    @Composable
    actual infix fun provides(value: String?): Array<ProvidedValue<*>> {
        val currentConfiguration = LocalConfiguration.current
        if (defaultLocale == null) {
            defaultLocale = Locale.getDefault()
        }
        val newLocale = value?.let(Locale::forLanguageTag) ?: defaultLocale!!
        val newLocaleTag = newLocale.toLanguageTag()
        if (lastAppliedLocaleTag != newLocaleTag) {
            println(
                "[LanguageBootstrap][Apply] platform=android previous=${lastAppliedLocaleTag ?: "<none>"} next=$newLocaleTag requested=${value ?: "<device-default>"}"
            )
            lastAppliedLocaleTag = newLocaleTag
        }
        Locale.setDefault(newLocale)
        val localizedConfiguration = remember(currentConfiguration, newLocaleTag) {
            Configuration(currentConfiguration).apply {
                setLocale(newLocale)
            }
        }
        return arrayOf(
            LocalLocale.provides(newLocaleTag),
            LocalConfiguration.provides(localizedConfiguration),
        )
    }
}

actual fun currentDeviceLocaleTag(): String = Locale.getDefault().toLanguageTag()

