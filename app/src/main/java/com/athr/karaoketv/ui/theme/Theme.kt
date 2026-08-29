package com.athr.karaoketv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

/**
 * Built on Compose for TV's Material design system rather than the phone one, so
 * focus scale, borders, glow and the type scale are Google's TV values rather than
 * numbers picked here. Only the palette is ours.
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
    border = KaraokeColors.Divider,
    error = KaraokeColors.Danger,
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
        content = content,
    )
}
