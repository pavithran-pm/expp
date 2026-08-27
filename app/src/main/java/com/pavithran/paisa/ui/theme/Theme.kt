package com.pavithran.paisa.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ReviewAmber = Saffron400

private val LightColors = lightColorScheme(
    primary = Emerald700,
    onPrimary = Color.White,
    primaryContainer = Emerald50,
    onPrimaryContainer = Emerald900,
    secondary = Saffron600,
    onSecondary = Color.White,
    secondaryContainer = Saffron100,
    onSecondaryContainer = Color(0xFF5B3B00),
    tertiary = Emerald500,
    background = Paper,
    onBackground = Ink,
    surface = PaperCard,
    onSurface = Ink,
    surfaceVariant = Emerald50,
    onSurfaceVariant = InkSoft,
    outline = Color(0xFFC9D6D2),
    outlineVariant = Color(0xFFE1EAE7),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFF9DEDC)
)

private val DarkColors = darkColorScheme(
    primary = Emerald200,
    onPrimary = Color(0xFF00382F),
    primaryContainer = Emerald900,
    onPrimaryContainer = Emerald50,
    secondary = Saffron400,
    onSecondary = Color(0xFF3A2600),
    secondaryContainer = Color(0xFF553B08),
    onSecondaryContainer = Saffron100,
    tertiary = Emerald500,
    background = NightBase,
    onBackground = NightInk,
    surface = NightCard,
    onSurface = NightInk,
    surfaceVariant = NightLine,
    onSurfaceVariant = NightInkSoft,
    outline = Color(0xFF3A4A46),
    outlineVariant = NightLine,
    error = Color(0xFFF2B8B5),
    errorContainer = Color(0xFF8C1D18)
)

@Composable
fun PaisaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Deliberately not dynamic colour: the palette is part of the app's identity.
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = PaisaTypography,
        content = content
    )
}
