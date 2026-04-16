package com.ugurbuga.stackshift.settings

import com.ugurbuga.stackshift.game.model.AppColorPalette
import com.ugurbuga.stackshift.game.model.AppLanguage
import com.ugurbuga.stackshift.game.model.AppThemeMode
import com.ugurbuga.stackshift.game.model.BlockColorPalette
import com.ugurbuga.stackshift.game.model.BlockVisualStyle
import com.ugurbuga.stackshift.game.model.BoardBlockStyleMode

data class AppSettings(
    val language: AppLanguage = AppLanguage.English,
    val themeMode: AppThemeMode = AppThemeMode.System,
    val themeColorPalette: AppColorPalette = AppColorPalette.Classic,
    val blockColorPalette: BlockColorPalette = BlockColorPalette.Classic,
    val blockVisualStyle: BlockVisualStyle = BlockVisualStyle.Flat,
    val boardBlockStyleMode: BoardBlockStyleMode = BoardBlockStyleMode.MatchSelectedBlockStyle,
    val hasSeenTutorial: Boolean = false,
    val hasShownInteractiveOnboarding: Boolean = false,
    val hasInitializedLanguage: Boolean = false,
)
