package com.ugurbuga.blockgames.settings

import com.ugurbuga.blockgames.game.model.GameState
import java.util.prefs.Preferences

actual object GameSessionStorage {
    private val prefs = Preferences.userRoot().node(Namespace)

    actual fun load(slot: GameSessionSlot): GameState? {
        val serialized = prefs.get(keyFor(slot), null) ?: return null
        return runCatching { GameSessionCodec.decode(serialized) }
            .getOrNull()
            .also { if (it == null) clear(slot) }
    }

    actual fun save(slot: GameSessionSlot, state: GameState) {
        prefs.put(keyFor(slot), GameSessionCodec.encode(state))
    }

    actual fun clear(slot: GameSessionSlot) {
        prefs.remove(keyFor(slot))
    }

    actual fun clear() {
        GameSessionSlot.entries.forEach { slot ->
            prefs.remove(keyFor(slot))
        }
    }

    private const val Namespace = "com.ugurbuga.blockgames.game_session"

    private fun keyFor(slot: GameSessionSlot): String = when (slot) {
        GameSessionSlot.Classic -> "gameState_classic"
        GameSessionSlot.TimeAttack -> "gameState_time_attack"
        GameSessionSlot.DailyChallenge -> "gameState_daily_challenge"
    }
}
