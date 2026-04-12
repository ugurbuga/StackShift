package com.ugurbuga.stackshift.settings

import com.ugurbuga.stackshift.game.model.AppColorPalette
import com.ugurbuga.stackshift.game.model.AppLanguage
import com.ugurbuga.stackshift.game.model.AppThemeMode
import com.ugurbuga.stackshift.game.model.BlockColorPalette
import com.ugurbuga.stackshift.game.model.BlockVisualStyle
import com.ugurbuga.stackshift.game.model.BoardBlockStyleMode

actual object AppSettingsStorage {
    actual fun load(): AppSettings {
        val parts = BrowserStorage.get(StorageKey)
            ?.split(Separator)
            ?.takeIf { it.size == FieldCount }
            ?: return AppSettings()

        return AppSettings(
            language = AppLanguage.entries.getOrElse(parts[0].toIntOrNull() ?: -1) { AppLanguage.English },
            themeMode = AppThemeMode.entries.getOrElse(parts[1].toIntOrNull() ?: -1) { AppThemeMode.System },
            themeColorPalette = AppColorPalette.entries.getOrElse(parts[2].toIntOrNull() ?: -1) { AppColorPalette.Classic },
            blockColorPalette = BlockColorPalette.entries.getOrElse(parts[3].toIntOrNull() ?: -1) { BlockColorPalette.Classic },
            blockVisualStyle = BlockVisualStyle.entries.getOrElse(parts[4].toIntOrNull() ?: -1) { BlockVisualStyle.Flat },
            boardBlockStyleMode = BoardBlockStyleMode.entries.getOrElse(parts[5].toIntOrNull() ?: -1) { BoardBlockStyleMode.MatchSelectedBlockStyle },
        )
    }

    actual fun save(settings: AppSettings) {
        BrowserStorage.set(
            StorageKey,
            listOf(
                settings.language.ordinal,
                settings.themeMode.ordinal,
                settings.themeColorPalette.ordinal,
                settings.blockColorPalette.ordinal,
                settings.blockVisualStyle.ordinal,
                settings.boardBlockStyleMode.ordinal,
            ).joinToString(separator = Separator.toString()),
        )
    }

    private const val StorageKey = "stackshift.settings"
    private const val Separator = ','
    private const val FieldCount = 6
}

