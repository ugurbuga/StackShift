package com.ugurbuga.stackshift.settings

import com.ugurbuga.stackshift.game.model.AppLanguage
import com.ugurbuga.stackshift.game.model.AppThemeMode
import platform.Foundation.NSUserDefaults

actual object AppSettingsStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun load(): AppSettings = AppSettings(
        language = AppLanguage.entries[defaults.integerForKey(KeyLanguage).toIntOrDefault(AppLanguage.English.ordinal)],
        themeMode = AppThemeMode.entries[defaults.integerForKey(KeyThemeMode).toIntOrDefault(AppThemeMode.System.ordinal)],
    )

    actual fun save(settings: AppSettings) {
        defaults.setInteger(settings.language.ordinal.toLong(), forKey = KeyLanguage)
        defaults.setInteger(settings.themeMode.ordinal.toLong(), forKey = KeyThemeMode)
    }

    private const val KeyLanguage = "language"
    private const val KeyThemeMode = "themeMode"
}

private fun Long.toIntOrDefault(defaultValue: Int): Int = toInt().takeIf { it >= 0 } ?: defaultValue
