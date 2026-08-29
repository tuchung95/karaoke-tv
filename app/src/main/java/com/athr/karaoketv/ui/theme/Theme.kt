package com.athr.karaoketv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

/**
 * Built on Compose for TV's Material design system rather than the phone one, so
 * focus scale, borders, glow and the type scale are Google's TV values rather than
 * numbers picked here. Only the hues are ours.
 *
 * Dark by default, as the TV colour guidance asks: a lit-up panel is the only
 * light source in most rooms where this app runs.
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

    /**
     * Raised from a near-invisible #2A3140, which measured 1.53:1 against the
     * background — well under the 3:1 that a UI boundary needs to be seen at all.
     */
    val Divider = Color(0xFF5A6577)

    /** Decorative separators only, never a control's edge. */
    val DividerSubtle = Color(0xFF2A3140)
    val Success = Color(0xFF4ADE80)
    val Danger = Color(0xFFFF6B6B)
}

/**
 * Every one of the scheme's roles, not just the handful the screens name directly.
 *
 * Leaving the rest unset does not leave them empty: they fall back to Material's
 * baseline purple, so any component reaching for `tertiaryContainer` or
 * `surfaceTint` would quietly paint itself a colour from another app's brand.
 * Tones follow the Material 3 dark-theme convention — accent at 80, its "on" at
 * 20, container at 30, that container's "on" at 90 — except where a brand colour
 * already clears the contrast bar on its own.
 */
private val TvColorScheme = darkColorScheme(
    primary = KaraokeColors.Primary,
    onPrimary = KaraokeColors.OnPrimary,
    primaryContainer = Color(0xFF950434),
    onPrimaryContainer = Color(0xFFFECDDD),
    inversePrimary = Color(0xFFC70546),

    secondary = Color(0xFFDBBDC7),
    onSecondary = Color(0xFF42242E),
    secondaryContainer = Color(0xFF633645),
    onSecondaryContainer = Color(0xFFEDDEE3),

    tertiary = KaraokeColors.Accent,
    onTertiary = Color(0xFF201400),
    tertiaryContainer = Color(0xFF956504),
    onTertiaryContainer = Color(0xFFFEEECD),

    background = KaraokeColors.Background,
    onBackground = KaraokeColors.OnSurface,
    surface = KaraokeColors.Surface,
    onSurface = KaraokeColors.OnSurface,
    surfaceVariant = KaraokeColors.SurfaceHigh,
    onSurfaceVariant = KaraokeColors.Muted,
    surfaceTint = KaraokeColors.Primary,
    inverseSurface = KaraokeColors.OnSurface,
    inverseOnSurface = Color(0xFF2E3138),

    error = KaraokeColors.Danger,
    onError = Color(0xFF5C110A),
    errorContainer = Color(0xFF8A190F),
    onErrorContainer = Color(0xFFFAD4D1),

    border = KaraokeColors.Divider,
    borderVariant = KaraokeColors.DividerSubtle,
    // Material puts the scrim at pure black and varies only its alpha.
    scrim = Color(0xFF000000),
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
