package com.ugurbuga.blockgames.settings

import com.ugurbuga.blockgames.game.model.GameState
import com.ugurbuga.blockgames.platform.GlobalPlatformConfig

actual object GameSessionStorage {
    actual fun load(slot: GameSessionSlot): GameState? = BrowserStorage.get(keyFor(slot))
        ?.takeIf(String::isNotBlank)
        ?.let(GameSessionCodec::decode)

    actual fun save(slot: GameSessionSlot, state: GameState) {
        BrowserStorage.set(keyFor(slot), GameSessionCodec.encode(state))
    }

    actual fun clear(slot: GameSessionSlot) {
        BrowserStorage.remove(keyFor(slot))
    }

    actual fun clear() {
        BrowserStorage.keys().filter {
            it == "gameState_classic" ||
                it.startsWith("gameState_classic_") ||
                it == "gameState_time_attack" ||
                it.startsWith("gameState_time_attack_") ||
                it.startsWith("gameState_daily_challenge_")
        }
            .forEach { BrowserStorage.remove(it) }
    }

    actual fun cleanup(allowedDateIds: List<String>) {
        val style = GlobalPlatformConfig.gameplayStyle.name.lowercase()
        val allowedKeys = allowedDateIds.map { "gameState_daily_challenge_${style}_$it" }.toSet()
        BrowserStorage.keys()
            .filter { it.startsWith("gameState_daily_challenge_${style}_") && it !in allowedKeys }
            .forEach { BrowserStorage.remove(it) }
    }

    actual fun getSavedDailyChallengeDays(yearMonth: String): Set<Int> {
        val style = GlobalPlatformConfig.gameplayStyle.name.lowercase()
        val prefix = "gameState_daily_challenge_${style}_$yearMonth-"
        return BrowserStorage.keys()
            .filter { it.startsWith(prefix) }
            .mapNotNull { it.substringAfterLast("-").toIntOrNull() }
            .toSet()
    }

    private fun keyFor(slot: GameSessionSlot): String = "gameState_${slot.key}"
}

