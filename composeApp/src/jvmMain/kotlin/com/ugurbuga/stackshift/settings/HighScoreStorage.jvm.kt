package com.ugurbuga.stackshift.settings

import com.ugurbuga.stackshift.game.model.GameMode
import java.util.prefs.Preferences

actual object HighScoreStorage {
    private val prefs = Preferences.userRoot().node(Namespace)

    actual fun load(mode: GameMode): Int = prefs.getInt(keyFor(mode), DefaultHighScore)

    actual fun save(highScore: Int, mode: GameMode) {
        prefs.putInt(keyFor(mode), highScore.coerceAtLeast(DefaultHighScore))
    }

    private const val Namespace = "com.ugurbuga.stackshift.high_score"
    private const val DefaultHighScore = 0

    private fun keyFor(mode: GameMode): String = when (mode) {
        GameMode.Classic -> "highScoreClassic"
        GameMode.TimeAttack -> "highScoreTimeAttack"
    }
}
