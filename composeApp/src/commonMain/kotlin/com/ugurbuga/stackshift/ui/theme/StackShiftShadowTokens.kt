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
fun Modifier.blockGamesSurfaceShadow(
    shape: Shape,
    elevation: Dp = 9.dp,
): Modifier {
    val settings = LocalAppSettings.current
    val isDark = isBlockGamesDarkTheme(settings)
    val colorScheme = MaterialTheme.colorScheme
    
    val ambientColor = if (isDark) {
        Color(0xFF030305).copy(alpha = 0.62f)
    } else {
        colorScheme.primary.copy(alpha = 0.28f)
    }
    val spotColor = if (isDark) {
        colorScheme.primary.copy(alpha = 0.18f)
    } else {
        colorScheme.primary.copy(alpha = 0.34f)
    }
    return shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = ambientColor,
        spotColor = spotColor,
        clip = false,
    )
}

