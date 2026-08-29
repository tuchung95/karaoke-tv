package com.athr.karaoketv.ui.browse

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.athr.karaoketv.data.db.SongEntity
import com.athr.karaoketv.data.repo.LibraryRepository
import com.athr.karaoketv.ui.components.SongCard
import com.athr.karaoketv.ui.components.TvCard
import com.athr.karaoketv.ui.theme.KaraokeColors
import com.athr.karaoketv.ui.theme.TvSpacing
import com.athr.karaoketv.util.formatCount

/**
 * The drive as it actually is, one folder at a time.
 *
 * Rip collections come organised however whoever filled the drive felt like doing
 * it, and that shape is often the only index there is — "the folder Dad put the
 * bolero in" beats any tag the filenames happen to carry. The other screens infer
 * groupings; this one just shows what is there.
 */
@Composable
fun FolderScreen(
    title: String,
    path: String,
    folders: List<LibraryRepository.FolderChild>,
    songs: List<SongEntity>,
    onOpenFolder: (LibraryRepository.FolderChild) -> Unit,
    onSongClick: (SongEntity) -> Unit,
    onSongOptions: (SongEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Column(
            Modifier.padding(
                start = TvSpacing.ScreenHorizontal,
                end = TvSpacing.ScreenHorizontal,
                top = 8.dp,
                bottom = 12.dp,
            )
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = KaraokeColors.OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (path.isBlank()) "Gốc thư viện" else path,
                style = MaterialTheme.typography.bodyMedium,
                color = KaraokeColors.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (folders.isEmpty() && songs.isEmpty()) {
            Text(
                text = "Thư mục này trống",
                style = MaterialTheme.typography.titleMedium,
                color = KaraokeColors.Muted,
                modifier = Modifier.padding(start = TvSpacing.ScreenHorizontal),
            )
            return@Column
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(TvSpacing.CardWidth4Up),
            horizontalArrangement = Arrangement.spacedBy(TvSpacing.CardGap),
            verticalArrangement = Arrangement.spacedBy(TvSpacing.CardGap),
            contentPadding = PaddingValues(
                start = TvSpacing.ScreenHorizontal,
                end = TvSpacing.ScreenHorizontal,
                bottom = 56.dp,
            ),
        ) {
            if (folders.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionLabel("${folders.size} thư mục con")
                }
                items(folders, key = { it.path }) { folder ->
                    TvCard(
                        onClick = { onOpenFolder(folder) },
                        modifier = Modifier.width(TvSpacing.CardWidth4Up),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Folder,
                                contentDescription = null,
                                tint = KaraokeColors.Accent,
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = folder.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "${formatCount(folder.songCount)} bài",
                            style = MaterialTheme.typography.bodyMedium,
                            color = KaraokeColors.Accent,
                        )
                    }
                }
            }

            if (songs.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionLabel("${formatCount(songs.size)} bài trong thư mục này")
                }
                items(songs, key = { it.id }) { song ->
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
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = KaraokeColors.Muted,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp),
    )
}
