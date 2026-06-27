package com.netrecon.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val NeonGreen = Color(0xFF00FF41)
val DarkGreen = Color(0xFF003B00)
val MatrixGreen = Color(0xFF008F11)
val BackgroundDark = Color(0xFF0D0D0D)
val SurfaceDark = Color(0xFF1A1A1A)
val SurfaceVariant = Color(0xFF242424)
val ErrorRed = Color(0xFFFF3B3B)
val WarningAmber = Color(0xFFFFB300)
val InfoCyan = Color(0xFF00E5FF)
val TextPrimary = Color(0xFFE0E0E0)
val TextSecondary = Color(0xFF9E9E9E)

private val DarkColorScheme = darkColorScheme(
    primary = NeonGreen,
    onPrimary = BackgroundDark,
    primaryContainer = DarkGreen,
    onPrimaryContainer = NeonGreen,
    secondary = InfoCyan,
    onSecondary = BackgroundDark,
    secondaryContainer = Color(0xFF003544),
    onSecondaryContainer = InfoCyan,
    tertiary = WarningAmber,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    onError = Color.White,
    outline = Color(0xFF444444)
)

@Composable
fun NetReconTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
