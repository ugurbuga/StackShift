package com.ugurbuga.stackshift.platform

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class AndroidNotificationManager(private val context: Context) : NotificationManager {

    override fun scheduleMissYouNotification() {
        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(15, TimeUnit.MINUTES)
            .setInitialDelay(15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "miss_you_notification",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }

    override fun cancelMissYouNotification() {
        WorkManager.getInstance(context).cancelUniqueWork("miss_you_notification")
    }

    @Composable
    override fun RequestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val launcher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { _ -> }
            
            LaunchedEffect(Unit) {
                // Small delay to ensure activity is fully resumed
                kotlinx.coroutines.delay(500)
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
actual fun rememberNotificationManager(): NotificationManager {
    val context = LocalContext.current
    return remember(context) { AndroidNotificationManager(context.applicationContext) }
}
