package com.ugurbuga.stackshift.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.graphics.toColorInt
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ugurbuga.blockgames.R
import com.ugurbuga.stackshift.settings.AppSettingsStorage
import com.ugurbuga.stackshift.settings.shouldSendDailyChallengeReminder
import com.ugurbuga.stackshift.settings.shouldSendMissYouReminder
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import stackshift.composeapp.generated.resources.Res
import stackshift.composeapp.generated.resources.app_title
import stackshift.composeapp.generated.resources.notification_daily_challenge_body
import stackshift.composeapp.generated.resources.notification_daily_challenge_title
import stackshift.composeapp.generated.resources.notification_miss_you_body
import stackshift.composeapp.generated.resources.notification_miss_you_title
import stackshift.composeapp.generated.resources.notification_reminders_channel_name
import stackshift.composeapp.generated.resources.notification_test_body
import java.util.Locale

class NotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val CHANNEL_ID = "stackshift_notification_channel_v2"

        const val TAG_MISS_YOU = "miss_you_notification"
        const val TAG_DAILY_CHALLENGE = "daily_challenge_notification"

        const val DATA_KEY_TYPE = "notification_type"

        const val TYPE_MISS_YOU = "miss_you"
        const val TYPE_DAILY_CHALLENGE = "daily_challenge"
        const val TYPE_TEST = "test"
    }

    override suspend fun doWork(): Result {
        val type = inputData.getString(DATA_KEY_TYPE) ?: TYPE_MISS_YOU
        val settings = AppSettingsStorage.load()
        val shouldShow = when (type) {
            TYPE_DAILY_CHALLENGE -> settings.shouldSendDailyChallengeReminder(getCurrentDate())
            TYPE_MISS_YOU -> settings.shouldSendMissYouReminder(currentEpochMillis())
            TYPE_TEST -> true
            else -> true
        }
        if (shouldShow) showNotification(type)
        return Result.success()
    }

    private suspend fun showNotification(type: String) {
        val settings = AppSettingsStorage.load()
        val appLocale = Locale.forLanguageTag(settings.language.localeTag)

        // Temporarily set default locale to fetch strings in app-selected language
        val originalLocale = Locale.getDefault()
        Locale.setDefault(appLocale)

        suspend fun localized(resource: StringResource): String =
            runCatching { getString(resource) }.getOrDefault("")

        val appName = applicationContext.applicationInfo
            .loadLabel(applicationContext.packageManager)
            .toString()

        val title = when (type) {
            TYPE_DAILY_CHALLENGE -> localized(Res.string.notification_daily_challenge_title)
            TYPE_TEST -> localized(Res.string.app_title)
            else -> localized(Res.string.notification_miss_you_title)
        }.ifBlank { appName }

        val message = when (type) {
            TYPE_DAILY_CHALLENGE -> localized(Res.string.notification_daily_challenge_body)
            TYPE_TEST -> localized(Res.string.notification_test_body)
            else -> localized(Res.string.notification_miss_you_body)
        }

        val channelName = localized(Res.string.notification_reminders_channel_name).ifBlank { appName }

        Locale.setDefault(originalLocale)

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationId = when (type) {
            TYPE_DAILY_CHALLENGE -> 1002
            TYPE_TEST -> 1003
            else -> 1001
        }
        val largeIcon = runCatching {
            applicationContext.packageManager
                .getApplicationIcon(applicationContext.packageName)
                .toBitmap()
        }.getOrNull()

        val notificationBuilder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor("#4FC3F7".toColorInt())
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        largeIcon?.let(notificationBuilder::setLargeIcon)

        val notification = notificationBuilder.build()

        notificationManager.notify(notificationId, notification)
    }
}

private fun Drawable.toBitmap(): Bitmap {
    if (this is BitmapDrawable) {
        bitmap?.let { return it }
    }

    val width = intrinsicWidth.takeIf { it > 0 } ?: 256
    val height = intrinsicHeight.takeIf { it > 0 } ?: 256
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap
}

