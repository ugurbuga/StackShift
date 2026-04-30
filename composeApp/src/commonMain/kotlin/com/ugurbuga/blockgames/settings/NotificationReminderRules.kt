package com.ugurbuga.blockgames.settings

import com.ugurbuga.blockgames.platform.CurrentDate

private const val HourMillis = 60L * 60L * 1000L
const val MissYouReminderMinimumInactivityMillis = 6L * HourMillis
const val NotificationReminderSchedulingHorizonDays = 30

data class ReminderTimeSlot(
    val hour: Int,
    val minute: Int,
    val idSuffix: String,
)

val DailyChallengeReminderSlots = listOf(
    ReminderTimeSlot(hour = 12, minute = 0, idSuffix = "1200"),
    ReminderTimeSlot(hour = 18, minute = 0, idSuffix = "1800"),
)

val MissYouReminderSlots = listOf(
    ReminderTimeSlot(hour = 15, minute = 0, idSuffix = "1500"),
    ReminderTimeSlot(hour = 21, minute = 0, idSuffix = "2100"),
)

fun AppSettings.recordAppOpened(epochMillis: Long): AppSettings =
    this.copy(lastAppOpenedAtEpochMillis = epochMillis.coerceAtLeast(0L)).sanitized()

fun AppSettings.hasCompletedChallengeForDate(date: CurrentDate): Boolean =
    date.day in challengeProgress.completedDays[completedChallengeDaysKey(date.year, date.month)].orEmpty()

fun AppSettings.shouldSendDailyChallengeReminder(date: CurrentDate): Boolean =
    !hasCompletedChallengeForDate(date)

fun AppSettings.shouldSendMissYouReminder(nowEpochMillis: Long): Boolean {
    val lastOpenedAt = lastAppOpenedAtEpochMillis
    if (lastOpenedAt <= 0L) return true
    return (nowEpochMillis - lastOpenedAt).coerceAtLeast(0L) >= MissYouReminderMinimumInactivityMillis
}

