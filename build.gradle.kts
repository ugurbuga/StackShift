import java.io.File

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeHotReload) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.googleServices) apply false
    alias(libs.plugins.firebaseCrashlytics) apply false
}

private val buildAllArtifactsTaskName = "buildAllArtifacts"
private val artifactsDirName = "artifacts"
private val androidDebugApkSource = "androidApp/build/outputs/apk/debug/androidApp-debug.apk"
private val androidDebugApkTarget = "artifacts/android/androidApp-debug.apk"
private val iosArchiveTargetName = "artifacts/ios/StackShift.xcarchive"
private val macosAppSource = "composeApp/build/compose/binaries/main/app/com.ugurbuga.stackshift.app"
private val macosAppTarget = "artifacts/macos/app/com.ugurbuga.stackshift.app"
private val macosDmgSource = "composeApp/build/compose/binaries/main/dmg/com.ugurbuga.stackshift-1.0.0.dmg"
private val macosDmgTarget = "artifacts/macos/dmg/com.ugurbuga.stackshift-1.0.0.dmg"
private val childGradleFlags = listOf("--no-configuration-cache", "--no-daemon")

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

tasks.register(buildAllArtifactsTaskName) {
    group = "distribution"
    description = "Attempts Android, iOS, macOS, and Windows builds, then collects the available outputs under artifacts/."
    notCompatibleWithConfigurationCache("Runs external build tools and child Gradle processes.")

    doLast {
        File(artifactsDirName).mkdirs()

        runCommand("android", listOf("./gradlew", *childGradleFlags.toTypedArray(), ":androidApp:assembleDebug"))
        moveIfExists(androidDebugApkSource, androidDebugApkTarget)

        val iosArchiveTarget = File(iosArchiveTargetName)
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
                "../$iosArchiveTargetName",
                "CODE_SIGNING_ALLOWED=NO",
                "CODE_SIGNING_REQUIRED=NO",
                "DEVELOPMENT_TEAM=",
            ),
            File("iosApp"),
        )

        runCommand("macos-app", listOf("./gradlew", *childGradleFlags.toTypedArray(), ":composeApp:packageDesktopApp"))
        moveIfExists(macosAppSource, macosAppTarget)

        runCommand("macos-dmg", listOf("./gradlew", *childGradleFlags.toTypedArray(), ":composeApp:packageDmg"))
        moveIfExists(macosDmgSource, macosDmgTarget)

        runCommand("windows-msi", listOf("./gradlew", *childGradleFlags.toTypedArray(), ":composeApp:packageMsi"))

        logger.lifecycle("[artifacts] collection finished")
    }
}
