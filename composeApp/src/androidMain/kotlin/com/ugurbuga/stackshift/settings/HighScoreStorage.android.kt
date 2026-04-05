package com.ugurbuga.stackshift.settings

import android.content.Context

actual object HighScoreStorage {
    private const val Namespace = "com.ugurbuga.stackshift.high_score"
    private const val KeyHighScore = "highScore"
    private const val DefaultHighScore = 0

    private val prefs by lazy {
        AppContextHolder.context.getSharedPreferences(Namespace, Context.MODE_PRIVATE)
    }

    actual fun load(): Int = prefs.getInt(KeyHighScore, DefaultHighScore)

    actual fun save(highScore: Int) {
        prefs.edit()
            .putInt(KeyHighScore, highScore.coerceAtLeast(DefaultHighScore))
            .apply()
    }
}
