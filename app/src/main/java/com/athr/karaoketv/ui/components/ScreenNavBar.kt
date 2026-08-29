package com.athr.karaoketv.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Tv
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.athr.karaoketv.ui.theme.TvSpacing

/**
 * On-screen way back, on every screen below Home.
 *
 * BACK on the remote already does this, but a TV app cannot rely on that alone:
 * remotes differ, some boxes ship one without a clearly marked back key, and
 * guests handed the remote mid-party have no idea the gesture exists. "Xem video"
 * appears only while something is playing, because that is the one navigation
 * step with no obvious button — it hides the whole menu.
 */
@Composable
fun ScreenNavBar(
    onBack: () -> Unit,
    onHome: () -> Unit,
    onWatchVideo: (() -> Unit)?,
    modifier: Modifier = Modifier,
    takeInitialFocus: Boolean = true,
) {
    // Every screen below Home starts with focus here. Without an explicit target
    // Compose assigns focus on the first key press instead of before it, so the
    // viewer's first press on the remote does nothing at all.
    val backFocus = remember { FocusRequester() }
    if (takeInitialFocus) RequestInitialFocus(backFocus)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(
                start = TvSpacing.ScreenHorizontal,
                end = TvSpacing.ScreenHorizontal,
                top = 20.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TvButton(
            text = "Quay lại",
            onClick = onBack,
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            focusRequester = if (takeInitialFocus) backFocus else null,
        )
        TvButton(text = "Trang chủ", onClick = onHome, icon = Icons.Filled.Home)
        if (onWatchVideo != null) {
            TvButton(text = "Xem video", onClick = onWatchVideo, icon = Icons.Filled.Tv)
        }
    }
}
