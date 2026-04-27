package com.ugurbuga.stackshift.settings

import com.ugurbuga.stackshift.game.model.AppColorPalette
import com.ugurbuga.stackshift.game.model.AppLanguage
import com.ugurbuga.stackshift.game.model.AppThemeMode
import com.ugurbuga.stackshift.game.model.BlockColorPalette
import com.ugurbuga.stackshift.game.model.BlockVisualStyle
import com.ugurbuga.stackshift.game.model.BoardBlockStyleMode
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
            tokenBalance = prefs.getInt(KeyTokenBalance, defaultSettings.tokenBalance),
            unlockedThemeModes = decodeEnumSet(prefs.getSafeString(KeyUnlockedThemeModes, null), AppThemeMode.entries),
            unlockedThemePalettes = decodeEnumSet(prefs.getSafeString(KeyUnlockedThemePalettes, null), AppColorPalette.entries),
            unlockedBlockStyles = decodeEnumSet(prefs.getSafeString(KeyUnlockedBlockStyles, null), BlockVisualStyle.entries),
            challengeProgress = decodeChallengeProgress(prefs.getSafeString(KeyChallengeProgress, null)),
            lastAppOpenedAtEpochMillis = prefs.getLong(KeyLastAppOpenedAtEpochMillis, defaultSettings.lastAppOpenedAtEpochMillis),
        ).sanitized()
    }

    actual fun save(settings: AppSettings) {
        val sanitized = settings.sanitized()
        prefs.putInt(KeyLanguage, sanitized.language.ordinal)
        prefs.putInt(KeyThemeMode, sanitized.themeMode.ordinal)
        prefs.putInt(KeyThemeColorPalette, sanitized.themeColorPalette.ordinal)
        prefs.putInt(KeyBlockColorPalette, sanitized.blockColorPalette.ordinal)
        prefs.putInt(KeyBlockVisualStyle, normalizeBlockVisualStyle(sanitized.blockVisualStyle).ordinal)
        prefs.putInt(KeyBoardBlockStyleMode, sanitized.boardBlockStyleMode.ordinal)
        prefs.putBoolean(KeyHasSeenTutorial, sanitized.hasSeenTutorial)
        prefs.putBoolean(KeyHasShownInteractiveOnboarding, sanitized.hasShownInteractiveOnboarding)
        prefs.putBoolean(KeyHasInitializedLanguage, sanitized.hasInitializedLanguage)
        prefs.putInt(KeyTokenBalance, sanitized.tokenBalance)
        prefs.put(KeyUnlockedThemeModes, encodeEnumSet(sanitized.unlockedThemeModes))
        prefs.put(KeyUnlockedThemePalettes, encodeEnumSet(sanitized.unlockedThemePalettes))
        prefs.put(KeyUnlockedBlockStyles, encodeEnumSet(sanitized.unlockedBlockStyles))
        prefs.put(KeyChallengeProgress, encodeChallengeProgress(sanitized.challengeProgress))
        prefs.putLong(KeyLastAppOpenedAtEpochMillis, sanitized.lastAppOpenedAtEpochMillis)
    }

    private const val Namespace = "com.ugurbuga.stackshift.settings"
    private const val KeyLastAppOpenedAtEpochMillis = "lastAppOpenedAtEpochMillis"
    private const val KeyChallengeProgress = "challengeProgress"
    private const val KeyTokenBalance = "tokenBalance"
    private const val KeyUnlockedThemeModes = "unlockedThemeModes"
    private const val KeyUnlockedThemePalettes = "unlockedThemePalettes"
    private const val KeyUnlockedBlockStyles = "unlockedBlockStyles"
    private const val KeyLanguage = "language"
    private const val KeyThemeMode = "themeMode"
    private const val KeyThemeColorPalette = "themeColorPalette"
    private const val KeyBlockColorPalette = "blockColorPalette"
    private const val KeyBlockVisualStyle = "blockVisualStyle"
    private const val KeyBoardBlockStyleMode = "boardBlockStyleMode"
    private const val KeyHasSeenTutorial = "hasSeenTutorial"
    private const val KeyHasShownInteractiveOnboarding = "hasShownInteractiveOnboarding"
    private const val KeyHasInitializedLanguage = "hasInitializedLanguage"

    private fun Preferences.getSafeString(key: String, defaultValue: String?): String? =
        runCatching { get(key, defaultValue) }.getOrDefault(defaultValue)
}
