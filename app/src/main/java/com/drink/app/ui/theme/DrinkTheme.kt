package com.drink.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun DrinkTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(
        primary = Color.Black, onPrimary = Color.White,
        secondary = Color.Black, onSecondary = Color.White,
        tertiary = Color.Black, onTertiary = Color.White,
        background = Color.White, onBackground = Color.Black,
        surface = Color.White, onSurface = Color.Black,
        surfaceVariant = Color(0xFFF4F4F4), onSurfaceVariant = Color.Black,
        primaryContainer = Color(0xFFEEEEEE), onPrimaryContainer = Color.Black,
        secondaryContainer = Color(0xFFEEEEEE), onSecondaryContainer = Color.Black,
        outline = Color.Gray, error = Color.Black, onError = Color.White
    ), content = content)
}
