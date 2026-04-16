package com.ugurbuga.stackshift.settings

import com.ugurbuga.stackshift.game.model.AppLanguage
import com.ugurbuga.stackshift.telemetry.AppTelemetry
import com.ugurbuga.stackshift.telemetry.NoOpAppTelemetry

enum class AppLanguageBootstrapReason {
    SavedLanguagePresent,
    DeviceLanguageMatched,
    FallbackToEnglish,
}

data class AppSettingsBootstrapResult(
    val settings: AppSettings,
    val shouldPersist: Boolean,
    val deviceLocaleTag: String,
    val normalizedDeviceLocaleTag: String?,
    val savedLanguage: AppLanguage?,
    val matchedDeviceLanguage: AppLanguage?,
    val resolvedLanguage: AppLanguage,
    val reason: AppLanguageBootstrapReason,
)

fun initializeAppSettingsForFirstLaunch(
    settings: AppSettings,
    deviceLocaleTag: String,
): AppSettingsBootstrapResult {
    val normalizedDeviceLocaleTag = deviceLocaleTag
        .trim()
        .replace('_', '-')
        .takeIf(String::isNotBlank)

    if (settings.hasInitializedLanguage) {
        return AppSettingsBootstrapResult(
            settings = settings,
            shouldPersist = false,
            deviceLocaleTag = deviceLocaleTag,
            normalizedDeviceLocaleTag = normalizedDeviceLocaleTag,
            savedLanguage = settings.language,
            matchedDeviceLanguage = AppLanguage.fromDeviceLocaleTag(normalizedDeviceLocaleTag),
            resolvedLanguage = settings.language,
            reason = AppLanguageBootstrapReason.SavedLanguagePresent,
        )
    }

    val matchedDeviceLanguage = AppLanguage.fromDeviceLocaleTag(normalizedDeviceLocaleTag)
    val bootstrapReason = if (matchedDeviceLanguage != null) {
        AppLanguageBootstrapReason.DeviceLanguageMatched
    } else {
        AppLanguageBootstrapReason.FallbackToEnglish
    }
    val resolvedLanguage = matchedDeviceLanguage ?: AppLanguage.English

    return AppSettingsBootstrapResult(
        settings = settings.copy(
            language = resolvedLanguage,
            hasInitializedLanguage = true,
        ),
        shouldPersist = true,
        deviceLocaleTag = deviceLocaleTag,
        normalizedDeviceLocaleTag = normalizedDeviceLocaleTag,
        savedLanguage = null,
        matchedDeviceLanguage = matchedDeviceLanguage,
        resolvedLanguage = resolvedLanguage,
        reason = bootstrapReason,
    )
}

fun logLanguageBootstrapDecision(
    source: String,
    result: AppSettingsBootstrapResult,
    telemetry: AppTelemetry = NoOpAppTelemetry,
) {
    val message = buildString {
        append("[LanguageBootstrap]")
        append(" source=")
        append(source)
        append(" reason=")
        append(result.reason.name)
        append(" deviceLocaleRaw=")
        append(result.deviceLocaleTag)
        append(" deviceLocaleNormalized=")
        append(result.normalizedDeviceLocaleTag ?: "<blank>")
        append(" savedLanguage=")
        append(result.savedLanguage?.localeTag ?: "<none>")
        append(" matchedDeviceLanguage=")
        append(result.matchedDeviceLanguage?.localeTag ?: "<none>")
        append(" resolvedLanguage=")
        append(result.resolvedLanguage.localeTag)
        append(" shouldPersist=")
        append(result.shouldPersist)
        append(" hasInitializedLanguage=")
        append(result.settings.hasInitializedLanguage)
    }
    println(message)
    telemetry.logEvent(
        eventName = "language_bootstrap",
        parameters = mapOf(
            "source" to source,
            "reason" to result.reason.name,
            "device_locale_raw" to result.deviceLocaleTag,
            "device_locale_normalized" to (result.normalizedDeviceLocaleTag ?: ""),
            "saved_language" to (result.savedLanguage?.localeTag ?: ""),
            "matched_device_language" to (result.matchedDeviceLanguage?.localeTag ?: ""),
            "resolved_language" to result.resolvedLanguage.localeTag,
            "should_persist" to result.shouldPersist.toString(),
            "has_initialized_language" to result.settings.hasInitializedLanguage.toString(),
        ),
    )
}

