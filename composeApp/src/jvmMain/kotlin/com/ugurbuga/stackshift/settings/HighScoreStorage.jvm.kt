package com.ugurbuga.stackshift.settings

import java.util.prefs.Preferences

actual object HighScoreStorage {
    private val prefs = Preferences.userRoot().node(Namespace)

    actual fun load(): Int = prefs.getInt(KeyHighScore, DefaultHighScore)

    actual fun save(highScore: Int) {
        prefs.putInt(KeyHighScore, highScore.coerceAtLeast(DefaultHighScore))
    }

    private const val Namespace = "com.ugurbuga.stackshift.high_score"
    private const val KeyHighScore = "highScore"
    private const val DefaultHighScore = 0
}
