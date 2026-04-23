package com.ugurbuga.stackshift.platform

import androidx.compose.runtime.Composable

interface NotificationManager {
    fun scheduleMissYouNotification()
    fun cancelMissYouNotification()

    fun scheduleDailyChallengeNotification()
    fun cancelDailyChallengeNotification()
    
    @Composable
    fun RequestPermission()
}

@Composable
expect fun rememberNotificationManager(): NotificationManager
