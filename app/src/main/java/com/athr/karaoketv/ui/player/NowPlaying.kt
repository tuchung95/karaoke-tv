package com.athr.karaoketv.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.athr.karaoketv.player.ChannelMixProcessor
import com.athr.karaoketv.player.PlaybackPosition
import com.athr.karaoketv.player.QueueItem
import com.athr.karaoketv.ui.components.Pill
import com.athr.karaoketv.ui.components.TvButton
import com.athr.karaoketv.ui.theme.KaraokeColors
import com.athr.karaoketv.util.formatDuration

/**
 * The heads-up display over the video: what is playing, how far in, and what is
 * next. It carries no focusable elements so it can sit on screen while people
 * sing without stealing D-pad input.
 */
@Composable
fun NowPlayingHud(
    current: QueueItem?,
    nextUp: QueueItem?,
    position: PlaybackPosition,
    queueSize: Int,
    pitchSemitones: Int,
    vocalMode: ChannelMixProcessor.Mode,
    visible: Boolean,
    showNextUpBanner: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible && current != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopStart),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(Color(0xCC000000), Color.Transparent))
                    )
                    .padding(horizontal = 58.dp, vertical = 28.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (current?.song?.songNumber != null) {
                                Pill(current.song.songNumber!!)
                                Spacer(Modifier.width(8.dp))
                            }
                            if (current?.song?.tone != null) {
                                Pill(current.song.tone!!, color = KaraokeColors.Primary)
                                Spacer(Modifier.width(8.dp))
                            }
                            if (pitchSemitones != 0) {
                                Pill(
                                    "Tông ${if (pitchSemitones > 0) "+" else ""}$pitchSemitones",
                                    color = KaraokeColors.Success,
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            if (vocalMode != ChannelMixProcessor.Mode.STEREO) {
                                Pill(vocalMode.label(), color = KaraokeColors.Accent)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = current?.song?.title.orEmpty(),
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val subtitle = current?.song?.artist
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = KaraokeColors.Muted,
                                maxLines = 1,
                            )
                        }
                    }
                    if (queueSize > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = null,
                                tint = KaraokeColors.Accent,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "$queueSize bài chờ",
                                style = MaterialTheme.typography.titleMedium,
                                color = KaraokeColors.Accent,
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = visible && current != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)))
                    )
                    .padding(horizontal = 58.dp, vertical = 28.dp)
            ) {
                ProgressBar(position)
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = formatDuration(position.positionMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = KaraokeColors.Muted,
                    )
                    Text(
                        text = "-${formatDuration(position.remainingMs)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = KaraokeColors.Muted,
                    )
                }
            }
        }

        // Warn the room a few seconds early so the next singer can get ready.
        val nearingEnd = position.durationMs > 0L && position.remainingMs in 1..NEXT_UP_LEAD_MS
        AnimatedVisibility(
            visible = showNextUpBanner && nearingEnd && nextUp != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 58.dp, bottom = 96.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(KaraokeColors.Primary, RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Icon(
                    Icons.Filled.SkipNext,
                    contentDescription = null,
                    tint = KaraokeColors.OnPrimary,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Bài tiếp theo: ${nextUp?.song?.title.orEmpty()}",
                    style = MaterialTheme.typography.titleMedium,
                    color = KaraokeColors.OnPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ProgressBar(position: PlaybackPosition) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color.White.copy(alpha = 0.18f))
    ) {
        Box(
            Modifier
                .fillMaxWidth(position.fraction)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(KaraokeColors.Primary)
        )
    }
}

/** The transport strip that drops in over the video when someone presses OK. */
@Composable
fun ControlBar(
    isPlaying: Boolean,
    pitchSemitones: Int,
    vocalMode: ChannelMixProcessor.Mode,
    scaleMode: Int,
    hasAudioTrackChoice: Boolean,
    onPlayPause: () -> Unit,
    onRestart: () -> Unit,
    onNext: () -> Unit,
    onPitchDown: () -> Unit,
    onPitchUp: () -> Unit,
    onCycleVocal: () -> Unit,
    onCycleAudioTrack: () -> Unit,
    onCycleScale: () -> Unit,
    onOpenQueue: () -> Unit,
    firstFocus: FocusRequester,
    modifier: Modifier = Modifier,
) {
    // Scrolls as a plain Row for the same reason as the home shelf: a LazyRow's
    // items compose too late for the focus request below to attach.
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE6000000))))
            .horizontalScroll(rememberScrollState())
            .focusGroup()
            .padding(horizontal = 58.dp, vertical = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TvButton(
            text = if (isPlaying) "Tạm dừng" else "Phát",
            icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            onClick = onPlayPause,
            emphasised = true,
            focusRequester = firstFocus,
        )
        TvButton("Hát lại", onRestart, icon = Icons.Filled.Replay)
        TvButton("Bài kế", onNext, icon = Icons.Filled.SkipNext)
        TvButton("Tông −", onPitchDown)
        Text(
            text = if (pitchSemitones == 0) "Tông gốc"
            else "Tông ${if (pitchSemitones > 0) "+" else ""}$pitchSemitones",
            style = MaterialTheme.typography.titleMedium,
            color = if (pitchSemitones == 0) KaraokeColors.Muted else KaraokeColors.Success,
        )
        TvButton("Tông +", onPitchUp)
        TvButton(vocalMode.label(), onCycleVocal, icon = Icons.Filled.GraphicEq)
        if (hasAudioTrackChoice) TvButton("Kênh tiếng", onCycleAudioTrack)
        TvButton(scaleLabel(scaleMode), onCycleScale)
        TvButton("Hàng chờ", onOpenQueue, icon = Icons.AutoMirrored.Filled.QueueMusic)
    }
}

fun ChannelMixProcessor.Mode.label(): String = when (this) {
    ChannelMixProcessor.Mode.STEREO -> "Âm thanh gốc"
    ChannelMixProcessor.Mode.LEFT_ONLY -> "Chỉ kênh trái"
    ChannelMixProcessor.Mode.RIGHT_ONLY -> "Chỉ kênh phải"
    ChannelMixProcessor.Mode.MONO -> "Trộn mono"
}

private fun scaleLabel(mode: Int): String = when (mode) {
    1 -> "Hình: phóng to"
    2 -> "Hình: kéo đầy"
    else -> "Hình: vừa khung"
}

private const val NEXT_UP_LEAD_MS = 25_000L
