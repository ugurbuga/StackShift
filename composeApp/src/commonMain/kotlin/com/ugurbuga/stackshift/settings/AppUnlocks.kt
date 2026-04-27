package com.ugurbuga.stackshift.settings

import com.ugurbuga.stackshift.game.model.AppColorPalette
import com.ugurbuga.stackshift.game.model.AppThemeMode
import com.ugurbuga.stackshift.game.model.BlockVisualStyle
import com.ugurbuga.stackshift.game.model.ChallengeProgress
import com.ugurbuga.stackshift.game.model.DailyChallenge
import com.ugurbuga.stackshift.game.model.normalizeBlockVisualStyle
import com.ugurbuga.stackshift.platform.isDebugBuild

const val DailyChallengeTokenReward = 10
const val RewardedTokenAdReward = 10
const val ScorePointsPerToken = 1_000

private val DefaultUnlockedThemeModes = AppThemeMode.entries.toSet()
private val DefaultUnlockedThemePalettes = setOf(AppColorPalette.Classic)
private val DefaultUnlockedBlockStyles = setOf(BlockVisualStyle.Flat)

internal fun resolvedThemePaletteUnlocks(
    debugBuild: Boolean,
    unlockedThemePalettes: Set<AppColorPalette>,
): Set<AppColorPalette> =
    if (debugBuild) AppColorPalette.entries.toSet() else unlockedThemePalettes + DefaultUnlockedThemePalettes

fun AppSettings.availableThemeModes(): Set<AppThemeMode> =
    this.unlockedThemeModes + DefaultUnlockedThemeModes

fun AppSettings.availableThemePalettes(): Set<AppColorPalette> =
    resolvedThemePaletteUnlocks(
        debugBuild = isDebugBuild(),
        unlockedThemePalettes = this.unlockedThemePalettes,
    )

fun AppSettings.availableBlockStyles(): Set<BlockVisualStyle> =
    ((if (isDebugBuild()) BlockVisualStyle.entries.toSet() else this.unlockedBlockStyles) + DefaultUnlockedBlockStyles)
        .map(::normalizeBlockVisualStyle)
        .toSet()

fun AppThemeMode.tokenCost(): Int = 0

fun AppColorPalette.tokenCost(): Int = when (this) {
    AppColorPalette.Classic -> 0
    AppColorPalette.Aurora -> 48
    AppColorPalette.Sunset -> 48
    AppColorPalette.ModernNeon -> 55
    AppColorPalette.SoftPastel -> 42
    AppColorPalette.MinimalMonochrome -> 42
}

fun BlockVisualStyle.tokenCost(): Int = when (normalizeBlockVisualStyle(this)) {
    BlockVisualStyle.Flat -> 0
    BlockVisualStyle.Bubble -> 24
    BlockVisualStyle.Outline -> 22
    BlockVisualStyle.Sharp3D -> 32
    BlockVisualStyle.Wood -> 28
    BlockVisualStyle.GridSplit -> 28
    BlockVisualStyle.Crystal -> 36
    BlockVisualStyle.DynamicLiquid -> 42
    BlockVisualStyle.Tornado -> 38
    BlockVisualStyle.HoneycombTexture -> 30
    BlockVisualStyle.SpiderWeb -> 30
    BlockVisualStyle.Cosmic -> 42
    BlockVisualStyle.Brick -> 24
    BlockVisualStyle.SoundWave -> 34
    BlockVisualStyle.Prism -> 36
    else -> 18
}

fun AppSettings.isThemeModeUnlocked(mode: AppThemeMode): Boolean = mode in availableThemeModes()

fun AppSettings.isThemePaletteUnlocked(palette: AppColorPalette): Boolean = palette in availableThemePalettes()

fun AppSettings.isBlockStyleUnlocked(style: BlockVisualStyle): Boolean =
    isDebugBuild() || normalizeBlockVisualStyle(style) in availableBlockStyles()

fun AppSettings.sanitized(): AppSettings {
    val isDebug = isDebugBuild()
    val sanitizedUnlockedBlockStyles: Set<BlockVisualStyle> =
        ((if (isDebug) BlockVisualStyle.entries.toSet() else this.unlockedBlockStyles) + DefaultUnlockedBlockStyles)
            .map(::normalizeBlockVisualStyle)
            .toSet()
    val sanitizedThemeModes: Set<AppThemeMode> = AppThemeMode.entries.toSet()
    val sanitizedThemePalettes: Set<AppColorPalette> = resolvedThemePaletteUnlocks(
        debugBuild = isDebug,
        unlockedThemePalettes = this.unlockedThemePalettes,
    )
    val sanitizedThemeMode = this.themeMode.takeIf { it in sanitizedThemeModes } ?: AppThemeMode.System
    val sanitizedThemePalette = this.themeColorPalette.takeIf { it in sanitizedThemePalettes } ?: AppColorPalette.Classic
    val normalizedBlockStyle = normalizeBlockVisualStyle(this.blockVisualStyle)
    val sanitizedBlockStyle = normalizedBlockStyle.takeIf { it in sanitizedUnlockedBlockStyles } ?: BlockVisualStyle.Flat
    return this.copy(
        themeMode = sanitizedThemeMode,
        themeColorPalette = sanitizedThemePalette,
        blockVisualStyle = sanitizedBlockStyle,
        unlockedThemeModes = sanitizedThemeModes,
        unlockedThemePalettes = sanitizedThemePalettes,
        unlockedBlockStyles = sanitizedUnlockedBlockStyles,
        tokenBalance = this.tokenBalance.coerceAtLeast(0),
        lastAppOpenedAtEpochMillis = this.lastAppOpenedAtEpochMillis.coerceAtLeast(0L),
        soundEnabled = false,
    )
}

fun AppSettings.selectThemeMode(mode: AppThemeMode): AppSettings =
    this.copy(themeMode = mode).sanitized()

fun AppSettings.selectThemePalette(palette: AppColorPalette): AppSettings =
    this.copy(themeColorPalette = palette).sanitized()

fun AppSettings.selectBlockStyle(style: BlockVisualStyle): AppSettings =
    this.copy(blockVisualStyle = normalizeBlockVisualStyle(style)).sanitized()

fun AppSettings.unlockThemeMode(mode: AppThemeMode): AppSettings = selectThemeMode(mode)

fun AppSettings.unlockThemePalette(palette: AppColorPalette): AppSettings? {
    if (isThemePaletteUnlocked(palette)) return selectThemePalette(palette)
    val price = palette.tokenCost()
    if (this.tokenBalance < price) return null
    return this.copy(
        tokenBalance = this.tokenBalance - price,
        unlockedThemePalettes = this.unlockedThemePalettes + palette,
        themeColorPalette = palette,
    ).sanitized()
}

fun AppSettings.unlockBlockStyle(style: BlockVisualStyle): AppSettings? {
    val normalizedStyle = normalizeBlockVisualStyle(style)
    if (isBlockStyleUnlocked(normalizedStyle)) return selectBlockStyle(normalizedStyle)
    val price = normalizedStyle.tokenCost()
    if (this.tokenBalance < price) return null
    return this.copy(
        tokenBalance = this.tokenBalance - price,
        unlockedBlockStyles = this.unlockedBlockStyles + normalizedStyle,
        blockVisualStyle = normalizedStyle,
    ).sanitized()
}

fun AppSettings.completedChallengeDaysKey(year: Int, month: Int): String =
    "$year-${month.toString().padStart(2, '0')}"

fun AppSettings.hasCompletedChallenge(challenge: DailyChallenge): Boolean =
    challenge.day in challengeProgress.completedDays[completedChallengeDaysKey(challenge.year, challenge.month)].orEmpty()

fun AppSettings.awardCompletedChallenge(challenge: DailyChallenge): AppSettings {
    if (hasCompletedChallenge(challenge)) return sanitized()
    val key = completedChallengeDaysKey(challenge.year, challenge.month)
    val currentDays = this.challengeProgress.completedDays[key].orEmpty()
    return this.copy(
        tokenBalance = this.tokenBalance + DailyChallengeTokenReward,
        challengeProgress = ChallengeProgress(
            completedDays = this.challengeProgress.completedDays + (key to (currentDays + challenge.day)),
        ),
    ).sanitized()
}

fun AppSettings.awardScoreTokens(score: Int): AppSettings {
    val awardedTokens = score.coerceAtLeast(0) / ScorePointsPerToken
    if (awardedTokens <= 0) return sanitized()
    return this.copy(tokenBalance = this.tokenBalance + awardedTokens).sanitized()
}

fun AppSettings.awardBonusTokens(amount: Int): AppSettings {
    if (amount <= 0) return sanitized()
    return this.copy(tokenBalance = this.tokenBalance + amount).sanitized()
}

internal fun encodeEnumSet(values: Set<Enum<*>>): String =
    values.joinToString(separator = ";") { it.name }

internal fun <T : Enum<T>> decodeEnumSet(raw: String?, entries: List<T>): Set<T> {
    val normalized = normalizePersistedDelimitedValue(raw) ?: return emptySet()
    return normalized
        .split(';', ',')
        .mapNotNull { encoded -> entries.firstOrNull { it.name == encoded.trim() } }
        .toSet()
}

internal fun encodeChallengeProgress(progress: ChallengeProgress): String =
    progress.completedDays
        .flatMap { entry -> entry.value.map { "${entry.key}|$it" } }
        .joinToString(separator = ";")

internal fun decodeChallengeProgress(raw: String?): ChallengeProgress = ChallengeProgress(
    completedDays = normalizePersistedDelimitedValue(raw)
        ?.split(';', ',')
        ?.filter { it.isNotBlank() }
        ?.mapNotNull { item ->
            val parts = item.split("|")
            val day = parts.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
            val key = parts.getOrNull(0)?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            key to day
        }
        ?.groupBy({ it.first }, { it.second })
        ?.mapValues { entry -> entry.value.toSet() }
        ?: emptyMap(),
)

private fun normalizePersistedDelimitedValue(raw: String?): String? = raw
    ?.trim()
    ?.removePrefix("[")
    ?.removeSuffix("]")
    ?.removePrefix("{")
    ?.removeSuffix("}")
    ?.takeIf(String::isNotBlank)

