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
    primary = BentoPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF312E81),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = BentoSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF075985),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = BentoTertiary,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = Color(0xFFF8FAFC),
    surface = DarkSurface,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF475569),
    outlineVariant = BentoBorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = BentoPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEF2FF),
    onPrimaryContainer = Color(0xFF3730A3),
    secondary = BentoSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0F9FF),
    onSecondaryContainer = Color(0xFF0369A1),
    tertiary = BentoTertiary,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = Color(0xFF0F172A),
    surface = LightSurface,
    onSurface = Color(0xFF1E293B),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFCBD5E1),
    outlineVariant = BentoBorderLight
)

@Composable
fun QuizPlatformTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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
