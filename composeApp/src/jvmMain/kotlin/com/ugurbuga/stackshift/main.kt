package com.ugurbuga.stackshift

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.Dimension
import java.awt.Taskbar
import java.awt.Toolkit
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.util.prefs.Preferences
import kotlin.math.abs
import kotlin.math.roundToInt
import com.ugurbuga.stackshift.localization.appStringResource
import org.jetbrains.compose.resources.stringResource
import stackshift.composeapp.generated.resources.Res
import stackshift.composeapp.generated.resources.app_title_stackshift

fun main() = application {
    val initialSnapshot = remember { DesktopWindowPrefs.load() }
    val appTitle = appStringResource(Res.string.app_title_stackshift)
    val windowState = rememberWindowState(
        size = DpSize(initialSnapshot.width.dp, initialSnapshot.height.dp),
        position = initialSnapshot.x?.let { x ->
            initialSnapshot.y?.let { y -> WindowPosition(x.dp, y.dp) }
        } ?: WindowPosition.PlatformDefault,
    )

    Window(
        state = windowState,
        resizable = true,
        onCloseRequest = ::exitApplication,
        title = appTitle,
    ) {
        DisposableEffect(window) {
            windowIconResourceNames().asSequence()
                .mapNotNull(Thread.currentThread().contextClassLoader::getResource)
                .firstOrNull()?.let { resource ->
                Toolkit.getDefaultToolkit().getImage(resource).also { image ->
                    window.iconImage = image
                    if (Taskbar.isTaskbarSupported()) {
                        runCatching {
                            Taskbar.getTaskbar().iconImage = image
                        }
                    }
                }
            }
            val controller = AspectLockedWindowController(
                window = window,
                initialSnapshot = initialSnapshot,
            )
            onDispose(controller::dispose)
        }
        App()
    }
}

private fun windowIconResourceNames(): List<String> {
    val isMacOs = System.getProperty("os.name").orEmpty().contains("mac", ignoreCase = true)
    return if (isMacOs) {
        listOf("app_icon_window_macos.png", "app_icon_window.png")
    } else {
        listOf("app_icon_window.png")
    }
}

private data class DesktopWindowSnapshot(
    val x: Int?,
    val y: Int?,
    val width: Int,
    val height: Int,
)

private object DesktopWindowPrefs {
    private val prefs = Preferences.userRoot().node("com.ugurbuga.stackshift.window")

    fun load(): DesktopWindowSnapshot {
        val width = prefs.getInt("width", DefaultWidth)
        val height = prefs.getInt("height", DefaultHeight)
        val hasPosition = prefs.getBoolean("hasPosition", false)
        return DesktopWindowSnapshot(
            x = prefs.getInt("x", 0).takeIf { hasPosition },
            y = prefs.getInt("y", 0).takeIf { hasPosition },
            width = width,
            height = height,
        ).normalized()
    }

    fun save(snapshot: DesktopWindowSnapshot) {
        prefs.putInt("width", snapshot.width)
        prefs.putInt("height", snapshot.height)
        if (snapshot.x != null && snapshot.y != null) {
            prefs.putBoolean("hasPosition", true)
            prefs.putInt("x", snapshot.x)
            prefs.putInt("y", snapshot.y)
        }
    }
}

private class AspectLockedWindowController(
    private val window: java.awt.Window,
    initialSnapshot: DesktopWindowSnapshot,
) {
    private var adjusting = false
    private var lastWidth = initialSnapshot.width
    private var lastHeight = initialSnapshot.height

    private val listener = object : ComponentAdapter() {
        override fun componentResized(event: ComponentEvent?) {
            if (adjusting) return
            val currentWidth = window.width.coerceAtLeast(MinWidth)
            val currentHeight = window.height.coerceAtLeast(MinHeight)
            val adjusted = adjustedForResize(currentWidth, currentHeight)
            adjusting = true
            if (adjusted.width != window.width || adjusted.height != window.height) {
                window.setSize(adjusted)
            }
            adjusting = false
            lastWidth = window.width
            lastHeight = window.height
            persist()
        }

        override fun componentMoved(event: ComponentEvent?) {
            persist()
        }
    }

    init {
        window.minimumSize = Dimension(MinWidth, MinHeight)
        val normalized = initialSnapshot.normalized()
        window.setSize(normalized.width, normalized.height)
        if (normalized.x != null && normalized.y != null) {
            window.setLocation(normalized.x, normalized.y)
        }
        lastWidth = normalized.width
        lastHeight = normalized.height
        window.addComponentListener(listener)
        persist()
    }

    fun dispose() {
        persist()
        window.removeComponentListener(listener)
    }

    private fun adjustedForResize(width: Int, height: Int): Dimension {
        val widthDelta = abs(width - lastWidth)
        val heightDelta = abs(height - lastHeight)
        return if (widthDelta >= heightDelta) {
            Dimension(width, (width / AspectRatio).roundToInt().coerceAtLeast(MinHeight))
        } else {
            Dimension((height * AspectRatio).roundToInt().coerceAtLeast(MinWidth), height)
        }
    }

    private fun persist() {
        DesktopWindowPrefs.save(
            DesktopWindowSnapshot(
                x = window.x,
                y = window.y,
                width = window.width,
                height = window.height,
            ).normalized(),
        )
    }
}

private fun DesktopWindowSnapshot.normalized(): DesktopWindowSnapshot {
    val normalizedWidth = width.coerceAtLeast(MinWidth)
    val normalizedHeight = height.coerceAtLeast(MinHeight)
    val widthDrivenHeight = (normalizedWidth / AspectRatio).roundToInt()
    val useWidth = abs(widthDrivenHeight - normalizedHeight) <= abs((normalizedHeight * AspectRatio).roundToInt() - normalizedWidth)
    val finalWidth = if (useWidth) normalizedWidth else (normalizedHeight * AspectRatio).roundToInt().coerceAtLeast(MinWidth)
    val finalHeight = if (useWidth) widthDrivenHeight.coerceAtLeast(MinHeight) else normalizedHeight
    return copy(width = finalWidth, height = finalHeight)
}

private const val DefaultWidth = 720
private const val DefaultHeight = 1280
private const val MinWidth = 360
private const val MinHeight = 640
private const val AspectRatio = 9f / 16f
