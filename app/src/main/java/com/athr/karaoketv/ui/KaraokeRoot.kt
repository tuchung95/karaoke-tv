package com.athr.karaoketv.ui

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.athr.karaoketv.data.db.SongEntity
import com.athr.karaoketv.ui.components.SongActionSheet
import com.athr.karaoketv.ui.player.ControlBar
import com.athr.karaoketv.ui.player.IdleStage
import com.athr.karaoketv.ui.player.NowPlayingHud
import com.athr.karaoketv.ui.player.VideoStage
import com.athr.karaoketv.ui.setup.SetupScreen
import com.athr.karaoketv.ui.theme.KaraokeColors
import kotlinx.coroutines.delay

/**
 * The whole app in one place. The video is the bottom layer and never stops for
 * navigation; the browser floats over it with a scrim, and BACK peels the layers
 * off one at a time until the picture is clear again.
 */
@Composable
fun KaraokeRoot(vm: KaraokeViewModel, onExit: () -> Unit) {
    val songCount by vm.songCount.collectAsStateWithLifecycle()
    val scanState by vm.scanState.collectAsStateWithLifecycle()

    var setupVisible by remember { mutableStateOf(!vm.libraryConfigured) }
    val leaveSetup: () -> Unit = {
        vm.dismissScanResult()
        setupVisible = false
    }

    if (setupVisible) {
        SetupScreen(
            scanState = scanState,
            currentLabel = vm.libraryLabel,
            songCount = songCount,
            onTreePicked = { uri, label -> vm.useDocumentTree(uri, label) },
            onDirectPicked = { path, label -> vm.useDirectPath(path, label) },
            onRescan = vm::startScan,
            onDone = if (vm.libraryConfigured) leaveSetup else null,
        )
        BackHandler(enabled = vm.libraryConfigured, onBack = leaveSetup)
        return
    }

    val player = vm.player
    val current by player.current.collectAsStateWithLifecycle()
    val queue by player.queue.collectAsStateWithLifecycle()
    val isPlaying by player.isPlaying.collectAsStateWithLifecycle()
    val position by player.position.collectAsStateWithLifecycle()
    val pitch by player.pitchSemitones.collectAsStateWithLifecycle()
    val vocalMode by player.vocalMode.collectAsStateWithLifecycle()
    val scaleMode by player.scaleMode.collectAsStateWithLifecycle()
    val audioTracks by player.audioTracks.collectAsStateWithLifecycle()

    val backStack = remember { mutableStateListOf<Screen>(Screen.Home) }
    var browserVisible by remember { mutableStateOf(true) }
    var controlsVisible by remember { mutableStateOf(false) }
    var hudVisible by remember { mutableStateOf(true) }
    var actionSheetSong by remember { mutableStateOf<SongEntity?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }
    var exitArmed by remember { mutableStateOf(false) }

    val stageFocus = remember { FocusRequester() }
    val controlFocus = remember { FocusRequester() }

    fun push(screen: Screen) {
        backStack.add(screen)
        browserVisible = true
    }

    LaunchedEffect(Unit) {
        player.messages.collect { message ->
            toast = message
        }
    }
    LaunchedEffect(toast) {
        if (toast != null) {
            delay(2500)
            toast = null
        }
    }

    // The HUD announces a new song then gets out of the way.
    LaunchedEffect(current?.uid) {
        if (current != null) {
            hudVisible = true
            delay(7000)
            hudVisible = false
        }
    }

    // With the browser closed and no controls up, the stage owns the remote.
    // Retried, because a dropped focus request leaves the remote doing nothing.
    LaunchedEffect(browserVisible, controlsVisible) {
        val target = when {
            controlsVisible -> controlFocus
            !browserVisible -> stageFocus
            else -> null
        } ?: return@LaunchedEffect
        repeat(15) {
            if (runCatching { target.requestFocus() }.isSuccess) return@LaunchedEffect
            delay(40)
        }
    }

    // Controls fade out on their own so nothing covers the lyrics mid-song.
    LaunchedEffect(controlsVisible, isPlaying, pitch, vocalMode, scaleMode) {
        if (controlsVisible) {
            delay(9000)
            controlsVisible = false
        }
    }

    LaunchedEffect(exitArmed) {
        if (exitArmed) {
            delay(3000)
            exitArmed = false
        }
    }

    BackHandler {
        when {
            controlsVisible -> controlsVisible = false
            browserVisible && backStack.size > 1 -> backStack.removeAt(backStack.lastIndex)
            browserVisible && current != null -> browserVisible = false
            !browserVisible -> browserVisible = true
            // A shared remote gets pressed by everyone; one stray BACK should not
            // end the night. Confirm before leaving.
            exitArmed -> onExit()
            else -> {
                exitArmed = true
                toast = "Nhấn BACK lần nữa để thoát Karaoke"
            }
        }
    }

    Box(Modifier.fillMaxSize().background(KaraokeColors.Background)) {
        if (current != null) {
            VideoStage(player = player.player, scaleMode = scaleMode)
        } else {
            IdleStage(libraryLabel = vm.libraryLabel, songCount = songCount)
        }

        // Invisible key sink for the "just watching the video" state. It only
        // exists while the browser is hidden so D-pad focus can never fall into it.
        if (!browserVisible) {
            Box(
                Modifier
                    .fillMaxSize()
                    .focusRequester(stageFocus)
                    .focusable()
                    .onPreviewKeyEvent { event ->
                        if (event.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) {
                            return@onPreviewKeyEvent false
                        }
                        when (event.nativeKeyEvent.keyCode) {
                            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER,
                            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_MENU,
                            -> {
                                controlsVisible = true
                                hudVisible = true
                                true
                            }
                            KeyEvent.KEYCODE_DPAD_UP -> {
                                browserVisible = true
                                true
                            }
                            KeyEvent.KEYCODE_MEDIA_NEXT -> { player.next(); true }
                            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                                player.togglePlayPause()
                                true
                            }
                            // Typing a song number on the remote jumps straight to search.
                            in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> {
                                vm.setQuery(
                                    (event.nativeKeyEvent.keyCode - KeyEvent.KEYCODE_0).toString()
                                )
                                push(Screen.Search)
                                true
                            }
                            else -> false
                        }
                    }
            )
        }

        NowPlayingHud(
            current = current,
            nextUp = queue.firstOrNull(),
            position = position,
            queueSize = queue.size,
            pitchSemitones = pitch,
            vocalMode = vocalMode,
            visible = !browserVisible && (hudVisible || controlsVisible),
            showNextUpBanner = vm.prefs.showNextUpBanner && !browserVisible,
        )

        AnimatedVisibility(
            visible = !browserVisible && controlsVisible,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            ControlBar(
                isPlaying = isPlaying,
                pitchSemitones = pitch,
                vocalMode = vocalMode,
                scaleMode = scaleMode,
                hasAudioTrackChoice = audioTracks.size > 1,
                onPlayPause = player::togglePlayPause,
                onRestart = player::restart,
                onNext = player::next,
                onPitchDown = { player.nudgePitch(-1) },
                onPitchUp = { player.nudgePitch(+1) },
                onCycleVocal = player::cycleVocalMode,
                onCycleAudioTrack = {
                    val currentIndex = audioTracks.indexOfFirst { it.selected }.coerceAtLeast(0)
                    player.selectAudioTrack((currentIndex + 1) % audioTracks.size)
                },
                onCycleScale = player::cycleScaleMode,
                onOpenQueue = { push(Screen.Queue) },
                firstFocus = controlFocus,
            )
        }

        AnimatedVisibility(
            visible = browserVisible,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(Modifier.fillMaxSize().background(KaraokeColors.Scrim)) {
                BrowserContent(
                    vm = vm,
                    screen = backStack.last(),
                    onNavigate = ::push,
                    onSongSelected = { song -> player.enqueue(song) },
                    onSongOptions = { song -> actionSheetSong = song },
                    onOpenSetup = { setupVisible = true },
                    onBack = {
                        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                    },
                    onHome = {
                        if (backStack.size > 1) backStack.removeRange(1, backStack.size)
                    },
                    onWatchVideo = if (current != null) {
                        { browserVisible = false }
                    } else {
                        null
                    },
                )
            }
        }

        AnimatedVisibility(
            visible = toast != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
        ) {
            Text(
                text = toast.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                color = KaraokeColors.OnSurface,
                modifier = Modifier
                    .background(KaraokeColors.SurfaceHigh, RoundedCornerShape(12.dp))
                    .padding(horizontal = 24.dp, vertical = 14.dp),
            )
        }
    }

    val sheetSong = actionSheetSong
    if (sheetSong != null) {
        SongActionSheet(
            song = sheetSong,
            onDismiss = { actionSheetSong = null },
            onPlayNow = { player.playNow(sheetSong) },
            onQueue = { player.enqueue(sheetSong) },
            onQueueNext = { player.enqueueNext(sheetSong) },
            onToggleFavorite = { vm.toggleFavorite(sheetSong) },
            onSwapTitleArtist = { vm.swapTitleAndArtist(sheetSong) },
        )
    }
}
