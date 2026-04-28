package com.ugurbuga.stackshift.settings

import android.content.Context
import android.content.SharedPreferences
import com.ugurbuga.stackshift.game.model.AppColorPalette
import com.ugurbuga.stackshift.game.model.AppLanguage
import com.ugurbuga.stackshift.game.model.AppThemeMode
import com.ugurbuga.stackshift.game.model.BlockColorPalette
import com.ugurbuga.stackshift.game.model.BlockVisualStyle
import com.ugurbuga.stackshift.game.model.BoardBlockStyleMode
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
            blockVisualStyle = BlockVisualStyle.entries.getOrElse(
                prefs.getInt(
                    KeyBlockVisualStyle,
                    defaultSettings.blockVisualStyle.ordinal
                )
            ) { defaultSettings.blockVisualStyle },
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
            tokenBalance = prefs.getInt(KeyTokenBalance, defaultSettings.tokenBalance),
            unlockedThemeModes = decodeEnumSet(
                prefs.getSafeString(KeyUnlockedThemeModes, null),
                AppThemeMode.entries,
            ),
            unlockedThemePalettes = decodeEnumSet(
                prefs.getSafeString(KeyUnlockedThemePalettes, null),
                AppColorPalette.entries,
            ),
            unlockedBlockStyles = decodeEnumSet(
                prefs.getSafeString(KeyUnlockedBlockStyles, null),
                BlockVisualStyle.entries,
            ),
            challengeProgress = decodeChallengeProgress(
                prefs.getSafeString(KeyChallengeProgress, null)
            ),
            lastAppOpenedAtEpochMillis = prefs.getLong(
                KeyLastAppOpenedAtEpochMillis,
                defaultSettings.lastAppOpenedAtEpochMillis,
            ),
            isHighScoresClearedOnce = prefs.getBoolean(
                KeyIsHighScoresClearedOnce,
                defaultSettings.isHighScoresClearedOnce
            ),
        ).sanitized()
    }

    private fun SharedPreferences.getSafeString(key: String, defaultValue: String?): String? {
        return try {
            getString(key, defaultValue)
        } catch (_: ClassCastException) {
            getStringSet(key, null)?.joinToString(";") ?: defaultValue
        }
    }

    actual fun save(settings: AppSettings) {
        val sanitized = settings.sanitized()
        prefs.edit()
            .putInt(KeyLanguage, sanitized.language.ordinal)
            .putInt(KeyThemeMode, sanitized.themeMode.ordinal)
            .putInt(KeyThemeColorPalette, sanitized.themeColorPalette.ordinal)
            .putInt(KeyBlockColorPalette, sanitized.blockColorPalette.ordinal)
            .putInt(KeyBlockVisualStyle, sanitized.blockVisualStyle.ordinal)
            .putInt(KeyBoardBlockStyleMode, sanitized.boardBlockStyleMode.ordinal)
            .putBoolean(KeyHasSeenTutorial, sanitized.hasSeenTutorial)
            .putBoolean(KeyHasShownInteractiveOnboarding, sanitized.hasShownInteractiveOnboarding)
            .putBoolean(KeyHasInitializedLanguage, sanitized.hasInitializedLanguage)
            .putInt(KeyTokenBalance, sanitized.tokenBalance)
            .putString(KeyUnlockedThemeModes, encodeEnumSet(sanitized.unlockedThemeModes))
            .putString(KeyUnlockedThemePalettes, encodeEnumSet(sanitized.unlockedThemePalettes))
            .putString(KeyUnlockedBlockStyles, encodeEnumSet(sanitized.unlockedBlockStyles))
            .putString(KeyChallengeProgress, encodeChallengeProgress(sanitized.challengeProgress))
            .putLong(KeyLastAppOpenedAtEpochMillis, sanitized.lastAppOpenedAtEpochMillis)
            .putBoolean(KeyIsHighScoresClearedOnce, sanitized.isHighScoresClearedOnce)
            .apply()
    }

    private const val KeyIsHighScoresClearedOnce = "isHighScoresClearedOnce"
    private const val KeyLastAppOpenedAtEpochMillis = "lastAppOpenedAtEpochMillis"
    private const val KeyChallengeProgress = "challengeProgress"
    private const val KeyTokenBalance = "tokenBalance"
    private const val KeyUnlockedThemeModes = "unlockedThemeModes"
    private const val KeyUnlockedThemePalettes = "unlockedThemePalettes"
    private const val KeyUnlockedBlockStyles = "unlockedBlockStyles"

    private const val KeyThemeColorPalette = "themeColorPalette"
    private const val KeyBlockColorPalette = "blockColorPalette"
    private const val KeyBlockVisualStyle = "blockVisualStyle"
    private const val KeyBoardBlockStyleMode = "boardBlockStyleMode"
    private const val KeyHasSeenTutorial = "hasSeenTutorial"
    private const val KeyHasShownInteractiveOnboarding = "hasShownInteractiveOnboarding"
    private const val KeyHasInitializedLanguage = "hasInitializedLanguage"
}
