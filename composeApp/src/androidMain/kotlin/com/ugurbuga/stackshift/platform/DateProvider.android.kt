package com.ugurbuga.stackshift.platform

import java.util.Calendar

actual fun getCurrentDate(): CurrentDate {
    val calendar = Calendar.getInstance()
    return CurrentDate(
        year = calendar.get(Calendar.YEAR),
        month = calendar.get(Calendar.MONTH) + 1,
        day = calendar.get(Calendar.DAY_OF_MONTH)
    )
}

actual fun currentEpochMillis(): Long = System.currentTimeMillis()

