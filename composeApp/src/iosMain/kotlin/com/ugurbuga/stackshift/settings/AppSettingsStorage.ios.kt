package com.ugurbuga.stackshift.settings

import com.ugurbuga.stackshift.game.model.AppColorPalette
import com.ugurbuga.stackshift.game.model.AppLanguage
import com.ugurbuga.stackshift.game.model.AppThemeMode
import com.ugurbuga.stackshift.game.model.BlockColorPalette
import com.ugurbuga.stackshift.game.model.BlockVisualStyle
import com.ugurbuga.stackshift.game.model.BoardBlockStyleMode
import platform.Foundation.NSUserDefaults

actual object AppSettingsStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun load(): AppSettings = AppSettings(
        language = AppLanguage.entries[defaults.integerForKey(KeyLanguage).toIntOrDefault(AppLanguage.English.ordinal)],
        themeMode = AppThemeMode.entries[defaults.integerForKey(KeyThemeMode).toIntOrDefault(AppThemeMode.System.ordinal)],
        themeColorPalette = AppColorPalette.entries[defaults.integerForKey(KeyThemeColorPalette).toIntOrDefault(AppColorPalette.Classic.ordinal)],
        blockColorPalette = BlockColorPalette.entries[defaults.integerForKey(KeyBlockColorPalette).toIntOrDefault(BlockColorPalette.Classic.ordinal)],
        blockVisualStyle = BlockVisualStyle.entries[defaults.integerForKey(KeyBlockVisualStyle).toIntOrDefault(BlockVisualStyle.Flat.ordinal)],
        boardBlockStyleMode = BoardBlockStyleMode.entries[defaults.integerForKey(KeyBoardBlockStyleMode).toIntOrDefault(BoardBlockStyleMode.MatchSelectedBlockStyle.ordinal)],
    )

    actual fun save(settings: AppSettings) {
        defaults.setInteger(settings.language.ordinal.toLong(), forKey = KeyLanguage)
        defaults.setInteger(settings.themeMode.ordinal.toLong(), forKey = KeyThemeMode)
        defaults.setInteger(settings.themeColorPalette.ordinal.toLong(), forKey = KeyThemeColorPalette)
        defaults.setInteger(settings.blockColorPalette.ordinal.toLong(), forKey = KeyBlockColorPalette)
        defaults.setInteger(settings.blockVisualStyle.ordinal.toLong(), forKey = KeyBlockVisualStyle)
        defaults.setInteger(settings.boardBlockStyleMode.ordinal.toLong(), forKey = KeyBoardBlockStyleMode)
    }

    private const val KeyLanguage = "language"
    private const val KeyThemeMode = "themeMode"
    private const val KeyThemeColorPalette = "themeColorPalette"
    private const val KeyBlockColorPalette = "blockColorPalette"
    private const val KeyBlockVisualStyle = "blockVisualStyle"
    private const val KeyBoardBlockStyleMode = "boardBlockStyleMode"
}

private fun Long.toIntOrDefault(defaultValue: Int): Int = toInt().takeIf { it >= 0 } ?: defaultValue
