package com.pulsedesk.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color.Black,
    secondary = NeonTeal,
    background = BgDarkStart,
    onBackground = TextWhite,
    surface = GlassSurface,
    onSurface = TextWhite
)

@Composable
fun PulseDeskTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content
    )
}
