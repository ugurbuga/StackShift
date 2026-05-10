package com.ugurbuga.blockgames.settings

import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.platform.GlobalPlatformConfig
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
        val keys = defaults.dictionaryRepresentation().keys
        keys.filterIsInstance<String>()
            .filter {
                it == "gameState_classic" ||
                    it.startsWith("gameState_classic_") ||
                    it == "gameState_time_attack" ||
                    it.startsWith("gameState_time_attack_") ||
                    it.startsWith("gameState_daily_challenge_")
            }
            .forEach { defaults.removeObjectForKey(it) }
    }

    actual fun cleanup(allowedDateIds: List<String>) {
        val style = GlobalPlatformConfig.gameplayStyle.name.lowercase()
        val allowedKeys = allowedDateIds.map { "gameState_daily_challenge_${style}_$it" }.toSet()
        val keys = defaults.dictionaryRepresentation().keys
        keys.filterIsInstance<String>()
            .filter { it.startsWith("gameState_daily_challenge_${style}_") && it !in allowedKeys }
            .forEach { defaults.removeObjectForKey(it) }
    }

    actual fun getSavedDailyChallengeDays(yearMonth: String): Set<Int> {
        val style = GlobalPlatformConfig.gameplayStyle.name.lowercase()
        val prefix = "gameState_daily_challenge_${style}_$yearMonth-"
        val keys = defaults.dictionaryRepresentation().keys
        return keys.filterIsInstance<String>()
            .filter { it.startsWith(prefix) }
            .mapNotNull { it.substringAfterLast("-").toIntOrNull() }
            .toSet()
    }

    private fun keyFor(slot: GameSessionSlot): String = "gameState_${slot.key}"
}
