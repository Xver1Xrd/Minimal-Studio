package com.minimalstudio.app.ui

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

data class ThemeConfig(val preset: String = "midnight", val accent: String = "lilac", val cornerStyle: String = "soft")

class ThemeStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("studio_theme", Context.MODE_PRIVATE)
    fun load() = ThemeConfig(prefs.getString("preset", "midnight") ?: "midnight", prefs.getString("accent", "lilac") ?: "lilac", prefs.getString("corners", "soft") ?: "soft")
    fun save(config: ThemeConfig) { prefs.edit().putString("preset", config.preset).putString("accent", config.accent).putString("corners", config.cornerStyle).apply() }
}

data class StudioPalette(val ink: Color, val surface: Color, val surfaceRaised: Color, val accent: Color, val muted: Color, val paper: Color, val rose: Color)

private fun paletteFor(config: ThemeConfig): StudioPalette {
    val base = when (config.preset) {
        "amazed_black" -> StudioPalette(
            ink = Color(0xFF08090C),
            surface = Color(0xFF11131A),
            surfaceRaised = Color(0xFF181B24),
            accent = Color(0xFFB79BFF),
            muted = Color(0xFF9EA4B5),
            paper = Color(0xFFF3F1F7),
            rose = Color(0xFFFF9EBB)
        )
        "paper" -> StudioPalette(Color(0xFFF7F5F0), Color.White, Color(0xFFE9E4DB), Color(0xFF5B4B8A), Color(0xFF665F58), Color(0xFF24211D), Color(0xFFB64C6E))
        "forest" -> StudioPalette(Color(0xFF101914), Color(0xFF1A2920), Color(0xFF293A2E), Color(0xFFB8D99B), Color(0xFFB0BBB0), Color(0xFFF0F5ED), Color(0xFFF1A9A9))
        "ocean" -> StudioPalette(Color(0xFF0D1720), Color(0xFF172936), Color(0xFF263B4A), Color(0xFF9DD7F0), Color(0xFFAAB8C2), Color(0xFFF1F6F8), Color(0xFFFFB1C2))
        "rose" -> StudioPalette(Color(0xFF1B1117), Color(0xFF2A1A23), Color(0xFF3A2831), Color(0xFFFFB4CD), Color(0xFFC9B2BD), Color(0xFFFFF4F7), Color(0xFFFFB0C7))
        else -> StudioPalette(Color(0xFF101115), Color(0xFF1B1C23), Color(0xFF292A34), Color(0xFFDAC8FE), Color(0xFFA9AAB8), Color(0xFFF4F2F8), Color(0xFFF0AFC5))
    }
    val accent = when (config.accent) { "mint" -> Color(0xFFA8E6CF); "sky" -> Color(0xFF9DD7F0); "peach" -> Color(0xFFFFC59C); "rose" -> Color(0xFFFFB4CD); "lemon" -> Color(0xFFF2DE8E); else -> base.accent }
    return base.copy(accent = accent)
}

val LocalStudioPalette = staticCompositionLocalOf { paletteFor(ThemeConfig()) }
val Ink: Color @Composable @ReadOnlyComposable get() = LocalStudioPalette.current.ink
val Surface: Color @Composable @ReadOnlyComposable get() = LocalStudioPalette.current.surface
val SurfaceRaised: Color @Composable @ReadOnlyComposable get() = LocalStudioPalette.current.surfaceRaised
val Accent: Color @Composable @ReadOnlyComposable get() = LocalStudioPalette.current.accent
val Muted: Color @Composable @ReadOnlyComposable get() = LocalStudioPalette.current.muted
val Paper: Color @Composable @ReadOnlyComposable get() = LocalStudioPalette.current.paper
val Rose: Color @Composable @ReadOnlyComposable get() = LocalStudioPalette.current.rose

@Composable
fun StudioTheme(config: ThemeConfig, content: @Composable () -> Unit) {
    val targetPalette = remember(config) { paletteFor(config) }
    val palette = StudioPalette(
        ink = animateColorAsState(targetPalette.ink, tween(450), label = "themeInk").value,
        surface = animateColorAsState(targetPalette.surface, tween(450), label = "themeSurface").value,
        surfaceRaised = animateColorAsState(targetPalette.surfaceRaised, tween(450), label = "themeSurfaceRaised").value,
        accent = animateColorAsState(targetPalette.accent, tween(450), label = "themeAccent").value,
        muted = animateColorAsState(targetPalette.muted, tween(450), label = "themeMuted").value,
        paper = animateColorAsState(targetPalette.paper, tween(450), label = "themePaper").value,
        rose = animateColorAsState(targetPalette.rose, tween(450), label = "themeRose").value
    )
    val colors = darkColorScheme(primary = palette.accent, onPrimary = palette.ink, background = palette.ink, onBackground = palette.paper, surface = palette.surface, onSurface = palette.paper, surfaceVariant = palette.surfaceRaised, onSurfaceVariant = palette.muted, secondary = palette.rose, onSecondary = palette.ink, outline = palette.muted.copy(alpha = .3f), error = Color(0xFFFFADAA))
    CompositionLocalProvider(LocalStudioPalette provides palette) {
        MaterialTheme(colorScheme = colors, typography = Typography().run { copy(headlineLarge = headlineLarge.copy(fontWeight = FontWeight.SemiBold), headlineMedium = headlineMedium.copy(fontWeight = FontWeight.SemiBold), titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold), titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold)) }, content = content)
    }
}
