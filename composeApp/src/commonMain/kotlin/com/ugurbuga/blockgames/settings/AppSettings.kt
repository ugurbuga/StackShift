package com.ugurbuga.blockgames.settings

import androidx.compose.runtime.Immutable
import com.ugurbuga.blockgames.game.model.AppColorPalette
import com.ugurbuga.blockgames.game.model.AppLanguage
import com.ugurbuga.blockgames.game.model.AppThemeMode
import com.ugurbuga.blockgames.game.model.BlockColorPalette
import com.ugurbuga.blockgames.game.model.BlockVisualStyle
import com.ugurbuga.blockgames.game.model.BoardBlockStyleMode
import com.ugurbuga.blockgames.game.model.ChallengeProgress
import com.ugurbuga.blockgames.game.model.GameplayStyle

@Immutable
data class AppSettings(
    val language: AppLanguage = AppLanguage.English,
    val themeMode: AppThemeMode = AppThemeMode.System,
    val themeColorPalette: AppColorPalette = AppColorPalette.Classic,
    val blockVisualStyle: BlockVisualStyle = BlockVisualStyle.Flat,
    val boardBlockStyleMode: BoardBlockStyleMode = BoardBlockStyleMode.MatchSelectedBlockStyle,
    val tokenBalance: Int = 0,
    val unlockedThemeModes: Set<AppThemeMode> = AppThemeMode.entries.toSet(),
    val unlockedThemePalettes: Set<AppColorPalette> = setOf(AppColorPalette.Classic),
    val unlockedBlockStyles: Set<BlockVisualStyle> = setOf(BlockVisualStyle.Flat),
    val challengeProgress: ChallengeProgress = ChallengeProgress(),
    val lastAppOpenedAtEpochMillis: Long = 0L,
    val hasSeenTutorial: Boolean = false,
    val hasShownInteractiveOnboarding: Boolean = false,
    val hasInitializedLanguage: Boolean = false,
    val soundEnabled: Boolean = false,
    val isHighScoresClearedOnce: Boolean = false,
    val lastActiveSlot: GameSessionSlot? = null,
    val selectedGameplayStyle: GameplayStyle? = null,
) {
    val blockColorPalette: BlockColorPalette
        get() = when (themeColorPalette) {
            AppColorPalette.Classic -> BlockColorPalette.Classic
            AppColorPalette.Aurora -> BlockColorPalette.Aurora
            AppColorPalette.Sunset -> BlockColorPalette.Sunset
            AppColorPalette.ModernNeon -> BlockColorPalette.Neon
            AppColorPalette.SoftPastel -> BlockColorPalette.SoftPastel
            AppColorPalette.MinimalMonochrome -> BlockColorPalette.Monochrome
        }
}
