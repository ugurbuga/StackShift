package com.ugurbuga.stackshift.settings

import android.content.Context
import android.content.SharedPreferences
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
    private const val Namespace = "com.ugurbuga.stackshift.settings"
    private const val KeyLanguage = "language"
    private const val KeyThemeMode = "themeMode"

    private val prefs: SharedPreferences by lazy {
        AppContextHolder.context.getSharedPreferences(Namespace, Context.MODE_PRIVATE)
    }

    actual fun load(): AppSettings {
        val hasStoredLanguage = prefs.contains(KeyLanguage)
        val defaultSettings = AppSettings()
        val legacyThemePalette = prefs.takeIf { it.contains(KeyThemeColorPalette) }?.let {
            AppColorPalette.entries.getOrElse(
                prefs.getInt(KeyThemeColorPalette, defaultSettings.themeColorPalette.ordinal)
            ) { defaultSettings.themeColorPalette }
        }
        val legacyBlockPalette = prefs.takeIf { it.contains(KeyBlockColorPalette) }?.let {
            BlockColorPalette.entries.getOrElse(
                prefs.getInt(KeyBlockColorPalette, defaultSettings.blockColorPalette.ordinal)
            ) { defaultSettings.blockColorPalette }
        }
        return AppSettings(
            language = AppLanguage.entries.getOrElse(
                prefs.getInt(
                    KeyLanguage,
                    defaultSettings.language.ordinal
                )
            ) { defaultSettings.language },
            themeMode = AppThemeMode.entries.getOrElse(
                prefs.getInt(
                    KeyThemeMode,
                    defaultSettings.themeMode.ordinal
                )
            ) { defaultSettings.themeMode },
            themeColorPalette = resolveUnifiedThemePalette(
                themePalette = legacyThemePalette,
                blockPalette = legacyBlockPalette,
            ),
            blockVisualStyle = normalizeBlockVisualStyle(
                BlockVisualStyle.entries.getOrElse(
                    prefs.getInt(
                        KeyBlockVisualStyle,
                        defaultSettings.blockVisualStyle.ordinal
                    )
                ) { defaultSettings.blockVisualStyle }
            ),
            boardBlockStyleMode = BoardBlockStyleMode.entries.getOrElse(
                prefs.getInt(
                    KeyBoardBlockStyleMode,
                    defaultSettings.boardBlockStyleMode.ordinal
                )
            ) { defaultSettings.boardBlockStyleMode },
            hasSeenTutorial = prefs.getBoolean(KeyHasSeenTutorial, defaultSettings.hasSeenTutorial),
            hasShownInteractiveOnboarding = prefs.getBoolean(
                KeyHasShownInteractiveOnboarding,
                defaultSettings.hasShownInteractiveOnboarding
            ),
            hasInitializedLanguage = prefs.getBoolean(
                KeyHasInitializedLanguage,
                defaultSettings.hasInitializedLanguage
            ) || hasStoredLanguage,
            soundEnabled = prefs.getBoolean(KeySoundEnabled, defaultSettings.soundEnabled),
            challengeProgress = ChallengeProgress(
                completedDays = prefs.getStringSet(KeyChallengeProgress, emptySet())
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
        prefs.edit()
            .putInt(KeyLanguage, settings.language.ordinal)
            .putInt(KeyThemeMode, settings.themeMode.ordinal)
            .putInt(KeyThemeColorPalette, settings.themeColorPalette.ordinal)
            .putInt(KeyBlockColorPalette, settings.blockColorPalette.ordinal)
            .putInt(
                KeyBlockVisualStyle,
                normalizeBlockVisualStyle(settings.blockVisualStyle).ordinal
            )
            .putInt(KeyBoardBlockStyleMode, settings.boardBlockStyleMode.ordinal)
            .putBoolean(KeyHasSeenTutorial, settings.hasSeenTutorial)
            .putBoolean(KeyHasShownInteractiveOnboarding, settings.hasShownInteractiveOnboarding)
            .putBoolean(KeyHasInitializedLanguage, settings.hasInitializedLanguage)
            .putBoolean(KeySoundEnabled, settings.soundEnabled)
            .putStringSet(
                KeyChallengeProgress,
                settings.challengeProgress.completedDays.flatMap { entry ->
                    entry.value.map { "${entry.key}|$it" }
                }.toSet()
            )
            .apply()
    }

    private const val KeyChallengeProgress = "challengeProgress"

    private const val KeyThemeColorPalette = "themeColorPalette"
    private const val KeyBlockColorPalette = "blockColorPalette"
    private const val KeyBlockVisualStyle = "blockVisualStyle"
    private const val KeyBoardBlockStyleMode = "boardBlockStyleMode"
    private const val KeyHasSeenTutorial = "hasSeenTutorial"
    private const val KeyHasShownInteractiveOnboarding = "hasShownInteractiveOnboarding"
    private const val KeyHasInitializedLanguage = "hasInitializedLanguage"
    private const val KeySoundEnabled = "soundEnabled"
}
