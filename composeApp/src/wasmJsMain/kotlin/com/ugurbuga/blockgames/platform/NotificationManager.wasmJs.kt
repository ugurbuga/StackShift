package com.ugurbuga.blockgames.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

class WasmNotificationManager : NotificationManager {
    override fun scheduleMissYouNotification() {}
    override fun cancelMissYouNotification() {}
    override fun scheduleDailyChallengeNotification() {}
    override fun cancelDailyChallengeNotification() {}
    override fun sendTestNotification() {}
    @Composable override fun RequestPermission() {}
}

@Composable
actual fun rememberNotificationManager(): NotificationManager {
    return remember { WasmNotificationManager() }
}
