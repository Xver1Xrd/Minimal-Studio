package com.minimalstudio.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Typography

val Ink = Color(0xFF101115)
val Surface = Color(0xFF1B1C23)
val SurfaceRaised = Color(0xFF292A34)
val Accent = Color(0xFFDAC8FE)
val Muted = Color(0xFFA9AAB8)
val Paper = Color(0xFFF4F2F8)
val Rose = Color(0xFFF0AFC5)

private val colors = darkColorScheme(
    primary = Accent,
    onPrimary = Ink,
    background = Ink,
    onBackground = Paper,
    surface = Surface,
    onSurface = Paper,
    surfaceVariant = SurfaceRaised,
    onSurfaceVariant = Muted,
    secondary = Rose,
    onSecondary = Ink,
    outline = Color(0xFF3B3D48),
    error = Color(0xFFFFADAA)
)

@Composable
fun StudioTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colors,
        typography = Typography().run {
            copy(
                headlineLarge = headlineLarge.copy(fontWeight = FontWeight.SemiBold),
                headlineMedium = headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
                titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        },
        content = content
    )
}
