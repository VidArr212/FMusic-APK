package com.fmusic.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Spotify-inspired Dark Blue & Black Palette
val DarkBackground = Color(0xFF070B12)       // Deep Midnight Pitch Black/Blue
val DarkSurface = Color(0xFF0D1524)          // Card Dark Blue
val DarkSurfaceVariant = Color(0xFF131F35)   // Elevated Surface Blue
val DarkSurfaceElevated = Color(0xFF182742)  // Dialog / Floating player surface

// Electric Neon Blue & Cyan Accents (Replaces Spotify Green)
val NeonCyan = Color(0xFF00E5FF)             // Vibrant Neon Cyan for Seekbar / Active states
val NeonBlue = Color(0xFF00D2FF)             // Bright Neon Blue
val AccentBlue = Color(0xFF1E90FF)           // Dodger Blue
val RoyalBlue = Color(0xFF0052D4)            // Rich Gradient Deep Blue

// Text Colors
val TextWhite = Color(0xFFFFFFFF)
val TextGray = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)
val TextDisabled = Color(0xFF334155)

// Status Colors
val HeartRed = Color(0xFFFF3366)
val ExplicitGray = Color(0xFF475569)
val SleepTimerAmber = Color(0xFFFFB703)

// Glow Gradients
val NeonBlueGlowGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF00D2FF), Color(0xFF00E5FF), Color(0xFF80EEFF))
)

val CardGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF131F35), Color(0xFF0D1524))
)

val HeaderGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF102A4E), Color(0xFF070B12))
)
