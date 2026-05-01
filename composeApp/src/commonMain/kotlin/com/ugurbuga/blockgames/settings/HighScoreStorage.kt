package com.ugurbuga.blockgames.settings

import com.ugurbuga.blockgames.game.model.GameMode
import com.ugurbuga.blockgames.game.model.GameplayStyle

expect object HighScoreStorage {
    fun load(mode: GameMode = GameMode.Classic, gameplayStyle: GameplayStyle = GameplayStyle.StackShift): Int
    fun save(highScore: Int, mode: GameMode = GameMode.Classic, gameplayStyle: GameplayStyle = GameplayStyle.StackShift)
}
