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
import platform.Foundation.NSUserDefaults

actual object AppSettingsStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun load(): AppSettings {
        val hasStoredLanguage = defaults.objectForKey(KeyLanguage) != null
        val defaultSettings = AppSettings()
        val legacyThemePalette = defaults.objectForKey(KeyThemeColorPalette)?.let {
            AppColorPalette.entries.getOrElse(defaults.integerForKey(KeyThemeColorPalette).toInt()) { defaultSettings.themeColorPalette }
        }
        val legacyBlockPalette = defaults.objectForKey(KeyBlockColorPalette)?.let {
            BlockColorPalette.entries.getOrElse(defaults.integerForKey(KeyBlockColorPalette).toInt()) { defaultSettings.blockColorPalette }
        }
        return AppSettings(
            language = AppLanguage.entries.getOrElse(defaults.integerForKey(KeyLanguage).toInt()) { defaultSettings.language },
            themeMode = AppThemeMode.entries.getOrElse(defaults.integerForKey(KeyThemeMode).toInt()) { defaultSettings.themeMode },
            themeColorPalette = resolveUnifiedThemePalette(themePalette = legacyThemePalette, blockPalette = legacyBlockPalette),
            blockVisualStyle = normalizeBlockVisualStyle(
                BlockVisualStyle.entries.getOrElse(defaults.integerForKey(KeyBlockVisualStyle).toInt()) { defaultSettings.blockVisualStyle }
            ),
            boardBlockStyleMode = BoardBlockStyleMode.entries.getOrElse(defaults.integerForKey(KeyBoardBlockStyleMode).toInt()) { defaultSettings.boardBlockStyleMode },
            hasSeenTutorial = defaults.boolForKey(KeyHasSeenTutorial),
            hasShownInteractiveOnboarding = defaults.boolForKey(KeyHasShownInteractiveOnboarding),
            hasInitializedLanguage = defaults.boolForKey(KeyHasInitializedLanguage) || hasStoredLanguage,
            soundEnabled = defaults.boolForKey(KeySoundEnabled),
            challengeProgress = ChallengeProgress(
                completedDays = (defaults.stringArrayForKey(KeyChallengeProgress) as? List<String>)
                    ?.mapNotNull { item ->
                        val parts = item.split("|")
                        if (parts.size == 2) parts[0] to parts[1].toInt() else null
                    }
                    ?.groupBy({ it.first }, { it.second })
                    ?.mapValues { it.value.toSet() } ?: emptyMap()
            )
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
        defaults.setBool(settings.soundEnabled, forKey = KeySoundEnabled)
        defaults.setObject(
            settings.challengeProgress.completedDays.flatMap { entry ->
                entry.value.map { "${entry.key}|$it" }
            },
            forKey = KeyChallengeProgress
        )
    }

    private const val KeyChallengeProgress = "challengeProgress"

    private const val KeyLanguage = "language"
    private const val KeyThemeMode = "themeMode"
    private const val KeyThemeColorPalette = "themeColorPalette"
    private const val KeyBlockColorPalette = "blockColorPalette"
    private const val KeyBlockVisualStyle = "blockVisualStyle"
    private const val KeyBoardBlockStyleMode = "boardBlockStyleMode"
    private const val KeyHasSeenTutorial = "hasSeenTutorial"
    private const val KeyHasShownInteractiveOnboarding = "hasShownInteractiveOnboarding"
    private const val KeyHasInitializedLanguage = "hasInitializedLanguage"
    private const val KeySoundEnabled = "soundEnabled"
}

