package com.ugurbuga.blockgames.settings

import android.content.Context
import com.ugurbuga.blockgames.game.model.GameMode
import com.ugurbuga.blockgames.game.model.GameplayStyle

actual object HighScoreStorage {
    private const val Namespace = "com.ugurbuga.blockgames.high_score"
    private const val DefaultHighScore = 0

    private val prefs by lazy {
        AppContextHolder.context.getSharedPreferences(Namespace, Context.MODE_PRIVATE)
    }

    actual fun load(mode: GameMode, gameplayStyle: GameplayStyle): Int = prefs.getInt(keyFor(mode, gameplayStyle), DefaultHighScore)

    actual fun save(highScore: Int, mode: GameMode, gameplayStyle: GameplayStyle) {
        prefs.edit()
            .putInt(keyFor(mode, gameplayStyle), highScore.coerceAtLeast(DefaultHighScore))
            .apply()
    }

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
