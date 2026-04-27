import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.firebaseCrashlytics)
}

private val adsPropertiesFileName = "ads.properties"
private val adsAndroidApplicationIdProperty = "ads.android.applicationId"

private val keystorePropertiesFileName = "keystore.properties"
private val keystoreStoreFileProperty = "keystore.storeFile"
private val keystoreStorePasswordProperty = "keystore.storePassword"
private val keystoreKeyAliasProperty = "keystore.keyAlias"
private val keystoreKeyPasswordProperty = "keystore.keyPassword"

private val googlePropertiesFileName = "google.properties"

private val adsProperties = Properties().apply {
    val adsPropertiesFile = File(rootProject.projectDir, adsPropertiesFileName)
    if (adsPropertiesFile.exists()) {
        adsPropertiesFile.inputStream().use(::load)
    }
}

private val keystoreProperties = Properties().apply {
    val keystorePropertiesFile = File(rootProject.projectDir, keystorePropertiesFileName)
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use(::load)
    }
}

private fun adsProperty(name: String): String = adsProperties.getProperty(name).orEmpty()
private fun keystoreProperty(name: String): String = keystoreProperties.getProperty(name).orEmpty()


val generateLocalFirebaseConfig by tasks.registering {
    group = "build setup"
    description = "Generates local Firebase config files from google.properties when available."
    val sourcePropertiesFile = rootProject.layout.projectDirectory.file(googlePropertiesFileName)
    val androidGoogleServicesFile = layout.projectDirectory.file("google-services.json")
    val defaultAndroidPackageName = "com.ugurbuga.stackshift"

    inputs.file(sourcePropertiesFile).optional()
    outputs.file(androidGoogleServicesFile)

    doLast {
        fun Properties.trimmed(name: String): String? = getProperty(name)?.trim()?.takeIf(String::isNotEmpty)
        fun Properties.requireTrimmed(name: String): String? = trimmed(name)

        fun String.escapeJson(): String = buildString(length) {
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

        val googlePropertiesFile = sourcePropertiesFile.asFile
        val androidConfigFile = androidGoogleServicesFile.asFile

        if (!googlePropertiesFile.exists()) {
            if (androidConfigFile.exists()) {
                androidConfigFile.delete()
            }
            return@doLast
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
            ?: defaultAndroidPackageName

        if ((projectNumber == null) || (projectId == null) || (androidAppId == null) || (androidApiKey == null)) {
            if (androidConfigFile.exists()) {
                androidConfigFile.delete()
            }
            return@doLast
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
        if ((!androidConfigFile.exists()) || (androidConfigFile.readText() != googleServicesJson)) {
            androidConfigFile.writeText(googleServicesJson)
        }
    }
}

tasks.matching { task ->
    task.name.matches(Regex("process.+GoogleServices"))
}.configureEach {
    dependsOn(generateLocalFirebaseConfig)
}

android {
    namespace = "com.ugurbuga.stackshift.app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    signingConfigs {
        create("release") {
            storeFile = keystoreProperty(keystoreStoreFileProperty)
                .takeIf(String::isNotBlank)
                ?.let(rootProject::file)
                ?: rootProject.file("keystore/StackShift.jks")
            storePassword = keystoreProperty(keystoreStorePasswordProperty)
            keyAlias = keystoreProperty(keystoreKeyAliasProperty)
            keyPassword = keystoreProperty(keystoreKeyPasswordProperty)
        }
    }

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.ugurbuga.stackshift"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 4
        versionName = "1.0.3"
        manifestPlaceholders["adsApplicationId"] = adsProperty(adsAndroidApplicationIdProperty)
        buildConfigField("String", "ADS_BANNER_UNIT_ID", "\"${adsProperty("ads.android.bannerUnitId")}\"")
        buildConfigField("String", "ADS_INTERSTITIAL_UNIT_ID", "\"${adsProperty("ads.android.interstitialUnitId")}\"")
        buildConfigField("String", "ADS_REWARDED_UNIT_ID", "\"${adsProperty("ads.android.rewardedUnitId")}\"")
        buildConfigField("String", "ADS_REWARDED_SPECIAL_UNIT_ID", "\"${adsProperty("ads.android.rewardedSpecialUnitId")}\"")
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(project(":composeApp"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.google.play.services.ads)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    debugImplementation(libs.compose.uiTooling)
}
