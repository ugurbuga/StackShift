package com.ugurbuga.blockgames.settings

import com.ugurbuga.blockgames.game.model.GameMode
import com.ugurbuga.blockgames.game.model.GameplayStyle

actual object HighScoreStorage {
    actual fun load(mode: GameMode, gameplayStyle: GameplayStyle): Int = BrowserStorage.get(keyFor(mode, gameplayStyle))?.toIntOrNull() ?: 0

    actual fun save(highScore: Int, mode: GameMode, gameplayStyle: GameplayStyle) {
        BrowserStorage.set(keyFor(mode, gameplayStyle), highScore.toString())
    }

    private fun keyFor(mode: GameMode, gameplayStyle: GameplayStyle): String = when (gameplayStyle) {
        GameplayStyle.StackShift -> when (mode) {
            GameMode.Classic -> "stackshift.highscore.classic"
            GameMode.TimeAttack -> "stackshift.highscore.time_attack"
        }
        GameplayStyle.BlockWise -> when (mode) {
            GameMode.Classic -> "blockwise.highscore.classic"
            GameMode.TimeAttack -> "blockwise.highscore.time_attack"
        }
        GameplayStyle.MergeShift -> when (mode) {
            GameMode.Classic -> "mergeshift.highscore.classic"
            GameMode.TimeAttack -> "mergeshift.highscore.time_attack"
        }
    }
}

