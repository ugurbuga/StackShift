import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.File
import java.util.Properties

private fun File.hasJpackage(): Boolean = resolve("bin/jpackage").canExecute()

private fun findDesktopJdkHome(): File? {
    val candidateHomes = buildList {
        System.getenv("JAVA_HOME")?.let(::File)?.let(::add)
        System.getProperty("java.home")?.let(::File)?.let(::add)

        File(System.getProperty("user.home"), "Library/Java/JavaVirtualMachines")
            .listFiles()
            .orEmpty()
            .map { File(it, "Contents/Home") }
            .forEach(::add)

        File("/Library/Java/JavaVirtualMachines")
            .listFiles()
            .orEmpty()
            .map { File(it, "Contents/Home") }
            .forEach(::add)
    }

    return candidateHomes.firstOrNull { it.hasJpackage() }
}

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.firebaseCrashlytics)
}

val desktopJdkHome = findDesktopJdkHome()
private val AdsPropertiesFileName = "ads.properties"
private val AdsAndroidApplicationIdProperty = "ads.android.applicationId"
private val AdsAndroidBannerUnitIdProperty = "ads.android.bannerUnitId"
private val AdsAndroidInterstitialUnitIdProperty = "ads.android.interstitialUnitId"
private val AdsAndroidRewardedUnitIdProperty = "ads.android.rewardedUnitId"
private val GooglePropertiesFileName = "google.properties"
private val FirebaseProjectNumberProperty = "firebase.projectNumber"
private val FirebaseProjectIdProperty = "firebase.projectId"
private val FirebaseStorageBucketProperty = "firebase.storageBucket"
private val FirebaseAndroidAppIdProperty = "firebase.android.appId"
private val FirebaseAndroidApiKeyProperty = "firebase.android.apiKey"
private val FirebaseAndroidPackageNameProperty = "firebase.android.packageName"
private val KeystorePropertiesFileName = "keystore.properties"
private val KeystoreStoreFileProperty = "keystore.storeFile"
private val KeystoreStorePasswordProperty = "keystore.storePassword"
private val KeystoreKeyAliasProperty = "keystore.keyAlias"
private val KeystoreKeyPasswordProperty = "keystore.keyPassword"

private val adsProperties = Properties().apply {
    val adsPropertiesFile = File(rootProject.projectDir, AdsPropertiesFileName)
    if (adsPropertiesFile.exists()) {
        adsPropertiesFile.inputStream().use(::load)
    }
}

private val googleProperties = Properties().apply {
    val googlePropertiesFile = File(rootProject.projectDir, GooglePropertiesFileName)
    if (googlePropertiesFile.exists()) {
        googlePropertiesFile.inputStream().use(::load)
    }
}

private val keystoreProperties = Properties().apply {
    val keystorePropertiesFile = File(rootProject.projectDir, KeystorePropertiesFileName)
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use(::load)
    }
}

private fun adsProperty(name: String): String = adsProperties.getProperty(name).orEmpty()
private fun googleProperty(name: String): String = googleProperties.getProperty(name).orEmpty().trim()
private fun keystoreProperty(name: String): String = keystoreProperties.getProperty(name).orEmpty()

private fun writeTextIfChanged(target: File, content: String) {
    target.parentFile?.mkdirs()
    if (!target.exists() || target.readText() != content) {
        target.writeText(content)
    }
}

private fun String.escapeJson(): String = buildString(length) {
    for (character in this@escapeJson) {
        when (character) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(character)
        }
    }
}

abstract class GenerateLocalFirebaseConfigTask : DefaultTask() {
    @get:InputFile
    @get:Optional
    abstract val sourcePropertiesFile: RegularFileProperty

    @get:OutputFile
    abstract val androidGoogleServicesFile: RegularFileProperty

    @get:Input
    abstract val defaultAndroidPackageName: Property<String>

    @TaskAction
    fun generate() {
        val googlePropertiesFile = sourcePropertiesFile.asFile.orNull
        val androidConfigFile = androidGoogleServicesFile.asFile.get()

        if (googlePropertiesFile == null || !googlePropertiesFile.exists()) {
            if (androidConfigFile.exists()) {
                androidConfigFile.delete()
            }
            return
        }

        val googleProperties = Properties().apply {
            googlePropertiesFile.inputStream().use(::load)
        }

        val projectNumber = googleProperties.requireTrimmed("firebase.projectNumber")
        val projectId = googleProperties.requireTrimmed("firebase.projectId")
        val storageBucket = googleProperties.trimmed("firebase.storageBucket")
        val androidAppId = googleProperties.requireTrimmed("firebase.android.appId")
        val androidApiKey = googleProperties.requireTrimmed("firebase.android.apiKey")
        val androidPackageName = googleProperties.trimmed("firebase.android.packageName")
            ?: defaultAndroidPackageName.get()

        if (projectNumber == null || projectId == null || androidAppId == null || androidApiKey == null) {
            if (androidConfigFile.exists()) {
                androidConfigFile.delete()
            }
            return
        }

        val googleServicesJson = """
            {
              "project_info": {
                "project_number": "${projectNumber.escapeJson()}",
                "project_id": "${projectId.escapeJson()}",
                "storage_bucket": "${storageBucket.orEmpty().escapeJson()}"
              },
              "client": [
                {
                  "client_info": {
                    "mobilesdk_app_id": "${androidAppId.escapeJson()}",
                    "android_client_info": {
                      "package_name": "${androidPackageName.escapeJson()}"
                    }
                  },
                  "oauth_client": [],
                  "api_key": [
                    {
                      "current_key": "${androidApiKey.escapeJson()}"
                    }
                  ],
                  "services": {
                    "appinvite_service": {
                      "other_platform_oauth_client": []
                    }
                  }
                }
              ],
              "configuration_version": "1"
            }
        """.trimIndent() + "\n"

        androidConfigFile.parentFile?.mkdirs()
        if (!androidConfigFile.exists() || androidConfigFile.readText() != googleServicesJson) {
            androidConfigFile.writeText(googleServicesJson)
        }
    }

    private fun Properties.trimmed(name: String): String? = getProperty(name)?.trim()?.takeIf(String::isNotEmpty)

    private fun Properties.requireTrimmed(name: String): String? = trimmed(name)

    private fun String.escapeJson(): String = buildString(length) {
        for (character in this@escapeJson) {
            when (character) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(character)
            }
        }
    }
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.firebase.analytics)
            implementation(libs.firebase.crashlytics)
            implementation(libs.google.play.services.ads)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.coroutines.core)
        }
        iosMain.dependencies {
            implementation(libs.gitlive.firebase.app)
            implementation(libs.gitlive.firebase.analytics)
            implementation(libs.gitlive.firebase.crashlytics)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.gitlive.firebase.app)
            implementation(libs.gitlive.firebase.analytics)
            implementation(libs.gitlive.firebase.java.sdk)
        }
    }
}

val generateLocalFirebaseConfig by tasks.registering(GenerateLocalFirebaseConfigTask::class) {
    group = "build setup"
    description = "Generates local Firebase config files from google.properties when available."
    sourcePropertiesFile.set(rootProject.layout.projectDirectory.file(GooglePropertiesFileName))
    androidGoogleServicesFile.set(layout.projectDirectory.file("google-services.json"))
    defaultAndroidPackageName.set("com.ugurbuga.stackshift")
}

tasks.matching { task ->
    task.name.matches(Regex("process.+GoogleServices"))
}.configureEach {
    dependsOn(generateLocalFirebaseConfig)
}

android {
    namespace = "com.ugurbuga.stackshift"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    signingConfigs {
        create("release") {
            storeFile = keystoreProperty(KeystoreStoreFileProperty)
                .takeIf(String::isNotBlank)
                ?.let(rootProject::file)
                ?: rootProject.file("keystore/StackShift.jks")
            storePassword = keystoreProperty(KeystoreStorePasswordProperty)
            keyAlias = keystoreProperty(KeystoreKeyAliasProperty)
            keyPassword = keystoreProperty(KeystoreKeyPasswordProperty)
        }
    }

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.ugurbuga.stackshift"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        manifestPlaceholders["adsApplicationId"] = adsProperty(AdsAndroidApplicationIdProperty)
        buildConfigField("String", "ADS_BANNER_UNIT_ID", "\"${adsProperty(AdsAndroidBannerUnitIdProperty)}\"")
        buildConfigField("String", "ADS_INTERSTITIAL_UNIT_ID", "\"${adsProperty(AdsAndroidInterstitialUnitIdProperty)}\"")
        buildConfigField("String", "ADS_REWARDED_UNIT_ID", "\"${adsProperty(AdsAndroidRewardedUnitIdProperty)}\"")
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "com.ugurbuga.stackshift.MainKt"

        if (desktopJdkHome != null) {
            javaHome = desktopJdkHome.absolutePath

            nativeDistributions {
                targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
                packageName = "com.ugurbuga.stackshift"
                packageVersion = "1.0.0"

                macOS {
                    iconFile.set(project.file("desktop-icons/stackshift.icns"))
                }
                windows {
                    iconFile.set(project.file("desktop-icons/stackshift.ico"))
                }
                linux {
                    iconFile.set(project.file("desktop-icons/stackshift.png"))
                }
            }
        }
    }
}

tasks.register("packageDesktopApp") {
    group = "distribution"
    description = "Builds the portable desktop application image for the current OS."
    if (desktopJdkHome != null) {
        dependsOn("createDistributable")
    } else {
        doLast {
            error(
                "Desktop packaging requires a JDK with jpackage. Set JAVA_HOME to a full JDK or install one before running packageDesktopApp.",
            )
        }
    }
}


tasks.register("runWeb") {
    group = "application"
    description = "Runs the wasm web app in a browser development server."
    dependsOn("wasmJsBrowserDevelopmentRun")
}

