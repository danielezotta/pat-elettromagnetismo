package it.danielezotta.patelettromagnetismo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = TrentinoRedLight,
    onPrimary = TrentinoOnContainer,
    primaryContainer = TrentinoRedDark,
    onPrimaryContainer = TrentinoOnRed,
    secondary = Color(0xFF3C3C3C),
    onSecondary = Color(0xFFE0E0E0),
    secondaryContainer = Color(0xFF2C2C2C),
    onSecondaryContainer = Color(0xFFE0E0E0),
    tertiary = Color(0xFF3C3C3C),
    onTertiary = Color(0xFFE0E0E0),
    tertiaryContainer = Color(0xFF2C2C2C),
    onTertiaryContainer = Color(0xFFE0E0E0),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF212121),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFBDBDBD),
    surfaceContainer = Color(0xFF2A2A2A),
    surfaceContainerHigh = Color(0xFF303030),
    surfaceContainerHighest = Color(0xFF383838),
    surfaceContainerLow = Color(0xFF1E1E1E),
    surfaceContainerLowest = Color(0xFF181818)
)

private val LightColorScheme = lightColorScheme(
    primary = TrentinoRed,
    onPrimary = TrentinoOnRed,
    primaryContainer = TrentinoContainer,
    onPrimaryContainer = TrentinoOnContainer,
    secondary = Color(0xFFD9D9D9),
    onSecondary = Color(0xFF424242),
    secondaryContainer = Color(0xFFEEEEEE),
    onSecondaryContainer = Color(0xFF212121),
    tertiary = Color(0xFFD9D9D9),
    onTertiary = Color(0xFF424242),
    tertiaryContainer = Color(0xFFEEEEEE),
    onTertiaryContainer = Color(0xFF212121),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF212121),
    surface = Color(0xFFF5F5F5),
    onSurface = Color(0xFF212121),
    surfaceVariant = Color(0xFFEEEEEE),
    onSurfaceVariant = Color(0xFF616161),
    surfaceContainer = Color(0xFFF0F0F0),
    surfaceContainerHigh = Color(0xFFE8E8E8),
    surfaceContainerHighest = Color(0xFFE0E0E0),
    surfaceContainerLow = Color(0xFFF5F5F5),
    surfaceContainerLowest = Color(0xFFFFFFFF)
)

@Composable
fun PATPermessiElettromagnetismoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}