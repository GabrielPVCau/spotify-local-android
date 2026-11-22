package com.spotifylocal.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val SpotifyGreen = Color(0xFF1DB954)
val SpotifyBlack = Color(0xFF121212)
val SpotifySurface = Color(0xFF181818)
val SpotifyElevated = Color(0xFF282828)
val SpotifyTextPrimary = Color(0xFFFFFFFF)
val SpotifyTextSecondary = Color(0xFFB3B3B3)

private val SpotifyDarkColors = darkColorScheme(
    primary = SpotifyGreen,
    onPrimary = Color.Black,
    secondary = SpotifyGreen,
    onSecondary = Color.Black,
    background = SpotifyBlack,
    onBackground = SpotifyTextPrimary,
    surface = SpotifySurface,
    onSurface = SpotifyTextPrimary,
    surfaceVariant = SpotifyElevated,
    onSurfaceVariant = SpotifyTextSecondary
)

@Composable
fun SpotifyLocalTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SpotifyDarkColors,
        content = content
    )
}
