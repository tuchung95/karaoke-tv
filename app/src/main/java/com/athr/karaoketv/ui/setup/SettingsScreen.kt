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
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.athr.karaoketv.ui.ScanState
import com.athr.karaoketv.ui.UpdateState
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
    appendKaraokeToYouTube: Boolean,
    youTubeAvailable: Boolean,
    currentVersion: String,
    updateState: UpdateState,
    scaleModeLabel: String,
    pitchSemitones: Int,
    onChangeLibrary: () -> Unit,
    onRescan: () -> Unit,
    onToggleAutoNext: () -> Unit,
    onToggleNextUpBanner: () -> Unit,
    onToggleYouTubeKeyword: () -> Unit,
    onCycleScale: () -> Unit,
    onResetPitch: () -> Unit,
    onClearLibrary: () -> Unit,
    onUpdateAction: () -> Unit,
    onOpenHomeLayout: () -> Unit,
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
                title = "Bố cục màn hình chính",
                description = "Chọn hàng nào hiện và theo thứ tự nào",
                value = "Sắp xếp",
                highlighted = false,
                onClick = onOpenHomeLayout,
            )
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
        if (youTubeAvailable) {
            item {
                SettingRow(
                    title = "Thêm \"karaoke\" khi tìm trên YouTube",
                    description = "Gõ \"gần như là\" sẽ tìm \"gần như là karaoke\". " +
                        "Bài mở bằng app YouTube — đăng nhập Premium ở đó thì không quảng cáo.",
                    value = if (appendKaraokeToYouTube) "Bật" else "Tắt",
                    highlighted = appendKaraokeToYouTube,
                    onClick = onToggleYouTubeKeyword,
                )
            }
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
                title = "Phiên bản $currentVersion",
                description = updateDescription(updateState),
                value = updateAction(updateState),
                highlighted = updateState is UpdateState.Available ||
                    updateState is UpdateState.Ready ||
                    updateState is UpdateState.InstallManually,
                onClick = onUpdateAction,
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

private fun updateDescription(state: UpdateState): String = when (state) {
    is UpdateState.Idle -> "Tải bản mới từ GitHub và cài đè lên bản đang dùng"
    is UpdateState.Checking -> "Đang kiểm tra…"
    is UpdateState.UpToDate -> "Đang dùng bản mới nhất"
    is UpdateState.Available ->
        "Có bản ${state.release.versionName}" +
            state.release.notes.lineSequence().firstOrNull()
                ?.takeIf { it.isNotBlank() }
                ?.let { " — $it" }
                .orEmpty()
    is UpdateState.Downloading -> "Đang tải… ${state.percent}%"
    is UpdateState.Ready -> "Đã tải bản ${state.versionName}, bấm để cài"
    is UpdateState.InstallManually ->
        "Máy không có trình cài đặt. Mở file này bằng trình quản lý file: ${state.path}"
    is UpdateState.Failed -> state.message
}

private fun updateAction(state: UpdateState): String = when (state) {
    is UpdateState.Idle -> "Kiểm tra"
    is UpdateState.Checking -> "…"
    is UpdateState.UpToDate -> "Mới nhất"
    is UpdateState.Available -> "Tải về"
    is UpdateState.Downloading -> "${state.percent}%"
    is UpdateState.Ready -> "Cài đặt"
    is UpdateState.InstallManually -> "Xem đường dẫn"
    is UpdateState.Failed -> "Thử lại"
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
