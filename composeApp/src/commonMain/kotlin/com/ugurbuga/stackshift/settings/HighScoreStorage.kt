package com.ugurbuga.stackshift.settings

import com.ugurbuga.stackshift.game.model.GameMode

expect object HighScoreStorage {
    fun load(mode: GameMode = GameMode.Classic): Int
    fun save(highScore: Int, mode: GameMode = GameMode.Classic)
}
