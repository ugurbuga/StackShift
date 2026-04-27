package com.ugurbuga.stackshift.settings

import com.ugurbuga.stackshift.game.model.GameMode
import platform.Foundation.NSUserDefaults

actual object HighScoreStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun load(mode: GameMode): Int = defaults.integerForKey(keyFor(mode)).toIntOrDefault(DefaultHighScore)

    actual fun save(highScore: Int, mode: GameMode) {
        defaults.setInteger(highScore.coerceAtLeast(DefaultHighScore).toLong(), forKey = keyFor(mode))
    }

    private const val DefaultHighScore = 0

    private fun keyFor(mode: GameMode): String = when (mode) {
        GameMode.Classic -> "highScoreClassic"
        GameMode.TimeAttack -> "highScoreTimeAttack"
    }
}

private fun Long.toIntOrDefault(defaultValue: Int): Int = toInt().takeIf { it >= 0 } ?: defaultValue
