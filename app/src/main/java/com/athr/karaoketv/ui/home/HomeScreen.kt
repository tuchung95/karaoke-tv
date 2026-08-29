package com.athr.karaoketv.ui.home

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.athr.karaoketv.data.db.LibraryGroup
import com.athr.karaoketv.data.db.SongEntity
import com.athr.karaoketv.data.prefs.HomeShelf
import com.athr.karaoketv.player.QueueItem
import com.athr.karaoketv.ui.components.GroupCard
import com.athr.karaoketv.ui.components.RequestInitialFocus
import com.athr.karaoketv.ui.components.SectionHeader
import com.athr.karaoketv.ui.components.SongCard
import com.athr.karaoketv.ui.components.TvButton
import com.athr.karaoketv.ui.theme.KaraokeColors
import com.athr.karaoketv.ui.theme.TvSpacing
import com.athr.karaoketv.util.formatCount

data class HomeShelves(
    val queue: List<QueueItem>,
    val recentlyPlayed: List<SongEntity>,
    val mostPlayed: List<SongEntity>,
    val favorites: List<SongEntity>,
    val recentlyAdded: List<SongEntity>,
    val categories: List<LibraryGroup>,
    val artists: List<LibraryGroup>,
)

data class HomeActions(
    val onSearch: () -> Unit,
    val onQueue: () -> Unit,
    val onCategories: () -> Unit,
    val onFolders: () -> Unit,
    val onArtists: () -> Unit,
    val onAllSongs: () -> Unit,
    val onFavorites: () -> Unit,
    val onShuffle: () -> Unit,
    val onSettings: () -> Unit,
    val onWatchVideo: (() -> Unit)?,
    val onSongClick: (SongEntity) -> Unit,
    val onSongOptions: (SongEntity) -> Unit,
    val onCategoryClick: (String) -> Unit,
    val onArtistClick: (String) -> Unit,
)

@Composable
fun HomeScreen(
    songCount: Int,
    libraryLabel: String,
    nowPlaying: SongEntity?,
    shelves: HomeShelves,
    shelfOrder: List<HomeShelf>,
    hiddenShelves: Set<HomeShelf>,
    actions: HomeActions,
    modifier: Modifier = Modifier,
) {
    val searchFocus = remember { FocusRequester() }
    RequestInitialFocus(searchFocus)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        // No horizontal padding here: the hero artwork is a background element and
        // the guidelines want those running to the panel edge. Every other block
        // applies the safe margin itself.
        contentPadding = PaddingValues(
            top = TvSpacing.ScreenVertical,
            bottom = 64.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            HomeTopBar(
                selected = HomeTab.HOME,
                onSearch = actions.onSearch,
                onTab = { tab ->
                    when (tab) {
                        HomeTab.HOME -> Unit
                        HomeTab.CATEGORIES -> actions.onCategories()
                        HomeTab.FOLDERS -> actions.onFolders()
                        HomeTab.ARTISTS -> actions.onArtists()
                        HomeTab.ALL_SONGS -> actions.onAllSongs()
                        HomeTab.FAVORITES -> actions.onFavorites()
                    }
                },
                onQueue = actions.onQueue,
                onShuffle = actions.onShuffle,
                onSettings = actions.onSettings,
                queueSize = shelves.queue.size,
                searchFocus = searchFocus,
                modifier = Modifier.padding(horizontal = TvSpacing.ScreenHorizontal),
            )
        }

        item {
            val playing = shelves.queue.firstOrNull()?.song
            HeroBanner(
                song = nowPlaying ?: shelves.mostPlayed.firstOrNull()
                    ?: shelves.recentlyAdded.firstOrNull()
                    ?: playing,
                nowPlaying = nowPlaying != null,
                onWatchVideo = actions.onWatchVideo,
                onPlaySong = actions.onSongClick,
            )
        }

        // Order and visibility come from Settings -> Bố cục màn hình chính.
        shelfOrder.filterNot { it in hiddenShelves }.forEach { shelf ->
            when (shelf) {
                HomeShelf.QUEUE -> if (shelves.queue.isNotEmpty()) {
                    songShelf(
                        title = shelf.label,
                        subtitle = "${shelves.queue.size} bài",
                        songs = shelves.queue.map { it.song },
                        onClick = { actions.onQueue() },
                        onOptions = actions.onSongOptions,
                    )
                }
                HomeShelf.RECENTLY_PLAYED -> if (shelves.recentlyPlayed.isNotEmpty()) {
                    songShelf(shelf.label, null, shelves.recentlyPlayed, actions.onSongClick, actions.onSongOptions)
                }
                HomeShelf.FAVORITES -> if (shelves.favorites.isNotEmpty()) {
                    songShelf(shelf.label, null, shelves.favorites, actions.onSongClick, actions.onSongOptions)
                }
                HomeShelf.MOST_PLAYED -> if (shelves.mostPlayed.isNotEmpty()) {
                    songShelf(shelf.label, null, shelves.mostPlayed, actions.onSongClick, actions.onSongOptions)
                }
                HomeShelf.CATEGORIES -> if (shelves.categories.isNotEmpty()) {
                    groupShelf(shelf.label, shelves.categories, actions.onCategoryClick)
                }
                HomeShelf.ARTISTS -> if (shelves.artists.isNotEmpty()) {
                    groupShelf(shelf.label, shelves.artists, actions.onArtistClick)
                }
                HomeShelf.RECENTLY_ADDED -> if (shelves.recentlyAdded.isNotEmpty()) {
                    songShelf(shelf.label, null, shelves.recentlyAdded, actions.onSongClick, actions.onSongOptions)
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.songShelf(
    title: String,
    subtitle: String?,
    songs: List<SongEntity>,
    onClick: (SongEntity) -> Unit,
    onOptions: (SongEntity) -> Unit,
) {
    item(key = "shelf-$title") {
        Column {
            SectionHeader(
                title,
                trailing = subtitle,
                modifier = Modifier.padding(start = TvSpacing.ScreenHorizontal),
            )
            Spacer(Modifier.height(12.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(TvSpacing.CardGap),
                contentPadding = PaddingValues(horizontal = TvSpacing.ScreenHorizontal),
            ) {
                // No stable key: the same song may legitimately sit in the queue twice.
                items(songs) { song ->
                    SongCard(
                        song = song,
                        onClick = { onClick(song) },
                        onLongClick = { onOptions(song) },
                    )
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.groupShelf(
    title: String,
    groups: List<LibraryGroup>,
    onClick: (String) -> Unit,
) {
    item(key = "groups-$title") {
        Column {
            SectionHeader(
                title,
                trailing = "${groups.size} mục",
                modifier = Modifier.padding(start = TvSpacing.ScreenHorizontal),
            )
            Spacer(Modifier.height(12.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(TvSpacing.CardGap),
                contentPadding = PaddingValues(horizontal = TvSpacing.ScreenHorizontal),
            ) {
                items(groups, key = { it.name }) { group ->
                    GroupCard(
                        name = group.name,
                        songCount = group.songCount,
                        onClick = { onClick(group.name) },
                    )
                }
            }
        }
    }
}
