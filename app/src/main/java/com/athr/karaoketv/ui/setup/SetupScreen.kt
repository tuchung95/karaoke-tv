package com.athr.karaoketv.ui.setup

import android.Manifest
import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Usb
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.athr.karaoketv.data.library.StorageAccess
import com.athr.karaoketv.data.library.StorageVolumes
import com.athr.karaoketv.ui.ScanState
import com.athr.karaoketv.ui.components.RequestInitialFocus
import com.athr.karaoketv.ui.components.TvButton
import androidx.tv.material3.ListItem
import com.athr.karaoketv.ui.components.karaokeListItemColors
import com.athr.karaoketv.ui.theme.KaraokeColors
import com.athr.karaoketv.ui.theme.TvSpacing
import com.athr.karaoketv.util.formatCount

/**
 * First run, and the place people come back to when they swap drives.
 *
 * Which route works depends entirely on the box. Plenty of TV boxes ship without
 * a system folder picker, and from Android 11 the File API cannot reach a USB
 * drive without All-files access — so the screen asks what is actually available
 * and only offers that, instead of presenting a button that silently does nothing.
 */
@Composable
fun SetupScreen(
    scanState: ScanState,
    currentLabel: String,
    songCount: Int,
    onTreePicked: (Uri, String) -> Unit,
    onDirectPicked: (String, String) -> Unit,
    onRescan: () -> Unit,
    onDone: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val firstFocus = remember { FocusRequester() }
    var pickerError by remember { mutableStateOf<String?>(null) }
    var accessNonce by remember { mutableStateOf(0) }

    val volumes = remember { StorageVolumes.list(context) }
    val pickerAvailable = remember { StorageAccess.documentPickerAvailable(context) }
    val allFilesIntent = remember { StorageAccess.allFilesAccessIntent(context) }
    // Re-read after the viewer comes back from a permission screen.
    val fileAccess = remember(accessNonce) { StorageAccess.hasFileSystemAccess(context) }
    val needsAllFiles = remember(accessNonce) { StorageAccess.needsAllFilesAccess(context) }

    RequestInitialFocus(firstFocus)

    val treePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val label = uri.lastPathSegment?.substringAfterLast(':')?.ifBlank { null }
            onTreePicked(uri, label ?: "Ổ đã chọn")
        }
    }

    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { accessNonce++ }

    val legacyPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { accessNonce++ }

    fun openTreePicker(initial: Uri?) {
        pickerError = null
        try {
            treePicker.launch(initial)
        } catch (e: ActivityNotFoundException) {
            pickerError = "Máy không có trình chọn thư mục của hệ thống."
        }
    }

    fun requestFileAccess() {
        pickerError = null
        when {
            allFilesIntent != null -> settingsLauncher.launch(allFilesIntent)
            !needsAllFiles -> legacyPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            else -> pickerError = "Máy không có màn hình cấp quyền truy cập tệp."
        }
    }

    fun chooseVolume(volume: StorageVolumes.Volume) {
        pickerError = null
        when {
            fileAccess -> onDirectPicked(volume.path, volume.label)
            pickerAvailable -> openTreePicker(StorageVolumes.documentInitialUri(volume.path))
            else -> requestFileAccess()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KaraokeColors.Background)
            .padding(
                horizontal = TvSpacing.ScreenHorizontal,
                vertical = TvSpacing.ScreenVertical,
            ),
    ) {
        Text(
            text = "Thư viện karaoke",
            style = MaterialTheme.typography.displayMedium,
            color = KaraokeColors.Primary,
        )
        Text(
            text = if (currentLabel.isBlank()) {
                "Chọn thư mục chứa video karaoke trên ổ cứng"
            } else {
                "Đang dùng: $currentLabel · ${formatCount(songCount)} bài"
            },
            style = MaterialTheme.typography.titleMedium,
            color = KaraokeColors.Muted,
        )

        Spacer(Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            var focusTaken = false
            if (pickerAvailable) {
                TvButton(
                    text = "Chọn thư mục…",
                    icon = Icons.Filled.FolderOpen,
                    emphasised = true,
                    focusRequester = firstFocus,
                    onClick = { openTreePicker(null) },
                )
                focusTaken = true
            }
            if (!fileAccess) {
                TvButton(
                    text = "Cấp quyền đọc ổ cứng",
                    icon = Icons.Filled.Lock,
                    emphasised = !focusTaken,
                    focusRequester = if (focusTaken) null else firstFocus,
                    onClick = { requestFileAccess() },
                )
                focusTaken = true
            }
            if (currentLabel.isNotBlank()) {
                TvButton(
                    text = "Quét lại",
                    icon = Icons.Filled.Refresh,
                    focusRequester = if (focusTaken) null else firstFocus,
                    onClick = onRescan,
                )
                focusTaken = true
            }
            if (onDone != null) {
                TvButton(
                    text = "Xong",
                    focusRequester = if (focusTaken) null else firstFocus,
                    onClick = onDone,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = accessAdvice(pickerAvailable, fileAccess, needsAllFiles, allFilesIntent != null),
            style = MaterialTheme.typography.bodyLarge,
            color = KaraokeColors.Muted,
        )

        if (pickerError != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = pickerError!!,
                style = MaterialTheme.typography.bodyLarge,
                color = KaraokeColors.Danger,
            )
        }

        Spacer(Modifier.height(24.dp))
        ScanStatus(scanState)
        Spacer(Modifier.height(24.dp))

        Text(
            text = "Ổ đĩa tìm thấy trên máy",
            style = MaterialTheme.typography.titleLarge,
            color = KaraokeColors.OnSurface,
        )
        Spacer(Modifier.height(12.dp))

        if (volumes.isEmpty()) {
            Text(
                text = "Không thấy ổ nào. Cắm ổ cứng rồi mở lại màn hình này.",
                style = MaterialTheme.typography.bodyLarge,
                color = KaraokeColors.Muted,
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 48.dp),
            ) {
                items(volumes, key = { it.path }) { volume ->
                    ListItem(
                        selected = false,
                        onClick = { chooseVolume(volume) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = karaokeListItemColors(),
                        leadingContent = {
                            Icon(Icons.Filled.Usb, contentDescription = null)
                        },
                        headlineContent = {
                            Text(
                                text = volume.label,
                                style = MaterialTheme.typography.titleMedium,
                            )
                        },
                        supportingContent = {
                            Text(
                                text = volume.path,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                    )
                }
            }
        }
    }
}

private fun accessAdvice(
    pickerAvailable: Boolean,
    fileAccess: Boolean,
    needsAllFiles: Boolean,
    hasSettingsScreen: Boolean,
): String = when {
    fileAccess -> "Chọn một ổ ở dưới để quét."
    !pickerAvailable && needsAllFiles && hasSettingsScreen ->
        "Máy này không có trình chọn thư mục. Bấm \"Cấp quyền đọc ổ cứng\", bật " +
            "Karaoke TV trong danh sách, rồi quay lại đây."
    !pickerAvailable && !hasSettingsScreen ->
        "Máy này không có trình chọn thư mục lẫn màn hình cấp quyền. Cần cài thêm " +
            "một trình quản lý file có hỗ trợ, hoặc cấp quyền qua adb."
    needsAllFiles ->
        "Chọn thư mục qua trình chọn của hệ thống, hoặc cấp quyền đọc ổ cứng để quét " +
            "thẳng — cách này nhanh hơn với thư viện lớn."
    else -> "Chọn thư mục, hoặc cấp quyền rồi chọn ổ ở dưới."
}

@Composable
private fun ScanStatus(state: ScanState) {
    when (state) {
        is ScanState.Idle -> Text(
            text = "Sẵn sàng quét.",
            style = MaterialTheme.typography.bodyLarge,
            color = KaraokeColors.Muted,
        )
        is ScanState.Running -> Column {
            Text(
                text = "Đang quét… ${formatCount(state.filesFound)} bài",
                style = MaterialTheme.typography.headlineMedium,
                color = KaraokeColors.Accent,
            )
            Text(
                text = state.folder,
                style = MaterialTheme.typography.bodyMedium,
                color = KaraokeColors.Muted,
                maxLines = 1,
            )
        }
        is ScanState.Done -> Text(
            text = "Xong: ${formatCount(state.totalSongs)} bài trong ${state.elapsedMs / 1000}s" +
                if (state.removed > 0) " · gỡ ${state.removed} bài không còn trên ổ" else "",
            style = MaterialTheme.typography.headlineMedium,
            color = KaraokeColors.Success,
        )
        is ScanState.Failed -> Text(
            text = state.message,
            style = MaterialTheme.typography.titleMedium,
            color = KaraokeColors.Danger,
        )
    }
}
