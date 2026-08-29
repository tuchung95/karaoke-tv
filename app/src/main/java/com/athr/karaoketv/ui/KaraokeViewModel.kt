package com.athr.karaoketv.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.athr.karaoketv.BuildConfig
import com.athr.karaoketv.KaraokeApp
import com.athr.karaoketv.data.db.SongEntity
import com.athr.karaoketv.data.library.LibrarySource
import com.athr.karaoketv.data.library.ScanProgress
import com.athr.karaoketv.data.prefs.HomeShelf
import com.athr.karaoketv.data.update.ApkInstaller
import com.athr.karaoketv.data.update.UpdateChecker
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

/** Where the sideloaded app is in the check -> download -> install cycle. */
sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val release: UpdateChecker.Release) : UpdateState
    data class Downloading(val percent: Int) : UpdateState
    data class Ready(
        val apk: java.io.File,
        val versionName: String,
        /** Set when the APK landed somewhere a file manager can reach it. */
        val userVisiblePath: String?,
    ) : UpdateState
    /** Downloaded, but this box has no installer UI to hand it to. */
    data class InstallManually(val path: String) : UpdateState
    data class Failed(val message: String) : UpdateState
}

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
    private var updateJob: Job? = null

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: kotlinx.coroutines.flow.StateFlow<UpdateState> = _updateState.asStateFlow()

    val currentVersion: String = BuildConfig.VERSION_NAME

    private val _uiSounds = MutableStateFlow(prefs.uiSounds)
    val uiSounds: kotlinx.coroutines.flow.StateFlow<Boolean> = _uiSounds.asStateFlow()

    fun toggleUiSounds() {
        prefs.uiSounds = !prefs.uiSounds
        _uiSounds.value = prefs.uiSounds
    }

    data class HomeLayout(
        val order: List<HomeShelf>,
        val hidden: Set<HomeShelf>,
    )

    private val _homeLayout = MutableStateFlow(
        HomeLayout(prefs.homeShelfOrder, prefs.hiddenShelves)
    )
    val homeLayout: kotlinx.coroutines.flow.StateFlow<HomeLayout> = _homeLayout.asStateFlow()

    private fun refreshHomeLayout() {
        _homeLayout.value = HomeLayout(prefs.homeShelfOrder, prefs.hiddenShelves)
    }

    fun toggleShelf(shelf: HomeShelf) {
        prefs.setShelfVisible(shelf, shelf in prefs.hiddenShelves)
        refreshHomeLayout()
    }

    fun moveShelf(shelf: HomeShelf, delta: Int) {
        prefs.moveShelf(shelf, delta)
        refreshHomeLayout()
    }

    fun resetHomeLayout() {
        prefs.resetHomeLayout()
        refreshHomeLayout()
    }

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

    fun useMediaLibrary(label: String = "Video trên máy") {
        repo.useMediaLibrary(label)
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

    fun swapTitleAndArtist(song: SongEntity) {
        viewModelScope.launch { repo.swapTitleAndArtist(song) }
    }

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

    // ---- updates ------------------------------------------------------------

    private var launchCheckDone = false

    /** One quiet check per launch, so an out-of-date box does not stay that way. */
    fun checkForUpdateOnLaunch() {
        if (launchCheckDone) return
        launchCheckDone = true
        checkForUpdate()
    }

    fun checkForUpdate() {
        updateJob?.cancel()
        updateJob = viewModelScope.launch {
            _updateState.value = UpdateState.Checking
            _updateState.value = try {
                val release = UpdateChecker.fetchLatest()
                when {
                    release == null -> UpdateState.Failed("Chưa có bản phát hành nào")
                    UpdateChecker.isNewer(release.versionName, currentVersion) ->
                        UpdateState.Available(release)
                    else -> UpdateState.UpToDate
                }
            } catch (e: Exception) {
                UpdateState.Failed("Không kiểm tra được: cần mạng")
            }
        }
    }

    fun downloadUpdate(context: android.content.Context) {
        val available = _updateState.value as? UpdateState.Available ?: return
        updateJob?.cancel()
        updateJob = viewModelScope.launch {
            _updateState.value = UpdateState.Downloading(0)
            _updateState.value = try {
                val result = ApkInstaller.download(
                    context = context,
                    url = available.release.apkUrl,
                    versionName = available.release.versionName,
                ) { percent ->
                    _updateState.value = UpdateState.Downloading(percent)
                }
                UpdateState.Ready(
                    apk = result.file,
                    versionName = available.release.versionName,
                    userVisiblePath = result.userVisiblePath,
                )
            } catch (e: Exception) {
                UpdateState.Failed("Tải bản cập nhật thất bại")
            }
        }
    }

    /**
     * Hands the APK to the system installer, first sending the viewer to grant
     * install permission if the box has not trusted this app yet.
     */
    fun installUpdate(context: android.content.Context) {
        val ready = _updateState.value as? UpdateState.Ready ?: return
        if (!ApkInstaller.canInstall(context)) {
            val intent = ApkInstaller.installPermissionIntent(context)
            if (intent == null) {
                _updateState.value =
                    UpdateState.Failed("Máy không có màn hình cấp quyền cài đặt")
            } else {
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            return
        }
        if (ApkInstaller.install(context, ready.apk)) return

        // No installer activity on this box. The APK is still on disk, so say
        // where it is rather than leaving the viewer pressing a dead button.
        _updateState.value = ready.userVisiblePath
            ?.let { UpdateState.InstallManually(it) }
            ?: UpdateState.Failed("Máy không có trình cài đặt APK")
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
