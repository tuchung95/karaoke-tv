package com.athr.karaoketv.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.window.Popup
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabDefaults
import androidx.tv.material3.TabRow
import androidx.tv.material3.TabRowDefaults
import androidx.tv.material3.Text
import com.athr.karaoketv.ui.theme.KaraokeColors

/** The destinations that sit in the tab strip, in order. */
enum class HomeTab(val label: String) {
    HOME("Trang chủ"),
    CATEGORIES("Thể loại"),
    ARTISTS("Ca sĩ"),
    ALL_SONGS("Tất cả bài"),
    FAVORITES("Yêu thích"),
}

/**
 * The top strip: a search key and the destination tabs in one pill, the utility
 * actions in another — the shape Google TV uses on its own home screen, so the
 * remote lands where a viewer already expects.
 */
@Composable
fun HomeTopBar(
    selected: HomeTab,
    onSearch: () -> Unit,
    onTab: (HomeTab) -> Unit,
    onQueue: () -> Unit,
    onShuffle: () -> Unit,
    onSettings: () -> Unit,
    queueSize: Int,
    searchFocus: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RoundIconButton(
            icon = Icons.Filled.Search,
            description = "Tìm bài",
            onClick = onSearch,
            focusRequester = searchFocus,
        )

        TabRow(
            selectedTabIndex = HomeTab.entries.indexOf(selected),
            separator = { Spacer(Modifier.width(4.dp)) },
            indicator = { tabPositions, doesTabRowHaveFocus ->
                // Same fill whether or not the row has focus. Left to itself the
                // pill switches between a bright and a dim surface, and then no
                // single label colour is readable in both states — one of them
                // ends up light-on-light or dark-on-dark.
                TabRowDefaults.PillIndicator(
                    currentTabPosition = tabPositions[HomeTab.entries.indexOf(selected)],
                    doesTabRowHaveFocus = doesTabRowHaveFocus,
                    activeColor = KaraokeColors.OnSurface,
                    inactiveColor = KaraokeColors.OnSurface,
                )
            },
        ) {
            HomeTab.entries.forEach { tab ->
                Tab(
                    selected = tab == selected,
                    onFocus = {},
                    onClick = { onTab(tab) },
                    // The pill flips between a dim and a bright fill depending on
                    // whether the row has focus, so the label has to flip with it:
                    // light on the dim pill, dark on the bright one. Using one
                    // colour for both leaves the selected tab unreadable in one
                    // state or the other.
                    // The pill is always light, so every selected state is dark
                    // text on it and every unselected one is light text on the bar.
                    colors = TabDefaults.pillIndicatorTabColors(
                        contentColor = KaraokeColors.Muted,
                        inactiveContentColor = KaraokeColors.Muted,
                        selectedContentColor = KaraokeColors.Background,
                        focusedContentColor = KaraokeColors.Muted,
                        focusedSelectedContentColor = KaraokeColors.Background,
                    ),
                ) {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
            }
        }

        Spacer(Modifier.width(4.dp))
        RoundIconButton(
            icon = Icons.AutoMirrored.Filled.QueueMusic,
            description = if (queueSize == 0) "Hàng chờ" else "Hàng chờ, $queueSize bài",
            onClick = onQueue,
            highlighted = queueSize > 0,
        )
        RoundIconButton(Icons.Filled.Casino, "Hát ngẫu nhiên", onShuffle)
        RoundIconButton(Icons.Filled.Settings, "Cài đặt", onSettings)

    }
}

@Composable
private fun RoundIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    highlighted: Boolean = false,
    focusRequester: FocusRequester? = null,
) {
    var focused by remember { mutableStateOf(false) }

    Box {
        // IconButton, not Button with zero padding: the design system sizes and
        // centres the glyph itself, which is what the hand-rolled version got wrong.
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(IconButtonDefaults.MediumButtonSize)
                .onFocusChanged { focused = it.isFocused }
                .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier),
            shape = IconButtonDefaults.shape(shape = CircleShape),
            colors = IconButtonDefaults.colors(
                containerColor = KaraokeColors.Surface,
                contentColor = if (highlighted) KaraokeColors.Accent else KaraokeColors.Muted,
                focusedContainerColor = KaraokeColors.OnSurface,
                focusedContentColor = KaraokeColors.Background,
            ),
        ) {
            Icon(
                icon,
                contentDescription = description,
                modifier = Modifier.size(IconButtonDefaults.MediumIconSize),
            )
        }

        if (focused) {
            // A popup rather than a reserved row: an icon with no label is a
            // guess, but a label that appears in the layout would shove the whole
            // bar sideways every time focus moved.
            Popup(
                alignment = Alignment.BottomCenter,
                offset = IntOffset(0, TOOLTIP_OFFSET_PX),
            ) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelMedium,
                    color = KaraokeColors.Background,
                    maxLines = 1,
                    modifier = Modifier
                        .background(KaraokeColors.OnSurface, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
    }
}

/** Just clear of the button, in raw pixels as Popup offsets require. */
private const val TOOLTIP_OFFSET_PX = 42

/** Kept so the bar can sit on its own tinted band, as in the reference layout. */
@Composable
fun TopBarBackground(modifier: Modifier = Modifier) {
    Spacer(modifier.background(KaraokeColors.Scrim))
}
