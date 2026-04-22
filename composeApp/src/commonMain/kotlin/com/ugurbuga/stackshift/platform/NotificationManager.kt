package com.ugurbuga.stackshift.platform

import androidx.compose.runtime.Composable

interface NotificationManager {
    fun scheduleMissYouNotification()
    fun cancelMissYouNotification()
    
    @Composable
    fun RequestPermission()
}

@Composable
expect fun rememberNotificationManager(): NotificationManager
