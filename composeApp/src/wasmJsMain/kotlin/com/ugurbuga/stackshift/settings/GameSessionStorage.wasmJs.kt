package com.ugurbuga.stackshift.settings

import com.ugurbuga.stackshift.game.model.GameState

actual object GameSessionStorage {
    actual fun load(): GameState? = BrowserStorage.get(StorageKey)
        ?.takeIf(String::isNotBlank)
        ?.let(GameSessionCodec::decode)

    actual fun save(state: GameState) {
        BrowserStorage.set(StorageKey, GameSessionCodec.encode(state))
    }

    actual fun clear() {
        BrowserStorage.remove(StorageKey)
    }

    private const val StorageKey = "stackshift.game.session"
}

