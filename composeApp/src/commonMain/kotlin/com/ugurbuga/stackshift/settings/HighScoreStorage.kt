package com.ugurbuga.stackshift.settings

import com.ugurbuga.stackshift.game.model.GameMode
import com.ugurbuga.stackshift.game.model.GameplayStyle

expect object HighScoreStorage {
    fun load(mode: GameMode = GameMode.Classic, gameplayStyle: GameplayStyle = GameplayStyle.StackShift): Int
    fun save(highScore: Int, mode: GameMode = GameMode.Classic, gameplayStyle: GameplayStyle = GameplayStyle.StackShift)
}
