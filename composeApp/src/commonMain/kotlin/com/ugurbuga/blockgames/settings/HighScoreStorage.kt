package com.ugurbuga.blockgames.settings

import com.ugurbuga.blockgames.game.model.GameMode

expect object HighScoreStorage {
    fun load(mode: GameMode = GameMode.Classic): Int
    fun save(highScore: Int, mode: GameMode = GameMode.Classic)
}
