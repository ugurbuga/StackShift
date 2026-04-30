package com.ugurbuga.blockgames.platform

import java.time.LocalDate

actual fun getCurrentDate(): CurrentDate {
    val now = LocalDate.now()
    return CurrentDate(
        year = now.year,
        month = now.monthValue,
        day = now.dayOfMonth
    )
}

actual fun currentEpochMillis(): Long = System.currentTimeMillis()

