package com.athr.karaoketv.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.NavigationDrawer
import androidx.tv.material3.NavigationDrawerItem
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import com.athr.karaoketv.ui.ScanState
import com.athr.karaoketv.ui.UpdateState
import com.athr.karaoketv.ui.components.Pill
import com.athr.karaoketv.ui.components.TvButton
import com.athr.karaoketv.ui.components.fullWidthRowScale
import com.athr.karaoketv.ui.components.karaokeListItemColors
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
    var section by remember { mutableStateOf(SettingsSection.LIBRARY) }

    NavigationDrawer(
        modifier = modifier.fillMaxSize(),
        drawerContent = { drawerValue ->
            Column(
                Modifier
                    .fillMaxHeight()
                    .padding(start = 12.dp, top = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SettingsSection.entries.forEach { entry ->
                    if (entry == SettingsSection.YOUTUBE && !youTubeAvailable) return@forEach
                    NavigationDrawerItem(
                        selected = entry == section,
                        onClick = { section = entry },
                        leadingContent = {
                            Icon(entry.icon, contentDescription = null)
                        },
                    ) {
                        Text(entry.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(
                    start = 24.dp,
                    end = TvSpacing.ScreenHorizontal,
                    top = 12.dp,
                    bottom = TvSpacing.ScreenVertical,
                )
        ) {
            Text(
                text = section.label,
                style = MaterialTheme.typography.headlineLarge,
                color = KaraokeColors.OnSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = section.blurb,
                style = MaterialTheme.typography.bodyLarge,
                color = KaraokeColors.Muted,
            )
            Spacer(Modifier.height(20.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 48.dp),
            ) {
                when (section) {
                    SettingsSection.LIBRARY -> {
                        item {
                            Column {
                                Text(
                                    text = if (songCount > 0) {
                                        "${formatCount(songCount)} bài · $libraryLabel"
                                    } else {
                                        "Chưa quét được bài nào"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = KaraokeColors.OnSurface,
                                )
                                ScanLine(scanState)
                                Spacer(Modifier.height(14.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    TvButton("Đổi thư mục", onChangeLibrary, emphasised = true)
                                    TvButton("Quét lại ổ cứng", onRescan)
                                }
                                Spacer(Modifier.height(6.dp))
                            }
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

                    SettingsSection.HOME -> item {
                        SettingRow(
                            title = "Bố cục màn hình chính",
                            description = "Chọn hàng nào hiện và theo thứ tự nào",
                            value = "Sắp xếp",
                            highlighted = false,
                            onClick = onOpenHomeLayout,
                        )
                    }

                    SettingsSection.PLAYBACK -> {
                        item {
                            SettingRow(
                                title = "Tự động hát bài kế",
                                description = "Khi hết bài, tự chuyển sang bài đầu hàng chờ",
                                value = if (autoNext) "Bật" else "Tắt",
                                highlighted = autoNext,
                                checked = autoNext,
                                onClick = onToggleAutoNext,
                            )
                        }
                        item {
                            SettingRow(
                                title = "Báo bài tiếp theo",
                                description = "Hiện tên bài kế 25 giây trước khi hết bài",
                                value = if (nextUpBanner) "Bật" else "Tắt",
                                highlighted = nextUpBanner,
                                checked = nextUpBanner,
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
                                value = if (pitchSemitones == 0) {
                                    "Gốc"
                                } else {
                                    "${if (pitchSemitones > 0) "+" else ""}$pitchSemitones"
                                },
                                highlighted = pitchSemitones != 0,
                                onClick = onResetPitch,
                            )
                        }
                    }

                    SettingsSection.YOUTUBE -> item {
                        SettingRow(
                            title = "Thêm \"karaoke\" vào từ khoá",
                            description = "Gõ \"gần như là\" sẽ tìm \"gần như là karaoke\". " +
                                "Bài mở bằng app YouTube — đăng nhập Premium ở đó thì không quảng cáo.",
                            value = if (appendKaraokeToYouTube) "Bật" else "Tắt",
                            highlighted = appendKaraokeToYouTube,
                            checked = appendKaraokeToYouTube,
                            onClick = onToggleYouTubeKeyword,
                        )
                    }

                    SettingsSection.APP -> item {
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
                }
            }
        }
    }
}

/**
 * Settings split into destinations down the left, their controls on the right.
 *
 * This is what the navigation drawer is for — Google's guidance puts it at three to
 * seven app destinations, and five sections fit that exactly. It also means no
 * section is more than one press deep, instead of a single column people scroll
 * past four unrelated things to reach the one they wanted.
 */
private enum class SettingsSection(
    val label: String,
    val blurb: String,
    val icon: ImageVector,
) {
    LIBRARY("Thư viện", "Ổ cứng đang dùng và việc quét bài", Icons.Filled.Folder),
    HOME("Màn hình chính", "Những hàng hiện ở trang chủ", Icons.Filled.Home),
    PLAYBACK("Phát nhạc", "Cách bài hát chạy và hiển thị", Icons.Filled.PlayArrow),
    YOUTUBE("YouTube", "Khi tìm bài trên app YouTube của box", Icons.Filled.PlayCircleOutline),
    APP("Ứng dụng", "Phiên bản và cập nhật", Icons.Filled.Info),
}

@Composable
private fun ScanLine(state: ScanState) {
    val text = when (state) {
        is ScanState.Idle -> null
        is ScanState.Running -> "Đang quét… ${formatCount(state.filesFound)} bài"
        is ScanState.Done -> "Quét xong: ${formatCount(state.totalSongs)} bài"
        is ScanState.Failed -> state.message
    } ?: return
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (state is ScanState.Failed) KaraokeColors.Danger else KaraokeColors.Accent,
    )
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
    /** Non-null renders the design system's Switch instead of a text badge. */
    checked: Boolean? = null,
) {
    ListItem(
        selected = false,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = karaokeListItemColors(),
        scale = fullWidthRowScale(),
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (danger) KaraokeColors.Danger else Color.Unspecified,
            )
        },
        supportingContent = {
            Text(text = description, style = MaterialTheme.typography.bodyMedium)
        },
        trailingContent = {
            if (checked != null) {
                Switch(checked = checked, onCheckedChange = { onClick() })
            } else {
                Pill(
                    text = value,
                    color = when {
                        danger -> KaraokeColors.Danger
                        highlighted -> KaraokeColors.Success
                        else -> KaraokeColors.Accent
                    },
                )
            }
        },
    )
}
