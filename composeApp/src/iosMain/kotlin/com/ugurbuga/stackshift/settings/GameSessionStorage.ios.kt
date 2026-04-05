package com.ugurbuga.stackshift.settings

import com.ugurbuga.stackshift.game.model.GameState
import platform.Foundation.NSUserDefaults

actual object GameSessionStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun load(): GameState? {
        val serialized = defaults.stringForKey(KeyGameState) ?: return null
        return runCatching { GameSessionCodec.decode(serialized) }
            .getOrNull()
            .also { if (it == null) clear() }
    }

    actual fun save(state: GameState) {
        defaults.setObject(GameSessionCodec.encode(state), forKey = KeyGameState)
    }

    actual fun clear() {
        defaults.removeObjectForKey(KeyGameState)
    }

    private const val KeyGameState = "gameState"
}
