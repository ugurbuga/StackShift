package com.ugurbuga.stackshift.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ugurbuga.stackshift.localization.LocalAppSettings

@Composable
fun Modifier.stackShiftSurfaceShadow(
    shape: Shape,
    elevation: Dp = 9.dp,
): Modifier {
    val settings = LocalAppSettings.current
    val ambientColor = if (isStackShiftDarkTheme(settings)) {
        Color.Black.copy(alpha = 0.54f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    }
    val spotColor = if (isStackShiftDarkTheme(settings)) {
        Color.Black.copy(alpha = 0.44f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
    }
    return shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = ambientColor,
        spotColor = spotColor,
        clip = false,
    )
}

