package com.athr.karaoketv.ui.queue

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.NavigationDrawer
import androidx.tv.material3.NavigationDrawerItem
import androidx.tv.material3.Text
import com.athr.karaoketv.player.QueueItem
import com.athr.karaoketv.ui.components.Pill
import com.athr.karaoketv.ui.components.SongThumbnail
import com.athr.karaoketv.ui.components.TvButton
import com.athr.karaoketv.ui.theme.KaraokeColors
import com.athr.karaoketv.ui.theme.TvSpacing

/**
 * Queue on the left in a navigation drawer, the picked song and everything you can
 * do to it on the right.
 *
 * The drawer is Google's component for app destinations and their guidance caps it
 * at five or six; a karaoke queue has no such ceiling, so it scrolls. What the
 * shape buys is worth that: entries collapse to thumbnails until focus reaches
 * them, and the five actions live once in the content pane instead of being
 * repeated on every single row, which is what made this screen a wall of buttons.
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
    var selectedUid by remember { mutableStateOf<Long?>(null) }
    val selected = queue.firstOrNull { it.uid == selectedUid } ?: queue.firstOrNull()
    val selectedIndex = queue.indexOfFirst { it.uid == selected?.uid }

    NavigationDrawer(
        modifier = modifier.fillMaxSize(),
        drawerContent = { drawerValue ->
            val expanded = drawerValue == DrawerValue.Open
            Column(
                Modifier
                    .fillMaxHeight()
                    .padding(start = 12.dp, top = 8.dp, bottom = 8.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (expanded) {
                    Text(
                        text = "Hàng chờ · ${queue.size} bài",
                        style = MaterialTheme.typography.titleMedium,
                        color = KaraokeColors.Muted,
                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp),
                    )
                }
                queue.forEachIndexed { index, item ->
                    NavigationDrawerItem(
                        selected = item.uid == selected?.uid,
                        onClick = { selectedUid = item.uid },
                        onLongClick = { onPlayNow(item) },
                        leadingContent = {
                            Box(contentAlignment = Alignment.Center) {
                                SongThumbnail(
                                    uri = item.song.uri,
                                    iconSize = 14,
                                    modifier = Modifier
                                        .size(width = 40.dp, height = 40.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                                )
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = KaraokeColors.OnSurface,
                                )
                            }
                        },
                        supportingContent = {
                            Text(
                                text = item.song.artist ?: item.song.collection ?: "—",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    ) {
                        Text(
                            text = item.song.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        },
    ) {
        QueueDetail(
            current = current,
            selected = selected,
            position = if (selectedIndex >= 0) selectedIndex + 1 else 0,
            total = queue.size,
            canMoveUp = selectedIndex > 0,
            canMoveDown = selectedIndex >= 0 && selectedIndex < queue.lastIndex,
            onPlayNow = { selected?.let(onPlayNow) },
            onPrioritise = { selected?.let(onPrioritise) },
            onMoveUp = { selected?.let(onMoveUp) },
            onMoveDown = { selected?.let(onMoveDown) },
            onRemove = { selected?.let(onRemove) },
            onClearAll = onClearAll,
            onSkipCurrent = onSkipCurrent,
        )
    }
}

@Composable
private fun QueueDetail(
    current: QueueItem?,
    selected: QueueItem?,
    position: Int,
    total: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onPlayNow: () -> Unit,
    onPrioritise: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onClearAll: () -> Unit,
    onSkipCurrent: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(
                start = 24.dp,
                end = TvSpacing.ScreenHorizontal,
                top = 8.dp,
                bottom = TvSpacing.ScreenVertical,
            )
    ) {
        if (current != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill("ĐANG HÁT", color = KaraokeColors.Success)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = current.song.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = KaraokeColors.OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                TvButton("Bỏ qua", onSkipCurrent, icon = Icons.Filled.SkipNext)
            }
            Spacer(Modifier.height(24.dp))
        }

        if (selected == null) {
            Text(
                text = "Chưa có bài nào đang chờ",
                style = MaterialTheme.typography.headlineMedium,
                color = KaraokeColors.Muted,
            )
            return@Column
        }

        SongThumbnail(
            uri = selected.song.uri,
            iconSize = 48,
            modifier = Modifier
                .fillMaxWidth(0.52f)
                .height(200.dp)
                .clip(RoundedCornerShape(14.dp)),
        )
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pill("Thứ $position / $total")
            if (selected.song.songNumber != null) {
                Spacer(Modifier.width(8.dp))
                Pill(selected.song.songNumber)
            }
            if (selected.song.tone != null) {
                Spacer(Modifier.width(8.dp))
                Pill(selected.song.tone, color = KaraokeColors.Primary)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = selected.song.title,
            style = MaterialTheme.typography.headlineLarge,
            color = KaraokeColors.OnSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val singer = selected.song.artist
        if (!singer.isNullOrBlank()) {
            Text(
                text = singer,
                style = MaterialTheme.typography.titleMedium,
                color = KaraokeColors.Muted,
                maxLines = 1,
            )
        }

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TvButton("Hát ngay", onPlayNow, icon = Icons.Filled.PlayArrow, emphasised = true)
            TvButton("Ưu tiên", onPrioritise, icon = Icons.Filled.VerticalAlignTop)
            if (canMoveUp) TvButton("Lên", onMoveUp, icon = Icons.Filled.ArrowUpward)
            if (canMoveDown) TvButton("Xuống", onMoveDown, icon = Icons.Filled.ArrowDownward)
            TvButton("Xóa", onRemove, icon = Icons.Filled.Delete)
        }
        Spacer(Modifier.height(12.dp))
        TvButton("Xóa hết hàng chờ", onClearAll, icon = Icons.Filled.Delete)
    }
}
