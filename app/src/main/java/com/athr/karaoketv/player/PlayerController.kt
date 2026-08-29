package com.athr.karaoketv.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import com.athr.karaoketv.data.db.SongEntity
import com.athr.karaoketv.data.prefs.AppPrefs
import com.athr.karaoketv.data.repo.LibraryRepository
import kotlin.math.pow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AudioTrackOption(
    val groupIndex: Int,
    val label: String,
    val selected: Boolean,
)

data class PlaybackPosition(
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
) {
    val remainingMs: Long get() = (durationMs - positionMs).coerceAtLeast(0L)
    val fraction: Float
        get() = if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
}

/**
 * Owns the single ExoPlayer instance and the sing-along queue.
 *
 * The queue is deliberately *not* the ExoPlayer playlist: a karaoke room needs to
 * reorder, bump and drop entries constantly, and driving one media item at a time
 * keeps that logic in one place and makes "hát lại" / "bài kế" behave predictably.
 */
@OptIn(UnstableApi::class)
class PlayerController(
    context: Context,
    private val repo: LibraryRepository,
    private val prefs: AppPrefs,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val appContext = context.applicationContext

    val channelMix = ChannelMixProcessor()

    val player: ExoPlayer = ExoPlayer.Builder(
        appContext,
        object : DefaultRenderersFactory(appContext) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean,
            ): AudioSink = DefaultAudioSink.Builder(context)
                .setAudioProcessors(arrayOf(channelMix))
                .setEnableFloatOutput(enableFloatOutput)
                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .build()
        }.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF),
    ).build()

    private val _queue = MutableStateFlow<List<QueueItem>>(emptyList())
    val queue: StateFlow<List<QueueItem>> = _queue.asStateFlow()

    private val _current = MutableStateFlow<QueueItem?>(null)
    val current: StateFlow<QueueItem?> = _current.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _position = MutableStateFlow(PlaybackPosition())
    val position: StateFlow<PlaybackPosition> = _position.asStateFlow()

    private val _pitchSemitones = MutableStateFlow(prefs.pitchSemitones)
    val pitchSemitones: StateFlow<Int> = _pitchSemitones.asStateFlow()

    private val _vocalMode = MutableStateFlow(ChannelMixProcessor.Mode.STEREO)
    val vocalMode: StateFlow<ChannelMixProcessor.Mode> = _vocalMode.asStateFlow()

    private val _audioTracks = MutableStateFlow<List<AudioTrackOption>>(emptyList())
    val audioTracks: StateFlow<List<AudioTrackOption>> = _audioTracks.asStateFlow()

    private val _scaleMode = MutableStateFlow(prefs.videoScaleMode)
    val scaleMode: StateFlow<Int> = _scaleMode.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages = _messages.asSharedFlow()

    private var uidSeed = 1L
    private var ticker: Job? = null
    private var latestTracks: Tracks = Tracks.EMPTY

    init {
        applyPitch(_pitchSemitones.value)
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) onSongFinished()
                if (state == Player.STATE_READY) {
                    val id = _current.value?.song?.id
                    val duration = player.duration
                    if (id != null && duration > 0L) {
                        scope.launch { repo.rememberDuration(id, duration) }
                    }
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
                if (playing) startTicker() else stopTicker()
            }

            override fun onTracksChanged(tracks: Tracks) {
                latestTracks = tracks
                _audioTracks.value = tracks.groups
                    .filter { it.type == C.TRACK_TYPE_AUDIO }
                    .mapIndexed { index, group -> group.toOption(index) }
            }

            override fun onPlayerError(error: PlaybackException) {
                val title = _current.value?.song?.title ?: "bài hát"
                _messages.tryEmit("Không phát được \"$title\" — chuyển bài kế.")
                next()
            }
        })
    }

    // ---- queue -------------------------------------------------------------

    /** Adds to the end of the queue, or starts playing if the room is idle. */
    fun enqueue(song: SongEntity) {
        val item = QueueItem(uidSeed++, song)
        if (_current.value == null) {
            startItem(item)
        } else {
            _queue.value = _queue.value + item
            _messages.tryEmit("Đã thêm \"${song.title}\" vào hàng chờ")
        }
    }

    /** Jumps a song to the front of the queue without cutting off the current one. */
    fun enqueueNext(song: SongEntity) {
        val item = QueueItem(uidSeed++, song)
        if (_current.value == null) {
            startItem(item)
        } else {
            _queue.value = listOf(item) + _queue.value
            _messages.tryEmit("\"${song.title}\" sẽ hát tiếp theo")
        }
    }

    /**
     * Cuts to a song immediately. The interrupted song goes back to the head of
     * the queue rather than being lost — whoever picked it still gets their turn.
     */
    fun playNow(song: SongEntity) {
        val interrupted = _current.value
        val item = QueueItem(uidSeed++, song)
        if (interrupted != null) {
            _queue.value = listOf(interrupted) + _queue.value
        }
        startItem(item)
    }

    fun removeAt(uid: Long) {
        _queue.value = _queue.value.filterNot { it.uid == uid }
    }

    fun moveUp(uid: Long) = move(uid, -1)

    fun moveDown(uid: Long) = move(uid, +1)

    private fun move(uid: Long, delta: Int) {
        val list = _queue.value.toMutableList()
        val from = list.indexOfFirst { it.uid == uid }
        if (from < 0) return
        val to = (from + delta).coerceIn(0, list.lastIndex)
        if (to == from) return
        list.add(to, list.removeAt(from))
        _queue.value = list
    }

    /** Bumps an already-queued entry to the front — the "ưu tiên" button. */
    fun prioritise(uid: Long) {
        val list = _queue.value
        val item = list.firstOrNull { it.uid == uid } ?: return
        _queue.value = listOf(item) + list.filterNot { it.uid == uid }
    }

    fun clearQueue() {
        _queue.value = emptyList()
    }

    fun nextUp(): QueueItem? = _queue.value.firstOrNull()

    // ---- transport ---------------------------------------------------------

    fun next() {
        val head = _queue.value.firstOrNull()
        if (head == null) {
            stopPlayback()
            return
        }
        _queue.value = _queue.value.drop(1)
        startItem(head)
    }

    fun restart() {
        player.seekTo(0L)
        player.play()
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekBy(deltaMs: Long) {
        val target = (player.currentPosition + deltaMs)
            .coerceIn(0L, player.duration.coerceAtLeast(0L))
        player.seekTo(target)
    }

    fun stopPlayback() {
        player.stop()
        player.clearMediaItems()
        _current.value = null
        _position.value = PlaybackPosition()
        _audioTracks.value = emptyList()
        stopTicker()
    }

    private fun startItem(item: QueueItem) {
        _current.value = item
        _position.value = PlaybackPosition(0L, item.song.durationMs)
        player.setMediaItem(MediaItem.fromUri(Uri.parse(item.song.uri)))
        player.prepare()
        player.play()
        scope.launch { repo.markPlayed(item.song.id) }
    }

    private fun onSongFinished() {
        if (prefs.autoNext) {
            next()
        } else {
            player.pause()
        }
    }

    // ---- sound and picture -------------------------------------------------

    fun setPitch(semitones: Int) {
        val clamped = semitones.coerceIn(-6, 6)
        _pitchSemitones.value = clamped
        prefs.pitchSemitones = clamped
        applyPitch(clamped)
    }

    fun nudgePitch(delta: Int) = setPitch(_pitchSemitones.value + delta)

    private fun applyPitch(semitones: Int) {
        val ratio = 2.0.pow(semitones / 12.0).toFloat()
        player.playbackParameters = PlaybackParameters(1f, ratio)
    }

    fun setVocalMode(mode: ChannelMixProcessor.Mode) {
        _vocalMode.value = mode
        channelMix.mode = mode
    }

    fun cycleVocalMode() {
        val order = ChannelMixProcessor.Mode.entries
        val nextMode = order[(order.indexOf(_vocalMode.value) + 1) % order.size]
        setVocalMode(nextMode)
    }

    fun selectAudioTrack(groupIndex: Int) {
        val group = latestTracks.groups
            .filter { it.type == C.TRACK_TYPE_AUDIO }
            .getOrNull(groupIndex) ?: return
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, 0))
            .build()
        _audioTracks.value = _audioTracks.value.mapIndexed { i, option ->
            option.copy(selected = i == groupIndex)
        }
    }

    fun cycleScaleMode() {
        val next = (_scaleMode.value + 1) % 3
        _scaleMode.value = next
        prefs.videoScaleMode = next
    }

    // ---- position ticker ---------------------------------------------------

    private fun startTicker() {
        if (ticker?.isActive == true) return
        ticker = scope.launch {
            while (true) {
                _position.value = PlaybackPosition(
                    positionMs = player.currentPosition.coerceAtLeast(0L),
                    durationMs = player.duration.coerceAtLeast(0L),
                )
                delay(400L)
            }
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    fun release() {
        stopTicker()
        player.release()
    }

    private fun Tracks.Group.toOption(index: Int): AudioTrackOption {
        val format = getTrackFormat(0)
        val language = format.language?.takeIf { it.isNotBlank() && it != "und" }
        val channels = format.channelCount.takeIf { it > 0 }
        val details = listOfNotNull(
            language?.uppercase(),
            channels?.let { if (it >= 2) "Stereo" else "Mono" },
        ).joinToString(" · ")
        val label = buildString {
            append("Kênh tiếng ${index + 1}")
            if (details.isNotEmpty()) append(" ($details)")
        }
        return AudioTrackOption(
            groupIndex = index,
            label = label,
            selected = isSelected,
        )
    }
}
