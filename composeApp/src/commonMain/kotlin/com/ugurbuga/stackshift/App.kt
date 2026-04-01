package com.ugurbuga.stackshift

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.ugurbuga.stackshift.ui.game.StackShiftGameApp

private val StackShiftColors = darkColorScheme(
    primary = Color(0xFF7CFFB2),
    secondary = Color(0xFF9B8CFF),
    tertiary = Color(0xFFFFD166),
    background = Color(0xFF070B14),
    surface = Color(0xFF12182A),
    onPrimary = Color(0xFF04110A),
    onBackground = Color(0xFFF3F6FF),
    onSurface = Color(0xFFF3F6FF),
)

@Composable
fun StackShiftTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = StackShiftColors, content = content)
}

@Composable
@Preview
fun App() {
    StackShiftTheme {
        StackShiftGameApp()
    }
}