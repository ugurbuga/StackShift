package com.ugurbuga.stackshift.telemetry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable

@Stable
interface AppTelemetry {
    fun logScreen(screenName: String)
    fun logEvent(eventName: String, parameters: Map<String, String> = emptyMap())
    fun logUserProperty(name: String, value: String?)
    fun logHighScoreReached(newScore: Int, previousHighScore: Int)
    fun recordException(throwable: Throwable)
    fun logUserAction(action: String, parameters: Map<String, String> = emptyMap())
}

object NoOpAppTelemetry : AppTelemetry {
    override fun logScreen(screenName: String) = Unit
    override fun logEvent(eventName: String, parameters: Map<String, String>) = Unit
    override fun logUserProperty(name: String, value: String?) = Unit
    override fun logHighScoreReached(newScore: Int, previousHighScore: Int) = Unit
    override fun recordException(throwable: Throwable) = Unit
    override fun logUserAction(action: String, parameters: Map<String, String>) = Unit
}

object TelemetryEventNames {
    const val ScreenView = "screen_view"
    const val UserAction = "user_action"
    const val UserPropertyChanged = "user_property_changed"
    const val HighScoreReached = "high_score_reached"
    const val Exception = "exception"
}

object TelemetryActionNames {
    const val OpenSettings = "open_settings"
    const val OpenTutorial = "open_tutorial"
    const val OpenBlockProperties = "open_block_properties"
    const val OpenPieceProperties = "open_piece_properties"
    const val HoldPiece = "hold_piece"
    const val TogglePause = "toggle_pause"
    const val RestartGame = "restart_game"
    const val PlayAgain = "play_again"
    const val SettingsChanged = "settings_changed"
}

object TelemetryParamKeys {
    const val ClientId = "client_id"
    const val ScreenName = "screen_name"
    const val ScreenClass = "screen_class"
    const val Action = "action"
    const val SettingName = "setting_name"
    const val SettingValue = "setting_value"
    const val Score = "score"
    const val PreviousScore = "previous_score"
    const val ExceptionType = "exception_type"
    const val ExceptionMessage = "exception_message"
    const val PackageName = "package_name"
    const val Manufacturer = "manufacturer"
    const val Model = "model"
    const val Device = "device"
    const val Product = "product"
    const val Release = "release"
    const val SdkInt = "sdk_int"
    const val SharedClientId = "shared_client_id"
}

object TelemetryScreenNames {
    const val Game = "game"
    const val Settings = "settings"
    const val BlockProperties = "block_properties"
    const val PieceProperties = "piece_properties"
    const val Tutorial = "tutorial"
}

object TelemetryUserPropertyNames {
    const val ClientId = "client_id"
    const val Language = "language"
    const val ThemeMode = "theme_mode"
    const val ThemePalette = "theme_palette"
    const val BlockPalette = "block_palette"
    const val BlockStyle = "block_style"
    const val BoardBlockStyleMode = "board_block_style_mode"
    const val SharedClientId = "shared_client_id"
}

object TelemetryStorageKeys {
    const val ClientId = "stackshift.telemetry.client_id"
    const val SharedClientId = "stackshift.telemetry.shared_client_id"
}

@Composable
expect fun rememberAppTelemetry(): AppTelemetry

@Composable
fun LogScreen(telemetry: AppTelemetry, screenName: String) {
    LaunchedEffect(screenName) {
        telemetry.logScreen(screenName)
    }
}
