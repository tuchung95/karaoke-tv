package com.athr.karaoketv.ui.search

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import com.athr.karaoketv.ui.components.SongCard
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.NativeKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.dp
import com.athr.karaoketv.data.db.SongEntity
import com.athr.karaoketv.ui.components.RequestInitialFocus
import com.athr.karaoketv.ui.components.SongRow
import com.athr.karaoketv.ui.components.TvButton
import com.athr.karaoketv.ui.theme.KaraokeColors
import com.athr.karaoketv.ui.theme.TvSpacing

/**
 * Search is the fastest route to a song, so it gets the whole screen: keyboard on
 * the left, live results on the right. Number keys on the remote type straight
 * into the box, which is how anyone used to a karaoke deck expects to find a song
 * by its book number.
 */
@Composable
fun SearchScreen(
    query: String,
    results: List<SongEntity>,
    onQueryChange: (String) -> Unit,
    onKey: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onSongClick: (SongEntity) -> Unit,
    onSongOptions: (SongEntity) -> Unit,
    youTubeAvailable: Boolean,
    onYouTubeSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val firstKeyFocus = remember { FocusRequester() }
    RequestInitialFocus(firstKeyFocus)

    val voice = rememberVoiceSearch(
        onPartial = onQueryChange,
        onResult = onQueryChange,
    )

    Row(
        modifier = modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event -> handleRemoteTyping(event.nativeKeyEvent, onKey, onBackspace) }
            .padding(
                horizontal = TvSpacing.ScreenHorizontal,
                vertical = TvSpacing.ScreenVertical,
            ),
    ) {
        Column(
            Modifier
                .width(380.dp)
                .fillMaxHeight()
                // The nav bar above costs vertical room; scrolling guarantees the
                // space and delete keys stay reachable on shorter panels.
                .verticalScroll(rememberScrollState())
        ) {
            QueryDisplay(query = query, listening = voice.listening)
            Spacer(Modifier.height(12.dp))
            // Side by side: stacked, these two pushed the query box off the top of
            // a 540dp panel.
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TvButton(
                    text = when {
                        voice.listening -> "Đang nghe…"
                        !voice.available -> "Không hỗ trợ giọng nói"
                        else -> "Giọng nói"
                    },
                    icon = Icons.Filled.Mic,
                    emphasised = voice.listening,
                    onClick = { if (voice.listening) voice.stop() else voice.start() },
                    modifier = Modifier.weight(1f),
                )
                if (youTubeAvailable) {
                    TvButton(
                        text = "YouTube",
                        icon = Icons.Filled.PlayCircleOutline,
                        onClick = onYouTubeSearch,
                    )
                }
            }
            if (voice.error != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = voice.error!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = KaraokeColors.Danger,
                )
            }
            Spacer(Modifier.height(10.dp))
            OnScreenKeyboard(
                onKey = onKey,
                onBackspace = onBackspace,
                onClear = onClear,
                firstKeyFocus = firstKeyFocus,
            )
        }

        Spacer(Modifier.width(TvSpacing.Gutter))

        Column(Modifier.weight(1f).fillMaxHeight()) {
            Text(
                text = when {
                    query.isBlank() -> "Nhập tên bài, tên ca sĩ hoặc mã số"
                    results.isEmpty() -> "Không tìm thấy bài nào"
                    else -> "${results.size} kết quả"
                },
                style = MaterialTheme.typography.headlineMedium,
                color = KaraokeColors.OnSurface,
            )
            Spacer(Modifier.height(12.dp))
            // Cards, not rows: results are content, and a grid of thumbnails is
            // quicker to pick from across the room than a column of text.
            LazyVerticalGrid(
                columns = GridCells.Adaptive(TvSpacing.CardWidth4Up),
                horizontalArrangement = Arrangement.spacedBy(TvSpacing.CardGap),
                verticalArrangement = Arrangement.spacedBy(TvSpacing.CardGap),
                contentPadding = PaddingValues(bottom = 48.dp),
            ) {
                items(results, key = { it.id }) { song ->
                    SongCard(
                        song = song,
                        onClick = { onSongClick(song) },
                        onLongClick = { onSongOptions(song) },
                        width = TvSpacing.CardWidth4Up,
                    )
                }
            }
        }
    }
}


@Composable
private fun QueryDisplay(query: String, listening: Boolean) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(KaraokeColors.Surface, RoundedCornerShape(14.dp))
            .border(
                width = 2.dp,
                color = if (listening) KaraokeColors.Primary else KaraokeColors.Divider,
                shape = RoundedCornerShape(14.dp),
            )
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = query.ifBlank { "Tìm bài hát…" },
            style = MaterialTheme.typography.headlineMedium,
            color = if (query.isBlank()) KaraokeColors.Muted else KaraokeColors.OnSurface,
            maxLines = 1,
        )
    }
}

/**
 * Types into the query without moving focus off the keyboard grid. Covers the
 * number pad every karaoke remote has, and plain letters too — plenty of TV
 * remotes carry a small keyboard, and the phone remote apps people use send real
 * characters rather than D-pad events.
 */
private fun handleRemoteTyping(
    event: NativeKeyEvent,
    onKey: (String) -> Unit,
    onBackspace: () -> Unit,
): Boolean {
    if (event.action != KeyEvent.ACTION_DOWN) return false
    return when (event.keyCode) {
        in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> {
            onKey((event.keyCode - KeyEvent.KEYCODE_0).toString())
            true
        }
        in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z -> {
            onKey(('A' + (event.keyCode - KeyEvent.KEYCODE_A)).toString())
            true
        }
        KeyEvent.KEYCODE_SPACE -> {
            onKey(" ")
            true
        }
        KeyEvent.KEYCODE_DEL -> {
            onBackspace()
            true
        }
        else -> false
    }
}
