package com.ugurbuga.stackshift.settings

import kotlinx.browser.window

internal object BrowserStorage {
    fun get(key: String): String? = runCatching {
        window.localStorage.getItem(key)
    }.getOrNull()

    fun set(key: String, value: String) {
        runCatching {
            window.localStorage.setItem(key, value)
        }
    }

    fun remove(key: String) {
        runCatching {
            window.localStorage.removeItem(key)
        }
    }
}

