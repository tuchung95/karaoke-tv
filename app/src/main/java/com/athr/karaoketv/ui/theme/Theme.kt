package com.athr.karaoketv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

/**
 * Material 3's own baseline dark palette, unaltered.
 *
 * Dark by default, as the TV colour guidance asks: a lit panel is the only light
 * source in most rooms where this runs. The tones are Google's published baseline
 * rather than a house palette, so every role is one they have already balanced
 * against the others.
 */
object KaraokeColors {
    val Background = Color(0xFF141218)
    val Scrim = Color(0xF2000000)

    /** M3 surface containers: cards sit above the page, menus above cards. */
    val Surface = Color(0xFF211F26)
    val SurfaceHigh = Color(0xFF2B2930)

    val Primary = Color(0xFFD0BCFF)
    val OnPrimary = Color(0xFF381E72)
    val Accent = Color(0xFFEFB8C8)
    val OnSurface = Color(0xFFE6E0E9)
    val Muted = Color(0xFFCAC4D0)

    /** M3 `outline`: the tone meant for a control's edge. */
    val Divider = Color(0xFF938F99)

    /** M3 `outlineVariant`: decorative separators only. */
    val DividerSubtle = Color(0xFF49454F)

    val Success = Color(0xFF6DD58C)
    val Danger = Color(0xFFF2B8B5)
}

/** Every one of the scheme's 29 roles, straight from the M3 baseline dark palette. */
private val TvColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    inversePrimary = Color(0xFF6750A4),

    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),

    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),

    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E0E9),
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE6E0E9),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    surfaceTint = Color(0xFFD0BCFF),
    inverseSurface = Color(0xFFE6E0E9),
    inverseOnSurface = Color(0xFF322F35),

    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),

    border = Color(0xFF938F99),
    borderVariant = Color(0xFF49454F),
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
