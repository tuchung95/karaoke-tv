package com.athr.karaoketv.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A living room is dark and the viewer is three metres away, so the palette is
 * dark-only and the type scale is roughly 1.4x the phone defaults. Every value
 * here was picked to stay readable over a moving karaoke video.
 */
object KaraokeColors {
    val Background = Color(0xFF07090D)
    val Scrim = Color(0xF20A0D12)
    val Surface = Color(0xFF141922)
    val SurfaceHigh = Color(0xFF1E2531)
    val Primary = Color(0xFFFF4D8D)
    val OnPrimary = Color(0xFF1A0009)
    val Accent = Color(0xFFFFB020)
    val OnSurface = Color(0xFFF2F4F8)
    val Muted = Color(0xFF97A0B0)
    val Divider = Color(0xFF2A3140)
    val Success = Color(0xFF4ADE80)
    val Danger = Color(0xFFFF6B6B)
}

private val TvColorScheme = darkColorScheme(
    primary = KaraokeColors.Primary,
    onPrimary = KaraokeColors.OnPrimary,
    secondary = KaraokeColors.Accent,
    onSecondary = Color(0xFF201400),
    background = KaraokeColors.Background,
    onBackground = KaraokeColors.OnSurface,
    surface = KaraokeColors.Surface,
    onSurface = KaraokeColors.OnSurface,
    surfaceVariant = KaraokeColors.SurfaceHigh,
    onSurfaceVariant = KaraokeColors.Muted,
    outline = KaraokeColors.Divider,
    error = KaraokeColors.Danger,
)

private val TvTypography = Typography(
    displayLarge = TextStyle(fontSize = 48.sp, lineHeight = 56.sp, fontWeight = FontWeight.Bold),
    displayMedium = TextStyle(fontSize = 40.sp, lineHeight = 48.sp, fontWeight = FontWeight.Bold),
    headlineLarge = TextStyle(fontSize = 34.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 19.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontSize = 17.sp, lineHeight = 24.sp),
    labelLarge = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
)

/**
 * Android TV's layout grid, at the 960x540dp canvas every TV renders to.
 *
 * 48dp/24dp is the bare overscan margin; the guidelines put the recommended safe
 * zone at 58dp on the sides and 28dp top and bottom, which is what these are. The
 * 12-column grid is 52dp columns with 20dp gutters, leaving 844dp of usable width
 * — which is why the card widths below divide it exactly.
 */
object TvSpacing {
    val ScreenHorizontal = 58.dp
    val ScreenVertical = 28.dp
    val Gutter = 20.dp
    val CardGap = 20.dp

    /** 844dp of content width, split by the grid. */
    val CardWidth3Up = 268.dp
    val CardWidth4Up = 196.dp
}

@Composable
fun KaraokeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TvColorScheme,
        typography = TvTypography,
        content = content,
    )
}
