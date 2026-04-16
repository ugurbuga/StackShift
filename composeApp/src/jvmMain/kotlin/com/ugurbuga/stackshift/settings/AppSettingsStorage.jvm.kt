package com.ugurbuga.stackshift.settings

import com.ugurbuga.stackshift.game.model.AppColorPalette
import com.ugurbuga.stackshift.game.model.AppLanguage
import com.ugurbuga.stackshift.game.model.AppThemeMode
import com.ugurbuga.stackshift.game.model.BlockColorPalette
import com.ugurbuga.stackshift.game.model.BlockVisualStyle
import com.ugurbuga.stackshift.game.model.BoardBlockStyleMode
import java.util.prefs.Preferences

actual object AppSettingsStorage {
    private val prefs = Preferences.userRoot().node(Namespace)

    actual fun load(): AppSettings {
        val hasStoredLanguage = prefs.get(KeyLanguage, null) != null
        return AppSettings(
            language = AppLanguage.entries[prefs.getInt(KeyLanguage, AppLanguage.English.ordinal)],
            themeMode = AppThemeMode.entries[prefs.getInt(KeyThemeMode, AppThemeMode.System.ordinal)],
            themeColorPalette = AppColorPalette.entries[prefs.getInt(KeyThemeColorPalette, AppColorPalette.Classic.ordinal)],
            blockColorPalette = BlockColorPalette.entries[prefs.getInt(KeyBlockColorPalette, BlockColorPalette.Classic.ordinal)],
            blockVisualStyle = BlockVisualStyle.entries[prefs.getInt(KeyBlockVisualStyle, BlockVisualStyle.Flat.ordinal)],
            boardBlockStyleMode = BoardBlockStyleMode.entries[prefs.getInt(KeyBoardBlockStyleMode, BoardBlockStyleMode.MatchSelectedBlockStyle.ordinal)],
            hasSeenTutorial = prefs.getBoolean(KeyHasSeenTutorial, false),
            hasShownInteractiveOnboarding = prefs.getBoolean(KeyHasShownInteractiveOnboarding, false),
            hasInitializedLanguage = prefs.getBoolean(KeyHasInitializedLanguage, false) || hasStoredLanguage,
        )
    }

    actual fun save(settings: AppSettings) {
        prefs.putInt(KeyLanguage, settings.language.ordinal)
        prefs.putInt(KeyThemeMode, settings.themeMode.ordinal)
        prefs.putInt(KeyThemeColorPalette, settings.themeColorPalette.ordinal)
        prefs.putInt(KeyBlockColorPalette, settings.blockColorPalette.ordinal)
        prefs.putInt(KeyBlockVisualStyle, settings.blockVisualStyle.ordinal)
        prefs.putInt(KeyBoardBlockStyleMode, settings.boardBlockStyleMode.ordinal)
        prefs.putBoolean(KeyHasSeenTutorial, settings.hasSeenTutorial)
        prefs.putBoolean(KeyHasShownInteractiveOnboarding, settings.hasShownInteractiveOnboarding)
        prefs.putBoolean(KeyHasInitializedLanguage, settings.hasInitializedLanguage)
    }

    private const val Namespace = "com.ugurbuga.stackshift.settings"
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
