package com.ugurbuga.stackshift.settings

import android.content.Context
import android.content.SharedPreferences
import com.ugurbuga.stackshift.game.model.AppColorPalette
import com.ugurbuga.stackshift.game.model.AppLanguage
import com.ugurbuga.stackshift.game.model.AppThemeMode
import com.ugurbuga.stackshift.game.model.BlockColorPalette
import com.ugurbuga.stackshift.game.model.BlockVisualStyle
import com.ugurbuga.stackshift.game.model.BoardBlockStyleMode

actual object AppSettingsStorage {
    private const val Namespace = "com.ugurbuga.stackshift.settings"
    private const val KeyLanguage = "language"
    private const val KeyThemeMode = "themeMode"

    private val prefs: SharedPreferences by lazy {
        AppContextHolder.context.getSharedPreferences(Namespace, Context.MODE_PRIVATE)
    }

    actual fun load(): AppSettings = AppSettings(
        language = AppLanguage.entries[prefs.getInt(KeyLanguage, AppLanguage.English.ordinal)],
        themeMode = AppThemeMode.entries[prefs.getInt(KeyThemeMode, AppThemeMode.System.ordinal)],
        themeColorPalette = AppColorPalette.entries[prefs.getInt(KeyThemeColorPalette, AppColorPalette.Classic.ordinal)],
        blockColorPalette = BlockColorPalette.entries[prefs.getInt(KeyBlockColorPalette, BlockColorPalette.Classic.ordinal)],
        blockVisualStyle = BlockVisualStyle.entries[prefs.getInt(KeyBlockVisualStyle, BlockVisualStyle.Flat.ordinal)],
        boardBlockStyleMode = BoardBlockStyleMode.entries[prefs.getInt(KeyBoardBlockStyleMode, BoardBlockStyleMode.AlwaysFlat.ordinal)],
    )

    actual fun save(settings: AppSettings) {
        prefs.edit()
            .putInt(KeyLanguage, settings.language.ordinal)
            .putInt(KeyThemeMode, settings.themeMode.ordinal)
            .putInt(KeyThemeColorPalette, settings.themeColorPalette.ordinal)
            .putInt(KeyBlockColorPalette, settings.blockColorPalette.ordinal)
            .putInt(KeyBlockVisualStyle, settings.blockVisualStyle.ordinal)
            .putInt(KeyBoardBlockStyleMode, settings.boardBlockStyleMode.ordinal)
            .apply()
    }

    private const val KeyThemeColorPalette = "themeColorPalette"
    private const val KeyBlockColorPalette = "blockColorPalette"
    private const val KeyBlockVisualStyle = "blockVisualStyle"
    private const val KeyBoardBlockStyleMode = "boardBlockStyleMode"
}
