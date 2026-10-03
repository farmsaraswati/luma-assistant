package com.example.lumaassistant.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val LumaColorScheme = darkColorScheme(
    background = MidnightBase,
    surface = MidnightSurface,
    surfaceVariant = MidnightSurfaceRaised,
    primary = Amber,
    onPrimary = MidnightBase,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    secondary = AmberDim
)

@Composable
fun LumaAssistantTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LumaColorScheme,
        content = content
    )
}
