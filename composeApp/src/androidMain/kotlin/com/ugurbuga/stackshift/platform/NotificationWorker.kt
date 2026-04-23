package com.ugurbuga.stackshift.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ugurbuga.stackshift.settings.AppSettingsStorage
import org.jetbrains.compose.resources.getString
import stackshift.composeapp.generated.resources.Res
import stackshift.composeapp.generated.resources.notification_daily_challenge_body
import stackshift.composeapp.generated.resources.notification_daily_challenge_title
import stackshift.composeapp.generated.resources.notification_miss_you_body
import stackshift.composeapp.generated.resources.notification_miss_you_title
import stackshift.composeapp.generated.resources.notification_reminders_channel_name
import java.util.Locale

class NotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val CHANNEL_ID = "stackshift_notification_channel"
        private const val CHANNEL_NAME_DEFAULT = "Reminders"
        
        const val DATA_KEY_TYPE = "notification_type"
        const val TYPE_MISS_YOU = "miss_you"
        const val TYPE_DAILY_CHALLENGE = "daily_challenge"
    }

    override suspend fun doWork(): androidx.work.ListenableWorker.Result {
        val type = inputData.getString(DATA_KEY_TYPE) ?: TYPE_MISS_YOU
        showNotification(type)
        return androidx.work.ListenableWorker.Result.success()
    }

    private suspend fun showNotification(type: String) {
        val settings = AppSettingsStorage.load()
        val appLocale = Locale.forLanguageTag(settings.language.localeTag)
        
        // Temporarily set default locale to fetch strings in app-selected language
        val originalLocale = Locale.getDefault()
        Locale.setDefault(appLocale)
        
        val title = try {
            if (type == TYPE_DAILY_CHALLENGE) {
                getString(Res.string.notification_daily_challenge_title)
            } else {
                getString(Res.string.notification_miss_you_title)
            }
        } catch (_: Exception) {
            "StackShift"
        }
        
        val message = try {
            if (type == TYPE_DAILY_CHALLENGE) {
                getString(Res.string.notification_daily_challenge_body)
            } else {
                getString(Res.string.notification_miss_you_body)
            }
        } catch (_: Exception) {
            if (type == TYPE_DAILY_CHALLENGE) {
                "Have you completed today's daily challenge tasks yet?"
            } else {
                "We missed you! Come back and play some more."
            }
        }
        
        val channelName = try {
            getString(Res.string.notification_reminders_channel_name)
        } catch (_: Exception) {
            CHANNEL_NAME_DEFAULT
        }
        
        Locale.setDefault(originalLocale)

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationId = if (type == TYPE_DAILY_CHALLENGE) 1002 else 1001

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(applicationContext.applicationInfo.icon)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}
