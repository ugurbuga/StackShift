package com.ugurbuga.stackshift.platform

import java.time.LocalDate

actual fun getCurrentDate(): CurrentDate {
    val now = LocalDate.now()
    return CurrentDate(
        year = now.year,
        month = now.monthValue,
        day = now.dayOfMonth
    )
}
