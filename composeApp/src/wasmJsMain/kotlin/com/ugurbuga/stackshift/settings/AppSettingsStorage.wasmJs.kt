package com.ugurbuga.stackshift.settings

import com.ugurbuga.stackshift.game.model.AppColorPalette
import com.ugurbuga.stackshift.game.model.AppLanguage
import com.ugurbuga.stackshift.game.model.AppThemeMode
import com.ugurbuga.stackshift.game.model.BlockColorPalette
import com.ugurbuga.stackshift.game.model.BlockVisualStyle
import com.ugurbuga.stackshift.game.model.BoardBlockStyleMode
import com.ugurbuga.stackshift.game.model.ChallengeProgress
import com.ugurbuga.stackshift.game.model.normalizeBlockVisualStyle
import com.ugurbuga.stackshift.game.model.resolveUnifiedThemePalette

actual object AppSettingsStorage {
    actual fun load(): AppSettings {
        val parts = BrowserStorage.get(StorageKey)
            ?.split(Separator)
            ?.takeIf { it.size in SupportedFieldCounts }
            ?: return AppSettings()

        val defaultSettings = AppSettings()
        val legacyThemePalette = AppColorPalette.entries.getOrElse(parts[2].toIntOrNull() ?: -1) { defaultSettings.themeColorPalette }
        val legacyBlockPalette = BlockColorPalette.entries.getOrElse(parts[3].toIntOrNull() ?: -1) { defaultSettings.blockColorPalette }
        return AppSettings(
            language = AppLanguage.entries.getOrElse(parts[0].toIntOrNull() ?: -1) { defaultSettings.language },
            themeMode = AppThemeMode.entries.getOrElse(parts[1].toIntOrNull() ?: -1) { defaultSettings.themeMode },
            themeColorPalette = resolveUnifiedThemePalette(themePalette = legacyThemePalette, blockPalette = legacyBlockPalette),
            blockVisualStyle = normalizeBlockVisualStyle(
                BlockVisualStyle.entries.getOrElse(parts[4].toIntOrNull() ?: -1) { defaultSettings.blockVisualStyle }
            ),
            boardBlockStyleMode = BoardBlockStyleMode.entries.getOrElse(parts[5].toIntOrNull() ?: -1) { defaultSettings.boardBlockStyleMode },
            hasSeenTutorial = (parts[6].toIntOrNull() ?: 0) == 1,
            hasInitializedLanguage = (parts.getOrNull(7)?.toIntOrNull() ?: 0) == 1 || parts.isNotEmpty(),
            hasShownInteractiveOnboarding = (parts.getOrNull(8)?.toIntOrNull() ?: 0) == 1,
            soundEnabled = (parts.getOrNull(10)?.toIntOrNull() ?: 0) == 1,
            challengeProgress = ChallengeProgress(
                completedDays = parts.getOrNull(9)
                    ?.split(";")
                    ?.filter { it.isNotEmpty() }
                    ?.mapNotNull { item ->
                        val subParts = item.split("|")
                        if (subParts.size == 2) subParts[0] to subParts[1].toInt() else null
                    }
                    ?.groupBy({ it.first }, { it.second })
                    ?.mapValues { it.value.toSet() } ?: emptyMap()
            )
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
                normalizeBlockVisualStyle(settings.blockVisualStyle).ordinal,
                settings.boardBlockStyleMode.ordinal,
                if (settings.hasSeenTutorial) 1 else 0,
                if (settings.hasInitializedLanguage) 1 else 0,
                if (settings.hasShownInteractiveOnboarding) 1 else 0,
                if (settings.soundEnabled) 1 else 0,
                settings.challengeProgress.completedDays.flatMap { entry ->
                    entry.value.map { "${entry.key}|$it" }
                }.joinToString(separator = ";")
            ).joinToString(separator = Separator.toString()),
        )
    }

    private const val StorageKey = "stackshift.settings"
    private const val Separator = ','
    private val SupportedFieldCounts = 7..11
}

