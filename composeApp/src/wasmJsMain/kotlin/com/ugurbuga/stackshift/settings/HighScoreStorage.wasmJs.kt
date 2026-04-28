package com.ugurbuga.stackshift.settings

import com.ugurbuga.stackshift.game.model.GameMode
import com.ugurbuga.stackshift.game.model.GameplayStyle

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
            GameMode.Classic -> "stackshift.highscore.classic.blockwise"
            GameMode.TimeAttack -> "stackshift.highscore.time_attack.blockwise"
        }
    }
}

