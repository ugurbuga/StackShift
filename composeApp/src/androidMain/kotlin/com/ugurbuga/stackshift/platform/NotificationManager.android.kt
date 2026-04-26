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
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class AndroidNotificationManager(private val context: Context) : NotificationManager {

    override fun scheduleMissYouNotification() {
        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(24, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "miss_you_notification",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    override fun cancelMissYouNotification() {
        WorkManager.getInstance(context).cancelUniqueWork("miss_you_notification")
    }

    override fun scheduleDailyChallengeNotification() {
        val data = androidx.work.Data.Builder()
            .putString(NotificationWorker.DATA_KEY_TYPE, NotificationWorker.TYPE_DAILY_CHALLENGE)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(24, TimeUnit.HOURS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "daily_challenge_notification",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    override fun cancelDailyChallengeNotification() {
        WorkManager.getInstance(context).cancelUniqueWork("daily_challenge_notification")
    }

    override fun sendTestNotification() {
        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>().build()
        WorkManager.getInstance(context).enqueue(workRequest)
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
