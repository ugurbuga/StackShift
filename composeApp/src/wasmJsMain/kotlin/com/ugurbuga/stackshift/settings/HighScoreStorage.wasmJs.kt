package com.ugurbuga.stackshift.settings

import com.ugurbuga.stackshift.game.model.GameMode

actual object HighScoreStorage {
    actual fun load(mode: GameMode): Int = BrowserStorage.get(keyFor(mode))?.toIntOrNull() ?: 0

    actual fun save(highScore: Int, mode: GameMode) {
        BrowserStorage.set(keyFor(mode), highScore.toString())
    }

    private fun keyFor(mode: GameMode): String = when (mode) {
        GameMode.Classic -> "stackshift.highscore.classic"
        GameMode.TimeAttack -> "stackshift.highscore.time_attack"
    }
}

