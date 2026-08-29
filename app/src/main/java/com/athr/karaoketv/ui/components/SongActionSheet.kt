package com.athr.karaoketv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.athr.karaoketv.data.db.SongEntity
import com.athr.karaoketv.ui.theme.KaraokeColors

/**
 * Long-pressing OK on any song opens this. Pressing OK plainly just queues the
 * song, because that is what people want nine times out of ten and it should
 * cost exactly one keypress.
 */
@Composable
fun SongActionSheet(
    song: SongEntity,
    onDismiss: () -> Unit,
    onPlayNow: () -> Unit,
    onQueue: () -> Unit,
    onQueueNext: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSwapTitleArtist: () -> Unit,
) {
    val firstFocus = remember { FocusRequester() }
    RequestInitialFocus(firstFocus, key = song.id)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(560.dp)
                .background(KaraokeColors.Surface, RoundedCornerShape(20.dp))
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.headlineMedium,
                color = KaraokeColors.OnSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = listOfNotNull(song.artist, song.tone, song.relPath.ifBlank { null })
                .joinToString(" · ")
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = KaraokeColors.Muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(8.dp))
            TvButton(
                text = "Hát ngay",
                onClick = { onPlayNow(); onDismiss() },
                icon = Icons.Filled.PlayArrow,
                emphasised = true,
                modifier = Modifier.fillMaxWidth(),
                focusRequester = firstFocus,
            )
            TvButton(
                text = "Thêm vào hàng chờ",
                onClick = { onQueue(); onDismiss() },
                icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                modifier = Modifier.fillMaxWidth(),
            )
            TvButton(
                text = "Ưu tiên hát tiếp theo",
                onClick = { onQueueNext(); onDismiss() },
                icon = Icons.Filled.VerticalAlignTop,
                modifier = Modifier.fillMaxWidth(),
            )
            if (!song.artist.isNullOrBlank()) {
                TvButton(
                    // Filenames use both "Tên bài - Ca sĩ" and "Ca sĩ - Tên bài" and
                    // often give no way to tell which; one press fixes a wrong guess.
                    text = "Đổi tên bài ↔ ca sĩ",
                    onClick = { onSwapTitleArtist(); onDismiss() },
                    icon = Icons.Filled.SwapHoriz,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            TvButton(
                text = if (song.favorite) "Bỏ yêu thích" else "Thêm vào yêu thích",
                onClick = { onToggleFavorite(); onDismiss() },
                icon = if (song.favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
