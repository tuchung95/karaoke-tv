package com.athr.karaoketv.ui.player

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.athr.karaoketv.player.SessionSummary
import com.athr.karaoketv.ui.theme.KaraokeColors
import com.athr.karaoketv.util.formatCount

/**
 * The video fills the screen for the whole life of the app and every other
 * surface floats over it, so choosing the next song never interrupts the song
 * being sung.
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoStage(
    player: ExoPlayer,
    scaleMode: Int,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier.fillMaxSize().background(Color.Black),
        factory = { context ->
            PlayerView(context).apply {
                useController = false
                setShutterBackgroundColor(android.graphics.Color.BLACK)
                setKeepContentOnPlayerReset(true)
                this.player = player
                isFocusable = false
                isFocusableInTouchMode = false
            }
        },
        update = { view ->
            view.player = player
            view.resizeMode = when (scaleMode) {
                1 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                2 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
        },
    )
}

/**
 * Shown behind the UI when nothing is queued, so the screen is never plain black.
 *
 * With a [summary] it doubles as the closing card for a night of singing: the
 * queue draining is the one moment the room is guaranteed to look at the screen
 * together, and an ending people remember is worth more than a logo.
 */
@Composable
fun IdleStage(
    libraryLabel: String,
    songCount: Int,
    summary: SessionSummary? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF14061C), KaraokeColors.Background, Color(0xFF0A1A22)),
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = if (summary == null) "KARAOKE" else "HẾT HÀNG CHỜ",
                style = MaterialTheme.typography.displayLarge,
                color = KaraokeColors.Primary,
                textAlign = TextAlign.Center,
            )
            if (summary == null) {
                Text(
                    text = if (songCount > 0) {
                        "${formatCount(songCount)} bài trong $libraryLabel"
                    } else {
                        "Chưa quét bài nào — vào Cài đặt → Thư viện để quét ổ cứng"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = KaraokeColors.Muted,
                    textAlign = TextAlign.Center,
                )
            } else {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Nhà mình vừa hát ${summary.songsSung} bài",
                    style = MaterialTheme.typography.headlineSmall,
                    color = KaraokeColors.OnSurface,
                    textAlign = TextAlign.Center,
                )
                if (summary.mostSungCount > 1 && summary.mostSungTitle != null) {
                    Text(
                        text = "Nhiều nhất: ${summary.mostSungTitle} · ${summary.mostSungCount} lần",
                        style = MaterialTheme.typography.titleMedium,
                        color = KaraokeColors.Muted,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "Chọn bài ở menu để hát tiếp",
                    style = MaterialTheme.typography.titleMedium,
                    color = KaraokeColors.Accent,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
