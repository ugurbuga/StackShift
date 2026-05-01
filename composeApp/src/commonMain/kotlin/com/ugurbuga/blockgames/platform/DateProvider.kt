package com.ugurbuga.blockgames.platform

data class CurrentDate(val year: Int, val month: Int, val day: Int)

expect fun getCurrentDate(): CurrentDate

expect fun currentEpochMillis(): Long

