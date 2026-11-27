package com.originisle.android.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.originisle.android.cards.EarbudsCard
import com.originisle.android.cards.FlashlightCard
import com.originisle.android.service.NotificationCastListener

/**
 * Tab 2 — Tính năng Đảo theo chuẩn OriginUI ROM 16.0:
 * 1. Ứng dụng & Tiện ích VN (Zalo Call, Đặt xe & Giao đồ ăn, Ngân hàng VN & Thanh toán, Chuỗi TikTok)
 * 2. Giải trí & Kết nối (Nhạc bên thứ 3 & Bảng điều khiển nổi, Tai nghe & Popup Bluetooth)
 * 3. Hệ thống & Tiện ích tự làm (Tải xuống & Quick Share, Lịch hẹn, Đèn pin, Chế độ Chuông/Rung)
 */
@Composable
fun ModulesScreen(
    context: Context,
    prefs: SharedPreferences,
) {
    var castOn by remember { mutableStateOf(prefs.getBoolean("cast_notifications", true)) }

    // Nhóm 1: Ứng dụng & Tiện ích VN
    var zaloCallOn by remember { mutableStateOf(prefs.getBoolean("cast_zalo_call", true)) }
    var ridesOn by remember { mutableStateOf(prefs.getBoolean("cast_rides", true)) }
    var paymentOn by remember {
        mutableStateOf(prefs.getBoolean("cast_payment", prefs.getBoolean("cast_vietnam_banks", true)))
    }
    var tiktokStreakOn by remember { mutableStateOf(prefs.getBoolean("cast_tiktok_streak", true)) }
    var tiktokStreakMessage by remember {
        mutableStateOf(prefs.getString("tiktok_streak_custom_message", "\uD83D\uDD25") ?: "\uD83D\uDD25")
    }

    // Nhóm 2: Giải trí & Kết nối
    var mediaOn by remember { mutableStateOf(prefs.getBoolean("cast_media_sessions", true)) }
    var mediaOverlayExpand by remember { mutableStateOf(prefs.getBoolean(PREF_MEDIA_OVERLAY_EXPAND, true)) }
    var earbudsOn by remember {
        mutableStateOf(prefs.getBoolean("cast_earbuds", true) && EarbudsCard.hasPermission(context))
    }
    var btConnectPopup by remember { mutableStateOf(prefs.getBoolean(PREF_BT_CONNECT_POPUP, false)) }
    var btPopupSheetOpen by remember { mutableStateOf(false) }

    // Nhóm 3: Hệ thống & Tiện ích tự làm
    var downloadOn by remember { mutableStateOf(prefs.getBoolean("cast_downloads", true)) }
    var castCalendarEvents by remember { mutableStateOf(prefs.getBoolean("cast_calendar_events", true)) }
    var flashlightOn by remember { mutableStateOf(prefs.getBoolean("cast_flashlight", true)) }
    var ringerModeOn by remember { mutableStateOf(prefs.getBoolean("cast_ringer_mode", true)) }

    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
            when (key) {
                "cast_notifications" -> castOn = sp.getBoolean("cast_notifications", true)
                "cast_zalo_call" -> zaloCallOn = sp.getBoolean("cast_zalo_call", true)
                "cast_rides" -> ridesOn = sp.getBoolean("cast_rides", true)
                "cast_payment", "cast_vietnam_banks" -> {
                    paymentOn = sp.getBoolean("cast_payment", sp.getBoolean("cast_vietnam_banks", true))
                }
                "cast_tiktok_streak" -> tiktokStreakOn = sp.getBoolean("cast_tiktok_streak", true)
                "tiktok_streak_custom_message" -> {
                    tiktokStreakMessage = sp.getString("tiktok_streak_custom_message", "\uD83D\uDD25") ?: "\uD83D\uDD25"
                }
                "cast_media_sessions" -> mediaOn = sp.getBoolean("cast_media_sessions", true)
                PREF_MEDIA_OVERLAY_EXPAND -> mediaOverlayExpand = sp.getBoolean(PREF_MEDIA_OVERLAY_EXPAND, true)
                PREF_BT_CONNECT_POPUP -> btConnectPopup = sp.getBoolean(PREF_BT_CONNECT_POPUP, false)
                "cast_downloads" -> downloadOn = sp.getBoolean("cast_downloads", true)
                "cast_calendar_events" -> castCalendarEvents = sp.getBoolean("cast_calendar_events", true)
                "cast_flashlight" -> flashlightOn = sp.getBoolean("cast_flashlight", true)
                "cast_ringer_mode" -> ringerModeOn = sp.getBoolean("cast_ringer_mode", true)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    val tick = rememberResumeTick()
    LaunchedEffect(tick.intValue) {
        val now = prefs.getBoolean("cast_earbuds", true) && EarbudsCard.hasPermission(context)
        if (now != earbudsOn) {
            earbudsOn = now
            EarbudsCard.onSettingsChanged(context, NotificationCastListener.instance != null)
        }
    }

    val bluetoothPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        earbudsOn = granted
        if (granted) {
            EarbudsCard.onSettingsChanged(context, NotificationCastListener.instance != null)
        } else {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }

    if (btPopupSheetOpen) {
        BluetoothPopupSheet(context = context, onDismiss = { btPopupSheetOpen = false })
    }

    OriginCollapsingScaffold(
        title = "Tính năng Đảo",
        hasBottomNav = true,
    ) {
        // Nhóm 1: Ứng dụng & Tiện ích VN
        item(key = "modules_group_vn") {
            Column(modifier = Modifier.alpha(if (castOn) 1f else 0.45f)) {
                ModuleGroupHeader("Ứng dụng & Tiện ích VN")
                GroupCard {
                    ToggleRow(
                        icon = IsleIcons.Phone,
                        tileColor = IsleColors.Accent,
                        title = "Zalo Call",
                        subtitle = "Hiển thị cuộc gọi đến và thời lượng trên Đảo",
                        checked = zaloCallOn,
                    ) {
                        zaloCallOn = it
                        prefs.edit().putBoolean("cast_zalo_call", it).apply()
                    }

                    GroupDivider()

                    ToggleRow(
                        icon = IsleIcons.Smartphone,
                        tileColor = IsleColors.Accent,
                        title = "Đặt xe & Giao đồ ăn",
                        subtitle = "Grab, Be, Xanh SM, ShopeeFood",
                        checked = ridesOn,
                    ) {
                        ridesOn = it
                        prefs.edit().putBoolean("cast_rides", it).apply()
                    }

                    GroupDivider()

                    ToggleRow(
                        icon = IsleIcons.CreditCard,
                        tileColor = IsleColors.Accent,
                        title = "Ngân hàng VN & Thanh toán",
                        subtitle = "Biến động số dư và giao dịch tức thời",
                        checked = paymentOn,
                    ) {
                        paymentOn = it
                        prefs.edit().putBoolean("cast_payment", it).putBoolean("cast_vietnam_banks", it).apply()
                    }

                    GroupDivider()

                    ToggleRow(
                        icon = IsleIcons.Bolt,
                        tileColor = IsleColors.TileAmber,
                        title = "Chuỗi TikTok",
                        checked = tiktokStreakOn,
                    ) {
                        tiktokStreakOn = it
                        prefs.edit().putBoolean("cast_tiktok_streak", it).apply()
                    }

                    NestedSettingContainer(visible = tiktokStreakOn) {
                        Text(
                            text = "Tin nhắn gửi:",
                            fontSize = 13.sp,
                            color = IsleColors.TextSecondary,
                        )
                        val inputShape = G2RoundedCornerShape(18.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(inputShape)
                                .background(IsleColors.Surface)
                                .border(0.8.dp, IsleColors.BorderHairline, inputShape)
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BasicTextField(
                                value = tiktokStreakMessage,
                                onValueChange = {
                                    tiktokStreakMessage = it
                                    prefs.edit().putString("tiktok_streak_custom_message", it).apply()
                                },
                                singleLine = true,
                                textStyle = TextStyle(fontFamily = VivoSans, fontSize = 14.sp, color = IsleColors.Text),
                                cursorBrush = SolidColor(IsleColors.TileAmber),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }

        // Nhóm 2: Giải trí & Kết nối
        item(key = "modules_group_media_bt") {
            Column(modifier = Modifier.alpha(if (castOn) 1f else 0.45f)) {
                ModuleGroupHeader("Giải trí & Kết nối")
                GroupCard {
                    ToggleRow(
                        icon = IsleIcons.Music,
                        tileColor = IsleColors.Accent,
                        title = "Nhạc bên thứ 3 & Bảng điều khiển nổi",
                        subtitle = "Spotify, Zing MP3, NhacCuaTui, YouTube Music",
                        checked = mediaOn,
                    ) {
                        mediaOn = it
                        prefs.edit().putBoolean("cast_media_sessions", it).apply()
                    }

                    NestedSettingContainer(visible = mediaOn) {
                        ToggleRow(
                            icon = IsleIcons.Buttons,
                            tileColor = IsleColors.Accent,
                            title = "Mở rộng bảng điều khiển nổi",
                            checked = mediaOverlayExpand,
                        ) {
                            mediaOverlayExpand = it
                            prefs.edit().putBoolean(PREF_MEDIA_OVERLAY_EXPAND, it).apply()
                        }
                    }

                    GroupDivider()

                    ToggleRow(
                        icon = IsleIcons.Headphones,
                        tileColor = IsleColors.Accent,
                        title = "Tai nghe & Thiết bị đeo",
                        checked = earbudsOn,
                    ) {
                        prefs.edit().putBoolean("cast_earbuds", it).apply()
                        if (it && !EarbudsCard.hasPermission(context)) {
                            bluetoothPermission.launch(Manifest.permission.BLUETOOTH_CONNECT)
                        } else {
                            earbudsOn = it
                            EarbudsCard.onSettingsChanged(context, NotificationCastListener.instance != null)
                        }
                    }

                    GroupDivider()

                    NavRow(
                        icon = IsleIcons.Camera,
                        tileColor = IsleColors.TileBlue,
                        title = "Popup kết nối Bluetooth",
                        value = if (btConnectPopup) "Đang bật" else "Đang tắt",
                        onClick = { btPopupSheetOpen = true },
                    )
                }
            }
        }

        // Nhóm 3: Hệ thống & Tiện ích tự làm
        item(key = "modules_group_system") {
            Column(modifier = Modifier.alpha(if (castOn) 1f else 0.45f)) {
                ModuleGroupHeader("Hệ thống & Tiện ích tự làm")
                GroupCard {
                    ToggleRow(
                        icon = IsleIcons.Download,
                        tileColor = IsleColors.Accent,
                        title = "Tải xuống & Quick Share",
                        checked = downloadOn,
                    ) {
                        downloadOn = it
                        prefs.edit().putBoolean("cast_downloads", it).apply()
                    }

                    GroupDivider()

                    ToggleRow(
                        icon = IsleIcons.Clock,
                        tileColor = IsleColors.Accent,
                        title = "Lịch hẹn",
                        checked = castCalendarEvents,
                    ) {
                        castCalendarEvents = it
                        prefs.edit().putBoolean("cast_calendar_events", it).apply()
                    }

                    GroupDivider()

                    ToggleRow(
                        icon = IsleIcons.Sun,
                        tileColor = IsleColors.Accent,
                        title = "Đèn pin",
                        checked = flashlightOn,
                    ) {
                        flashlightOn = it
                        prefs.edit().putBoolean("cast_flashlight", it).apply()
                        FlashlightCard.refresh()
                    }

                    GroupDivider()

                    ToggleRow(
                        icon = IsleIcons.Bell,
                        tileColor = IsleColors.Accent,
                        title = "Chế độ Chuông/Rung",
                        checked = ringerModeOn,
                    ) {
                        ringerModeOn = it
                        prefs.edit().putBoolean("cast_ringer_mode", it).apply()
                    }
                }
            }
        }
    }
}

@Composable
private fun ModuleGroupHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = IsleColors.TextSub,
        modifier = Modifier.padding(start = 16.dp, bottom = 6.dp),
    )
}
