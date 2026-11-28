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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.originisle.android.cards.EarbudsCard
import com.originisle.android.cards.FlashlightCard
import com.originisle.android.cards.MediaCard
import com.originisle.android.service.MediaIslandOverlay
import com.originisle.android.service.NotificationCastListener

/**
 * Tab 2 — Tính năng Đảo: quản lý toàn bộ thẻ Đảo theo 2 nhóm sạch sẽ:
 * 1. Dịch vụ & Ứng dụng
 * 2. Hệ thống & Phần cứng
 */
@Composable
fun ModulesScreen(
    context: Context,
    prefs: SharedPreferences,
) {
    var castOn by remember { mutableStateOf(prefs.getBoolean("cast_notifications", true)) }

    // Nhóm 1: Dịch vụ & Ứng dụng
    var ridesOn by remember { mutableStateOf(prefs.getBoolean("cast_rides", true)) }
    var paymentOn by remember {
        mutableStateOf(prefs.getBoolean("cast_payment", prefs.getBoolean("cast_vietnam_banks", true)))
    }
    var zaloCallOn by remember { mutableStateOf(prefs.getBoolean("cast_zalo_call", true)) }
    var tiktokStreakOn by remember { mutableStateOf(prefs.getBoolean("cast_tiktok_streak", true)) }
    var tiktokStreakMessage by remember {
        mutableStateOf(prefs.getString("tiktok_streak_custom_message", "🔥") ?: "🔥")
    }
    var downloadOn by remember { mutableStateOf(prefs.getBoolean("cast_downloads", true)) }
    var castCalendarEvents by remember { mutableStateOf(prefs.getBoolean("cast_calendar_events", true)) }

    // Nhóm 2: Hệ thống & Phần cứng
    var mediaOn by remember { mutableStateOf(prefs.getBoolean("cast_media_sessions", true)) }
    var mediaOverlayExpand by remember {
        mutableStateOf(prefs.getBoolean(PREF_MEDIA_OVERLAY_EXPAND, true))
    }
    var earbudsOn by remember {
        mutableStateOf(prefs.getBoolean("cast_earbuds", true) && EarbudsCard.hasPermission(context))
    }
    var btConnectPopup by remember { mutableStateOf(prefs.getBoolean(PREF_BT_CONNECT_POPUP, false)) }
    var btPopupSheetOpen by remember { mutableStateOf(false) }
    var ringerModeOn by remember { mutableStateOf(prefs.getBoolean("cast_ringer_mode", true)) }
    var flashlightOn by remember { mutableStateOf(prefs.getBoolean("cast_flashlight", true)) }

    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
            when (key) {
                "cast_notifications" -> castOn = sp.getBoolean("cast_notifications", true)
                "cast_rides" -> ridesOn = sp.getBoolean("cast_rides", true)
                "cast_payment", "cast_vietnam_banks" -> {
                    paymentOn = sp.getBoolean("cast_payment", sp.getBoolean("cast_vietnam_banks", true))
                }
                "cast_zalo_call" -> zaloCallOn = sp.getBoolean("cast_zalo_call", true)
                "cast_tiktok_streak" -> tiktokStreakOn = sp.getBoolean("cast_tiktok_streak", true)
                "tiktok_streak_custom_message" -> {
                    tiktokStreakMessage = sp.getString("tiktok_streak_custom_message", "🔥") ?: "🔥"
                }
                "cast_downloads" -> downloadOn = sp.getBoolean("cast_downloads", true)
                "cast_calendar_events" -> castCalendarEvents = sp.getBoolean("cast_calendar_events", true)
                "cast_media_sessions" -> mediaOn = sp.getBoolean("cast_media_sessions", true)
                PREF_MEDIA_OVERLAY_EXPAND -> {
                    mediaOverlayExpand = sp.getBoolean(PREF_MEDIA_OVERLAY_EXPAND, true)
                }
                PREF_BT_CONNECT_POPUP -> btConnectPopup = sp.getBoolean(PREF_BT_CONNECT_POPUP, false)
                "cast_ringer_mode" -> ringerModeOn = sp.getBoolean("cast_ringer_mode", true)
                "cast_flashlight" -> flashlightOn = sp.getBoolean("cast_flashlight", true)
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = ScreenPadding,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            MainScreenHeader(title = "Tính năng Đảo")
        }

        // Nhóm 1 — Dịch vụ & Ứng dụng
        item {
            Column(
                modifier = Modifier.alpha(if (castOn) 1f else 0.45f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SectionLabel("Dịch vụ & Ứng dụng")
                GroupCard {
                    ToggleRow(
                        icon = IsleIcons.Smartphone,
                        tileColor = IsleColors.Accent,
                        title = "Gọi xe & Giao đồ ăn",
                        checked = ridesOn,
                        subtitle = "Grab, Be, Xanh SM, ShopeeFood",
                    ) {
                        ridesOn = it
                        prefs.edit().putBoolean("cast_rides", it).apply()
                    }

                    GroupDivider()

                    ToggleRow(
                        icon = IsleIcons.CreditCard,
                        tileColor = IsleColors.Accent,
                        title = "Biến động số dư Ngân hàng",
                        checked = paymentOn,
                    ) {
                        paymentOn = it
                        prefs.edit().putBoolean("cast_payment", it).putBoolean("cast_vietnam_banks", it).apply()
                    }

                    GroupDivider()

                    ToggleRow(
                        icon = IsleIcons.Phone,
                        tileColor = IsleColors.Accent,
                        title = "Cuộc gọi Zalo",
                        checked = zaloCallOn,
                    ) {
                        zaloCallOn = it
                        prefs.edit().putBoolean("cast_zalo_call", it).apply()
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
                            "Tin nhắn gửi:",
                            fontSize = 13.sp,
                            color = IsleColors.TextSecondary,
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(IsleColors.Surface)
                                .border(0.8.dp, IsleColors.BorderHairline, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BasicTextField(
                                value = tiktokStreakMessage,
                                onValueChange = {
                                    tiktokStreakMessage = it
                                    prefs.edit().putString("tiktok_streak_custom_message", it).apply()
                                },
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 14.sp, color = IsleColors.Text),
                                cursorBrush = SolidColor(IsleColors.TileAmber),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    GroupDivider()

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
                        title = "Nhắc lịch & Sự kiện",
                        checked = castCalendarEvents,
                    ) {
                        castCalendarEvents = it
                        prefs.edit().putBoolean("cast_calendar_events", it).apply()
                    }
                }
            }
        }

        // Nhóm 2 — Hệ thống & Phần cứng
        item {
            Column(
                modifier = Modifier.alpha(if (castOn) 1f else 0.45f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SectionLabel("Hệ thống & Phần cứng")
                GroupCard {
                    ToggleRow(
                        icon = IsleIcons.Music,
                        tileColor = IsleColors.Accent,
                        title = "Trình phát nhạc",
                        checked = mediaOn,
                    ) {
                        mediaOn = it
                        prefs.edit().putBoolean("cast_media_sessions", it).apply()
                    }

                    NestedSettingContainer(visible = mediaOn) {
                        ToggleRow(
                            icon = IsleIcons.Music,
                            tileColor = IsleColors.Accent,
                            title = "Bảng điều khiển nhạc tùy biến",
                            subtitle = "Bật để mở bảng điều khiển nổi; tắt để dùng đảo mở rộng mặc định",
                            checked = mediaOverlayExpand,
                        ) {
                            mediaOverlayExpand = it
                            prefs.edit().putBoolean(PREF_MEDIA_OVERLAY_EXPAND, it).apply()
                            if (!it) {
                                MediaIslandOverlay.dismiss()
                            }
                            MediaCard.refreshPresentation(context)
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

                    ToggleRow(
                        icon = IsleIcons.Headphones,
                        tileColor = IsleColors.Accent,
                        title = "Popup kết nối Bluetooth",
                        checked = btConnectPopup,
                    ) {
                        btConnectPopup = it
                        prefs.edit().putBoolean(PREF_BT_CONNECT_POPUP, it).apply()
                        if (it && !isOverlayPermissionGranted(context)) {
                            openOverlayPermissionSettings(context)
                        }
                    }

                    GroupDivider()

                    NavRow(
                        icon = IsleIcons.Camera,
                        tileColor = IsleColors.TileBlue,
                        title = "Tùy chỉnh tai nghe Bluetooth",
                        onClick = { btPopupSheetOpen = true },
                    )

                    GroupDivider()

                    ToggleRow(
                        icon = IsleIcons.Bell,
                        tileColor = IsleColors.Accent,
                        title = "Chế độ Âm thanh",
                        checked = ringerModeOn,
                    ) {
                        ringerModeOn = it
                        prefs.edit().putBoolean("cast_ringer_mode", it).apply()
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
                }
            }
        }

        item {
            Spacer(
                modifier = Modifier
                    .navigationBarsPadding()
                    .height(112.dp),
            )
        }
    }
}
