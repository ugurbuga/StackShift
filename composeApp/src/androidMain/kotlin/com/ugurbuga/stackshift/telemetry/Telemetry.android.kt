package com.ugurbuga.stackshift.telemetry

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.ugurbuga.stackshift.settings.AppContextHolder
import java.util.UUID

private const val TelemetryNamespace = "com.ugurbuga.blockgames.telemetry"
private const val DeviceInfoPrefix = "device_"
private const val CrashlyticsHighScoreMessage = "High score reached"

private class FirebaseAppTelemetry(
    context: Context,
) : AppTelemetry {
    private val applicationContext = context.applicationContext
    private val preferences = applicationContext.getSharedPreferences(TelemetryNamespace, Context.MODE_PRIVATE)
    private val analytics = FirebaseAnalytics.getInstance(applicationContext)
    private val crashlytics = FirebaseCrashlytics.getInstance()
    private val clientId = resolveClientId()

    init {
        setTelemetryIdentity()
    }

    override fun logScreen(screenName: String) {
        analytics.logEvent(
            TelemetryEventNames.ScreenView,
            bundleOf(
                telemetryParameters(
                    mapOf(
                        TelemetryParamKeys.ScreenName to screenName,
                        TelemetryParamKeys.ScreenClass to screenName,
                    ),
                ),
            ),
        )
        crashlytics.log("screen_view:$screenName client_id=$clientId")
    }

    override fun logEvent(eventName: String, parameters: Map<String, String>) {
        val mergedParameters = telemetryParameters(parameters)
        analytics.logEvent(eventName, bundleOf(mergedParameters))
        crashlytics.log("event:$eventName ${mergedParameters.entries.joinToString(separator = ";") { "${it.key}=${it.value}" }}")
    }

    override fun logUserProperty(name: String, value: String?) {
        analytics.setUserProperty(name, value)
        crashlytics.setCustomKey(name, value ?: "")
    }

    override fun logHighScoreReached(newScore: Int, previousHighScore: Int) {
        val eventParameters = buildMap<String, String> {
            put(TelemetryParamKeys.Score, newScore.toString())
            put(TelemetryParamKeys.PreviousScore, previousHighScore.toString())
            putAll(telemetryContext())
        }
        analytics.logEvent(TelemetryEventNames.HighScoreReached, bundleOf(eventParameters))
        crashlytics.log("$CrashlyticsHighScoreMessage client_id=$clientId")
        crashlytics.setCustomKey(TelemetryParamKeys.Score, newScore)
        crashlytics.setCustomKey(TelemetryParamKeys.PreviousScore, previousHighScore)
        telemetryContext().forEach { (key, value) -> crashlytics.setCustomKey(key, value) }
    }

    override fun recordException(throwable: Throwable) {
        analytics.logEvent(
            TelemetryEventNames.Exception,
            bundleOf(
                telemetryParameters(
                    mapOf(
                        TelemetryParamKeys.ExceptionType to throwable::class.simpleName.orEmpty(),
                        TelemetryParamKeys.ExceptionMessage to (throwable.message ?: throwable.toString()),
                    ),
                ),
            ),
        )
        crashlytics.recordException(throwable)
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
        crashlytics.setCustomKey(TelemetryParamKeys.ClientId, clientId)
        telemetryContext().forEach { (key, value) -> crashlytics.setCustomKey(key, value) }
        analytics.setUserProperty(TelemetryUserPropertyNames.ClientId, clientId)
    }

    private fun telemetryParameters(parameters: Map<String, String>): Map<String, String> = buildMap {
        putAll(telemetryContext())
        putAll(parameters)
    }

    private fun telemetryContext(): Map<String, String> = mapOf(
        TelemetryParamKeys.ClientId to clientId,
        TelemetryParamKeys.PackageName to applicationContext.packageName,
        TelemetryParamKeys.Manufacturer to Build.MANUFACTURER.orEmpty(),
        TelemetryParamKeys.Model to Build.MODEL.orEmpty(),
        TelemetryParamKeys.Device to Build.DEVICE.orEmpty(),
        TelemetryParamKeys.Product to Build.PRODUCT.orEmpty(),
        TelemetryParamKeys.Release to Build.VERSION.RELEASE.orEmpty(),
        TelemetryParamKeys.SdkInt to Build.VERSION.SDK_INT.toString(),
    ).mapKeys { (key, _) -> if (key == TelemetryParamKeys.ClientId) key else "$DeviceInfoPrefix$key" }

    private fun resolveClientId(): String = preferences.getString(TelemetryStorageKeys.ClientId, null)
        ?.takeIf(String::isNotBlank)
        ?: UUID.randomUUID().toString().also {
            preferences.edit().putString(TelemetryStorageKeys.ClientId, it).apply()
        }

    private fun bundleOf(parameters: Map<String, String>): Bundle = Bundle().apply {
        parameters.forEach { (key, value) -> putString(key, value) }
    }
}

@Composable
actual fun rememberAppTelemetry(): AppTelemetry {
    val context = AppContextHolder.context
    return remember(context) { FirebaseAppTelemetry(context) }
}
