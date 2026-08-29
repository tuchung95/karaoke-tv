package com.athr.karaoketv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.athr.karaoketv.data.db.SongEntity
import com.athr.karaoketv.ui.theme.KaraokeColors
import com.athr.karaoketv.ui.theme.TvSpacing
import com.athr.karaoketv.util.formatDuration

/** Wide card for the horizontal shelves on the home screen. */
@Composable
fun SongCard(
    song: SongEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
) {
    TvFocusable(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier.width(TvSpacing.CardWidth3Up),
        focusRequester = focusRequester,
        contentPadding = PaddingValues(18.dp),
    ) { focused ->
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = if (focused) KaraokeColors.Primary else KaraokeColors.Muted,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                if (song.songNumber != null) {
                    Pill(song.songNumber)
                    Spacer(Modifier.width(6.dp))
                }
                if (song.tone != null) Pill(song.tone, color = KaraokeColors.Primary)
                if (song.favorite) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = KaraokeColors.Primary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                color = KaraokeColors.OnSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.height(56.dp),
            )
            Text(
                text = song.artist ?: song.collection ?: "—",
                style = MaterialTheme.typography.bodyMedium,
                color = KaraokeColors.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Full-width row for search results and song lists. */
@Composable
fun SongRow(
    song: SongEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    ordinal: Int? = null,
    focusRequester: FocusRequester? = null,
) {
    TvFocusable(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        focusRequester = focusRequester,
    ) { focused ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(64.dp)) {
                Text(
                    text = song.songNumber ?: ordinal?.toString() ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (focused) KaraokeColors.Accent else KaraokeColors.Muted,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = KaraokeColors.OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOfNotNull(song.artist, song.relPath.ifBlank { null })
                        .joinToString(" · ")
                        .ifBlank { song.fileName },
                    style = MaterialTheme.typography.bodyMedium,
                    color = KaraokeColors.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (song.favorite) {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = "Yêu thích",
                        tint = KaraokeColors.Primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
                if (song.tone != null) Pill(song.tone, color = KaraokeColors.Primary)
                if (song.durationMs > 0L) {
                    Text(
                        text = formatDuration(song.durationMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = KaraokeColors.Muted,
                    )
                }
            }
        }
    }
}

/** Shelf tile for a folder, genre or artist. */
@Composable
fun GroupCard(
    name: String,
    songCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
) {
    TvFocusable(
        onClick = onClick,
        modifier = modifier.width(TvSpacing.CardWidth4Up),
        focusRequester = focusRequester,
        contentPadding = PaddingValues(18.dp),
    ) { _ ->
        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = KaraokeColors.OnSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.height(56.dp),
            )
            Text(
                text = "$songCount bài",
                style = MaterialTheme.typography.bodyMedium,
                color = KaraokeColors.Accent,
            )
        }
    }
}
