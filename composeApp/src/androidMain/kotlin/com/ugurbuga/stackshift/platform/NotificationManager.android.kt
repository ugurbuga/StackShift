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
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.ugurbuga.stackshift.settings.AppSettingsStorage
import com.ugurbuga.stackshift.settings.DailyChallengeReminderSlots
import com.ugurbuga.stackshift.settings.MissYouReminderMinimumInactivityMillis
import com.ugurbuga.stackshift.settings.MissYouReminderSlots
import com.ugurbuga.stackshift.settings.NotificationReminderSchedulingHorizonDays
import com.ugurbuga.stackshift.settings.hasCompletedChallengeForDate

import java.util.Calendar
import java.util.concurrent.TimeUnit

class AndroidNotificationManager(private val context: Context) : NotificationManager {

    private fun scheduleOneTimeNotification(
        uniqueWorkName: String,
        tag: String,
        type: String,
        triggerAtMillis: Long,
    ) {
        val delayMillis = (triggerAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(
                androidx.work.Data.Builder()
                    .putString(NotificationWorker.DATA_KEY_TYPE, type)
                    .build()
            )
            .addTag(tag)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueWorkName,
            ExistingWorkPolicy.REPLACE,
            workRequest,
        )
    }

    private fun slotCalendar(
        dayOffset: Int,
        hourOfDay: Int,
        minute: Int,
    ): Calendar = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, dayOffset)
        set(Calendar.HOUR_OF_DAY, hourOfDay)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private fun slotWorkName(
        prefix: String,
        calendar: Calendar,
        slotSuffix: String,
    ): String = buildString {
        append(prefix)
        append('_')
        append(calendar.get(Calendar.YEAR))
        append('_')
        append(calendar.get(Calendar.MONTH) + 1)
        append('_')
        append(calendar.get(Calendar.DAY_OF_MONTH))
        append('_')
        append(slotSuffix)
    }

    override fun scheduleMissYouNotification() {
        val settings = AppSettingsStorage.load()
        val nowMillis = System.currentTimeMillis()
        repeat(NotificationReminderSchedulingHorizonDays) { dayOffset ->
            MissYouReminderSlots.forEach { slot ->
                val trigger = slotCalendar(
                    dayOffset = dayOffset,
                    hourOfDay = slot.hour,
                    minute = slot.minute,
                )
                val triggerAtMillis = trigger.timeInMillis
                if (triggerAtMillis <= nowMillis) return@forEach
                val lastOpenedAt = settings.lastAppOpenedAtEpochMillis
                val inactiveEnough = lastOpenedAt <= 0L ||
                    (triggerAtMillis - lastOpenedAt) >= MissYouReminderMinimumInactivityMillis
                if (!inactiveEnough) return@forEach
                scheduleOneTimeNotification(
                    uniqueWorkName = slotWorkName("miss_you", trigger, slot.idSuffix),
                    tag = NotificationWorker.TAG_MISS_YOU,
                    type = NotificationWorker.TYPE_MISS_YOU,
                    triggerAtMillis = triggerAtMillis,
                )
            }
        }
    }

    override fun cancelMissYouNotification() {
        WorkManager.getInstance(context).cancelAllWorkByTag(NotificationWorker.TAG_MISS_YOU)
    }

    override fun scheduleDailyChallengeNotification() {
        val settings = AppSettingsStorage.load()
        val today = getCurrentDate()
        val todayCompleted = settings.hasCompletedChallengeForDate(today)
        val nowMillis = System.currentTimeMillis()
        repeat(NotificationReminderSchedulingHorizonDays) { dayOffset ->
            DailyChallengeReminderSlots.forEach { slot ->
                if (dayOffset == 0 && todayCompleted) return@forEach
                val trigger = slotCalendar(
                    dayOffset = dayOffset,
                    hourOfDay = slot.hour,
                    minute = slot.minute,
                )
                val triggerAtMillis = trigger.timeInMillis
                if (triggerAtMillis <= nowMillis) return@forEach
                scheduleOneTimeNotification(
                    uniqueWorkName = slotWorkName("daily_challenge", trigger, slot.idSuffix),
                    tag = NotificationWorker.TAG_DAILY_CHALLENGE,
                    type = NotificationWorker.TYPE_DAILY_CHALLENGE,
                    triggerAtMillis = triggerAtMillis,
                )
            }
        }
    }

    override fun cancelDailyChallengeNotification() {
        WorkManager.getInstance(context).cancelAllWorkByTag(NotificationWorker.TAG_DAILY_CHALLENGE)
    }

    override fun sendTestNotification() {
        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInputData(
                androidx.work.Data.Builder()
                    .putString(NotificationWorker.DATA_KEY_TYPE, NotificationWorker.TYPE_TEST)
                    .build()
            )
            .build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }

    @Composable
    override fun RequestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val launcher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { _ -> }

            LaunchedEffect(Unit) {
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
