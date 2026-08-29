package com.athr.karaoketv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.athr.karaoketv.data.db.SongEntity
import com.athr.karaoketv.data.youtube.YouTubeLauncher
import com.athr.karaoketv.ui.browse.GroupGridScreen
import com.athr.karaoketv.ui.browse.SongListScreen
import com.athr.karaoketv.ui.home.HomeActions
import com.athr.karaoketv.ui.home.HomeScreen
import com.athr.karaoketv.ui.home.HomeShelves
import com.athr.karaoketv.ui.components.ScreenNavBar
import com.athr.karaoketv.ui.queue.QueueScreen
import com.athr.karaoketv.ui.search.SearchScreen
import com.athr.karaoketv.ui.setup.SettingsScreen
import com.athr.karaoketv.ui.theme.KaraokeColors

/** Routes the current [Screen] and hangs the persistent key hints under it. */
@Composable
fun BrowserContent(
    vm: KaraokeViewModel,
    screen: Screen,
    onNavigate: (Screen) -> Unit,
    onSongSelected: (SongEntity) -> Unit,
    onSongOptions: (SongEntity) -> Unit,
    onOpenSetup: () -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onWatchVideo: (() -> Unit)?,
) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // Home is the root: nothing to go back to, and it already carries its
            // own button row.
            if (screen != Screen.Home) {
                ScreenNavBar(onBack = onBack, onHome = onHome, onWatchVideo = onWatchVideo)
            }
            Box(Modifier.weight(1f)) {
                when (screen) {
                    Screen.Home ->
                        HomeRoute(vm, onNavigate, onSongSelected, onSongOptions, onOpenSetup)
                    Screen.Search -> SearchRoute(vm, onSongSelected, onSongOptions)
                    Screen.Categories -> CategoriesRoute(vm, onNavigate)
                    Screen.Artists -> ArtistsRoute(vm, onNavigate)
                    Screen.Queue -> QueueRoute(vm)
                    Screen.Settings -> SettingsRoute(vm, onOpenSetup)
                    is Screen.SongList ->
                        SongListRoute(vm, screen, onSongSelected, onSongOptions)
                }
            }
        }
        KeyHints(modifier = Modifier.align(Alignment.BottomEnd))
    }
}

@Composable
private fun HomeRoute(
    vm: KaraokeViewModel,
    onNavigate: (Screen) -> Unit,
    onSongSelected: (SongEntity) -> Unit,
    onSongOptions: (SongEntity) -> Unit,
    onOpenSetup: () -> Unit,
) {
    val songCount by vm.songCount.collectAsStateWithLifecycle()
    val queue by vm.player.queue.collectAsStateWithLifecycle()
    val recentlyPlayed by vm.recentlyPlayed.collectAsStateWithLifecycle()
    val mostPlayed by vm.mostPlayed.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val recentlyAdded by vm.recentlyAdded.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val artists by vm.artists.collectAsStateWithLifecycle()

    HomeScreen(
        songCount = songCount,
        libraryLabel = vm.libraryLabel,
        shelves = HomeShelves(
            queue = queue,
            recentlyPlayed = recentlyPlayed,
            mostPlayed = mostPlayed,
            favorites = favorites,
            recentlyAdded = recentlyAdded,
            categories = categories,
            artists = artists,
        ),
        actions = HomeActions(
            onSearch = { onNavigate(Screen.Search) },
            onQueue = { onNavigate(Screen.Queue) },
            onCategories = { onNavigate(Screen.Categories) },
            onArtists = { onNavigate(Screen.Artists) },
            onAllSongs = {
                onNavigate(Screen.SongList("Tất cả bài hát", SongListSource.All))
            },
            onFavorites = {
                onNavigate(Screen.SongList("Yêu thích", SongListSource.Favorites))
            },
            onShuffle = { vm.shuffleIntoQueue() },
            onSettings = { onNavigate(Screen.Settings) },
            onSongClick = onSongSelected,
            onSongOptions = onSongOptions,
            onCategoryClick = { name ->
                onNavigate(Screen.SongList(name, SongListSource.Category(name)))
            },
            onArtistClick = { name ->
                onNavigate(Screen.SongList(name, SongListSource.Artist(name)))
            },
        ),
    )
}

@Composable
private fun SearchRoute(
    vm: KaraokeViewModel,
    onSongSelected: (SongEntity) -> Unit,
    onSongOptions: (SongEntity) -> Unit,
) {
    val context = LocalContext.current
    val query by vm.query.collectAsStateWithLifecycle()
    val results by vm.searchResults.collectAsStateWithLifecycle()
    val youTubeAvailable = remember { YouTubeLauncher.isAvailable(context) }

    SearchScreen(
        query = query,
        results = results,
        onQueryChange = vm::setQuery,
        onKey = vm::appendToQuery,
        onBackspace = vm::backspaceQuery,
        onClear = vm::clearQuery,
        onSongClick = onSongSelected,
        onSongOptions = onSongOptions,
        youTubeAvailable = youTubeAvailable,
        onYouTubeSearch = { vm.searchOnYouTube(context, query) },
    )
}

@Composable
private fun CategoriesRoute(vm: KaraokeViewModel, onNavigate: (Screen) -> Unit) {
    val categories by vm.categories.collectAsStateWithLifecycle()
    GroupGridScreen(
        title = "Thể loại / Thư mục",
        groups = categories,
        onGroupClick = { name ->
            onNavigate(Screen.SongList(name, SongListSource.Category(name)))
        },
    )
}

@Composable
private fun ArtistsRoute(vm: KaraokeViewModel, onNavigate: (Screen) -> Unit) {
    val artists by vm.artists.collectAsStateWithLifecycle()
    GroupGridScreen(
        title = "Ca sĩ",
        groups = artists,
        onGroupClick = { name ->
            onNavigate(Screen.SongList(name, SongListSource.Artist(name)))
        },
    )
}

@Composable
private fun QueueRoute(vm: KaraokeViewModel) {
    val current by vm.player.current.collectAsStateWithLifecycle()
    val queue by vm.player.queue.collectAsStateWithLifecycle()
    QueueScreen(
        current = current,
        queue = queue,
        onPlayNow = { item ->
            vm.player.removeAt(item.uid)
            vm.player.playNow(item.song)
        },
        onPrioritise = { vm.player.prioritise(it.uid) },
        onMoveUp = { vm.player.moveUp(it.uid) },
        onMoveDown = { vm.player.moveDown(it.uid) },
        onRemove = { vm.player.removeAt(it.uid) },
        onClearAll = vm.player::clearQueue,
        onSkipCurrent = vm.player::next,
    )
}

@Composable
private fun SettingsRoute(vm: KaraokeViewModel, onOpenSetup: () -> Unit) {
    val songCount by vm.songCount.collectAsStateWithLifecycle()
    val scanState by vm.scanState.collectAsStateWithLifecycle()
    val scaleMode by vm.player.scaleMode.collectAsStateWithLifecycle()
    val updateState by vm.updateState.collectAsStateWithLifecycle()
    val pitch by vm.player.pitchSemitones.collectAsStateWithLifecycle()
    var autoNext by remember { mutableStateOf(vm.prefs.autoNext) }
    var nextUpBanner by remember { mutableStateOf(vm.prefs.showNextUpBanner) }
    var youTubeKeyword by remember { mutableStateOf(vm.prefs.appendKaraokeToYouTube) }
    val context = LocalContext.current
    val youTubeAvailable = remember { YouTubeLauncher.isAvailable(context) }

    SettingsScreen(
        libraryLabel = vm.libraryLabel,
        songCount = songCount,
        scanState = scanState,
        autoNext = autoNext,
        nextUpBanner = nextUpBanner,
        appendKaraokeToYouTube = youTubeKeyword,
        youTubeAvailable = youTubeAvailable,
        currentVersion = vm.currentVersion,
        updateState = updateState,
        scaleModeLabel = when (scaleMode) {
            1 -> "Phóng to"
            2 -> "Kéo đầy"
            else -> "Vừa khung"
        },
        pitchSemitones = pitch,
        onChangeLibrary = onOpenSetup,
        onRescan = vm::startScan,
        onToggleAutoNext = {
            autoNext = !autoNext
            vm.prefs.autoNext = autoNext
        },
        onToggleNextUpBanner = {
            nextUpBanner = !nextUpBanner
            vm.prefs.showNextUpBanner = nextUpBanner
        },
        onToggleYouTubeKeyword = {
            youTubeKeyword = !youTubeKeyword
            vm.prefs.appendKaraokeToYouTube = youTubeKeyword
        },
        onCycleScale = vm.player::cycleScaleMode,
        onResetPitch = { vm.player.setPitch(0) },
        onClearLibrary = onOpenSetup,
        onUpdateAction = {
            when (updateState) {
                is UpdateState.Available -> vm.downloadUpdate(context)
                is UpdateState.Ready -> vm.installUpdate(context)
                is UpdateState.Downloading, is UpdateState.Checking -> Unit
                else -> vm.checkForUpdate()
            }
        },
    )
}

@Composable
private fun SongListRoute(
    vm: KaraokeViewModel,
    screen: Screen.SongList,
    onSongSelected: (SongEntity) -> Unit,
    onSongOptions: (SongEntity) -> Unit,
) {
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val mostPlayed by vm.mostPlayed.collectAsStateWithLifecycle()
    var loaded by remember(screen) { mutableStateOf<List<SongEntity>>(emptyList()) }
    var exhausted by remember(screen) { mutableStateOf(false) }
    var loading by remember(screen) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(screen) {
        when (val source = screen.source) {
            is SongListSource.Category -> {
                loaded = vm.songsInCategory(source.name)
                exhausted = true
            }
            is SongListSource.Artist -> {
                loaded = vm.songsByArtist(source.name)
                exhausted = true
            }
            SongListSource.All -> {
                loaded = vm.allSongs(PAGE_SIZE, 0)
                exhausted = loaded.size < PAGE_SIZE
            }
            SongListSource.Favorites, SongListSource.MostPlayed -> exhausted = true
        }
    }

    val songs = when (screen.source) {
        SongListSource.Favorites -> favorites
        SongListSource.MostPlayed -> mostPlayed
        else -> loaded
    }

    // Paged so a drive with twenty thousand files does not build one giant list.
    val loadNextPage: () -> Unit = {
        if (!loading) {
            loading = true
            scope.launch {
                val more = vm.allSongs(PAGE_SIZE, loaded.size)
                if (more.isEmpty()) exhausted = true else loaded = loaded + more
                loading = false
            }
        }
    }
    val pages = screen.source == SongListSource.All && !exhausted

    SongListScreen(
        title = screen.title,
        songs = songs,
        onSongClick = onSongSelected,
        onSongOptions = onSongOptions,
        onReachEnd = if (pages) loadNextPage else null,
    )
}

@Composable
private fun KeyHints(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0x99000000))
            .padding(horizontal = 48.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        Text(
            text = "BACK: quay lại · Giữ OK trên bài hát: thêm lựa chọn · " +
                "BACK ở màn hình chính: ẩn menu để xem video",
            style = MaterialTheme.typography.labelMedium,
            color = KaraokeColors.Muted,
        )
    }
}

private const val PAGE_SIZE = 300
