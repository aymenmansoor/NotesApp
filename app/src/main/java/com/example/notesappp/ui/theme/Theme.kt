package com.example.notesappp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LilacLight = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9DDFF),
    onPrimaryContainer = Color(0xFF22005D),
    background = Color(0xFFFFF7FF),
    onBackground = Color(0xFF1D1A20),
    surface = Color.White,
    onSurface = Color(0xFF1D1A20),
    outline = Color(0xFF7D7487)
)

private val LilacDark = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E0E9),
    surface = Color(0xFF211F26),
    onSurface = Color(0xFFE6E0E9),
    outline = Color(0xFF938F99)
)

@Composable
fun NotesAppTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) LilacDark else LilacLight,
        typography = Typography,
        content = content
    )
}