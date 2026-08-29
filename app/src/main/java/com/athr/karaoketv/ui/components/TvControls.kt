package com.athr.karaoketv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.athr.karaoketv.ui.theme.KaraokeColors

/**
 * The one focusable primitive the whole app is built from. A TV has no cursor, so
 * focus has to be unmistakable: the card grows, brightens, and gains a coloured
 * ring. Everything clickable in this app uses this so focus reads identically
 * across the home rows, the keyboard and the transport bar.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TvFocusable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(16.dp),
    containerColor: Color = KaraokeColors.Surface,
    focusedContainerColor: Color = KaraokeColors.SurfaceHigh,
    focusRing: Color = KaraokeColors.Primary,
    focusScale: Float = 1.05f,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
    focusRequester: FocusRequester? = null,
    content: @Composable BoxScope.(focused: Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) focusScale else 1f, label = "focusScale")

    Box(
        modifier = modifier
            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
            .scale(scale)
            .background(if (focused) focusedContainerColor else containerColor, shape)
            .border(
                BorderStroke(if (focused) 3.dp else 1.dp, if (focused) focusRing else KaraokeColors.Divider),
                shape,
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(contentPadding),
        contentAlignment = Alignment.CenterStart,
    ) {
        content(focused)
    }
}

@Composable
fun TvButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    emphasised: Boolean = false,
    focusRequester: FocusRequester? = null,
) {
    TvFocusable(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        containerColor = if (emphasised) KaraokeColors.Primary else KaraokeColors.Surface,
        focusedContainerColor = if (emphasised) KaraokeColors.Primary else KaraokeColors.SurfaceHigh,
        // A pink ring on a pink button is no ring at all: the emphasised button
        // needs a contrasting one or nobody can tell it is the selected control.
        focusRing = if (emphasised) KaraokeColors.OnSurface else KaraokeColors.Primary,
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
        focusRequester = focusRequester,
    ) { focused ->
        val tint = when {
            emphasised -> KaraokeColors.OnPrimary
            focused -> KaraokeColors.OnSurface
            else -> KaraokeColors.Muted
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = tint)
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = KaraokeColors.OnSurface,
        )
        if (trailing != null) {
            Text(
                text = "  $trailing",
                style = MaterialTheme.typography.bodyMedium,
                color = KaraokeColors.Muted,
            )
        }
    }
}

@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = KaraokeColors.Accent,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        maxLines = 1,
        modifier = modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
