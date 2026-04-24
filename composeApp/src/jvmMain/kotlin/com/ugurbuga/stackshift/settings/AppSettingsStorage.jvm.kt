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
import java.util.prefs.Preferences

actual object AppSettingsStorage {
    private val prefs = Preferences.userRoot().node(Namespace)

    actual fun load(): AppSettings {
        val hasStoredLanguage = prefs.get(KeyLanguage, null) != null
        val defaultSettings = AppSettings()
        val legacyThemePalette = prefs.get(KeyThemeColorPalette, null)?.let {
            AppColorPalette.entries.getOrElse(prefs.getInt(KeyThemeColorPalette, defaultSettings.themeColorPalette.ordinal)) { defaultSettings.themeColorPalette }
        }
        val legacyBlockPalette = prefs.get(KeyBlockColorPalette, null)?.let {
            BlockColorPalette.entries.getOrElse(prefs.getInt(KeyBlockColorPalette, defaultSettings.blockColorPalette.ordinal)) { defaultSettings.blockColorPalette }
        }
        return AppSettings(
            language = AppLanguage.entries.getOrElse(prefs.getInt(KeyLanguage, defaultSettings.language.ordinal)) { defaultSettings.language },
            themeMode = AppThemeMode.entries.getOrElse(prefs.getInt(KeyThemeMode, defaultSettings.themeMode.ordinal)) { defaultSettings.themeMode },
            themeColorPalette = resolveUnifiedThemePalette(themePalette = legacyThemePalette, blockPalette = legacyBlockPalette),
            blockVisualStyle = normalizeBlockVisualStyle(
                BlockVisualStyle.entries.getOrElse(prefs.getInt(KeyBlockVisualStyle, defaultSettings.blockVisualStyle.ordinal)) { defaultSettings.blockVisualStyle }
            ),
            boardBlockStyleMode = BoardBlockStyleMode.entries.getOrElse(prefs.getInt(KeyBoardBlockStyleMode, defaultSettings.boardBlockStyleMode.ordinal)) { defaultSettings.boardBlockStyleMode },
            hasSeenTutorial = prefs.getBoolean(KeyHasSeenTutorial, defaultSettings.hasSeenTutorial),
            hasShownInteractiveOnboarding = prefs.getBoolean(KeyHasShownInteractiveOnboarding, defaultSettings.hasShownInteractiveOnboarding),
            hasInitializedLanguage = prefs.getBoolean(KeyHasInitializedLanguage, defaultSettings.hasInitializedLanguage) || hasStoredLanguage,
            soundEnabled = prefs.getBoolean(KeySoundEnabled, defaultSettings.soundEnabled),
            challengeProgress = ChallengeProgress(
                completedDays = prefs.get(KeyChallengeProgress, "")
                    .split(",")
                    .filter { it.isNotEmpty() }
                    .mapNotNull { item ->
                        val parts = item.split("|")
                        if (parts.size == 2) parts[0] to parts[1].toInt() else null
                    }
                    .groupBy({ it.first }, { it.second })
                    .mapValues { it.value.toSet() }
            )
        )
    }

    actual fun save(settings: AppSettings) {
        prefs.putInt(KeyLanguage, settings.language.ordinal)
        prefs.putInt(KeyThemeMode, settings.themeMode.ordinal)
        prefs.putInt(KeyThemeColorPalette, settings.themeColorPalette.ordinal)
        prefs.putInt(KeyBlockColorPalette, settings.blockColorPalette.ordinal)
        prefs.putInt(KeyBlockVisualStyle, normalizeBlockVisualStyle(settings.blockVisualStyle).ordinal)
        prefs.putInt(KeyBoardBlockStyleMode, settings.boardBlockStyleMode.ordinal)
        prefs.putBoolean(KeyHasSeenTutorial, settings.hasSeenTutorial)
        prefs.putBoolean(KeyHasShownInteractiveOnboarding, settings.hasShownInteractiveOnboarding)
        prefs.putBoolean(KeyHasInitializedLanguage, settings.hasInitializedLanguage)
        prefs.putBoolean(KeySoundEnabled, settings.soundEnabled)
        prefs.put(
            KeyChallengeProgress,
            settings.challengeProgress.completedDays.flatMap { entry ->
                entry.value.map { "${entry.key}|$it" }
            }.joinToString(",")
        )
    }

    private const val Namespace = "com.ugurbuga.stackshift.settings"
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
