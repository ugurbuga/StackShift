package com.ugurbuga.stackshift.settings

import com.ugurbuga.stackshift.game.model.AppColorPalette
import com.ugurbuga.stackshift.game.model.AppLanguage
import com.ugurbuga.stackshift.game.model.AppThemeMode
import com.ugurbuga.stackshift.game.model.BlockColorPalette
import com.ugurbuga.stackshift.game.model.BlockVisualStyle
import com.ugurbuga.stackshift.game.model.BoardBlockStyleMode
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
            blockVisualStyle = BlockVisualStyle.entries.getOrElse(defaults.integerForKey(KeyBlockVisualStyle).toInt()) { defaultSettings.blockVisualStyle },
            boardBlockStyleMode = BoardBlockStyleMode.entries.getOrElse(defaults.integerForKey(KeyBoardBlockStyleMode).toInt()) { defaultSettings.boardBlockStyleMode },
            hasSeenTutorial = defaults.boolForKey(KeyHasSeenTutorial),
            hasShownInteractiveOnboarding = defaults.boolForKey(KeyHasShownInteractiveOnboarding),
            hasInitializedLanguage = defaults.boolForKey(KeyHasInitializedLanguage) || hasStoredLanguage,
            tokenBalance = defaults.integerForKey(KeyTokenBalance).toInt(),
            unlockedThemeModes = decodeEnumSet(defaults.getSafeString(KeyUnlockedThemeModes), AppThemeMode.entries),
            unlockedThemePalettes = decodeEnumSet(defaults.getSafeString(KeyUnlockedThemePalettes), AppColorPalette.entries),
            unlockedBlockStyles = decodeEnumSet(defaults.getSafeString(KeyUnlockedBlockStyles), BlockVisualStyle.entries),
            challengeProgress = decodeChallengeProgress(
                defaults.getSafeString(KeyChallengeProgress),
            ),
            lastAppOpenedAtEpochMillis = defaults.objectForKey(KeyLastAppOpenedAtEpochMillis)?.let {
                defaults.stringForKey(KeyLastAppOpenedAtEpochMillis)?.toLongOrNull()
                    ?: defaults.integerForKey(KeyLastAppOpenedAtEpochMillis).toLong()
            } ?: defaultSettings.lastAppOpenedAtEpochMillis,
        ).sanitized()
    }

    actual fun save(settings: AppSettings) {
        val sanitized = settings.sanitized()
        defaults.setInteger(sanitized.language.ordinal.toLong(), forKey = KeyLanguage)
        defaults.setInteger(sanitized.themeMode.ordinal.toLong(), forKey = KeyThemeMode)
        defaults.setInteger(sanitized.themeColorPalette.ordinal.toLong(), forKey = KeyThemeColorPalette)
        defaults.setInteger(sanitized.blockColorPalette.ordinal.toLong(), forKey = KeyBlockColorPalette)
        defaults.setInteger(sanitized.blockVisualStyle.ordinal.toLong(), forKey = KeyBlockVisualStyle)
        defaults.setInteger(sanitized.boardBlockStyleMode.ordinal.toLong(), forKey = KeyBoardBlockStyleMode)
        defaults.setBool(sanitized.hasSeenTutorial, forKey = KeyHasSeenTutorial)
        defaults.setBool(sanitized.hasShownInteractiveOnboarding, forKey = KeyHasShownInteractiveOnboarding)
        defaults.setBool(sanitized.hasInitializedLanguage, forKey = KeyHasInitializedLanguage)
        defaults.setInteger(sanitized.tokenBalance.toLong(), forKey = KeyTokenBalance)
        defaults.setObject(encodeEnumSet(sanitized.unlockedThemeModes), forKey = KeyUnlockedThemeModes)
        defaults.setObject(encodeEnumSet(sanitized.unlockedThemePalettes), forKey = KeyUnlockedThemePalettes)
        defaults.setObject(encodeEnumSet(sanitized.unlockedBlockStyles), forKey = KeyUnlockedBlockStyles)
        defaults.setObject(encodeChallengeProgress(sanitized.challengeProgress), forKey = KeyChallengeProgress)
        defaults.setObject(sanitized.lastAppOpenedAtEpochMillis.toString(), forKey = KeyLastAppOpenedAtEpochMillis)
    }

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

    private fun NSUserDefaults.getSafeString(key: String): String? {
        stringForKey(key)?.takeIf(String::isNotBlank)?.let { return it }
        return stringArrayForKey(key)
            ?.filterIsInstance<String>()
            ?.joinToString(separator = ";")
            ?.takeIf(String::isNotBlank)
    }
}

