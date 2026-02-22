package com.gymtracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Gym-focused dark palette
val Neon = Color(0xFFCCFF00)          // electric lime accent
val NeonDim = Color(0xFF99BF00)
val Surface = Color(0xFF0F0F0F)
val SurfaceVariant = Color(0xFF1C1C1C)
val Card = Color(0xFF1E1E1E)
val CardElevated = Color(0xFF252525)
val OnSurface = Color(0xFFE8E8E8)
val SubText = Color(0xFF888888)
val ErrorRed = Color(0xFFFF4444)
val PushColor = Color(0xFFFF6B35)
val PullColor = Color(0xFF4ECDC4)
val LegsColor = Color(0xFFFFE66D)
val OtherColor = Color(0xFFB8B8B8)

private val DarkColors = darkColorScheme(
    primary = Neon,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF2A3300),
    onPrimaryContainer = Neon,
    secondary = Color(0xFF4ECDC4),
    onSecondary = Color.Black,
    background = Surface,
    onBackground = OnSurface,
    surface = SurfaceVariant,
    onSurface = OnSurface,
    surfaceVariant = Card,
    onSurfaceVariant = SubText,
    error = ErrorRed,
    onError = Color.White,
)

@Composable
fun HumTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content
    )
}

fun categoryColor(category: String): Color = when (category.lowercase()) {
    "push" -> PushColor
    "pull" -> PullColor
    "legs" -> LegsColor
    else -> OtherColor
}
