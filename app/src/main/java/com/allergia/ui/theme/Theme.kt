package com.allergia.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val GreenPrimary = Color(0xFF2E7D32)
val GreenLight = Color(0xFF66BB6A)
val GreenContainer = Color(0xFFA5D6A7)
val OrangeSurface = Color(0xFFFFF8E1)
val RedWarning = Color(0xFFE53935)
val AmberMedium = Color(0xFFFFA726)

private val LightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = Color.White,
    primaryContainer = GreenContainer,
    onPrimaryContainer = Color(0xFF002204),
    secondary = Color(0xFF52634F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD5E8CE),
    background = Color(0xFFF6FBF1),
    surface = Color(0xFFF6FBF1),
    onBackground = Color(0xFF1A1C18),
    onSurface = Color(0xFF1A1C18),
    error = RedWarning
)

private val DarkColorScheme = darkColorScheme(
    primary = GreenLight,
    onPrimary = Color(0xFF003909),
    primaryContainer = Color(0xFF0A5E18),
    onPrimaryContainer = GreenContainer,
    secondary = Color(0xFFBACAB2),
    onSecondary = Color(0xFF253422),
    secondaryContainer = Color(0xFF3B4B38),
    background = Color(0xFF1A1C18),
    surface = Color(0xFF1A1C18),
    onBackground = Color(0xFFE2E3DC),
    onSurface = Color(0xFFE2E3DC),
    error = Color(0xFFFFB4AB)
)

@Composable
fun AllergiaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography(), content = content)
}
