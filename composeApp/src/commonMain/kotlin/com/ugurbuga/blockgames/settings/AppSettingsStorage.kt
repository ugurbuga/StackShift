package com.ugurbuga.blockgames.settings

expect object AppSettingsStorage {
    fun load(): AppSettings
    fun save(settings: AppSettings)
}
