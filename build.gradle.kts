import java.io.File

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeHotReload) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
}

private val BUILD_ALL_ARTIFACTS_TASK_NAME = "buildAllArtifacts"
private val ARTIFACTS_DIR_NAME = "artifacts"
private val ANDROID_DEBUG_APK_SOURCE = "composeApp/build/outputs/apk/debug/composeApp-debug.apk"
private val ANDROID_DEBUG_APK_TARGET = "artifacts/android/composeApp-debug.apk"
private val IOS_ARCHIVE_TARGET = "artifacts/ios/StackShift.xcarchive"
private val MACOS_APP_SOURCE = "composeApp/build/compose/binaries/main/app/com.ugurbuga.stackshift.app"
private val MACOS_APP_TARGET = "artifacts/macos/app/com.ugurbuga.stackshift.app"
private val MACOS_DMG_SOURCE = "composeApp/build/compose/binaries/main/dmg/com.ugurbuga.stackshift-1.0.0.dmg"
private val MACOS_DMG_TARGET = "artifacts/macos/dmg/com.ugurbuga.stackshift-1.0.0.dmg"
private val CHILD_GRADLE_FLAGS = listOf("--no-configuration-cache", "--no-daemon")

private fun Project.runCommand(stepName: String, command: List<String>, workingDirectory: File = projectDir): Boolean {
    logger.lifecycle("[$stepName] starting")

    val process = ProcessBuilder(command)
        .directory(workingDirectory)
        .redirectErrorStream(true)
        .apply {
            val environment = environment()
            val path = System.getenv("PATH")
            val home = System.getProperty("user.home")
            val javaHome = System.getProperty("java.home")

            environment.clear()
            if (!path.isNullOrBlank()) {
                environment["PATH"] = path
            }
            environment["HOME"] = home
            environment["USER"] = System.getProperty("user.name")
            environment["JAVA_HOME"] = javaHome
            environment["LANG"] = "en_US.UTF-8"
        }
        .start()

    process.inputStream.bufferedReader().useLines { lines ->
        lines.forEach { line ->
            logger.lifecycle("[$stepName] $line")
        }
    }

    val exitCode = process.waitFor()
    if (exitCode != 0) {
        logger.warn("[$stepName] failed with exit code $exitCode")
        return false
    }

    logger.lifecycle("[$stepName] finished successfully")
    return true
}

private fun Project.moveIfExists(sourcePath: String, targetPath: String) {
    val source = File(sourcePath)
    if (!source.exists()) {
        logger.lifecycle("[artifacts] skipped missing source: $sourcePath")
        return
    }

    val target = File(targetPath)
    target.parentFile.mkdirs()
    if (target.exists()) {
        target.deleteRecursively()
    }

    source.renameTo(target)
    if (source.exists()) {
        source.copyRecursively(target, overwrite = true)
        source.deleteRecursively()
    }

    logger.lifecycle("[artifacts] moved $sourcePath -> $targetPath")
}

tasks.register(BUILD_ALL_ARTIFACTS_TASK_NAME) {
    group = "distribution"
    description = "Attempts Android, iOS, macOS, and Windows builds, then collects the available outputs under artifacts/."
    notCompatibleWithConfigurationCache("Runs external build tools and child Gradle processes.")

    doLast {
        File(ARTIFACTS_DIR_NAME).mkdirs()

        runCommand("android", listOf("./gradlew", *CHILD_GRADLE_FLAGS.toTypedArray(), ":composeApp:assembleDebug"))
        moveIfExists(ANDROID_DEBUG_APK_SOURCE, ANDROID_DEBUG_APK_TARGET)

        val iosArchiveTarget = File(IOS_ARCHIVE_TARGET)
        iosArchiveTarget.parentFile.mkdirs()
        if (iosArchiveTarget.exists()) {
            iosArchiveTarget.deleteRecursively()
        }
        runCommand(
            "ios",
            listOf(
                "xcodebuild",
                "archive",
                "-project",
                "iosApp.xcodeproj",
                "-scheme",
                "iosApp",
                "-configuration",
                "Release",
                "-destination",
                "generic/platform=iOS",
                "-archivePath",
                "../$IOS_ARCHIVE_TARGET",
                "CODE_SIGNING_ALLOWED=NO",
                "CODE_SIGNING_REQUIRED=NO",
                "DEVELOPMENT_TEAM=",
            ),
            File("iosApp"),
        )

        runCommand("macos-app", listOf("./gradlew", *CHILD_GRADLE_FLAGS.toTypedArray(), ":composeApp:packageDesktopApp"))
        moveIfExists(MACOS_APP_SOURCE, MACOS_APP_TARGET)

        runCommand("macos-dmg", listOf("./gradlew", *CHILD_GRADLE_FLAGS.toTypedArray(), ":composeApp:packageDmg"))
        moveIfExists(MACOS_DMG_SOURCE, MACOS_DMG_TARGET)

        runCommand("windows-msi", listOf("./gradlew", *CHILD_GRADLE_FLAGS.toTypedArray(), ":composeApp:packageMsi"))

        logger.lifecycle("[artifacts] collection finished")
    }
}