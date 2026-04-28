package com.ugurbuga.stackshift.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.ugurbuga.stackshift.settings.AppSettingsStorage
import com.ugurbuga.stackshift.settings.DailyChallengeReminderSlots
import com.ugurbuga.stackshift.settings.MissYouReminderMinimumInactivityMillis
import com.ugurbuga.stackshift.settings.MissYouReminderSlots
import com.ugurbuga.stackshift.settings.NotificationReminderSchedulingHorizonDays
import com.ugurbuga.stackshift.settings.ReminderTimeSlot
import com.ugurbuga.stackshift.settings.hasCompletedChallengeForDate
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.NSUserDefaults
import platform.Foundation.timeIntervalSince1970
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import stackshift.composeapp.generated.resources.Res
import stackshift.composeapp.generated.resources.app_title
import stackshift.composeapp.generated.resources.notification_daily_challenge_body
import stackshift.composeapp.generated.resources.notification_daily_challenge_title
import stackshift.composeapp.generated.resources.notification_miss_you_body
import stackshift.composeapp.generated.resources.notification_miss_you_title
import stackshift.composeapp.generated.resources.notification_test_body

private const val DayMillis = 24L * 60L * 60L * 1000L
private const val MinuteMillis = 60L * 1000L
private const val AppleLanguagesKey = "AppleLanguages"

class IosNotificationManager : NotificationManager {

    private val center = UNUserNotificationCenter.currentNotificationCenter()
    private val calendar = NSCalendar.currentCalendar

    private fun currentReminderTime(): Pair<Int, Int> {
        val components = calendar.components(
            NSCalendarUnitHour or NSCalendarUnitMinute,
            fromDate = NSDate(),
        )
        return components.hour.toInt() to components.minute.toInt()
    }

    private fun slotPassedToday(
        slot: ReminderTimeSlot,
        currentHour: Int,
        currentMinute: Int,
    ): Boolean = slot.hour < currentHour || (slot.hour == currentHour && slot.minute <= currentMinute)

    private fun slotDateComponents(
        dayOffset: Int,
        slot: ReminderTimeSlot,
    ): NSDateComponents {
        val futureDate = NSDate(timeIntervalSinceReferenceDate = NSDate().timeIntervalSinceReferenceDate + (dayOffset * 86400.0))
        val base = calendar.components(
            NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay,
            fromDate = futureDate,
        )
        return NSDateComponents().apply {
            setYear(base.year)
            setMonth(base.month)
            setDay(base.day)
            setHour(slot.hour.toLong())
            setMinute(slot.minute.toLong())
        }
    }

    private fun slotIdentifier(
        prefix: String,
        components: NSDateComponents,
        slot: ReminderTimeSlot,
    ): String = buildString {
        append(prefix)
        append('_')
        append(components.year)
        append('_')
        append(components.month)
        append('_')
        append(components.day)
        append('_')
        append(slot.idSuffix)
    }

    private fun scheduledEpochMillis(
        dayOffset: Int,
        slot: ReminderTimeSlot,
        currentHour: Int,
        currentMinute: Int,
    ): Long {
        val now = currentEpochMillis()
        val currentMinutesOfDay = currentHour * 60 + currentMinute
        val targetMinutesOfDay = slot.hour * 60 + slot.minute
        val deltaMinutes = (dayOffset * 24 * 60) + (targetMinutesOfDay - currentMinutesOfDay)
        return now + (deltaMinutes.toLong() * MinuteMillis)
    }

    private fun pendingIdentifiers(prefix: String, slots: List<ReminderTimeSlot>): List<String> = buildList {
        repeat(NotificationReminderSchedulingHorizonDays) { dayOffset ->
            slots.forEach { slot ->
                add(slotIdentifier(prefix, slotDateComponents(dayOffset, slot), slot))
            }
        }
    }

    private fun <T> withPreferredLanguage(localeTag: String, block: () -> T): T {
        val defaults = NSUserDefaults.standardUserDefaults
        val previousLanguages = defaults.objectForKey(AppleLanguagesKey)
        if (localeTag.isBlank()) {
            defaults.removeObjectForKey(AppleLanguagesKey)
        } else {
            defaults.setObject(listOf(localeTag), forKey = AppleLanguagesKey)
        }
        return try {
            block()
        } finally {
            if (previousLanguages == null) {
                defaults.removeObjectForKey(AppleLanguagesKey)
            } else {
                defaults.setObject(previousLanguages, forKey = AppleLanguagesKey)
            }
        }
    }

    private fun localizedText(localeTag: String, resource: StringResource): String =
        withPreferredLanguage(localeTag) {
            runBlocking { getString(resource) }
        }

    override fun scheduleMissYouNotification() {
        val settings = AppSettingsStorage.load()
        val localeTag = settings.language.localeTag
        val title = localizedText(localeTag, Res.string.notification_miss_you_title)
        val body = localizedText(localeTag, Res.string.notification_miss_you_body)
        val (currentHour, currentMinute) = currentReminderTime()
        repeat(NotificationReminderSchedulingHorizonDays) { dayOffset ->
            MissYouReminderSlots.forEach { slot ->
                if (dayOffset == 0 && slotPassedToday(slot, currentHour, currentMinute)) return@forEach
                val components = slotDateComponents(dayOffset, slot)
                val scheduledAt = scheduledEpochMillis(dayOffset, slot, currentHour, currentMinute)
                val lastOpenedAt = settings.lastAppOpenedAtEpochMillis
                val inactiveEnough = lastOpenedAt <= 0L ||
                    (scheduledAt - lastOpenedAt) >= MissYouReminderMinimumInactivityMillis
                if (!inactiveEnough) return@forEach

                val content = UNMutableNotificationContent().apply {
                    setTitle(title)
                    setBody(body)
                    setSound(UNNotificationSound.defaultSound)
                }
                val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
                    dateComponents = components,
                    repeats = false,
                )
                val request = UNNotificationRequest.requestWithIdentifier(
                    slotIdentifier("miss_you", components, slot),
                    content,
                    trigger,
                )
                center.addNotificationRequest(request) { error ->
                    if (error != null) {
                        println("Error scheduling miss-you notification: ${error.localizedDescription}")
                    }
                }
            }
        }
    }

    override fun cancelMissYouNotification() {
        center.removePendingNotificationRequestsWithIdentifiers(pendingIdentifiers("miss_you", MissYouReminderSlots))
    }

    override fun scheduleDailyChallengeNotification() {
        val settings = AppSettingsStorage.load()
        val localeTag = settings.language.localeTag
        val title = localizedText(localeTag, Res.string.notification_daily_challenge_title)
        val body = localizedText(localeTag, Res.string.notification_daily_challenge_body)
        val today = getCurrentDate()
        val todayCompleted = settings.hasCompletedChallengeForDate(today)
        val (currentHour, currentMinute) = currentReminderTime()
        repeat(NotificationReminderSchedulingHorizonDays) { dayOffset ->
            DailyChallengeReminderSlots.forEach { slot ->
                if (dayOffset == 0 && todayCompleted) return@forEach
                if (dayOffset == 0 && slotPassedToday(slot, currentHour, currentMinute)) return@forEach
                val components = slotDateComponents(dayOffset, slot)
                val content = UNMutableNotificationContent().apply {
                    setTitle(title)
                    setBody(body)
                    setSound(UNNotificationSound.defaultSound)
                }
                val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
                    dateComponents = components,
                    repeats = false,
                )
                val request = UNNotificationRequest.requestWithIdentifier(
                    slotIdentifier("daily_challenge", components, slot),
                    content,
                    trigger,
                )
                center.addNotificationRequest(request) { error ->
                    if (error != null) {
                        println("Error scheduling daily challenge notification: ${error.localizedDescription}")
                    }
                }
            }
        }
    }

    override fun cancelDailyChallengeNotification() {
        center.removePendingNotificationRequestsWithIdentifiers(pendingIdentifiers("daily_challenge", DailyChallengeReminderSlots))
    }

    override fun sendTestNotification() {
        val localeTag = AppSettingsStorage.load().language.localeTag
        val content = UNMutableNotificationContent().apply {
            setTitle(localizedText(localeTag, Res.string.app_title))
            setBody(localizedText(localeTag, Res.string.notification_test_body))
            setSound(UNNotificationSound.defaultSound)
        }
        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(5.0, repeats = false)
        val request = UNNotificationRequest.requestWithIdentifier("test_notification", content, trigger)
        center.addNotificationRequest(request, withCompletionHandler = null)
    }

    @Composable
    override fun RequestPermission() {
        LaunchedEffect(Unit) {
            center.requestAuthorizationWithOptions(
                UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
            ) { _, error ->
                if (error != null) {
                    println("Error requesting notification permission: ${error.localizedDescription}")
                }
            }
        }
    }
}

@Composable
actual fun rememberNotificationManager(): NotificationManager {
    return remember { IosNotificationManager() }
}
