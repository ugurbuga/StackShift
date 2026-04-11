package com.ugurbuga.stackshift.telemetry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.analytics.analytics
import dev.gitlive.firebase.analytics.logEvent
import dev.gitlive.firebase.crashlytics.crashlytics
import dev.gitlive.firebase.apps
import dev.gitlive.firebase.initialize
import platform.Foundation.NSBundle
import platform.UIKit.UIDevice

@Composable
actual fun rememberAppTelemetry(): AppTelemetry {
    if (!isFirebaseConfigured()) {
        return NoOpAppTelemetry
    }

    runCatching {
        if (Firebase.apps().isEmpty()) {
            Firebase.initialize()
        }
    }.getOrElse {
        return NoOpAppTelemetry
    }

    return remember { IosFirebaseTelemetry(::iosDeviceInfo) }
}

private fun isFirebaseConfigured(): Boolean = NSBundle.mainBundle.pathForResource(
    name = "GoogleService-Info",
    ofType = "plist",
) != null

private fun iosDeviceInfo(): Map<String, String> = mapOf(
    TelemetryParamKeys.PackageName to (NSBundle.mainBundle.bundleIdentifier ?: ""),
    TelemetryParamKeys.Manufacturer to "Apple",
    TelemetryParamKeys.Model to UIDevice.currentDevice.model,
    TelemetryParamKeys.Device to UIDevice.currentDevice.name,
    TelemetryParamKeys.Product to UIDevice.currentDevice.systemName,
    TelemetryParamKeys.Release to UIDevice.currentDevice.systemVersion,
    TelemetryParamKeys.SdkInt to UIDevice.currentDevice.systemVersion,
)

private class IosFirebaseTelemetry(
    private val deviceInfoProvider: () -> Map<String, String>,
) : AppTelemetry {
    override fun logScreen(screenName: String) {
        Firebase.analytics.logEvent(
            TelemetryEventNames.ScreenView,
            mapOf(
                TelemetryParamKeys.ScreenName to screenName,
                TelemetryParamKeys.ScreenClass to screenName,
            ),
        )
        Firebase.crashlytics.log("screen_view:$screenName")
    }

    override fun logEvent(eventName: String, parameters: Map<String, String>) {
        Firebase.analytics.logEvent(eventName, parameters.mapValues { it.value as Any })
        Firebase.crashlytics.log("event:$eventName ${parameters.entries.joinToString(separator = ";") { "${it.key}=${it.value}" }}")
    }

    override fun logUserProperty(name: String, value: String?) {
        Firebase.analytics.setUserProperty(name, value.orEmpty())
        Firebase.crashlytics.setCustomKey(name, value.orEmpty())
    }

    override fun logHighScoreReached(newScore: Int, previousHighScore: Int) {
        val deviceInfo = deviceInfoProvider()
        val eventParameters = buildMap<String, Any> {
            put(TelemetryParamKeys.Score, newScore)
            put(TelemetryParamKeys.PreviousScore, previousHighScore)
            deviceInfo.forEach { (key, value) -> put(key, value) }
        }
        Firebase.analytics.logEvent(TelemetryEventNames.HighScoreReached, eventParameters)
        Firebase.crashlytics.log("high_score_reached:$newScore")
        Firebase.crashlytics.setCustomKey(TelemetryParamKeys.Score, newScore)
        Firebase.crashlytics.setCustomKey(TelemetryParamKeys.PreviousScore, previousHighScore)
        deviceInfo.forEach { (key, value) -> Firebase.crashlytics.setCustomKey(key, value) }
    }

    override fun recordException(throwable: Throwable) {
        Firebase.crashlytics.recordException(throwable)
    }

    override fun logUserAction(action: String, parameters: Map<String, String>) {
        logEvent(
            TelemetryEventNames.UserAction,
            buildMap {
                put(TelemetryParamKeys.Action, action)
                putAll(parameters)
            },
        )
    }
}
