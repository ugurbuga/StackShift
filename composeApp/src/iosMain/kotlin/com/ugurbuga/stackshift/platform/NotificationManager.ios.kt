package com.ugurbuga.stackshift.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

class IosNotificationManager : NotificationManager {
    override fun scheduleMissYouNotification() {
        // TODO: Implement iOS local notifications
    }

    override fun cancelMissYouNotification() {
        // TODO: Implement iOS local notifications
    }

    @Composable
    override fun RequestPermission() {
        // TODO: Implement iOS notification permissions
    }
}

@Composable
actual fun rememberNotificationManager(): NotificationManager {
    return remember { IosNotificationManager() }
}
