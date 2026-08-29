package com.athr.karaoketv.ui.player

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.athr.karaoketv.ui.theme.KaraokeColors

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

/** Shown behind the UI when nothing is queued, so the screen is never plain black. */
@Composable
fun IdleStage(libraryLabel: String, songCount: Int, modifier: Modifier = Modifier) {
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
                text = "KARAOKE",
                style = MaterialTheme.typography.displayLarge,
                color = KaraokeColors.Primary,
            )
            Text(
                text = if (songCount > 0) {
                    "$songCount bài trong $libraryLabel"
                } else {
                    "Chưa có bài hát nào"
                },
                style = MaterialTheme.typography.titleMedium,
                color = KaraokeColors.Muted,
            )
        }
    }
}
