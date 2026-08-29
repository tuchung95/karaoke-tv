package com.athr.karaoketv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.athr.karaoketv.ui.theme.KaraokeColors

/**
 * A frame lifted out of the video itself, since karaoke rips carry no artwork.
 *
 * The frame is taken a little way in rather than at 0ms: karaoke videos almost
 * always open on black or a countdown, which would make every card look identical.
 */
@Composable
fun SongThumbnail(
    uri: String,
    modifier: Modifier = Modifier,
    iconSize: Int = 28,
) {
    Box(
        modifier = modifier.background(KaraokeColors.SurfaceHigh),
        contentAlignment = Alignment.Center,
    ) {
        // Shows through until the frame decodes, and stays put if it never does.
        Icon(
            Icons.Filled.MusicNote,
            contentDescription = null,
            tint = KaraokeColors.Muted,
            modifier = Modifier.size(iconSize.dp),
        )
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(uri)
                .videoFrameMillis(THUMBNAIL_FRAME_MS)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private const val THUMBNAIL_FRAME_MS = 20_000L
