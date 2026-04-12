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
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIDevice

private const val TelemetryNamespace = "com.ugurbuga.stackshift.telemetry"
private const val DeviceInfoPrefix = "device_"
private const val CrashlyticsScreenViewLog = "screen_view"
private const val CrashlyticsEventLog = "event"
private const val CrashlyticsHighScoreLog = "high_score_reached"
private const val AppleManufacturer = "Apple"

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
    TelemetryParamKeys.ClientId to resolveIosClientId(),
    TelemetryParamKeys.PackageName to (NSBundle.mainBundle.bundleIdentifier ?: ""),
    TelemetryParamKeys.Manufacturer to AppleManufacturer,
    TelemetryParamKeys.Model to UIDevice.currentDevice.model,
    TelemetryParamKeys.Device to UIDevice.currentDevice.name,
    TelemetryParamKeys.Product to UIDevice.currentDevice.systemName,
    TelemetryParamKeys.Release to UIDevice.currentDevice.systemVersion,
    TelemetryParamKeys.SdkInt to UIDevice.currentDevice.systemVersion,
).mapKeys { (key, _) -> if (key == TelemetryParamKeys.ClientId) key else "$DeviceInfoPrefix$key" }

private class IosFirebaseTelemetry(
    private val deviceInfoProvider: () -> Map<String, String>,
) : AppTelemetry {
    private val defaults = NSUserDefaults.standardUserDefaults
    private val clientId = resolveClientId()

    init {
        setTelemetryIdentity()
    }

    override fun logScreen(screenName: String) {
        Firebase.analytics.logEvent(
            TelemetryEventNames.ScreenView,
            telemetryParameters(
                mapOf(
                    TelemetryParamKeys.ScreenName to screenName,
                    TelemetryParamKeys.ScreenClass to screenName,
                ),
            ),
        )
        Firebase.crashlytics.log("$CrashlyticsScreenViewLog:$screenName client_id=$clientId")
    }

    override fun logEvent(eventName: String, parameters: Map<String, String>) {
        val mergedParameters = telemetryParameters(parameters)
        Firebase.analytics.logEvent(eventName, mergedParameters)
        Firebase.crashlytics.log("$CrashlyticsEventLog:$eventName ${mergedParameters.entries.joinToString(separator = ";") { "${it.key}=${it.value}" }}")
    }

    override fun logUserProperty(name: String, value: String?) {
        Firebase.analytics.setUserProperty(name, value.orEmpty())
        Firebase.crashlytics.setCustomKey(name, value.orEmpty())
    }

    override fun logHighScoreReached(newScore: Int, previousHighScore: Int) {
        val eventParameters = buildMap<String, Any> {
            put(TelemetryParamKeys.Score, newScore)
            put(TelemetryParamKeys.PreviousScore, previousHighScore)
            telemetryContext().forEach { (key, value) -> put(key, value) }
        }
        Firebase.analytics.logEvent(TelemetryEventNames.HighScoreReached, eventParameters)
        Firebase.crashlytics.log("$CrashlyticsHighScoreLog:$newScore client_id=$clientId")
        Firebase.crashlytics.setCustomKey(TelemetryParamKeys.Score, newScore)
        Firebase.crashlytics.setCustomKey(TelemetryParamKeys.PreviousScore, previousHighScore)
        telemetryContext().forEach { (key, value) -> Firebase.crashlytics.setCustomKey(key, value) }
    }

    override fun recordException(throwable: Throwable) {
        Firebase.analytics.logEvent(
            TelemetryEventNames.Exception,
            telemetryParameters(
                mapOf(
                    TelemetryParamKeys.ExceptionType to throwable::class.simpleName.orEmpty(),
                    TelemetryParamKeys.ExceptionMessage to (throwable.message ?: throwable.toString()),
                ),
            ),
        )
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

    private fun setTelemetryIdentity() {
        val context = telemetryContext()
        Firebase.analytics.setUserProperty(TelemetryUserPropertyNames.ClientId, clientId)
        Firebase.crashlytics.setCustomKey(TelemetryParamKeys.ClientId, clientId)
        context.forEach { (key, value) -> Firebase.crashlytics.setCustomKey(key, value) }
    }

    private fun telemetryParameters(parameters: Map<String, String>): Map<String, Any> = buildMap {
        telemetryContext().forEach { (key, value) -> put(key, value) }
        parameters.forEach { (key, value) -> put(key, value) }
    }

    private fun telemetryContext(): Map<String, String> = buildMap {
        put(TelemetryParamKeys.ClientId, clientId)
        putAll(deviceInfoProvider())
    }

    private fun resolveClientId(): String = defaults.stringForKey(TelemetryStorageKeys.ClientId)
        ?.takeIf(String::isNotBlank)
        ?: NSUUID().UUIDString.also { defaults.setObject(it, forKey = TelemetryStorageKeys.ClientId) }
}

private fun resolveIosClientId(): String = NSUserDefaults.standardUserDefaults.stringForKey(TelemetryStorageKeys.ClientId)
    ?.takeIf(String::isNotBlank)
    ?: NSUUID().UUIDString.also {
        NSUserDefaults.standardUserDefaults.setObject(it, forKey = TelemetryStorageKeys.ClientId)
    }
