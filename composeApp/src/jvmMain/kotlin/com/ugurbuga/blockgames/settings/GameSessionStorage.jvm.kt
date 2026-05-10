package com.ugurbuga.blockgames.settings

import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.platform.GlobalPlatformConfig
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
        prefs.keys()
            .filter {
                it == "gameState_classic" ||
                    it.startsWith("gameState_classic_") ||
                    it == "gameState_time_attack" ||
                    it.startsWith("gameState_time_attack_") ||
                    it.startsWith("gameState_daily_challenge_")
            }
            .forEach { prefs.remove(it) }
    }

    actual fun cleanup(allowedDateIds: List<String>) {
        val style = GlobalPlatformConfig.gameplayStyle.name.lowercase()
        val allowedKeys = allowedDateIds.map { "gameState_daily_challenge_${style}_$it" }.toSet()
        val prefix = "gameState_daily_challenge_${style}_"
        prefs.keys()
            .filter { it.startsWith(prefix) && it !in allowedKeys }
            .forEach { prefs.remove(it) }
    }

    actual fun getSavedDailyChallengeDays(yearMonth: String): Set<Int> {
        val style = GlobalPlatformConfig.gameplayStyle.name.lowercase()
        val prefix = "gameState_daily_challenge_${style}_$yearMonth-"
        return prefs.keys()
            .filter { it.startsWith(prefix) }
            .mapNotNull { it.substringAfterLast("-").toIntOrNull() }
            .toSet()
    }

    private const val Namespace = "com.ugurbuga.blockgames.game_session"

    private fun keyFor(slot: GameSessionSlot): String = "gameState_${slot.key}"
}
