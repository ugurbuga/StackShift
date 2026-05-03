package com.ugurbuga.blockgames.settings

import com.ugurbuga.blockgames.game.model.GameMode
import com.ugurbuga.blockgames.game.model.GameplayStyle
import java.util.prefs.Preferences

actual object HighScoreStorage {
    private val prefs = Preferences.userRoot().node(Namespace)

    actual fun load(mode: GameMode, gameplayStyle: GameplayStyle): Int = prefs.getInt(keyFor(mode, gameplayStyle), DefaultHighScore)

    actual fun save(highScore: Int, mode: GameMode, gameplayStyle: GameplayStyle) {
        prefs.putInt(keyFor(mode, gameplayStyle), highScore.coerceAtLeast(DefaultHighScore))
    }

    private const val Namespace = "com.ugurbuga.blockgames.high_score"
    private const val DefaultHighScore = 0

    private fun keyFor(mode: GameMode, gameplayStyle: GameplayStyle): String = when (gameplayStyle) {
        GameplayStyle.StackShift -> when (mode) {
            GameMode.Classic -> "highScoreClassic"
            GameMode.TimeAttack -> "highScoreTimeAttack"
        }
        GameplayStyle.BlockWise -> when (mode) {
            GameMode.Classic -> "highScoreClassicBlockWise"
            GameMode.TimeAttack -> "highScoreTimeAttackBlockWise"
        }
        GameplayStyle.MergeShift -> when (mode) {
            GameMode.Classic -> "highScoreClassicMergeShift"
            GameMode.TimeAttack -> "highScoreTimeAttackMergeShift"
        }
    }
}
