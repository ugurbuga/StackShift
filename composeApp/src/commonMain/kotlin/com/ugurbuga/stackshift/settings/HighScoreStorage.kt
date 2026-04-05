package com.ugurbuga.stackshift.settings

expect object HighScoreStorage {
    fun load(): Int
    fun save(highScore: Int)
}
