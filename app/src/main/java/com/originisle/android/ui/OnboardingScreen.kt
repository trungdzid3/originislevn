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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * First-run gate: walks through every authorization the app needs before letting the user into the
 * main screen. Only notification access is mandatory to continue; the rest are strongly recommended
 * (battery, keep-alive, auto-start) and can be granted later from Home's ⋯ › Permissions.
 *
 * The startup row matters more than "recommended" suggests: with "Associated startup" off, nothing
 * brings casting back after a swipe-away — not START_STICKY, not the restart alarm, not the
 * accessibility service — and only a reboot recovers.
 */
@Composable
fun OnboardingScreen(context: Context, prefs: SharedPreferences, onDone: () -> Unit) {
    val tick = rememberResumeTick()

    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { tick.intValue++ }

    val notifGranted = remember(tick.intValue) { isListenerEnabled(context) }
    val postGranted = remember(tick.intValue) {
        Build.VERSION.SDK_INT < 33 ||
            androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    val batteryOk = remember(tick.intValue) { isBatteryUnrestricted(context) }
    val a11yOk = remember(tick.intValue) { isAccessibilityEnabled(context) }
    var autoStartAck by remember {
        mutableStateOf(prefs.getBoolean("onboarding_autostart_ack", false))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(IsleColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                HeroCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 22.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(G2RoundedCornerShape(16.dp))
                                .background(IsleColors.AccentSoft),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = IsleIcons.Pulse,
                                contentDescription = null,
                                tint = IsleColors.Accent,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Chào mừng đến với Origin Isle",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp,
                                lineHeight = 34.sp,
                                color = IsleColors.Text,
                            )
                            Text(
                                "Cần cấp một số quyền để có thể chuyển tiếp thông báo lên đảo linh hoạt và giữ dịch vụ chạy ổn định trên OriginOS.",
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = IsleColors.TextSub,
                            )
                        }
                    }
                }
            }
            item {
                GroupCard {
                    OnboardingRow(
                        icon = IsleIcons.Bell,
                        tile = IsleColors.TileBlue,
                        title = "Truy cập thông báo",
                        description = "Bắt buộc — cho phép Origin Isle đọc thông báo để hiển thị lên đảo.",
                        granted = notifGranted,
                        mandatory = true,
                    ) { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
                    GroupDivider()
                    OnboardingRow(
                        icon = IsleIcons.Bell,
                        tile = IsleColors.TilePurple,
                        title = "Cho phép thông báo",
                        description = "Cho phép hiển thị thông báo trạng thái của ứng dụng.",
                        granted = postGranted,
                        mandatory = false,
                    ) {
                        if (Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    GroupDivider()
                    OnboardingRow(
                        icon = IsleIcons.Battery,
                        tile = IsleColors.TileGreen,
                        title = "Không hạn chế pin",
                        description = "Ngăn OriginOS tự ý tắt ứng dụng chạy ngầm để tiết kiệm pin.",
                        granted = batteryOk,
                        mandatory = false,
                    ) { requestIgnoreBattery(context) }
                    GroupDivider()
                    OnboardingRow(
                        icon = IsleIcons.Power,
                        tile = IsleColors.TileAmber,
                        title = "Tự khởi chạy & Chạy nền",
                        description = "Bật CẢ HAI mục Tự khởi chạy & Khởi chạy liên kết trên OriginOS — không có mục này, vuốt đóng app sẽ không thể tự khôi phục.",
                        granted = autoStartAck,
                        mandatory = false,
                    ) {
                        // Only ack if a screen actually opened — a ✓ the user never saw is worse than
                        // no ✓ for the one setting that decides whether casting survives a swipe-away.
                        if (openAutoStartSettings(context)) {
                            autoStartAck = true
                            prefs.edit().putBoolean("onboarding_autostart_ack", true).apply()
                        }
                    }
                    GroupDivider()
                    OnboardingRow(
                        icon = IsleIcons.Shield,
                        tile = IsleColors.TileTeal,
                        title = "Hỗ trợ tiếp cận",
                        description = "Nhận diện lúc thoát Chrome, Edge, Play Store để tiến trình tải xuống tự chui vào Đảo.",
                        granted = a11yOk,
                        mandatory = false,
                    ) {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }
                    }
                }
            }
            item {
                PillButton(
                    text = if (notifGranted) "Tiếp tục" else "Vui lòng cấp quyền thông báo để tiếp tục",
                    onClick = {
                        prefs.edit().putBoolean("onboarding_done", true).apply()
                        onDone()
                    },
                    variant = PillButtonVariant.PRIMARY,
                    enabled = notifGranted,
                    modifier = Modifier.fillMaxWidth(),
                    height = 50.dp,
                )
            }
        }
    }
}

@Composable
private fun OnboardingRow(
    icon: ImageVector,
    tile: Color,
    title: String,
    description: String,
    granted: Boolean,
    mandatory: Boolean,
    onAction: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        IconTile(icon, tile)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    title,
                    modifier = Modifier.weight(1f, fill = false),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = IsleColors.Text,
                )
                if (mandatory) {
                    Text(
                        "Bắt buộc",
                        modifier = Modifier
                            .clip(G2RoundedCornerShape(10.dp))
                            .background(IsleColors.CastTagBg)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = IsleColors.CastTagFg,
                    )
                }
            }
            Text(description, fontSize = 13.sp, lineHeight = 18.sp, color = IsleColors.TextSub)
            if (!granted) {
                PillButton(
                    text = "Mở cài đặt",
                    onClick = onAction,
                    variant = PillButtonVariant.ALPHA_BLUE,
                    modifier = Modifier.padding(top = 8.dp),
                    height = 50.dp,
                )
            }
        }
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (granted) IsleColors.GoodIconBg else IsleColors.SurfaceHigh),
            contentAlignment = Alignment.Center,
        ) {
            if (granted) {
                Icon(IsleIcons.Check, "Đã cấp", Modifier.size(14.dp), tint = IsleColors.GoodText)
            }
        }
    }
}
