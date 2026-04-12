import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.File

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

val desktopJdkHome = findDesktopJdkHome()

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.firebaseCrashlytics)
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
            implementation("com.google.android.gms:play-services-ads:24.7.0")
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

android {
    namespace = "com.ugurbuga.stackshift"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.ugurbuga.stackshift"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
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

tasks.register("assembleDevDebug") {
    group = "build"
    description = "Compatibility alias for stale IDE configurations after removing Android product flavors."
    dependsOn("assembleDebug")
}

tasks.register("runWeb") {
    group = "application"
    description = "Runs the wasm web app in a browser development server."
    dependsOn("wasmJsBrowserDevelopmentRun")
}

