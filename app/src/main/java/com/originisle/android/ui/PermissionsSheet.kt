package com.originisle.android.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

/**
 * Everything the old Setup card held — the permission shortcuts, their live status and "Redo
 * first-run setup" — as a bottom sheet over Home, so it never pushes the main screen around.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsSheet(
    context: Context,
    prefs: SharedPreferences,
    onDismiss: () -> Unit,
    onRedoSetup: () -> Unit,
) {
    val tick = rememberResumeTick()
    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { tick.intValue++ }

    val listenerOk = remember(tick.intValue) { isListenerEnabled(context) }
    val postGranted = remember(tick.intValue) {
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    val batteryOk = remember(tick.intValue) { isBatteryUnrestricted(context) }
    val keepAliveOk = remember(tick.intValue) { isAccessibilityEnabled(context) }
    // Neither vivo toggle is readable, so all we can track is whether the user has been sent to that
    // screen — the same ack flag the onboarding row writes.
    var autoStartAck by remember { mutableStateOf(prefs.getBoolean("onboarding_autostart_ack", false)) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.statusBarsPadding(),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = IsleColors.Background,
        contentColor = IsleColors.Text,
        dragHandle = {
            Box(
                Modifier.padding(top = 10.dp, bottom = 6.dp).size(width = 36.dp, height = 5.dp)
                    .clip(CircleShape).background(IsleColors.SurfaceHigher),
            )
        },
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Quyền ứng dụng",
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
            )
            GroupCard {
                PermissionRow(
                    IsleIcons.Bell, IsleColors.TileBlue, "Truy cập thông báo",
                    ok = listenerOk, status = if (listenerOk) "Đã cấp" else "Chưa cấp — bắt buộc",
                ) { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
                GroupDivider()
                PermissionRow(
                    IsleIcons.Bell, IsleColors.TilePurple, "Thông báo Origin Isle",
                    ok = postGranted, status = if (postGranted) "Đã cho phép" else "Chưa cho phép",
                ) {
                    if (Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                GroupDivider()
                PermissionRow(
                    IsleIcons.Battery, IsleColors.TileGreen, "Tối ưu hóa pin",
                    ok = batteryOk, status = if (batteryOk) "Không giới hạn" else "Bị giới hạn — OriginOS có thể đóng app",
                ) { requestIgnoreBattery(context) }
                GroupDivider()
                PermissionRow(
                    IsleIcons.Heart, IsleColors.TileTeal, "Duy trì chạy ngầm (Trợ năng)",
                    ok = keepAliveOk,
                    status = if (keepAliveOk) "Đã bật — tự phục hồi sau khi bị đóng" else "Đang tắt — khôi phục chậm hơn",
                    note = "Bật \"Origin Isle keep-alive\" trong mục Trợ năng. Không hiện icon trạng thái, không đọc dữ liệu cá nhân.",
                ) { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                GroupDivider()
                PermissionRow(
                    IsleIcons.Power, IsleColors.TileAmber, "Tự khởi chạy & Khởi chạy liên kết",
                    ok = autoStartAck,
                    status = if (autoStartAck) "Đã mở cài đặt — hãy đảm bảo đã bật cả hai" else "Chưa xác nhận",
                    note = "Bật CẢ HAI mục, nếu không khi vuốt đóng đa nhiệm đảo sẽ dừng hoạt động cho đến khi khởi động lại máy.",
                ) {
                    if (openAutoStartSettings(context)) {
                        autoStartAck = true
                        prefs.edit().putBoolean("onboarding_autostart_ack", true).apply()
                    }
                }
            }
            PillButton(
                "Thiết lập lại từ đầu",
                onClick = onRedoSetup,
                modifier = Modifier.fillMaxWidth(),
                icon = IsleIcons.Refresh,
            )
        }
    }
}

/** Tile, title, coloured status line (+ optional note), and a check or chevron. Tapping opens the setting. */
@Composable
private fun PermissionRow(
    icon: ImageVector,
    tile: Color,
    title: String,
    ok: Boolean,
    status: String,
    note: String? = null,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        IconTile(icon, tile)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(
                status,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Medium,
                color = if (ok) IsleColors.GoodText else IsleColors.Warn,
            )
            if (note != null) {
                Text(note, fontSize = 12.sp, lineHeight = 16.sp, color = IsleColors.TextSecondary)
            }
        }
        if (ok) {
            Box(
                Modifier.size(26.dp).clip(CircleShape).background(IsleColors.GoodIconBg),
                contentAlignment = Alignment.Center,
            ) { Icon(IsleIcons.Check, "Done", Modifier.size(13.dp), tint = IsleColors.GoodText) }
        } else {
            Icon(IsleIcons.ChevronRight, null, Modifier.size(18.dp), tint = IsleColors.Chevron)
        }
    }
}

/** The one-line banner Home shows while notification access or battery is broken. */
@Composable
fun SetupAttentionBanner(onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(IsleColors.WarnBg)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 60.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        IconTile(IsleIcons.Shield, IsleColors.TileOrange)
        Column(Modifier.weight(1f)) {
            Text("Cài đặt cần lưu ý", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text("Đảo có thể không hoạt động ổn định", fontSize = 13.sp, color = IsleColors.Warn)
        }
        Icon(IsleIcons.ChevronRight, null, Modifier.size(18.dp), tint = IsleColors.Warn)
    }
}
