package com.ugurbuga.stackshift.settings

import android.content.Context
import com.ugurbuga.stackshift.game.model.GameState

actual object GameSessionStorage {
    private const val Namespace = "com.ugurbuga.stackshift.game_session"
    private const val KeyGameState = "gameState"

    private val prefs by lazy {
        AppContextHolder.context.getSharedPreferences(Namespace, Context.MODE_PRIVATE)
    }

    actual fun load(): GameState? {
        val serialized = prefs.getString(KeyGameState, null) ?: return null
        return runCatching { GameSessionCodec.decode(serialized) }
            .getOrNull()
            .also { if (it == null) clear() }
    }

    actual fun save(state: GameState) {
        prefs.edit()
            .putString(KeyGameState, GameSessionCodec.encode(state))
            .apply()
    }

    actual fun clear() {
        prefs.edit()
            .remove(KeyGameState)
            .apply()
    }
}
