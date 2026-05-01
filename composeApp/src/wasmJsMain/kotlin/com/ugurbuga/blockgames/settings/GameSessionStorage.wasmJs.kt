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
        GameSessionSlot.entries.forEach { slot ->
            BrowserStorage.remove(keyFor(slot))
        }
    }

    private fun keyFor(slot: GameSessionSlot): String = when (slot) {
        GameSessionSlot.Classic -> "stackshift.game.session.classic"
        GameSessionSlot.TimeAttack -> "stackshift.game.session.time_attack"
        GameSessionSlot.DailyChallenge -> "stackshift.game.session.daily_challenge"
    }
}

