package com.example.myapplication

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Define the light color scheme
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6200EE), // Purple
    onPrimary = Color.White, // Text color on primary
    secondary = Color(0xFF03DAC6), // Teal
    onSecondary = Color.Black, // Text color on secondary
    background = Color(0xFFF6F6F6), // Light background
    onBackground = Color.Black, // Text color on background
    surface = Color.White, // Surface color
    onSurface = Color.Black // Text color on surface
)

// Define the dark color scheme (optional)
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFBB86FC), // Light purple
    onPrimary = Color.Black, // Text color on primary
    secondary = Color(0xFF03DAC6), // Teal
    onSecondary = Color.Black, // Text color on secondary
    background = Color(0xFF121212), // Dark background
    onBackground = Color.White, // Text color on background
    surface = Color(0xFF1E1E1E), // Dark surface
    onSurface = Color.White // Text color on surface
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Parameter to toggle dark theme
    content: @Composable () -> Unit
) {
    // Choose color scheme based on the theme preference
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
