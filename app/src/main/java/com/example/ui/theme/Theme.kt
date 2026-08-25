package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Green80,
    secondary = GreenGrey80,
    tertiary = Gold80,
    background = Color(0xFF121411),
    surface = Color(0xFF1A1C18),
    onPrimary = Color(0xFF00390E),
    onSecondary = Color(0xFF00390E),
    onSurface = Color(0xFFE2E3DC)
)

private val LightColorScheme = lightColorScheme(
    primary = FarmGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = FarmGreenLight,
    onPrimaryContainer = FarmGreenPrimary,
    secondary = FarmGreenSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCEDC8),
    onSecondaryContainer = Color(0xFF1B5E20),
    tertiary = FarmGold,
    onTertiary = Color.White,
    tertiaryContainer = FarmGoldLight,
    onTertiaryContainer = Color(0xFF5D4037),
    background = BackgroundLight,
    onBackground = Color(0xFF1A1C19),
    surface = SurfaceLight,
    onSurface = Color(0xFF1A1C19),
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = Color(0xFF424940)
)

@Composable
fun SejahteraBersamaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our signature agricultural theme by default
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
