package com.ugurbuga.blockgames.settings

import com.ugurbuga.blockgames.game.model.AppColorPalette
import com.ugurbuga.blockgames.game.model.AppLanguage
import com.ugurbuga.blockgames.game.model.AppThemeMode
import com.ugurbuga.blockgames.game.model.BlockColorPalette
import com.ugurbuga.blockgames.game.model.BlockVisualStyle
import com.ugurbuga.blockgames.game.model.BoardBlockStyleMode
import com.ugurbuga.blockgames.game.model.GameplayStyle
import com.ugurbuga.blockgames.game.model.gameplayStyleFromPersistedValue
import com.ugurbuga.blockgames.game.model.normalizeBlockVisualStyle
import com.ugurbuga.blockgames.game.model.resolveUnifiedThemePalette

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
            tokenBalance = parts.getOrNull(11)?.toIntOrNull() ?: defaultSettings.tokenBalance,
            unlockedThemeModes = decodeEnumSet(parts.getOrNull(12), AppThemeMode.entries),
            unlockedThemePalettes = decodeEnumSet(parts.getOrNull(13), AppColorPalette.entries),
            unlockedBlockStyles = decodeEnumSet(parts.getOrNull(14), BlockVisualStyle.entries),
            styleChallengeProgress = decodeStyleChallengeProgress(parts.getOrNull(10) ?: parts.getOrNull(9)),
            lastAppOpenedAtEpochMillis = parts.getOrNull(15)?.toLongOrNull() ?: defaultSettings.lastAppOpenedAtEpochMillis,
            lastActiveSlot = parts.getOrNull(17)?.let { GameSessionSlot.fromKey(it) },
            selectedGameplayStyle = parts.getOrNull(18)?.takeIf(String::isNotBlank)?.let(::gameplayStyleFromPersistedValue),
            seenTutorialStyles = decodeEnumSet(parts.getOrNull(19), GameplayStyle.entries),
            shownInteractiveOnboardingStyles = decodeEnumSet(parts.getOrNull(20), GameplayStyle.entries),
        ).sanitized()
    }

    actual fun save(settings: AppSettings) {
        val sanitized = settings.sanitized()
        BrowserStorage.set(
            StorageKey,
            listOf(
                sanitized.language.ordinal,
                sanitized.themeMode.ordinal,
                sanitized.themeColorPalette.ordinal,
                sanitized.blockColorPalette.ordinal,
                normalizeBlockVisualStyle(sanitized.blockVisualStyle).ordinal,
                sanitized.boardBlockStyleMode.ordinal,
                if (sanitized.hasSeenTutorial) 1 else 0,
                if (sanitized.hasInitializedLanguage) 1 else 0,
                if (sanitized.hasShownInteractiveOnboarding) 1 else 0,
                0,
                encodeStyleChallengeProgress(sanitized.styleChallengeProgress),
                sanitized.tokenBalance,
                encodeEnumSet(sanitized.unlockedThemeModes),
                encodeEnumSet(sanitized.unlockedThemePalettes),
                encodeEnumSet(sanitized.unlockedBlockStyles),
                sanitized.lastAppOpenedAtEpochMillis,
                sanitized.lastActiveSlot?.key ?: "",
                sanitized.selectedGameplayStyle?.name ?: "",
                encodeEnumSet(sanitized.seenTutorialStyles),
                encodeEnumSet(sanitized.shownInteractiveOnboardingStyles),
            ).joinToString(separator = Separator.toString()),
        )
    }

    private const val StorageKey = "stackshift.settings"
    private const val Separator = ','
    private val SupportedFieldCounts = 7..21

    private fun decodeStyleChallengeProgress(encoded: String?): Map<GameplayStyle, com.ugurbuga.blockgames.game.model.ChallengeProgress> {
        val legacy = decodeChallengeProgress(encoded)
        return if (legacy.completedDays.isEmpty()) {
            emptyMap()
        } else {
            mapOf(GameplayStyle.StackShift to legacy)
        }
    }

    private fun encodeStyleChallengeProgress(progressByStyle: Map<GameplayStyle, com.ugurbuga.blockgames.game.model.ChallengeProgress>): String {
        return encodeChallengeProgress(progressByStyle[GameplayStyle.StackShift] ?: com.ugurbuga.blockgames.game.model.ChallengeProgress())
    }
}

