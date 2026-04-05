package com.ugurbuga.stackshift.settings

import com.ugurbuga.stackshift.game.model.GameState
import java.util.prefs.Preferences

actual object GameSessionStorage {
    private val prefs = Preferences.userRoot().node(Namespace)

    actual fun load(): GameState? {
        val serialized = prefs.get(KeyGameState, null) ?: return null
        return runCatching { GameSessionCodec.decode(serialized) }
            .getOrNull()
            .also { if (it == null) clear() }
    }

    actual fun save(state: GameState) {
        prefs.put(KeyGameState, GameSessionCodec.encode(state))
    }

    actual fun clear() {
        prefs.remove(KeyGameState)
    }

    private const val Namespace = "com.ugurbuga.stackshift.game_session"
    private const val KeyGameState = "gameState"
}
