package com.ugurbuga.stackshift.telemetry

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.analytics.analytics
import dev.gitlive.firebase.analytics.logEvent

class FirebaseKmpTelemetry(
    private val deviceInfoProvider: () -> Map<String, String> = { emptyMap() },
) : AppTelemetry {
    override fun logScreen(screenName: String) {
        Firebase.analytics.logEvent(
            TelemetryEventNames.ScreenView,
            mapOf(
                TelemetryParamKeys.ScreenName to screenName,
                TelemetryParamKeys.ScreenClass to screenName,
            ),
        )
    }

    override fun logEvent(eventName: String, parameters: Map<String, String>) {
        Firebase.analytics.logEvent(eventName, parameters.mapValues { it.value as Any })
    }

    override fun logUserProperty(name: String, value: String?) {
        Firebase.analytics.setUserProperty(name, value.orEmpty())
    }

    override fun logHighScoreReached(newScore: Int, previousHighScore: Int) {
        val deviceInfo = deviceInfoProvider()
        val eventParameters = buildMap<String, Any> {
            put(TelemetryParamKeys.Score, newScore)
            put(TelemetryParamKeys.PreviousScore, previousHighScore)
            deviceInfo.forEach { (key, value) -> put(key, value) }
        }
        Firebase.analytics.logEvent(TelemetryEventNames.HighScoreReached, eventParameters)
    }

    override fun recordException(throwable: Throwable) {
        Firebase.analytics.logEvent(
            TelemetryEventNames.Exception,
            mapOf(
                TelemetryParamKeys.ExceptionType to throwable::class.simpleName.orEmpty(),
                TelemetryParamKeys.ExceptionMessage to (throwable.message ?: throwable.toString()),
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
}
