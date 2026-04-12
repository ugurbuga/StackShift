package com.ugurbuga.stackshift.telemetry

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.analytics.analytics
import dev.gitlive.firebase.analytics.logEvent
import java.util.UUID
import java.util.prefs.Preferences

private const val TelemetryNamespace = "com.ugurbuga.stackshift.telemetry"
private const val DeviceInfoPrefix = "device_"

class FirebaseKmpTelemetry(
    private val deviceInfoProvider: () -> Map<String, String> = { emptyMap() },
) : AppTelemetry {
    init {
        Firebase.analytics.setUserProperty(
            TelemetryUserPropertyNames.ClientId,
            deviceInfoProvider()[TelemetryParamKeys.ClientId].orEmpty(),
        )
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
    }

    override fun logEvent(eventName: String, parameters: Map<String, String>) {
        Firebase.analytics.logEvent(eventName, telemetryParameters(parameters))
    }

    override fun logUserProperty(name: String, value: String?) {
        Firebase.analytics.setUserProperty(name, value.orEmpty())
    }

    override fun logHighScoreReached(newScore: Int, previousHighScore: Int) {
        val eventParameters = buildMap<String, Any> {
            put(TelemetryParamKeys.Score, newScore)
            put(TelemetryParamKeys.PreviousScore, previousHighScore)
            telemetryContext().forEach { (key, value) -> put(key, value) }
        }
        Firebase.analytics.logEvent(TelemetryEventNames.HighScoreReached, eventParameters)
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

    private fun telemetryParameters(parameters: Map<String, String>): Map<String, Any> = buildMap {
        telemetryContext().forEach { (key, value) -> put(key, value) }
        parameters.forEach { (key, value) -> put(key, value) }
    }

    private fun telemetryContext(): Map<String, String> = deviceInfoProvider().let { context ->
        val clientId = context[TelemetryParamKeys.ClientId].orEmpty()
        if (clientId.isBlank()) {
            context
        } else {
            context
        }
    }
}

internal fun desktopDeviceInfo(): Map<String, String> = buildMap {
    put(TelemetryParamKeys.ClientId, resolveDesktopClientId())
    put(TelemetryParamKeys.PackageName, "com.ugurbuga.stackshift.desktop")
    put(TelemetryParamKeys.Manufacturer, System.getProperty("os.name").orEmpty())
    put(TelemetryParamKeys.Model, System.getProperty("os.arch").orEmpty())
    put(TelemetryParamKeys.Device, System.getProperty("os.name").orEmpty())
    put(TelemetryParamKeys.Product, System.getProperty("java.vm.name").orEmpty())
    put(TelemetryParamKeys.Release, System.getProperty("os.version").orEmpty())
    put(TelemetryParamKeys.SdkInt, System.getProperty("java.version").orEmpty())
}.mapKeys { (key, _) -> if (key == TelemetryParamKeys.ClientId) key else "$DeviceInfoPrefix$key" }

private fun resolveDesktopClientId(): String {
    val prefs = Preferences.userRoot().node(TelemetryNamespace)
    return prefs.get(TelemetryStorageKeys.ClientId, null)
        ?.takeIf(String::isNotBlank)
        ?: UUID.randomUUID().toString().also {
            prefs.put(TelemetryStorageKeys.ClientId, it)
        }
}
