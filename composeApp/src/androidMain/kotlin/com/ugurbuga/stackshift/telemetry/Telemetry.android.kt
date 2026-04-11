package com.ugurbuga.stackshift.telemetry

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.ugurbuga.stackshift.settings.AppContextHolder

private const val DeviceInfoPrefix = "device_"
private const val CrashlyticsHighScoreMessage = "High score reached"

private class FirebaseAppTelemetry(
    context: Context,
) : AppTelemetry {
    private val applicationContext = context.applicationContext
    private val analytics = FirebaseAnalytics.getInstance(applicationContext)
    private val crashlytics = FirebaseCrashlytics.getInstance()

    override fun logScreen(screenName: String) {
        analytics.logEvent(
            TelemetryEventNames.ScreenView,
            bundleOf(
                mapOf(
                    TelemetryParamKeys.ScreenName to screenName,
                    TelemetryParamKeys.ScreenClass to screenName,
                ),
            ),
        )
        crashlytics.log("screen_view:$screenName")
    }

    override fun logEvent(eventName: String, parameters: Map<String, String>) {
        analytics.logEvent(eventName, bundleOf(parameters))
        crashlytics.log("event:$eventName ${parameters.entries.joinToString(separator = ";") { "${it.key}=${it.value}" }}")
    }

    override fun logUserProperty(name: String, value: String?) {
        analytics.setUserProperty(name, value)
        crashlytics.setCustomKey(name, value ?: "")
    }

    override fun logHighScoreReached(newScore: Int, previousHighScore: Int) {
        val deviceInfo = deviceInfo()
        val eventParameters = buildMap {
            put(TelemetryParamKeys.Score, newScore.toString())
            put(TelemetryParamKeys.PreviousScore, previousHighScore.toString())
            deviceInfo.forEach { (key, value) -> put(key, value) }
        }
        analytics.logEvent(TelemetryEventNames.HighScoreReached, bundleOf(eventParameters))
        crashlytics.log(CrashlyticsHighScoreMessage)
        crashlytics.setCustomKey(TelemetryParamKeys.Score, newScore)
        crashlytics.setCustomKey(TelemetryParamKeys.PreviousScore, previousHighScore)
        deviceInfo.forEach { (key, value) -> crashlytics.setCustomKey(key, value) }
    }

    override fun recordException(throwable: Throwable) {
        crashlytics.recordException(throwable)
    }

    override fun logUserAction(action: String, parameters: Map<String, String>) {
        logEvent(TelemetryEventNames.UserAction, buildMap {
            put(TelemetryParamKeys.Action, action)
            putAll(parameters)
        })
    }

    private fun deviceInfo(): Map<String, String> = mapOf(
        TelemetryParamKeys.PackageName to applicationContext.packageName,
        TelemetryParamKeys.Manufacturer to Build.MANUFACTURER.orEmpty(),
        TelemetryParamKeys.Model to Build.MODEL.orEmpty(),
        TelemetryParamKeys.Device to Build.DEVICE.orEmpty(),
        TelemetryParamKeys.Product to Build.PRODUCT.orEmpty(),
        TelemetryParamKeys.Release to Build.VERSION.RELEASE.orEmpty(),
        TelemetryParamKeys.SdkInt to Build.VERSION.SDK_INT.toString(),
    ).mapKeys { (key, _) -> "$DeviceInfoPrefix$key" }

    private fun bundleOf(parameters: Map<String, String>): Bundle = Bundle().apply {
        parameters.forEach { (key, value) -> putString(key, value) }
    }
}

@Composable
actual fun rememberAppTelemetry(): AppTelemetry {
    val context = AppContextHolder.context
    return remember(context) { FirebaseAppTelemetry(context) }
}
