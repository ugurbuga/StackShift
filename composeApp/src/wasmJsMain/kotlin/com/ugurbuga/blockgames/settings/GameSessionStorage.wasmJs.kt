package com.ugurbuga.blockgames.settings

import com.ugurbuga.blockgames.game.model.GameState

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
        BrowserStorage.remove(keyFor(GameSessionSlot.Classic))
        BrowserStorage.remove(keyFor(GameSessionSlot.TimeAttack))
        BrowserStorage.keys().filter { it.startsWith("stackshift.game.session.daily_challenge_") }
            .forEach { BrowserStorage.remove(it) }
    }

    actual fun cleanup(allowedDateIds: List<String>) {
        val allowedKeys = allowedDateIds.map { "stackshift.game.session.daily_challenge_$it" }.toSet()
        BrowserStorage.keys()
            .filter { it.startsWith("stackshift.game.session.daily_challenge_") && it !in allowedKeys }
            .forEach { BrowserStorage.remove(it) }
    }

    private fun keyFor(slot: GameSessionSlot): String = when (slot) {
        GameSessionSlot.Classic -> "stackshift.game.session.classic"
        GameSessionSlot.TimeAttack -> "stackshift.game.session.time_attack"
        is GameSessionSlot.DailyChallenge -> "stackshift.game.session.daily_challenge_${slot.dateId}"
    }
}

