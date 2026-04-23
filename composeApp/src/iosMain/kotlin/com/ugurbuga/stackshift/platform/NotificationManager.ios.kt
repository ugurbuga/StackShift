package com.ugurbuga.stackshift.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationOptionBadge

class IosNotificationManager : NotificationManager {
    
    private val center = UNUserNotificationCenter.currentNotificationCenter()

    override fun scheduleMissYouNotification() {
        val content = UNMutableNotificationContent().apply {
            setTitle("StackShift")
            setBody("We missed you! Come back and play some more.")
            setSound(UNNotificationSound.defaultSound)
        }

        // 24 hours = 86400 seconds
        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(86400.0, repeats = true)
        val request = UNNotificationRequest.requestWithIdentifier("miss_you_notification", content, trigger)

        center.addNotificationRequest(request) { error ->
            if (error != null) {
                println("Error scheduling notification: ${error.localizedDescription}")
            }
        }
    }

    override fun cancelMissYouNotification() {
        center.removePendingNotificationRequestsWithIdentifiers(listOf("miss_you_notification"))
    }

    override fun scheduleDailyChallengeNotification() {
        val content = UNMutableNotificationContent().apply {
            setTitle("StackShift")
            setBody("Have you completed today's daily challenge tasks yet?")
            setSound(UNNotificationSound.defaultSound)
        }

        // 24 hours = 86400 seconds
        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(86400.0, repeats = true)
        val request = UNNotificationRequest.requestWithIdentifier("daily_challenge_notification", content, trigger)

        center.addNotificationRequest(request) { error ->
            if (error != null) {
                println("Error scheduling daily challenge notification: ${error.localizedDescription}")
            }
        }
    }

    override fun cancelDailyChallengeNotification() {
        center.removePendingNotificationRequestsWithIdentifiers(listOf("daily_challenge_notification"))
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
