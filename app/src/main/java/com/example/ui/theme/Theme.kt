package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = DarkTeal,
    secondary = EncryptionBlue,
    tertiary = TerminalGreen,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = SoftWhite,
    onSurface = SoftWhite,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = SoftGray,
    error = Color(0xFFFF4B4B)
)

private val LightColorScheme = lightColorScheme(
    primary = SoftMutedTeal,
    secondary = MutedBlue,
    tertiary = TerminalGreen,
    background = Color(0xFFF0F4F8),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1E252D),
    onSurface = Color(0xFF1E252D),
    surfaceVariant = Color(0xFFE2EAF0)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark Theme for tactility
    dynamicColor: Boolean = false, // Use our handcrafted uniform design
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
