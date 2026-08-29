package com.athr.karaoketv.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.athr.karaoketv.data.db.SongEntity
import com.athr.karaoketv.ui.components.Pill
import com.athr.karaoketv.ui.components.SongThumbnail
import com.athr.karaoketv.ui.theme.KaraokeColors
import com.athr.karaoketv.ui.theme.TvSpacing

/**
 * The featured band across the top, in the shape Google TV uses: artwork bleeding
 * off the right edge, a scrim carrying it into the text on the left, and the title
 * large enough to read from the sofa.
 *
 * It shows whatever is playing; with the room idle it puts forward the song this
 * house sings most, which is a better invitation than an empty header.
 */
@Composable
fun HeroBanner(
    song: SongEntity?,
    nowPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    if (song == null) return

    Box(modifier = modifier.fillMaxWidth().height(HERO_HEIGHT)) {
        SongThumbnail(
            uri = song.uri,
            iconSize = 64,
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.62f)
                .align(Alignment.CenterEnd),
        )
        // Two scrims: one carrying the art into the text, one settling it onto the
        // page so the row beneath does not fight the artwork.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to KaraokeColors.Background,
                        0.42f to KaraokeColors.Background,
                        0.72f to KaraokeColors.Background.copy(alpha = 0.55f),
                        1f to Color.Transparent,
                    )
                )
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.7f to Color.Transparent,
                        1f to KaraokeColors.Background,
                    )
                )
        )

        Column(
            Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(0.55f)
                // The artwork runs to the panel edge; the words must not.
                .padding(start = TvSpacing.ScreenHorizontal, bottom = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill(
                    text = if (nowPlaying) "ĐANG HÁT" else "HÁT NHIỀU NHẤT",
                    color = if (nowPlaying) KaraokeColors.Success else KaraokeColors.Accent,
                )
                if (song.songNumber != null) {
                    Spacer(Modifier.width(8.dp))
                    Pill(song.songNumber)
                }
                if (song.tone != null) {
                    Spacer(Modifier.width(8.dp))
                    Pill(song.tone, color = KaraokeColors.Primary)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = song.title,
                style = MaterialTheme.typography.displaySmall,
                color = KaraokeColors.OnSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val line = listOfNotNull(song.artist, song.collection).joinToString(" · ")
            if (line.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = line,
                    style = MaterialTheme.typography.titleMedium,
                    color = KaraokeColors.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private val HERO_HEIGHT = 260.dp
