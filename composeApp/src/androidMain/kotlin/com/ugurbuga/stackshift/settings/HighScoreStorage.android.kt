package com.ugurbuga.stackshift.settings

import android.content.Context
import com.ugurbuga.stackshift.game.model.GameMode

actual object HighScoreStorage {
    private const val Namespace = "com.ugurbuga.stackshift.high_score"
    private const val DefaultHighScore = 0

    private val prefs by lazy {
        AppContextHolder.context.getSharedPreferences(Namespace, Context.MODE_PRIVATE)
    }

    actual fun load(mode: GameMode): Int = prefs.getInt(keyFor(mode), DefaultHighScore)

    actual fun save(highScore: Int, mode: GameMode) {
        prefs.edit()
            .putInt(keyFor(mode), highScore.coerceAtLeast(DefaultHighScore))
            .apply()
    }

    private fun keyFor(mode: GameMode): String = when (mode) {
        GameMode.Classic -> "highScoreClassic"
        GameMode.TimeAttack -> "highScoreTimeAttack"
    }
}
