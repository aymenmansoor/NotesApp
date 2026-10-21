package com.example.notesappp

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// Option 0 is the default card color, options 1-5 are the pastel colors
const val NOTE_COLOR_OPTIONS = 6

private val lightPalette = listOf(
    0xFFFFE0E6, // pink
    0xFFFFF1C9, // yellow
    0xFFD9F2E1, // green
    0xFFD6E8FF, // blue
    0xFFE9DDFF  // lilac
).map { Color(it) }

private val darkPalette = listOf(
    0xFF5A2E3A,
    0xFF5A4A1F,
    0xFF264A33,
    0xFF263E5C,
    0xFF45386B
).map { Color(it) }

@Composable
fun noteContainerColor(index: Int): Color {
    val scheme = MaterialTheme.colorScheme
    if (index <= 0 || index >= NOTE_COLOR_OPTIONS) return scheme.surface
    val isDark = scheme.background.luminance() < 0.5f
    return (if (isDark) darkPalette else lightPalette)[index - 1]
}