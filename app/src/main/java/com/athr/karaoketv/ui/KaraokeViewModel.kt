package com.athr.karaoketv.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.athr.karaoketv.KaraokeApp
import com.athr.karaoketv.data.db.SongEntity
import com.athr.karaoketv.data.library.LibrarySource
import com.athr.karaoketv.data.library.ScanProgress
import com.athr.karaoketv.data.youtube.YouTubeLauncher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ScanState {
    data object Idle : ScanState
    data class Running(val filesFound: Int, val folder: String) : ScanState
    data class Done(val totalSongs: Int, val removed: Int, val elapsedMs: Long) : ScanState
    data class Failed(val message: String) : ScanState
}

@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class KaraokeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as KaraokeApp
    val repo = app.repository
    val player = app.playerController
    val prefs = app.prefs

    val songCount = repo.songCount.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    val recentlyPlayed = repo.recentlyPlayed.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val favorites = repo.favorites.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val mostPlayed = repo.mostPlayed.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val recentlyAdded = repo.recentlyAdded.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val categories = repo.categories.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val artists = repo.artists.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _query = MutableStateFlow("")
    val query: kotlinx.coroutines.flow.StateFlow<String> = _query.asStateFlow()

    /**
     * Results trail the keystrokes by a beat. On a remote each character costs
     * several D-pad presses, so a short debounce keeps the list from thrashing
     * while someone walks across the on-screen keyboard.
     */
    val searchResults = _query
        .debounce(180L)
        .distinctUntilChanged()
        .mapLatest { text -> if (text.isBlank()) emptyList() else repo.search(text) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: kotlinx.coroutines.flow.StateFlow<ScanState> = _scanState.asStateFlow()

    private var scanJob: Job? = null

    val libraryConfigured: Boolean get() = prefs.libraryConfigured
    val libraryLabel: String get() = prefs.libraryLabel

    fun setQuery(text: String) {
        _query.value = text
    }

    fun appendToQuery(ch: String) {
        _query.value = _query.value + ch
    }

    fun backspaceQuery() {
        _query.value = _query.value.dropLast(1)
    }

    fun clearQuery() {
        _query.value = ""
    }

    // ---- library setup -----------------------------------------------------

    fun useDocumentTree(uri: Uri, label: String) {
        repo.useDocumentTree(uri, label)
        startScan()
    }

    fun useDirectPath(path: String, label: String) {
        repo.useDirectPath(path, label)
        startScan()
    }

    fun startScan() {
        val source: LibrarySource = repo.currentSource() ?: run {
            _scanState.value = ScanState.Failed("Chưa chọn thư mục karaoke")
            return
        }
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _scanState.value = ScanState.Running(0, "…")
            repo.scan(source).collect { progress ->
                _scanState.value = when (progress) {
                    is ScanProgress.Working ->
                        ScanState.Running(progress.filesFound, progress.currentFolder)
                    is ScanProgress.Finished -> {
                        repo.markScanned()
                        ScanState.Done(progress.totalSongs, progress.removed, progress.elapsedMs)
                    }
                    is ScanProgress.Failed -> ScanState.Failed(progress.message)
                }
            }
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
        _scanState.value = ScanState.Idle
    }

    fun dismissScanResult() {
        _scanState.value = ScanState.Idle
    }

    // ---- song actions ------------------------------------------------------

    fun toggleFavorite(song: SongEntity) {
        viewModelScope.launch { repo.setFavorite(song.id, !song.favorite) }
    }

    /**
     * Hands the typed query to the YouTube app. Our own playback is paused first,
     * or two videos end up singing over each other on the same TV.
     */
    fun searchOnYouTube(context: android.content.Context, query: String): Boolean {
        if (query.isBlank()) return false
        player.player.pause()
        return YouTubeLauncher.openSearch(context, query, prefs.appendKaraokeToYouTube)
    }

    fun shuffleIntoQueue(count: Int = 10) {
        viewModelScope.launch {
            repo.random(count).forEach { player.enqueue(it) }
        }
    }

    suspend fun songsInCategory(name: String) = repo.songsInCategory(name)
    suspend fun songsByArtist(name: String) = repo.songsByArtist(name)
    suspend fun allSongs(limit: Int, offset: Int) = repo.allSongs(limit, offset)
}
