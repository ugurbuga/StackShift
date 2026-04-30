package com.ugurbuga.blockgames.platform

import androidx.compose.runtime.Composable

interface NotificationManager {
    fun scheduleMissYouNotification()
    fun cancelMissYouNotification()

    fun scheduleDailyChallengeNotification()
    fun cancelDailyChallengeNotification()

    fun sendTestNotification()
    
    @Composable
    fun RequestPermission()
}

@Composable
expect fun rememberNotificationManager(): NotificationManager
