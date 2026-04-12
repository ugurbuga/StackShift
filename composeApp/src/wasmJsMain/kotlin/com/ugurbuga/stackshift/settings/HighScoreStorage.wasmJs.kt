package com.ugurbuga.stackshift.settings

actual object HighScoreStorage {
    actual fun load(): Int = BrowserStorage.get(StorageKey)?.toIntOrNull() ?: 0

    actual fun save(highScore: Int) {
        BrowserStorage.set(StorageKey, highScore.toString())
    }

    private const val StorageKey = "stackshift.highscore"
}

