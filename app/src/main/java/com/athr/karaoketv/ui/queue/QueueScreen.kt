package com.athr.karaoketv.ui.queue

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.athr.karaoketv.player.QueueItem
import com.athr.karaoketv.ui.components.Pill
import com.athr.karaoketv.ui.components.SongCard
import com.athr.karaoketv.ui.components.SongThumbnail
import com.athr.karaoketv.ui.components.TvButton
import com.athr.karaoketv.ui.theme.KaraokeColors
import com.athr.karaoketv.ui.theme.TvSpacing

/**
 * The queue as an immersive list: whatever the remote is resting on fills the
 * screen behind, and the entries run along the bottom.
 *
 * This is the pattern Google documents for "move through a row, see the selection
 * in full" — and the right fit here, where the previous two attempts were not. A
 * button per action per row made a wall of controls; a navigation drawer is meant
 * for an app's three-to-seven destinations, not an unbounded queue.
 */
@Composable
fun QueueScreen(
    current: QueueItem?,
    queue: List<QueueItem>,
    onPlayNow: (QueueItem) -> Unit,
    onPrioritise: (QueueItem) -> Unit,
    onMoveUp: (QueueItem) -> Unit,
    onMoveDown: (QueueItem) -> Unit,
    onRemove: (QueueItem) -> Unit,
    onClearAll: () -> Unit,
    onSkipCurrent: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focusedUid by remember { mutableStateOf<Long?>(null) }
    val focused = queue.firstOrNull { it.uid == focusedUid } ?: queue.firstOrNull()
    val focusedIndex = queue.indexOfFirst { it.uid == focused?.uid }

    Box(modifier = modifier.fillMaxSize()) {
        // Backdrop: the entry under the remote, swapped without a jump cut.
        AnimatedContent(
            targetState = focused?.song?.uri,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "queue-backdrop",
        ) { uri ->
            if (uri != null) {
                SongThumbnail(
                    uri = uri,
                    iconSize = 96,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 420.dp),
                )
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to KaraokeColors.Background,
                        0.4f to KaraokeColors.Background,
                        0.85f to KaraokeColors.Background.copy(alpha = 0.65f),
                        1f to Color.Transparent,
                    )
                )
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.35f to Color.Transparent,
                        1f to KaraokeColors.Background,
                    )
                )
        )

        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .weight(1f)
                    .padding(
                        start = TvSpacing.ScreenHorizontal,
                        end = TvSpacing.ScreenHorizontal,
                        top = 4.dp,
                    ),
                verticalArrangement = Arrangement.Center,
            ) {
                if (current != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Pill("ĐANG HÁT", color = KaraokeColors.Success)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = current.song.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = KaraokeColors.Muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.width(560.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        TvButton("Bỏ qua", onSkipCurrent, icon = Icons.Filled.SkipNext)
                    }
                    Spacer(Modifier.height(20.dp))
                }

                if (focused == null) {
                    Text(
                        text = "Hàng chờ trống",
                        style = MaterialTheme.typography.displaySmall,
                        color = KaraokeColors.OnSurface,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Chọn một bài rồi bấm OK để thêm vào đây.",
                        style = MaterialTheme.typography.titleMedium,
                        color = KaraokeColors.Muted,
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Pill("Thứ ${focusedIndex + 1} / ${queue.size}")
                        if (focused.song.songNumber != null) {
                            Spacer(Modifier.width(8.dp))
                            Pill(focused.song.songNumber)
                        }
                        if (focused.song.tone != null) {
                            Spacer(Modifier.width(8.dp))
                            Pill(focused.song.tone, color = KaraokeColors.Primary)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = focused.song.title,
                        style = MaterialTheme.typography.displaySmall,
                        color = KaraokeColors.OnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val singer = focused.song.artist
                    if (!singer.isNullOrBlank()) {
                        Text(
                            text = singer,
                            style = MaterialTheme.typography.titleMedium,
                            color = KaraokeColors.Muted,
                            maxLines = 1,
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TvButton(
                            text = "Hát ngay",
                            onClick = { onPlayNow(focused) },
                            icon = Icons.Filled.PlayArrow,
                            emphasised = true,
                        )
                        TvButton(
                            text = "Ưu tiên",
                            onClick = { onPrioritise(focused) },
                            icon = Icons.Filled.VerticalAlignTop,
                        )
                        if (focusedIndex > 0) {
                            TvButton(
                                text = "Lên",
                                onClick = { onMoveUp(focused) },
                                icon = Icons.Filled.ArrowUpward,
                            )
                        }
                        if (focusedIndex < queue.lastIndex) {
                            TvButton(
                                text = "Xuống",
                                onClick = { onMoveDown(focused) },
                                icon = Icons.Filled.ArrowDownward,
                            )
                        }
                        TvButton(
                            text = "Xóa",
                            onClick = { onRemove(focused) },
                            icon = Icons.Filled.Delete,
                        )
                        TvButton("Xóa hết", onClearAll, icon = Icons.Filled.Delete)
                    }
                }
            }

            if (queue.isNotEmpty()) {
                Text(
                    text = "${queue.size} bài đang chờ",
                    style = MaterialTheme.typography.titleMedium,
                    color = KaraokeColors.Muted,
                    modifier = Modifier.padding(
                        start = TvSpacing.ScreenHorizontal,
                        bottom = 8.dp,
                    ),
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(TvSpacing.CardGap),
                    contentPadding = PaddingValues(
                        start = TvSpacing.ScreenHorizontal,
                        end = TvSpacing.ScreenHorizontal,
                        bottom = TvSpacing.ScreenVertical,
                    ),
                ) {
                    itemsIndexed(queue, key = { _, item -> item.uid }) { _, item ->
                        SongCard(
                            song = item.song,
                            onClick = { onPlayNow(item) },
                            onLongClick = { onPrioritise(item) },
                            modifier = Modifier.onFocusChanged { state ->
                                if (state.isFocused) focusedUid = item.uid
                            },
                        )
                    }
                }
            }
        }
    }
}
