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
        defaults.removeObjectForKey(keyFor(GameSessionSlot.Classic))
        defaults.removeObjectForKey(keyFor(GameSessionSlot.TimeAttack))
        // Daily challenges are handled via key prefix matching if we had a way to list keys,
        // but NSUserDefaults doesn't easily expose all keys.
        // For now, we only clear the known ones if we tracked them, or we clear all if needed.
        // On iOS, we can use dictionaryRepresentation to find keys.
        val keys = defaults.dictionaryRepresentation().keys
        keys.filterIsInstance<String>()
            .filter { it.startsWith("gameState_daily_challenge_") }
            .forEach { defaults.removeObjectForKey(it) }
    }

    actual fun cleanup(allowedDateIds: List<String>) {
        val allowedKeys = allowedDateIds.map { "gameState_daily_challenge_$it" }.toSet()
        val keys = defaults.dictionaryRepresentation().keys
        keys.filterIsInstance<String>()
            .filter { it.startsWith("gameState_daily_challenge_") && it !in allowedKeys }
            .forEach { defaults.removeObjectForKey(it) }
    }

    private fun keyFor(slot: GameSessionSlot): String = when (slot) {
        GameSessionSlot.Classic -> "gameState_classic"
        GameSessionSlot.TimeAttack -> "gameState_time_attack"
        is GameSessionSlot.DailyChallenge -> "gameState_daily_challenge_${slot.dateId}"
    }
}
