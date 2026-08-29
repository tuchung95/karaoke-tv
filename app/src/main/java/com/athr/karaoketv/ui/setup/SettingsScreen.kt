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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.athr.karaoketv.ui.ScanState
import com.athr.karaoketv.ui.components.Pill
import com.athr.karaoketv.ui.components.TvButton
import com.athr.karaoketv.ui.components.TvFocusable
import com.athr.karaoketv.ui.theme.KaraokeColors
import com.athr.karaoketv.ui.theme.TvSpacing
import com.athr.karaoketv.util.formatCount

@Composable
fun SettingsScreen(
    libraryLabel: String,
    songCount: Int,
    scanState: ScanState,
    autoNext: Boolean,
    nextUpBanner: Boolean,
    scaleModeLabel: String,
    pitchSemitones: Int,
    onChangeLibrary: () -> Unit,
    onRescan: () -> Unit,
    onToggleAutoNext: () -> Unit,
    onToggleNextUpBanner: () -> Unit,
    onCycleScale: () -> Unit,
    onResetPitch: () -> Unit,
    onClearLibrary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = TvSpacing.ScreenHorizontal,
            end = TvSpacing.ScreenHorizontal,
            top = TvSpacing.ScreenVertical,
            bottom = 64.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text(
                    text = "Cài đặt",
                    style = MaterialTheme.typography.headlineLarge,
                    color = KaraokeColors.OnSurface,
                )
                Text(
                    text = "${formatCount(songCount)} bài · ${libraryLabel.ifBlank { "chưa chọn ổ" }}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = KaraokeColors.Muted,
                )
                if (scanState is ScanState.Running) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Đang quét lại… ${formatCount(scanState.filesFound)} bài",
                        style = MaterialTheme.typography.titleMedium,
                        color = KaraokeColors.Accent,
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TvButton("Đổi thư mục", onChangeLibrary, emphasised = true)
                    TvButton("Quét lại ổ cứng", onRescan)
                }
            }
        }

        item {
            SettingRow(
                title = "Tự động hát bài kế",
                description = "Khi hết bài, tự chuyển sang bài đầu hàng chờ",
                value = if (autoNext) "Bật" else "Tắt",
                highlighted = autoNext,
                onClick = onToggleAutoNext,
            )
        }
        item {
            SettingRow(
                title = "Báo bài tiếp theo",
                description = "Hiện tên bài kế 25 giây trước khi hết bài",
                value = if (nextUpBanner) "Bật" else "Tắt",
                highlighted = nextUpBanner,
                onClick = onToggleNextUpBanner,
            )
        }
        item {
            SettingRow(
                title = "Tỉ lệ khung hình",
                description = "Dùng khi video 4:3 bị viền đen trên TV 16:9",
                value = scaleModeLabel,
                highlighted = false,
                onClick = onCycleScale,
            )
        }
        item {
            SettingRow(
                title = "Tông mặc định",
                description = "Tông được giữ nguyên giữa các bài",
                value = if (pitchSemitones == 0) "Gốc" else "${if (pitchSemitones > 0) "+" else ""}$pitchSemitones",
                highlighted = pitchSemitones != 0,
                onClick = onResetPitch,
            )
        }
        item {
            SettingRow(
                title = "Xóa dữ liệu thư viện",
                description = "Xóa danh mục đã quét, không đụng tới file trên ổ cứng",
                value = "Xóa",
                highlighted = false,
                danger = true,
                onClick = onClearLibrary,
            )
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    description: String,
    value: String,
    highlighted: Boolean,
    onClick: () -> Unit,
    danger: Boolean = false,
) {
    TvFocusable(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 18.dp),
    ) { _ ->
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (danger) KaraokeColors.Danger else KaraokeColors.OnSurface,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = KaraokeColors.Muted,
                )
            }
            Spacer(Modifier.width(16.dp))
            Pill(
                text = value,
                color = when {
                    danger -> KaraokeColors.Danger
                    highlighted -> KaraokeColors.Success
                    else -> KaraokeColors.Accent
                },
            )
        }
    }
}
