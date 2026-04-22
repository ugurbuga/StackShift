package com.ugurbuga.stackshift.settings

import android.content.Context
import android.content.SharedPreferences
import com.ugurbuga.stackshift.game.model.AppColorPalette
import com.ugurbuga.stackshift.game.model.AppLanguage
import com.ugurbuga.stackshift.game.model.AppThemeMode
import com.ugurbuga.stackshift.game.model.BlockColorPalette
import com.ugurbuga.stackshift.game.model.BlockVisualStyle
import com.ugurbuga.stackshift.game.model.BoardBlockStyleMode
import com.ugurbuga.stackshift.game.model.normalizeBlockVisualStyle

actual object AppSettingsStorage {
    private const val Namespace = "com.ugurbuga.stackshift.settings"
    private const val KeyLanguage = "language"
    private const val KeyThemeMode = "themeMode"

    private val prefs: SharedPreferences by lazy {
        AppContextHolder.context.getSharedPreferences(Namespace, Context.MODE_PRIVATE)
    }

    actual fun load(): AppSettings {
        val hasStoredLanguage = prefs.contains(KeyLanguage)
        val defaultSettings = AppSettings()
        return AppSettings(
            language = AppLanguage.entries.getOrElse(prefs.getInt(KeyLanguage, defaultSettings.language.ordinal)) { defaultSettings.language },
            themeMode = AppThemeMode.entries.getOrElse(prefs.getInt(KeyThemeMode, defaultSettings.themeMode.ordinal)) { defaultSettings.themeMode },
            themeColorPalette = AppColorPalette.entries.getOrElse(prefs.getInt(KeyThemeColorPalette, defaultSettings.themeColorPalette.ordinal)) { defaultSettings.themeColorPalette },
            blockColorPalette = BlockColorPalette.entries.getOrElse(prefs.getInt(KeyBlockColorPalette, defaultSettings.blockColorPalette.ordinal)) { defaultSettings.blockColorPalette },
            blockVisualStyle = normalizeBlockVisualStyle(
                BlockVisualStyle.entries.getOrElse(prefs.getInt(KeyBlockVisualStyle, defaultSettings.blockVisualStyle.ordinal)) { defaultSettings.blockVisualStyle }
            ),
            boardBlockStyleMode = BoardBlockStyleMode.entries.getOrElse(prefs.getInt(KeyBoardBlockStyleMode, defaultSettings.boardBlockStyleMode.ordinal)) { defaultSettings.boardBlockStyleMode },
            hasSeenTutorial = prefs.getBoolean(KeyHasSeenTutorial, defaultSettings.hasSeenTutorial),
            hasShownInteractiveOnboarding = prefs.getBoolean(KeyHasShownInteractiveOnboarding, defaultSettings.hasShownInteractiveOnboarding),
            hasInitializedLanguage = prefs.getBoolean(KeyHasInitializedLanguage, defaultSettings.hasInitializedLanguage) || hasStoredLanguage,
        )
    }

    actual fun save(settings: AppSettings) {
        prefs.edit()
            .putInt(KeyLanguage, settings.language.ordinal)
            .putInt(KeyThemeMode, settings.themeMode.ordinal)
            .putInt(KeyThemeColorPalette, settings.themeColorPalette.ordinal)
            .putInt(KeyBlockColorPalette, settings.blockColorPalette.ordinal)
            .putInt(KeyBlockVisualStyle, normalizeBlockVisualStyle(settings.blockVisualStyle).ordinal)
            .putInt(KeyBoardBlockStyleMode, settings.boardBlockStyleMode.ordinal)
            .putBoolean(KeyHasSeenTutorial, settings.hasSeenTutorial)
            .putBoolean(KeyHasShownInteractiveOnboarding, settings.hasShownInteractiveOnboarding)
            .putBoolean(KeyHasInitializedLanguage, settings.hasInitializedLanguage)
            .apply()
    }

    private const val KeyThemeColorPalette = "themeColorPalette"
    private const val KeyBlockColorPalette = "blockColorPalette"
    private const val KeyBlockVisualStyle = "blockVisualStyle"
    private const val KeyBoardBlockStyleMode = "boardBlockStyleMode"
    private const val KeyHasSeenTutorial = "hasSeenTutorial"
    private const val KeyHasShownInteractiveOnboarding = "hasShownInteractiveOnboarding"
    private const val KeyHasInitializedLanguage = "hasInitializedLanguage"
}
