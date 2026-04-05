package com.ugurbuga.stackshift.settings

import platform.Foundation.NSUserDefaults

actual object HighScoreStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun load(): Int = defaults.integerForKey(KeyHighScore).toIntOrDefault(DefaultHighScore)

    actual fun save(highScore: Int) {
        defaults.setInteger(highScore.coerceAtLeast(DefaultHighScore).toLong(), forKey = KeyHighScore)
    }

    private const val KeyHighScore = "highScore"
    private const val DefaultHighScore = 0
}

private fun Long.toIntOrDefault(defaultValue: Int): Int = toInt().takeIf { it >= 0 } ?: defaultValue
