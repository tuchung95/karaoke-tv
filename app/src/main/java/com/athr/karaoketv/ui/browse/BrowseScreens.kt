package com.athr.karaoketv.ui.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.athr.karaoketv.data.db.LibraryGroup
import com.athr.karaoketv.data.db.SongEntity
import com.athr.karaoketv.ui.components.GroupCard
import com.athr.karaoketv.ui.components.SongRow
import com.athr.karaoketv.ui.theme.KaraokeColors
import com.athr.karaoketv.ui.theme.TvSpacing
import com.athr.karaoketv.util.formatCount

/** Folder, genre and artist browsing — a grid the D-pad walks in both directions. */
@Composable
fun GroupGridScreen(
    title: String,
    groups: List<LibraryGroup>,
    onGroupClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        ScreenTitle(title, "${groups.size} mục")
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = TvSpacing.CardWidth4Up),
            contentPadding = PaddingValues(
                start = TvSpacing.ScreenHorizontal,
                end = TvSpacing.ScreenHorizontal,
                bottom = 64.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(TvSpacing.CardGap),
            verticalArrangement = Arrangement.spacedBy(TvSpacing.CardGap),
        ) {
            items(groups, key = { it.name }) { group ->
                GroupCard(
                    name = group.name,
                    songCount = group.songCount,
                    onClick = { onGroupClick(group.name) },
                )
            }
        }
    }
}

/** A flat list of songs: one folder, one artist, the favourites, or everything. */
@Composable
fun SongListScreen(
    title: String,
    songs: List<SongEntity>,
    onSongClick: (SongEntity) -> Unit,
    onSongOptions: (SongEntity) -> Unit,
    modifier: Modifier = Modifier,
    onReachEnd: (() -> Unit)? = null,
    emptyMessage: String? = null,
) {
    Column(modifier = modifier.fillMaxSize()) {
        ScreenTitle(title, "${formatCount(songs.size)} bài")

        if (songs.isEmpty() && emptyMessage != null) {
            // A blank list tells nobody how to stop it being blank.
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.titleMedium,
                color = KaraokeColors.Muted,
                modifier = Modifier.padding(
                    start = TvSpacing.ScreenHorizontal,
                    end = TvSpacing.ScreenHorizontal,
                ),
            )
            return@Column
        }
        LazyColumn(
            contentPadding = PaddingValues(
                start = TvSpacing.ScreenHorizontal,
                end = TvSpacing.ScreenHorizontal,
                bottom = 64.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(songs, key = { it.id }) { song ->
                SongRow(
                    song = song,
                    onClick = { onSongClick(song) },
                    onLongClick = { onSongOptions(song) },
                )
            }
            if (onReachEnd != null && songs.isNotEmpty()) {
                item {
                    // Reached while scrolling; loads the next page of a big library.
                    androidx.compose.runtime.LaunchedEffect(songs.size) { onReachEnd() }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ScreenTitle(title: String, subtitle: String) {
    Column(
        Modifier.padding(
            start = TvSpacing.ScreenHorizontal,
            end = TvSpacing.ScreenHorizontal,
            top = TvSpacing.ScreenVertical,
            bottom = 16.dp,
        )
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = KaraokeColors.OnSurface,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = KaraokeColors.Muted,
        )
    }
}
