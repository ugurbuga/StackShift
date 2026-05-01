package com.ugurbuga.blockgames.telemetry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import android.app.Application
import com.google.firebase.FirebasePlatform
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.apps
import dev.gitlive.firebase.initialize
import java.io.File
import java.util.Properties
import java.util.concurrent.atomic.AtomicBoolean

@Composable
actual fun rememberAppTelemetry(): AppTelemetry {
    val config = remember { loadDesktopFirebaseConfig() }
    if (config == null) {
        return NoOpAppTelemetry
    }

    runCatching {
        DesktopFirebaseBootstrap.ensureInitialized(config)
    }.getOrElse {
        return NoOpAppTelemetry
    }

    return remember { FirebaseKmpTelemetry(::desktopDeviceInfo) }
}

private object DesktopFirebaseBootstrap {
    private val initialized = AtomicBoolean(false)

    fun ensureInitialized(config: DesktopFirebaseConfig) {
        if (initialized.get()) return

        FirebasePlatform.initializeFirebasePlatform(object : FirebasePlatform() {
            private val storage = mutableMapOf<String, String>()

            override fun store(key: String, value: String) {
                storage[key] = value
            }

            override fun retrieve(key: String): String? = storage[key]

            override fun clear(key: String) {
                storage.remove(key)
            }

            override fun log(msg: String) {
                println(msg)
            }

            override fun getDatabasePath(name: String): File = File(
                System.getProperty("java.io.tmpdir"),
                name,
            )
        })

        if (Firebase.apps().isEmpty()) {
            Firebase.initialize(Application(), config.toFirebaseOptions())
        }

        initialized.set(true)
    }
}

private data class DesktopFirebaseConfig(
    val projectId: String,
    val applicationId: String,
    val apiKey: String,
    val storageBucket: String? = null,
    val gcmSenderId: String? = null,
    val authDomain: String? = null,
    val databaseUrl: String? = null,
    val gaTrackingId: String? = null,
)

private fun DesktopFirebaseConfig.toFirebaseOptions(): FirebaseOptions = FirebaseOptions(
    applicationId = applicationId,
    apiKey = apiKey,
    databaseUrl = databaseUrl,
    gaTrackingId = gaTrackingId,
    storageBucket = storageBucket,
    projectId = projectId,
    gcmSenderId = gcmSenderId,
    authDomain = authDomain,
)

private fun loadDesktopFirebaseConfig(): DesktopFirebaseConfig? {
    loadPropertiesResource(DesktopConfigResourceName, DesktopConfigFileName)
        ?.toLegacyDesktopFirebaseConfig()
        ?.let { return it }

    return loadPropertiesResource(GoogleConfigResourceName, GoogleConfigFileName)
        ?.toGoogleDesktopFirebaseConfig()
}

private const val DesktopConfigResourceName = "firebase-desktop.properties"
private const val DesktopConfigFileName = "firebase-desktop.properties"
private const val GoogleConfigResourceName = "google.properties"
private const val GoogleConfigFileName = "google.properties"

private fun loadPropertiesResource(resourceName: String, fileName: String): Properties? {
    val resource = Thread.currentThread().contextClassLoader.getResourceAsStream(resourceName)
        ?: File(fileName).takeIf(File::exists)?.inputStream()
        ?: return null

    return resource.use { inputStream ->
        Properties().apply { load(inputStream) }
    }
}

private fun Properties.toLegacyDesktopFirebaseConfig(): DesktopFirebaseConfig? {
    val projectId = getProperty("projectId")?.takeIf(String::isNotBlank) ?: return null
    val applicationId = getProperty("applicationId")?.takeIf(String::isNotBlank) ?: return null
    val apiKey = getProperty("apiKey")?.takeIf(String::isNotBlank) ?: return null
    return DesktopFirebaseConfig(
        projectId = projectId,
        applicationId = applicationId,
        apiKey = apiKey,
        storageBucket = getProperty("storageBucket")?.takeIf(String::isNotBlank),
        gcmSenderId = getProperty("gcmSenderId")?.takeIf(String::isNotBlank),
        authDomain = getProperty("authDomain")?.takeIf(String::isNotBlank),
        databaseUrl = getProperty("databaseUrl")?.takeIf(String::isNotBlank),
        gaTrackingId = getProperty("gaTrackingId")?.takeIf(String::isNotBlank),
    )
}

private fun Properties.toGoogleDesktopFirebaseConfig(): DesktopFirebaseConfig? {
    val projectId = getProperty("firebase.projectId")?.takeIf(String::isNotBlank) ?: return null
    val applicationId = getProperty("firebase.desktop.appId")
        ?.takeIf(String::isNotBlank)
        ?: getProperty("firebase.android.appId")?.takeIf(String::isNotBlank)
        ?: return null
    val apiKey = getProperty("firebase.desktop.apiKey")
        ?.takeIf(String::isNotBlank)
        ?: getProperty("firebase.android.apiKey")?.takeIf(String::isNotBlank)
        ?: return null

    return DesktopFirebaseConfig(
        projectId = projectId,
        applicationId = applicationId,
        apiKey = apiKey,
        storageBucket = getProperty("firebase.storageBucket")?.takeIf(String::isNotBlank),
        gcmSenderId = getProperty("firebase.gcmSenderId")?.takeIf(String::isNotBlank)
            ?: getProperty("firebase.projectNumber")?.takeIf(String::isNotBlank),
        authDomain = getProperty("firebase.desktop.authDomain")?.takeIf(String::isNotBlank),
        databaseUrl = getProperty("firebase.desktop.databaseUrl")?.takeIf(String::isNotBlank),
        gaTrackingId = getProperty("firebase.desktop.gaTrackingId")?.takeIf(String::isNotBlank),
    )
}
