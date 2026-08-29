package com.athr.karaoketv.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.athr.karaoketv.ui.UpdateState
import com.athr.karaoketv.ui.components.Pill
import com.athr.karaoketv.ui.components.RequestInitialFocus
import com.athr.karaoketv.ui.components.TvButton
import com.athr.karaoketv.ui.theme.KaraokeColors

/**
 * Stands in front of the app when a newer release exists.
 *
 * It comes back on every launch until the box is actually updated, which is the
 * point — a karaoke box lives in a cabinet and nobody goes looking for a settings
 * screen. What it will not do is lock the room out: some TV boxes have no package
 * installer at all, and on those the only honest move is to show where the file
 * landed and let people carry on singing.
 */
@Composable
fun UpdateGate(
    state: UpdateState,
    currentVersion: String,
    onAct: () -> Unit,
    onDismiss: () -> Unit,
) {
    val firstFocus = remember { FocusRequester() }
    RequestInitialFocus(firstFocus, key = state::class)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KaraokeColors.Scrim),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(720.dp)
                .background(KaraokeColors.Surface, RoundedCornerShape(20.dp))
                .padding(36.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill("BẢN MỚI", color = KaraokeColors.Accent)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Đang dùng $currentVersion",
                    style = MaterialTheme.typography.labelLarge,
                    color = KaraokeColors.Muted,
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = headline(state),
                style = MaterialTheme.typography.headlineLarge,
                color = KaraokeColors.OnSurface,
            )
            val detail = detail(state)
            if (detail != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodyLarge,
                    color = KaraokeColors.Muted,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val action = actionLabel(state)
                if (action != null) {
                    TvButton(
                        text = action,
                        onClick = onAct,
                        icon = Icons.Filled.SystemUpdate,
                        emphasised = true,
                        focusRequester = firstFocus,
                    )
                }
                TvButton(
                    text = "Để sau",
                    onClick = onDismiss,
                    focusRequester = if (action == null) firstFocus else null,
                )
            }
        }
    }
}

private fun headline(state: UpdateState): String = when (state) {
    is UpdateState.Available -> "Có bản ${state.release.versionName}"
    is UpdateState.Downloading -> "Đang tải bản mới… ${state.percent}%"
    is UpdateState.Ready -> "Đã tải xong bản ${state.versionName}"
    is UpdateState.InstallManually -> "Cần cài bằng tay"
    is UpdateState.Failed -> "Không cập nhật được"
    else -> "Đang kiểm tra…"
}

private fun detail(state: UpdateState): String? = when (state) {
    is UpdateState.Available -> state.release.notes.takeIf { it.isNotBlank() }
    is UpdateState.Ready -> "Bấm cài đặt rồi làm theo hướng dẫn của máy."
    is UpdateState.InstallManually ->
        "Máy này không có trình cài đặt APK. Mở file này bằng trình quản lý " +
            "file để cài: ${state.path}"
    is UpdateState.Failed -> state.message
    else -> null
}

private fun actionLabel(state: UpdateState): String? = when (state) {
    is UpdateState.Available -> "Cập nhật ngay"
    is UpdateState.Ready -> "Cài đặt"
    is UpdateState.Failed -> "Thử lại"
    is UpdateState.Downloading -> null
    else -> null
}
