package com.ugurbuga.stackshift.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

class JvmNotificationManager : NotificationManager {
    override fun scheduleMissYouNotification() {}
    override fun cancelMissYouNotification() {}
    override fun scheduleDailyChallengeNotification() {}
    override fun cancelDailyChallengeNotification() {}
    @Composable override fun RequestPermission() {}
}

@Composable
actual fun rememberNotificationManager(): NotificationManager {
    return remember { JvmNotificationManager() }
}
