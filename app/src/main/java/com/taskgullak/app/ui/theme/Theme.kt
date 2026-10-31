package com.taskgullak.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TaskGullakColorScheme = lightColorScheme(
    primary = TgAuthPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE7F6),
    onPrimaryContainer = TgAuthPurpleDark,
    secondary = TgBluePrimary,
    onSecondary = Color.White,
    tertiary = TgOrangePrimary,
    background = TgAuthBg,
    onBackground = TgAuthTextMain,
    surface = Color.White,
    onSurface = TgAuthTextMain,
    error = TgAuthError,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TaskGullakColorScheme,
        typography = Typography,
        content = content
    )
}
