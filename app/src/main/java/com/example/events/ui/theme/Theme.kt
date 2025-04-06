package com.example.events.ui.theme

import android.app.Activity
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
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

// En tu archivo Theme.kt o donde definas tus colores
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6200EE),  // Púrpura oscuro
    secondary = Color(0xFF03DAC6), // Turquesa
    tertiary = Color(0xFF3700B3),  // Púrpura más oscuro
    background = Color(0xFFF5F5F5), // Fondo claro
    surface = Color.White,         // Superficies blancas
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color(0xFF212121), // Texto oscuro
    onSurface = Color(0xFF212121),
)

@Composable
fun EventsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
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