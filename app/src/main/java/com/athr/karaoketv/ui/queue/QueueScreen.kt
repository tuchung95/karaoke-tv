package com.athr.karaoketv.ui.queue

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.athr.karaoketv.player.QueueItem
import com.athr.karaoketv.ui.components.Pill
import com.athr.karaoketv.ui.components.TvButton
import androidx.tv.material3.ListItem
import com.athr.karaoketv.ui.components.karaokeListItemColors
import com.athr.karaoketv.ui.theme.KaraokeColors
import com.athr.karaoketv.ui.theme.TvSpacing

/**
 * The list everyone in the room argues over. Each row carries its own controls
 * so nothing needs a long-press or a second menu: bump to the front, nudge up or
 * down, sing it now, or drop it.
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
    Column(modifier = modifier.fillMaxSize()) {
        Column(
            Modifier.padding(
                start = TvSpacing.ScreenHorizontal,
                end = TvSpacing.ScreenHorizontal,
                top = TvSpacing.ScreenVertical,
                bottom = 16.dp,
            )
        ) {
            Text(
                text = "Hàng chờ",
                style = MaterialTheme.typography.headlineLarge,
                color = KaraokeColors.OnSurface,
            )
            Text(
                text = if (queue.isEmpty()) "Chưa có bài nào đang chờ" else "${queue.size} bài đang chờ",
                style = MaterialTheme.typography.bodyLarge,
                color = KaraokeColors.Muted,
            )
            if (current != null) {
                Spacer(Modifier.height(16.dp))
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
                    TvButton("Bỏ qua", onSkipCurrent)
                }
            }
            if (queue.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                TvButton("Xóa hết hàng chờ", onClearAll, icon = Icons.Filled.Delete)
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(
                start = TvSpacing.ScreenHorizontal,
                end = TvSpacing.ScreenHorizontal,
                bottom = 64.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(queue, key = { _, item -> item.uid }) { index, item ->
                QueueRow(
                    ordinal = index + 1,
                    item = item,
                    canMoveUp = index > 0,
                    canMoveDown = index < queue.lastIndex,
                    onPlayNow = { onPlayNow(item) },
                    onPrioritise = { onPrioritise(item) },
                    onMoveUp = { onMoveUp(item) },
                    onMoveDown = { onMoveDown(item) },
                    onRemove = { onRemove(item) },
                )
            }
        }
    }
}

@Composable
private fun QueueRow(
    ordinal: Int,
    item: QueueItem,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onPlayNow: () -> Unit,
    onPrioritise: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ListItem(
            selected = false,
            onClick = onPlayNow,
            modifier = Modifier.weight(1f),
            colors = karaokeListItemColors(),
            leadingContent = {
                Text(
                    text = ordinal.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.width(56.dp),
                )
            },
            headlineContent = {
                Text(
                    text = item.song.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            supportingContent = {
                Text(
                    text = listOfNotNull(item.song.artist, item.song.tone)
                        .joinToString(" · ")
                        .ifBlank { "—" },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                )
            },
        )
        TvButton("Hát ngay", onPlayNow, icon = Icons.Filled.PlayArrow)
        TvButton("Ưu tiên", onPrioritise, icon = Icons.Filled.VerticalAlignTop)
        if (canMoveUp) TvButton("Lên", onMoveUp, icon = Icons.Filled.ArrowUpward)
        if (canMoveDown) TvButton("Xuống", onMoveDown, icon = Icons.Filled.ArrowDownward)
        TvButton("Xóa", onRemove, icon = Icons.Filled.Delete)
    }
}
