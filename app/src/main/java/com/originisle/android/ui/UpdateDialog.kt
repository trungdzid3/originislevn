package com.originisle.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

/**
 * Trạng thái của quá trình tải và cài đặt bản cập nhật
 */
private sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progress: Int, val downloadedBytes: Long, val totalBytes: Long) : DownloadState()
    data class Downloaded(val file: File) : DownloadState()
    data class Failed(val error: String) : DownloadState()
}

/**
 * Hộp thoại cập nhật phiên bản mới thiết kế chuẩn OriginUI ROM 16.0 (Vivo OriginOS 6).
 *
 * - Khung Dialog bo góc G2 30.dp, nền IsleColors.Surface, không bóng đổ thô.
 * - Header: "Cập nhật phiên bản mới" 20.sp Bold kèm badge `v[current] -> v[versionName]`.
 * - Changelog Card: Bo góc G2 18.dp, nền chip (IsleColors.SurfaceHigher), có thanh cuộn.
 * - Nút hành động 50.dp: PillButton ALPHA_GRAY ("Để sau") và PRIMARY ("Cập nhật ngay" / "Cài đặt lại").
 */
@Composable
fun UpdateDialog(
    release: UpdateChecker.ReleaseInfo,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var downloadState by remember { mutableStateOf<DownloadState>(DownloadState.Idle) }

    val currentVersion = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0"
    }

    val isDownloading = downloadState is DownloadState.Downloading

    Dialog(
        onDismissRequest = {
            if (!isDownloading) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = !isDownloading,
            dismissOnClickOutside = !isDownloading,
            usePlatformDefaultWidth = false,
        ),
    ) {
        val dialogShape = G2RoundedCornerShape(30.dp)
        val badgeShape = G2RoundedCornerShape(10.dp)
        val changelogShape = G2RoundedCornerShape(18.dp)
        val isDark = IsleColors.isDark

        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 460.dp)
                .clip(dialogShape)
                .then(
                    if (isDark) {
                        Modifier.border(
                            width = 0.8.dp,
                            brush = Brush.verticalGradient(listOf(IsleColors.EdgeLightTop, IsleColors.EdgeLightBottom)),
                            shape = dialogShape,
                        )
                    } else {
                        Modifier
                    },
                ),
            shape = dialogShape,
            color = IsleColors.Surface,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 1. Tiêu đề & Phiên bản Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    IconTile(
                        icon = IsleIcons.Download,
                        color = IsleColors.Accent,
                        size = 42.dp,
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Text(
                            text = "Cập nhật phiên bản mới",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp,
                            color = IsleColors.Text,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(badgeShape)
                                    .background(IsleColors.AccentSoft)
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                            ) {
                                Text(
                                    text = "v$currentVersion -> v${release.versionName}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = IsleColors.Accent,
                                )
                            }
                            if (release.title.isNotBlank() && release.title != release.versionName && release.title != "v${release.versionName}") {
                                Text(
                                    text = release.title,
                                    fontSize = 12.sp,
                                    color = IsleColors.TextSub,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }

                // 2. Thẻ hiển thị Nhật ký thay đổi (Changelog Card)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Nội dung cập nhật",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = IsleColors.TextSub,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 90.dp, max = 220.dp)
                            .clip(changelogShape)
                            .background(IsleColors.SurfaceHigher)
                            .padding(14.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Text(
                            text = release.changelog.ifBlank {
                                "Bản cập nhật tối ưu hóa hiệu năng, cải tiến giao diện Đảo Động và sửa các lỗi phát sinh."
                            },
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = IsleColors.Text,
                        )
                    }
                }

                // 3. Trạng thái tải & Tiến độ
                when (val state = downloadState) {
                    is DownloadState.Idle -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Dung lượng tải về",
                                fontSize = 13.sp,
                                color = IsleColors.TextSub,
                            )
                            Text(
                                text = formatFileSize(release.sizeBytes),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = IsleColors.Text,
                            )
                        }
                    }
                    is DownloadState.Downloading -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val progressFraction = (state.progress / 100f).coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = IsleColors.Accent,
                                trackColor = IsleColors.SurfaceHigher,
                            )
                            val total = if (state.totalBytes > 0) state.totalBytes else release.sizeBytes
                            Text(
                                text = "Đang tải... ${state.progress}% (${formatFileSize(state.downloadedBytes)} / ${formatFileSize(total)})",
                                fontSize = 12.sp,
                                color = IsleColors.Link,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                    is DownloadState.Downloaded -> {
                        VTipCard(
                            title = "Đã tải xong tệp cập nhật (v${release.versionName})",
                            body = "Hệ thống đang mở trình cài đặt gói. Nếu bị gián đoạn, bấm \"Cài đặt lại\".",
                            icon = IsleIcons.Check,
                            iconTint = IsleColors.GoodText,
                            containerColor = IsleColors.GoodBg,
                        )
                    }
                    is DownloadState.Failed -> {
                        VTipCard(
                            title = "Không thể tải bản cập nhật",
                            body = state.error,
                            icon = IsleIcons.Close,
                            iconTint = IsleColors.BadText,
                            containerColor = IsleColors.BadBg,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(IsleColors.Divider),
                )

                // 4. Hai nút bấm bo tròn chuẩn OriginUI ROM 16.0 (50.dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PillButton(
                        text = "Để sau",
                        onClick = onDismiss,
                        variant = PillButtonVariant.ALPHA_GRAY,
                        enabled = !isDownloading,
                        modifier = Modifier.weight(1f),
                        height = 50.dp,
                    )

                    val primaryButtonText = when (downloadState) {
                        is DownloadState.Idle -> "Cập nhật ngay"
                        is DownloadState.Downloading -> "Đang tải..."
                        is DownloadState.Downloaded -> "Cài đặt lại"
                        is DownloadState.Failed -> "Thử lại"
                    }

                    PillButton(
                        text = primaryButtonText,
                        onClick = {
                            if (isDownloading) return@PillButton
                            when (val state = downloadState) {
                                is DownloadState.Downloaded -> {
                                    UpdateChecker.installApk(context, state.file)
                                }
                                else -> {
                                    coroutineScope.launch {
                                        downloadState = DownloadState.Downloading(0, 0, release.sizeBytes)
                                        val apk = UpdateChecker.downloadApk(context, release) { p, downloaded, total ->
                                            downloadState = DownloadState.Downloading(p, downloaded, total)
                                        }
                                        if (apk != null && apk.exists()) {
                                            downloadState = DownloadState.Downloaded(apk)
                                            UpdateChecker.installApk(context, apk)
                                        } else {
                                            downloadState = DownloadState.Failed("Tải xuống thất bại. Vui lòng kiểm tra lại kết nối mạng.")
                                        }
                                    }
                                }
                            }
                        },
                        variant = PillButtonVariant.PRIMARY,
                        enabled = !isDownloading,
                        modifier = Modifier.weight(1.3f),
                        height = 50.dp,
                    )
                }
            }
        }
    }
}

/**
 * Định dạng dung lượng tệp từ byte sang MB (ví dụ: 69.0 MB)
 */
private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "Không xác định"
    val mb = bytes.toDouble() / (1024.0 * 1024.0)
    return String.format(Locale.US, "%.1f MB", mb)
}
