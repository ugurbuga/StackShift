package com.ugurbuga.stackshift.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

class WasmNotificationManager : NotificationManager {
    override fun scheduleMissYouNotification() {}
    override fun cancelMissYouNotification() {}
    @Composable override fun RequestPermission() {}
}

@Composable
actual fun rememberNotificationManager(): NotificationManager {
    return remember { WasmNotificationManager() }
}
