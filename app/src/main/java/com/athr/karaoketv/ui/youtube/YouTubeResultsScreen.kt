package com.athr.karaoketv.ui.youtube

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.athr.karaoketv.data.youtube.YouTubeVideo
import com.athr.karaoketv.ui.YouTubeState
import com.athr.karaoketv.ui.components.TvButton
import com.athr.karaoketv.ui.components.TvCard
import com.athr.karaoketv.ui.theme.KaraokeColors
import com.athr.karaoketv.ui.theme.TvSpacing

/**
 * YouTube results in this app's own grid.
 *
 * Handing the room off to YouTube's search page mid-party costs everyone the
 * navigation they just learned; picking from the same card grid as the drive's
 * songs keeps one way of choosing a song. The hand-off stays one button away,
 * because it is the only route that honours a Premium subscription.
 */
@Composable
fun YouTubeResultsScreen(
    query: String,
    state: YouTubeState,
    onPlay: (YouTubeVideo) -> Unit,
    onOpenInYouTubeApp: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = TvSpacing.ScreenHorizontal),
    ) {
        Text(
            text = when (state) {
                is YouTubeState.Results ->
                    if (state.videos.isEmpty()) {
                        "YouTube không có kết quả cho \"$query\""
                    } else {
                        "${state.videos.size} kết quả trên YouTube cho \"$query\""
                    }
                YouTubeState.Searching -> "Đang tìm \"$query\" trên YouTube…"
                is YouTubeState.Failed -> "Không tìm được trên YouTube"
                YouTubeState.Idle -> "YouTube"
            },
            style = MaterialTheme.typography.headlineMedium,
            color = KaraokeColors.OnSurface,
        )

        when (state) {
            is YouTubeState.Failed -> {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.titleMedium,
                    color = KaraokeColors.Danger,
                )
                if (onOpenInYouTubeApp != null) {
                    Spacer(Modifier.height(18.dp))
                    TvButton(
                        text = "Mở trong app YouTube",
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        emphasised = true,
                        onClick = onOpenInYouTubeApp,
                    )
                }
            }

            is YouTubeState.Results -> {
                Spacer(Modifier.height(6.dp))
                // Said once, here, rather than discovered one disappointment at a
                // time: these songs live outside our transport.
                Text(
                    text = "Bài từ YouTube không chỉnh tông, không bỏ giọng ca sĩ và " +
                        "không vào hàng chờ được — ba thứ đó chỉ áp dụng cho file trên ổ. " +
                        "Muốn giữ Premium không quảng cáo thì mở bằng app YouTube.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KaraokeColors.Muted,
                )
                Spacer(Modifier.height(14.dp))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(TvSpacing.CardWidth4Up),
                    horizontalArrangement = Arrangement.spacedBy(TvSpacing.CardGap),
                    verticalArrangement = Arrangement.spacedBy(TvSpacing.CardGap),
                    contentPadding = PaddingValues(bottom = 48.dp),
                ) {
                    items(state.videos, key = { it.id }) { video ->
                        YouTubeCard(video = video, onClick = { onPlay(video) })
                    }
                }
            }

            else -> Unit
        }
    }
}

@Composable
private fun YouTubeCard(video: YouTubeVideo, onClick: () -> Unit) {
    TvCard(
        onClick = onClick,
        modifier = Modifier.width(TvSpacing.CardWidth4Up),
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TvSpacing.CardWidth4Up * 9 / 16)
                .background(KaraokeColors.SurfaceHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.PlayCircleOutline,
                contentDescription = null,
                tint = KaraokeColors.Muted,
            )
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(video.thumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Column(Modifier.padding(14.dp)) {
            Text(
                text = video.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = video.channel,
                style = MaterialTheme.typography.bodyMedium,
                color = KaraokeColors.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
