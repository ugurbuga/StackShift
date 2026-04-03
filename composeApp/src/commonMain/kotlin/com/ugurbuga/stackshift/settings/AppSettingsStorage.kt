package com.ugurbuga.stackshift.settings

expect object AppSettingsStorage {
    fun load(): AppSettings
    fun save(settings: AppSettings)
}
