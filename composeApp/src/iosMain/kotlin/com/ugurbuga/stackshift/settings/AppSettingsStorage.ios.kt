package com.ugurbuga.stackshift.settings

import com.ugurbuga.stackshift.game.model.AppColorPalette
import com.ugurbuga.stackshift.game.model.AppLanguage
import com.ugurbuga.stackshift.game.model.AppThemeMode
import com.ugurbuga.stackshift.game.model.BlockColorPalette
import com.ugurbuga.stackshift.game.model.BlockVisualStyle
import com.ugurbuga.stackshift.game.model.BoardBlockStyleMode
import com.ugurbuga.stackshift.game.model.normalizeBlockVisualStyle
import platform.Foundation.NSUserDefaults

actual object AppSettingsStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun load(): AppSettings {
        val hasStoredLanguage = defaults.objectForKey(KeyLanguage) != null
        val defaultSettings = AppSettings()
        return AppSettings(
            language = AppLanguage.entries.getOrElse(defaults.integerForKey(KeyLanguage).toInt()) { defaultSettings.language },
            themeMode = AppThemeMode.entries.getOrElse(defaults.integerForKey(KeyThemeMode).toInt()) { defaultSettings.themeMode },
            themeColorPalette = AppColorPalette.entries.getOrElse(defaults.integerForKey(KeyThemeColorPalette).toInt()) { defaultSettings.themeColorPalette },
            blockColorPalette = BlockColorPalette.entries.getOrElse(defaults.integerForKey(KeyBlockColorPalette).toInt()) { defaultSettings.blockColorPalette },
            blockVisualStyle = normalizeBlockVisualStyle(
                BlockVisualStyle.entries.getOrElse(defaults.integerForKey(KeyBlockVisualStyle).toInt()) { defaultSettings.blockVisualStyle }
            ),
            boardBlockStyleMode = BoardBlockStyleMode.entries.getOrElse(defaults.integerForKey(KeyBoardBlockStyleMode).toInt()) { defaultSettings.boardBlockStyleMode },
            hasSeenTutorial = defaults.boolForKey(KeyHasSeenTutorial),
            hasShownInteractiveOnboarding = defaults.boolForKey(KeyHasShownInteractiveOnboarding),
            hasInitializedLanguage = defaults.boolForKey(KeyHasInitializedLanguage) || hasStoredLanguage,
        )
    }

    actual fun save(settings: AppSettings) {
        defaults.setInteger(settings.language.ordinal.toLong(), forKey = KeyLanguage)
        defaults.setInteger(settings.themeMode.ordinal.toLong(), forKey = KeyThemeMode)
        defaults.setInteger(settings.themeColorPalette.ordinal.toLong(), forKey = KeyThemeColorPalette)
        defaults.setInteger(settings.blockColorPalette.ordinal.toLong(), forKey = KeyBlockColorPalette)
        defaults.setInteger(normalizeBlockVisualStyle(settings.blockVisualStyle).ordinal.toLong(), forKey = KeyBlockVisualStyle)
        defaults.setInteger(settings.boardBlockStyleMode.ordinal.toLong(), forKey = KeyBoardBlockStyleMode)
        defaults.setBool(settings.hasSeenTutorial, forKey = KeyHasSeenTutorial)
        defaults.setBool(settings.hasShownInteractiveOnboarding, forKey = KeyHasShownInteractiveOnboarding)
        defaults.setBool(settings.hasInitializedLanguage, forKey = KeyHasInitializedLanguage)
    }

    private const val KeyLanguage = "language"
    private const val KeyThemeMode = "themeMode"
    private const val KeyThemeColorPalette = "themeColorPalette"
    private const val KeyBlockColorPalette = "blockColorPalette"
    private const val KeyBlockVisualStyle = "blockVisualStyle"
    private const val KeyBoardBlockStyleMode = "boardBlockStyleMode"
    private const val KeyHasSeenTutorial = "hasSeenTutorial"
    private const val KeyHasShownInteractiveOnboarding = "hasShownInteractiveOnboarding"
    private const val KeyHasInitializedLanguage = "hasInitializedLanguage"
}

