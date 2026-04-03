package com.ugurbuga.stackshift.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import android.content.res.Configuration
import java.util.Locale

actual object LocalAppLocale {
    private var defaultLocale: Locale? = null
    private val LocalLocale = staticCompositionLocalOf { Locale.getDefault().toLanguageTag() }

    actual val current: String
        @Composable get() = LocalLocale.current

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        val currentConfiguration = LocalConfiguration.current
        val resources = LocalContext.current.resources
        if (defaultLocale == null) {
            defaultLocale = Locale.getDefault()
        }
        val newLocale = value?.let(Locale::forLanguageTag) ?: defaultLocale!!
        Locale.setDefault(newLocale)
        val updatedConfiguration = Configuration(currentConfiguration).apply {
            setLocale(newLocale)
        }
        resources.updateConfiguration(updatedConfiguration, resources.displayMetrics)
        return LocalLocale.provides(newLocale.toLanguageTag())
    }
}
