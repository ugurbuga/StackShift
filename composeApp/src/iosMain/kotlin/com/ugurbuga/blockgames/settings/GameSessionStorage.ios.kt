package com.ugurbuga.blockgames.settings

import com.ugurbuga.blockgames.game.model.GameState
import platform.Foundation.NSUserDefaults

actual object GameSessionStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun load(slot: GameSessionSlot): GameState? {
        val serialized = defaults.stringForKey(keyFor(slot)) ?: return null
        return runCatching { GameSessionCodec.decode(serialized) }
            .getOrNull()
            .also { if (it == null) clear(slot) }
    }

    actual fun save(slot: GameSessionSlot, state: GameState) {
        defaults.setObject(GameSessionCodec.encode(state), forKey = keyFor(slot))
    }

    actual fun clear(slot: GameSessionSlot) {
        defaults.removeObjectForKey(keyFor(slot))
    }

    actual fun clear() {
        GameSessionSlot.entries.forEach { slot ->
            defaults.removeObjectForKey(keyFor(slot))
        }
    }

    private fun keyFor(slot: GameSessionSlot): String = when (slot) {
        GameSessionSlot.Classic -> "gameState_classic"
        GameSessionSlot.TimeAttack -> "gameState_time_attack"
        GameSessionSlot.DailyChallenge -> "gameState_daily_challenge"
    }
}
