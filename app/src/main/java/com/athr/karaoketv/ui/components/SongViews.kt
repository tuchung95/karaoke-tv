package com.athr.karaoketv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
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
    TvCard(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier.width(TvSpacing.CardWidth3Up),
        focusRequester = focusRequester,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.MusicNote,
                contentDescription = null,
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
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            // Min, not fixed: the TV type scale is larger than the phone one and a
            // hard height clips two-line titles.
            modifier = Modifier.heightIn(min = 64.dp),
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
    ListItem(
        selected = false,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier
            .fillMaxWidth()
            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier),
        colors = karaokeListItemColors(),
        leadingContent = {
            Text(
                text = song.songNumber ?: ordinal?.toString() ?: "",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.width(64.dp),
            )
        },
        headlineContent = {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(
                text = listOfNotNull(song.artist, song.relPath.ifBlank { null })
                    .joinToString(" · ")
                    .ifBlank { song.fileName },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = {
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
                    )
                }
            }
        },
    )
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
    TvCard(
        onClick = onClick,
        modifier = modifier.width(TvSpacing.CardWidth4Up),
        focusRequester = focusRequester,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.heightIn(min = 64.dp),
        )
        Text(
            text = "$songCount bài",
            style = MaterialTheme.typography.bodyMedium,
            color = KaraokeColors.Accent,
        )
    }
}

/** Shared list-row colours, so every list in the app reads the same. */
@Composable
fun karaokeListItemColors() = ListItemDefaults.colors(
    containerColor = KaraokeColors.Surface,
    contentColor = KaraokeColors.OnSurface,
    focusedContainerColor = KaraokeColors.OnSurface,
    focusedContentColor = KaraokeColors.Background,
)
