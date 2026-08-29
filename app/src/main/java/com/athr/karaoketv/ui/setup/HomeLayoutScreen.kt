package com.athr.karaoketv.ui.setup

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
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.athr.karaoketv.data.prefs.HomeShelf
import com.athr.karaoketv.ui.components.Pill
import com.athr.karaoketv.ui.components.TvButton
import com.athr.karaoketv.ui.components.TvFocusable
import com.athr.karaoketv.ui.theme.KaraokeColors
import com.athr.karaoketv.ui.theme.TvSpacing

/**
 * Rearranges the home screen. Every row on it is optional, and the one people
 * reach for most differs per room — a bolero house wants Thể loại at the top,
 * a family box may want nobody's history showing at all.
 */
@Composable
fun HomeLayoutScreen(
    order: List<HomeShelf>,
    hidden: Set<HomeShelf>,
    onToggle: (HomeShelf) -> Unit,
    onMoveUp: (HomeShelf) -> Unit,
    onMoveDown: (HomeShelf) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Column(
            Modifier.padding(
                start = TvSpacing.ScreenHorizontal,
                end = TvSpacing.ScreenHorizontal,
                top = 16.dp,
                bottom = 16.dp,
            )
        ) {
            Text(
                text = "Bố cục màn hình chính",
                style = MaterialTheme.typography.headlineLarge,
                color = KaraokeColors.OnSurface,
            )
            Text(
                text = "Chọn hàng nào hiện và hiện theo thứ tự nào. " +
                    "Hàng không có bài nào thì tự ẩn.",
                style = MaterialTheme.typography.bodyLarge,
                color = KaraokeColors.Muted,
            )
            Spacer(Modifier.height(12.dp))
            TvButton("Khôi phục mặc định", onReset, icon = Icons.Filled.Restore)
        }

        LazyColumn(
            contentPadding = PaddingValues(
                start = TvSpacing.ScreenHorizontal,
                end = TvSpacing.ScreenHorizontal,
                bottom = 64.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(order, key = { _, shelf -> shelf.key }) { index, shelf ->
                val visible = shelf !in hidden
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TvFocusable(
                        onClick = { onToggle(shelf) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    ) { focused ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (focused) KaraokeColors.Accent else KaraokeColors.Muted,
                                modifier = Modifier.width(48.dp),
                            )
                            Text(
                                text = shelf.label,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (visible) {
                                    KaraokeColors.OnSurface
                                } else {
                                    KaraokeColors.Muted
                                },
                                modifier = Modifier.weight(1f),
                            )
                            Pill(
                                text = if (visible) "Hiện" else "Ẩn",
                                color = if (visible) KaraokeColors.Success else KaraokeColors.Muted,
                            )
                        }
                    }
                    TvButton(
                        text = if (visible) "Ẩn" else "Hiện",
                        onClick = { onToggle(shelf) },
                        icon = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    )
                    if (index > 0) {
                        TvButton("Lên", { onMoveUp(shelf) }, icon = Icons.Filled.ArrowUpward)
                    }
                    if (index < order.lastIndex) {
                        TvButton("Xuống", { onMoveDown(shelf) }, icon = Icons.Filled.ArrowDownward)
                    }
                }
            }
        }
    }
}
