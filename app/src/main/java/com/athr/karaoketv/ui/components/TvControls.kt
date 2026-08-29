package com.athr.karaoketv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.athr.karaoketv.ui.theme.KaraokeColors

/**
 * Thin wrappers over Compose for TV's own components — these exist only to bind
 * the app's palette and the icon-plus-label shape every call site wants. The focus
 * behaviour, scale, borders and glow are entirely the design system's; nothing here
 * reimplements them.
 */

@Composable
fun TvButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    emphasised: Boolean = false,
    focusRequester: FocusRequester? = null,
) {
    val colors = if (emphasised) {
        ButtonDefaults.colors(
            containerColor = KaraokeColors.Primary,
            contentColor = KaraokeColors.OnPrimary,
            focusedContainerColor = KaraokeColors.OnSurface,
            focusedContentColor = KaraokeColors.Primary,
        )
    } else {
        ButtonDefaults.colors(
            containerColor = KaraokeColors.Surface,
            contentColor = KaraokeColors.Muted,
            focusedContainerColor = KaraokeColors.OnSurface,
            focusedContentColor = KaraokeColors.Background,
        )
    }
    Button(
        onClick = withSelectSound(onClick),
        modifier = modifier
            .navigationSound()
            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier),
        colors = colors,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun TvCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    focusRequester: FocusRequester? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        onClick = withSelectSound(onClick),
        onLongClick = onLongClick,
        modifier = modifier
            .navigationSound()
            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier),
        colors = CardDefaults.colors(
            containerColor = KaraokeColors.Surface,
            contentColor = KaraokeColors.OnSurface,
            focusedContainerColor = KaraokeColors.SurfaceHigh,
            focusedContentColor = KaraokeColors.OnSurface,
        ),
    ) {
        androidx.compose.foundation.layout.Column(Modifier.padding(contentPadding)) {
            content()
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
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
