package com.ugurbuga.blockgames.settings

import android.content.Context
import com.ugurbuga.blockgames.game.model.GameState

actual object GameSessionStorage {
    private const val Namespace = "com.ugurbuga.blockgames.game_session"

    private val prefs by lazy {
        AppContextHolder.context.getSharedPreferences(Namespace, Context.MODE_PRIVATE)
    }

    actual fun load(slot: GameSessionSlot): GameState? {
        val serialized = prefs.getString(keyFor(slot), null) ?: return null
        return runCatching { GameSessionCodec.decode(serialized) }
            .getOrNull()
            .also { if (it == null) clear(slot) }
    }

    actual fun save(slot: GameSessionSlot, state: GameState) {
        prefs.edit()
            .putString(keyFor(slot), GameSessionCodec.encode(state))
            .apply()
    }

    actual fun clear(slot: GameSessionSlot) {
        prefs.edit()
            .remove(keyFor(slot))
            .apply()
    }

    actual fun clear() {
        prefs.edit()
            .remove(keyFor(GameSessionSlot.Classic))
            .remove(keyFor(GameSessionSlot.TimeAttack))
            .apply()

        // Clear all daily challenge sessions
        prefs.all.keys.filter { it.startsWith("gameState_daily_challenge_") }
            .forEach { key ->
                prefs.edit().remove(key).apply()
            }
    }

    actual fun cleanup(allowedDateIds: List<String>) {
        val allowedKeys = allowedDateIds.map { "gameState_daily_challenge_$it" }.toSet()
        val toRemove = prefs.all.keys
            .filter { it.startsWith("gameState_daily_challenge_") && it !in allowedKeys }

        if (toRemove.isNotEmpty()) {
            val editor = prefs.edit()
            toRemove.forEach { editor.remove(it) }
            editor.apply()
        }
    }

    private fun keyFor(slot: GameSessionSlot): String = when (slot) {
        GameSessionSlot.Classic -> "gameState_classic"
        GameSessionSlot.TimeAttack -> "gameState_time_attack"
        is GameSessionSlot.DailyChallenge -> "gameState_daily_challenge_${slot.dateId}"
    }
}
