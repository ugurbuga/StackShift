package com.ugurbuga.blockgames.telemetry

import androidx.compose.runtime.Composable

@Composable
actual fun rememberAppTelemetry(): AppTelemetry = NoOpAppTelemetry

