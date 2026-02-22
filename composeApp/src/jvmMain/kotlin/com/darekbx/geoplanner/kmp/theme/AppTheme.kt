package com.darekbx.geoplanner.kmp.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primaryContainer = Color.Black,
    background = Color.Black,
    primary = Color(0xFFFFB300),
    onPrimary = Color.Black,
    secondary = Color.Cyan,
    error = Color(220, 70, 40)
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
