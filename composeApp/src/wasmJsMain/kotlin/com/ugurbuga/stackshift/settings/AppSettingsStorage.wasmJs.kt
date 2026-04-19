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
            ?.takeIf { it.size in SupportedFieldCounts }
            ?: return AppSettings()

        val defaultSettings = AppSettings()
        return AppSettings(
            language = AppLanguage.entries.getOrElse(parts[0].toIntOrNull() ?: -1) { defaultSettings.language },
            themeMode = AppThemeMode.entries.getOrElse(parts[1].toIntOrNull() ?: -1) { defaultSettings.themeMode },
            themeColorPalette = AppColorPalette.entries.getOrElse(parts[2].toIntOrNull() ?: -1) { defaultSettings.themeColorPalette },
            blockColorPalette = BlockColorPalette.entries.getOrElse(parts[3].toIntOrNull() ?: -1) { defaultSettings.blockColorPalette },
            blockVisualStyle = BlockVisualStyle.entries.getOrElse(parts[4].toIntOrNull() ?: -1) { defaultSettings.blockVisualStyle },
            boardBlockStyleMode = BoardBlockStyleMode.entries.getOrElse(parts[5].toIntOrNull() ?: -1) { defaultSettings.boardBlockStyleMode },
            hasSeenTutorial = (parts[6].toIntOrNull() ?: 0) == 1,
            hasInitializedLanguage = (parts.getOrNull(7)?.toIntOrNull() ?: 0) == 1 || parts.isNotEmpty(),
            hasShownInteractiveOnboarding = (parts.getOrNull(8)?.toIntOrNull() ?: 0) == 1,
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
                if (settings.hasSeenTutorial) 1 else 0,
                if (settings.hasInitializedLanguage) 1 else 0,
                if (settings.hasShownInteractiveOnboarding) 1 else 0,
            ).joinToString(separator = Separator.toString()),
        )
    }

    private const val StorageKey = "stackshift.settings"
    private const val Separator = ','
    private val SupportedFieldCounts = 7..9
}

