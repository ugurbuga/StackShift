package com.ugurbuga.stackshift.settings

import com.ugurbuga.stackshift.game.model.GameMode
import com.ugurbuga.stackshift.game.model.GameplayStyle
import platform.Foundation.NSUserDefaults

actual object HighScoreStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun load(mode: GameMode, gameplayStyle: GameplayStyle): Int = defaults.integerForKey(keyFor(mode, gameplayStyle)).toIntOrDefault(DefaultHighScore)

    actual fun save(highScore: Int, mode: GameMode, gameplayStyle: GameplayStyle) {
        defaults.setInteger(highScore.coerceAtLeast(DefaultHighScore).toLong(), forKey = keyFor(mode, gameplayStyle))
    }

    private const val DefaultHighScore = 0

    private fun keyFor(mode: GameMode, gameplayStyle: GameplayStyle): String = when (gameplayStyle) {
        GameplayStyle.StackShift -> when (mode) {
            GameMode.Classic -> "highScoreClassic"
            GameMode.TimeAttack -> "highScoreTimeAttack"
        }
        GameplayStyle.BlockWise -> when (mode) {
            GameMode.Classic -> "highScoreClassicBlockWise"
            GameMode.TimeAttack -> "highScoreTimeAttackBlockWise"
        }
    }
}

private fun Long.toIntOrDefault(defaultValue: Int): Int = toInt().takeIf { it >= 0 } ?: defaultValue
