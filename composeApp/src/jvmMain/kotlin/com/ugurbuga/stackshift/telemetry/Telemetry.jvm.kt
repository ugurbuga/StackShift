package com.ugurbuga.stackshift.telemetry

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
    val resource = Thread.currentThread().contextClassLoader.getResourceAsStream(DesktopConfigResourceName)
        ?: File(DesktopConfigFileName).takeIf(File::exists)?.inputStream()
        ?: return null

    return resource.use { inputStream ->
        Properties().apply { load(inputStream) }.let { properties ->
            val projectId = properties.getProperty("projectId")?.takeIf(String::isNotBlank) ?: return null
            val applicationId = properties.getProperty("applicationId")?.takeIf(String::isNotBlank) ?: return null
            val apiKey = properties.getProperty("apiKey")?.takeIf(String::isNotBlank) ?: return null
            DesktopFirebaseConfig(
                projectId = projectId,
                applicationId = applicationId,
                apiKey = apiKey,
                storageBucket = properties.getProperty("storageBucket")?.takeIf(String::isNotBlank),
                gcmSenderId = properties.getProperty("gcmSenderId")?.takeIf(String::isNotBlank),
                authDomain = properties.getProperty("authDomain")?.takeIf(String::isNotBlank),
                databaseUrl = properties.getProperty("databaseUrl")?.takeIf(String::isNotBlank),
                gaTrackingId = properties.getProperty("gaTrackingId")?.takeIf(String::isNotBlank),
            )
        }
    }
}

private fun desktopDeviceInfo(): Map<String, String> = mapOf(
    TelemetryParamKeys.PackageName to "com.ugurbuga.stackshift.desktop",
    TelemetryParamKeys.Manufacturer to System.getProperty("os.name").orEmpty(),
    TelemetryParamKeys.Model to System.getProperty("os.arch").orEmpty(),
    TelemetryParamKeys.Device to System.getProperty("os.name").orEmpty(),
    TelemetryParamKeys.Product to System.getProperty("java.vm.name").orEmpty(),
    TelemetryParamKeys.Release to System.getProperty("os.version").orEmpty(),
    TelemetryParamKeys.SdkInt to System.getProperty("java.version").orEmpty(),
)

private const val DesktopConfigResourceName = "firebase-desktop.properties"
private const val DesktopConfigFileName = "firebase-desktop.properties"
