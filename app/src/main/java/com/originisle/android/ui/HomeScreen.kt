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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.originisle.android.cards.ChargingCard
import com.originisle.android.cards.EarbudsCard
import com.originisle.android.cards.FlashlightCard
import com.originisle.android.cards.PrivacyCard
import com.originisle.android.island.PlaygroundService
import com.originisle.android.service.NotificationCastListener
import com.originisle.android.service.QuietHours
import com.originisle.android.ui.samples.IslandSamples
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * OriginOS 6 Settings Home:
 * - Tiêu đề "Cài đặt" & Thanh tìm kiếm Pill 42dp
 * - Thẻ chọn chế độ Sáng / Tối dạng phone mockup 3D
 * - Thẻ trạng thái Đảo Origin (Avatar tròn, trạng thái kết nối)
 * - Nhóm thẻ tính năng cốt lõi (Thông báo, Tải xuống, OTP, Số dư ngân hàng)
 * - Nhóm thẻ tiện ích & hệ thống (Âm nhạc, Camera & Mic, Tai nghe Bluetooth, Pin sạc)
 * - Nhóm thẻ quản lý & hỗ trợ (Ứng dụng, Quyền hệ thống, Nhật ký, Cập nhật)
 * - Trải nghiệm thử trên đảo và các nút điều khiển
 */
@Composable
fun HomeScreen(
    context: Context,
    prefs: SharedPreferences,
    onRedoSetup: () -> Unit,
    onOpenApps: () -> Unit,
    onOpenActivity: () -> Unit,
) {
    // Theme mode listener
    var themePref by remember { mutableStateOf(prefs.getString("app_theme", "SYSTEM") ?: "SYSTEM") }
    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
            if (key == "app_theme") {
                themePref = sp.getString("app_theme", "SYSTEM") ?: "SYSTEM"
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val systemDark = isSystemInDarkTheme()
    val isDarkActive = when (themePref) {
        "LIGHT" -> false
        "DARK" -> true
        else -> systemDark
    }

    var castOn by remember { mutableStateOf(prefs.getBoolean("cast_notifications", true)) }
    var mediaOn by remember { mutableStateOf(prefs.getBoolean("cast_media_sessions", false)) }
    var includeMessages by remember { mutableStateOf(prefs.getBoolean("cast_include_messages", false)) }
    var ignoreSilent by remember { mutableStateOf(prefs.getBoolean("cast_ignore_silent", true)) }
    var lockscreenLiveCard by remember { mutableStateOf(prefs.getBoolean("cast_lockscreen_live_card", false)) }
    var autoDismissSec by remember { mutableStateOf(prefs.getInt("cast_auto_dismiss_seconds", 0)) }
    var otpOn by remember { mutableStateOf(prefs.getBoolean("cast_otp", true)) }
    var downloadOn by remember { mutableStateOf(prefs.getBoolean("cast_downloads", true)) }
    var paymentOn by remember { mutableStateOf(prefs.getBoolean("cast_payment", true)) }
    var actionsOn by remember { mutableStateOf(prefs.getBoolean("cast_show_actions", true)) }
    var chargingOn by remember { mutableStateOf(prefs.getBoolean("cast_charging", true)) }
    var chargingKeep by remember { mutableStateOf(prefs.getBoolean("cast_charging_keep", false)) }
    var privacyDotOn by remember { mutableStateOf(prefs.getBoolean("cast_privacy_dot", true)) }
    var earbudsOn by remember {
        mutableStateOf(prefs.getBoolean("cast_earbuds", false) && EarbudsCard.hasPermission(context))
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

    var quietDnd by remember { mutableStateOf(prefs.getBoolean("cast_quiet_dnd", false)) }
    var quietSchedule by remember { mutableStateOf(prefs.getBoolean("cast_quiet_schedule", false)) }
    var quietStart by remember { mutableIntStateOf(prefs.getInt("cast_quiet_start", QuietHours.DEFAULT_START)) }
    var quietEnd by remember { mutableIntStateOf(prefs.getInt("cast_quiet_end", QuietHours.DEFAULT_END)) }
    val appsOff = remember { prefs.getStringSet("cast_ignored_apps", emptySet()).orEmpty().size }
    val tick = rememberResumeTick()

    LaunchedEffect(tick.intValue) {
        val now = prefs.getBoolean("cast_earbuds", false) && EarbudsCard.hasPermission(context)
        if (now != earbudsOn) {
            earbudsOn = now
            EarbudsCard.onSettingsChanged(context, NotificationCastListener.instance != null)
        }
    }

    val listenerOk = remember(tick.intValue) { isListenerEnabled(context) }
    val batteryOk = remember(tick.intValue) { isBatteryUnrestricted(context) }
    var permissionsOpen by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var updateStatus by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    var connected by remember { mutableStateOf(NotificationCastListener.instance != null) }
    LaunchedEffect(Unit) {
        while (true) {
            connected = NotificationCastListener.instance != null
            delay(1000)
        }
    }

    if (permissionsOpen) {
        PermissionsSheet(context, prefs, onDismiss = { permissionsOpen = false }, onRedoSetup = onRedoSetup)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = ScreenPadding,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // 1. Tiêu đề "Cài đặt" + Menu More + Thanh tìm kiếm Pill
        item {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Cài đặt",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = IsleColors.Text,
                        modifier = Modifier.weight(1f),
                    )
                    Box {
                        CircleIconButton(IsleIcons.More, "Tùy chọn khác", onClick = { menuOpen = true })
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                            offset = DpOffset(0.dp, 8.dp),
                            shape = RoundedCornerShape(18.dp),
                            containerColor = IsleColors.Menu,
                        ) {
                            MenuItem("Kiểm tra cập nhật") {
                                menuOpen = false
                                coroutineScope.launch {
                                    updateStatus = "Đang kiểm tra…"
                                    updateStatus = when (val result = UpdateChecker.check(context)) {
                                        is UpdateChecker.Result.UpToDate ->
                                            "Bạn đang dùng phiên bản mới nhất (v${result.current})."
                                        is UpdateChecker.Result.UpdateAvailable -> {
                                            UpdateChecker.openRelease(context, result.url)
                                            "Có bản cập nhật mới: v${result.latest}"
                                        }
                                        is UpdateChecker.Result.Error -> result.message
                                    }
                                }
                            }
                            MenuItem("Quyền hệ thống") {
                                menuOpen = false
                                permissionsOpen = true
                            }
                            MenuItem("Thiết lập lại") {
                                menuOpen = false
                                onRedoSetup()
                            }
                        }
                    }
                }

                // Thanh tìm kiếm Pill shape 42dp bo tròn 21dp
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(21.dp))
                        .background(IsleColors.SurfaceHigh)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            IsleIcons.Search,
                            contentDescription = null,
                            tint = IsleColors.TextSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "Tìm kiếm cài đặt",
                            color = IsleColors.TextSecondary,
                            fontSize = 15.sp,
                        )
                    }
                }
            }

            updateStatus?.let {
                Text(
                    it,
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp),
                    fontSize = 13.sp,
                    color = IsleColors.AccentText,
                )
            }
        }

        // Cảnh báo nếu quyền listener hoặc pin bị OriginOS hạn chế
        if (!listenerOk || !batteryOk) {
            item { SetupAttentionBanner(onClick = { permissionsOpen = true }) }
        }

        // 2. THẺ CHỌN CHẾ ĐỘ SÁNG / TỐI (Phone Mockups chuẩn Screenshot 2)
        item {
            GroupCard {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // Phone Mockup bên trái: Sáng
                        ThemePhoneMockup(
                            title = "Chế độ sáng",
                            isDarkMockup = false,
                            selected = themePref == "LIGHT" || (!isDarkActive && themePref != "DARK"),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                themePref = "LIGHT"
                                prefs.edit().putString("app_theme", "LIGHT").apply()
                            },
                        )

                        // Phone Mockup bên phải: Tối
                        ThemePhoneMockup(
                            title = "Chế độ tối",
                            isDarkMockup = true,
                            selected = themePref == "DARK" || (isDarkActive && themePref != "LIGHT"),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                themePref = "DARK"
                                prefs.edit().putString("app_theme", "DARK").apply()
                            },
                        )
                    }

                    Spacer(Modifier.height(14.dp))
                    GroupDivider(inset = 0.dp)

                    // Dòng "Nút chuyển theo lịch" -> "Tắt >" hoặc "Theo hệ thống >"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button) {
                                val nextPref = if (themePref == "SYSTEM") "DARK" else "SYSTEM"
                                themePref = nextPref
                                prefs.edit().putString("app_theme", nextPref).apply()
                            }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Nút chuyển theo lịch",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = IsleColors.Text,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            if (themePref == "SYSTEM") "Theo hệ thống" else "Tắt",
                            fontSize = 14.sp,
                            color = IsleColors.TextSecondary,
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(IsleIcons.ChevronRight, null, Modifier.size(16.dp), tint = IsleColors.Chevron)
                    }

                    GroupDivider(inset = 0.dp)

                    // Dòng "Cài đặt chế độ tối" -> ">"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button) {
                                val nextPref = if (isDarkActive) "LIGHT" else "DARK"
                                themePref = nextPref
                                prefs.edit().putString("app_theme", nextPref).apply()
                            }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Cài đặt chế độ tối",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = IsleColors.Text,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(IsleIcons.ChevronRight, null, Modifier.size(16.dp), tint = IsleColors.Chevron)
                    }
                }
            }
        }

        // 3. THẺ TRẠNG THÁI ĐẢO ORIGIN (Profile/Account Style chuẩn OriginOS 6)
        item {
            GroupCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button, onClick = onOpenActivity)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // Avatar tròn với icon đảo
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(IsleColors.Accent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White),
                        )
                    }

                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            "Đảo Origin",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = IsleColors.Text,
                        )
                        Text(
                            if (connected) "Trạng thái: Hoạt động · Đã kết nối" else "Trạng thái: Chưa kết nối dịch vụ",
                            fontSize = 13.sp,
                            color = if (connected) IsleColors.GoodText else IsleColors.Danger,
                        )
                    }

                    Icon(IsleIcons.ChevronRight, null, Modifier.size(20.dp), tint = IsleColors.Chevron)
                }
            }
        }

        // 4. NHÓM TÍNH NĂNG CỐT LÕI (Core Features Group)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("Tính năng cốt lõi")
                GroupCard {
                    ToggleRow(
                        IsleIcons.Bell, IsleColors.Accent, "Thông báo chung", castOn,
                        subtitle = "Hiển thị thông báo đến dạng thẻ đảo động",
                    ) {
                        castOn = it; prefs.edit().putBoolean("cast_notifications", it).apply()
                        if (it) PlaygroundService.keepAlive(context)
                        PrivacyCard.refresh()
                        FlashlightCard.refresh()
                        EarbudsCard.onSettingsChanged(context, NotificationCastListener.instance != null)
                    }
                    GroupDivider()
                    ToggleRow(
                        IsleIcons.Download, IsleColors.Accent, "Tiến trình tải xuống", downloadOn,
                        subtitle = "Chrome, Edge, Canary và tệp tin đang tải",
                    ) {
                        downloadOn = it; prefs.edit().putBoolean("cast_downloads", it).apply()
                    }
                    GroupDivider()
                    ToggleRow(
                        IsleIcons.Key, IsleColors.Accent, "Mã xác thực OTP", otpOn,
                        subtitle = "Tự động phát hiện mã xác minh (SMS)",
                    ) {
                        otpOn = it; prefs.edit().putBoolean("cast_otp", it).apply()
                    }
                    GroupDivider()
                    ToggleRow(
                        IsleIcons.CreditCard, IsleColors.Accent, "Biến động số dư ngân hàng", paymentOn,
                        subtitle = "Thông báo tiền vào (+), tiền ra (-) ngân hàng Việt Nam",
                    ) {
                        paymentOn = it; prefs.edit().putBoolean("cast_payment", it).apply()
                    }
                }
            }
        }

        // 5. NHÓM TIỆN ÍCH & HỆ THỐNG (Utilities & System Group)
        item {
            Column(
                Modifier.alpha(if (castOn) 1f else 0.4f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SectionLabel("Tiện ích & Hệ thống")
                GroupCard {
                    ToggleRow(
                        IsleIcons.Music, IsleColors.Accent, "Âm nhạc & Trình phát", mediaOn,
                        subtitle = "Trình phát nhạc thu nhỏ và thanh điều khiển",
                    ) {
                        mediaOn = it; prefs.edit().putBoolean("cast_media_sessions", it).apply()
                    }
                    GroupDivider()
                    ToggleRow(
                        IsleIcons.Camera, IsleColors.Accent, "Chỉ báo Camera & Micro", privacyDotOn,
                        subtitle = "Chấm bảo mật: Camera đỏ, Micro xanh lá bo góc",
                    ) {
                        privacyDotOn = it; prefs.edit().putBoolean("cast_privacy_dot", it).apply()
                        PrivacyCard.refresh()
                    }
                    GroupDivider()
                    ToggleRow(
                        IsleIcons.Headphones, IsleColors.Accent, "Tai nghe Bluetooth", earbudsOn,
                        subtitle = "Hiển thị phần trăm pin khi kết nối",
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
                        IsleIcons.Bolt, IsleColors.Accent, "Pin & Trạng thái sạc", chargingOn,
                        subtitle = "Hiển thị thẻ sạc pin trực quan",
                    ) {
                        chargingOn = it; prefs.edit().putBoolean("cast_charging", it).apply()
                        ChargingCard.onSettingsChanged(context)
                    }
                    if (chargingOn) {
                        Row(
                            Modifier.padding(start = 56.dp, end = 16.dp, bottom = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf(false to "Thoáng qua", true to "Suốt thời gian sạc").forEach { (keep, label) ->
                                ChoiceChip(
                                    label,
                                    selected = chargingKeep == keep,
                                    onClick = {
                                        chargingKeep = keep
                                        prefs.edit().putBoolean("cast_charging_keep", keep).apply()
                                        ChargingCard.onSettingsChanged(context)
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                    GroupDivider()
                    ToggleRow(
                        IsleIcons.Buttons, IsleColors.Accent, "Nút thao tác thông báo", actionsOn,
                        subtitle = "Hiển thị các nút trả lời nhanh, hủy, v.v.",
                    ) {
                        actionsOn = it; prefs.edit().putBoolean("cast_show_actions", it).apply()
                    }
                }
            }
        }

        // Tùy chọn thông báo nâng cao
        item {
            Column(
                Modifier.alpha(if (castOn) 1f else 0.4f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SectionLabel("Tùy chọn hiển thị")
                GroupCard {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SegmentedControl(
                            listOf("Chỉ thẻ động", "Tất cả"),
                            selected = if (includeMessages) 1 else 0,
                            onSelect = {
                                includeMessages = it == 1
                                prefs.edit().putBoolean("cast_include_messages", includeMessages).apply()
                            },
                        )
                        Text(
                            if (includeMessages) "Tất cả thông báo, bao gồm tin nhắn văn bản thông thường."
                            else "Chỉ thẻ động trực tiếp — gọi xe, giao hàng, thanh toán, tỷ số, v.v.",
                            modifier = Modifier.padding(horizontal = 4.dp),
                            fontSize = 13.sp,
                            color = IsleColors.TextSecondary,
                        )
                    }
                    if (includeMessages) {
                        GroupDivider()
                        SettingRow(IsleIcons.Clock, IsleColors.Accent, "Tự ẩn sau", subtitle = "Dành cho thẻ thông báo thường")
                        Row(
                            Modifier.padding(start = 56.dp, end = 16.dp, bottom = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            listOf(0 to "Không ẩn", 5 to "5s", 10 to "10s", 15 to "15s", 30 to "30s").forEach { (sec, label) ->
                                ChoiceChip(
                                    label,
                                    selected = autoDismissSec == sec,
                                    onClick = {
                                        autoDismissSec = sec
                                        prefs.edit().putInt("cast_auto_dismiss_seconds", sec).apply()
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                    GroupDivider()
                    ToggleRow(
                        IsleIcons.Moon, IsleColors.Accent, "Bỏ qua thông báo im lặng", ignoreSilent,
                        subtitle = "Không hiển thị thông báo không có chuông/rung",
                    ) {
                        ignoreSilent = it; prefs.edit().putBoolean("cast_ignore_silent", it).apply()
                    }
                    GroupDivider()
                    ToggleRow(
                        IsleIcons.Lock, IsleColors.Accent, "Màn hình khóa", lockscreenLiveCard,
                        subtitle = "Hiển thị thẻ động trên màn hình khóa",
                    ) {
                        lockscreenLiveCard = it
                        prefs.edit().putBoolean("cast_lockscreen_live_card", it).apply()
                    }
                }
            }
        }

        // Giờ yên tĩnh
        item {
            Column(
                Modifier.alpha(if (castOn) 1f else 0.4f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SectionLabel("Giờ yên tĩnh")
                GroupCard {
                    ToggleRow(
                        IsleIcons.Moon, IsleColors.Accent, "Theo chế độ Không làm phiền", quietDnd,
                        subtitle = "Tự động tắt đảo khi bật Không làm phiền",
                    ) {
                        quietDnd = it; prefs.edit().putBoolean("cast_quiet_dnd", it).apply()
                    }
                    GroupDivider()
                    ToggleRow(
                        IsleIcons.Clock, IsleColors.Accent, "Lịch đặt giờ", quietSchedule,
                        subtitle = "${QuietHours.format(quietStart)} – ${QuietHours.format(quietEnd)} hàng ngày",
                    ) {
                        quietSchedule = it; prefs.edit().putBoolean("cast_quiet_schedule", it).apply()
                    }
                    if (quietSchedule) {
                        Row(
                            Modifier.padding(start = 56.dp, end = 16.dp, bottom = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ChoiceChip(
                                "Từ ${QuietHours.format(quietStart)}",
                                selected = false,
                                onClick = {
                                    pickTime(context, quietStart) {
                                        quietStart = it; prefs.edit().putInt("cast_quiet_start", it).apply()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            )
                            ChoiceChip(
                                "Đến ${QuietHours.format(quietEnd)}",
                                selected = false,
                                onClick = {
                                    pickTime(context, quietEnd) {
                                        quietEnd = it; prefs.edit().putInt("cast_quiet_end", it).apply()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }

        // 6. NHÓM QUẢN LÝ & HỖ TRỢ (Management & Support Group)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("Quản lý & Hỗ trợ")
                GroupCard {
                    NavRow(
                        IsleIcons.Grid, IsleColors.Accent, "Ứng dụng",
                        value = if (appsOff == 0) "Đang bật tất cả" else "Đã tắt $appsOff",
                        onClick = onOpenApps,
                    )
                    GroupDivider()
                    NavRow(
                        IsleIcons.Shield, IsleColors.Accent, "Quyền hệ thống",
                        value = if (listenerOk && batteryOk) "Đã hoàn tất" else "Cần cấp quyền",
                        onClick = { permissionsOpen = true },
                    )
                    GroupDivider()
                    NavRow(
                        IsleIcons.Pulse, IsleColors.Accent, "Nhật ký hoạt động",
                        onClick = onOpenActivity,
                    )
                    GroupDivider()
                    NavRow(
                        IsleIcons.Refresh, IsleColors.Accent, "Kiểm tra cập nhật",
                        value = updateStatus,
                        onClick = {
                            coroutineScope.launch {
                                updateStatus = "Đang kiểm tra…"
                                updateStatus = when (val result = UpdateChecker.check(context)) {
                                    is UpdateChecker.Result.UpToDate ->
                                        "Phiên bản mới nhất (v${result.current})"
                                    is UpdateChecker.Result.UpdateAvailable -> {
                                        UpdateChecker.openRelease(context, result.url)
                                        "Có bản mới: v${result.latest}"
                                    }
                                    is UpdateChecker.Result.Error -> result.message
                                }
                            }
                        },
                    )
                }
            }
        }

        // 7. TRẢI NGHIỆM TRÊN ĐẢO & ĐIỀU KHIỂN
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionLabel("Trải nghiệm trước trên đảo")
                SampleTester(context)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButton(
                        "Hiện lại tất cả", onClick = { recastAll(context) },
                        modifier = Modifier.weight(1f), icon = IsleIcons.Refresh,
                        enabled = castOn || mediaOn,
                    )
                    PillButton(
                        "Dừng tất cả", onClick = { stopAll(context) },
                        modifier = Modifier.weight(1f), icon = IsleIcons.Stop, iconSize = 16.dp,
                        content = IsleColors.Danger,
                    )
                }
            }
        }
    }
}

/** Phone mockup mini hiển thị phong cách Sáng / Tối chuẩn Screenshot 2. */
@Composable
private fun ThemePhoneMockup(
    title: String,
    isDarkMockup: Boolean,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Khung điện thoại mini
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isDarkMockup) Color(0xFF000000) else Color(0xFFF2F4F7))
                .border(
                    width = if (selected) 1.5.dp else 0.8.dp,
                    color = if (selected) IsleColors.Accent else if (isDarkMockup) Color(0x33FFFFFF) else Color(0x1F000000),
                    shape = RoundedCornerShape(14.dp),
                )
                .padding(6.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Mini Island capsule
                Box(
                    modifier = Modifier
                        .width(26.dp)
                        .height(6.5.dp)
                        .clip(RoundedCornerShape(3.25.dp))
                        .background(if (isDarkMockup) Color(0xFF1E2028) else Color(0xFF000000)),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    if (isDarkMockup) {
                        Box(
                            Modifier
                                .padding(end = 2.dp)
                                .size(2.5.dp)
                                .clip(CircleShape)
                                .background(IsleColors.Accent),
                        )
                    }
                }

                // Mini Card 1
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDarkMockup) Color(0xFF17181C) else Color(0xFFFFFFFF))
                        .border(
                            0.5.dp,
                            if (isDarkMockup) Color(0x22FFFFFF) else Color(0x0F000000),
                            RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(IsleColors.Accent),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Box(
                                Modifier
                                    .width(34.dp)
                                    .height(3.dp)
                                    .background(if (isDarkMockup) Color(0xFF8E8E93) else Color(0xFF94A3B8)),
                            )
                            Box(
                                Modifier
                                    .width(20.dp)
                                    .height(2.dp)
                                    .background(if (isDarkMockup) Color(0xFF454854) else Color(0xFFCBD5E1)),
                            )
                        }
                    }
                }

                // Mini Card 2
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDarkMockup) Color(0xFF17181C) else Color(0xFFFFFFFF))
                        .border(
                            0.5.dp,
                            if (isDarkMockup) Color(0x22FFFFFF) else Color(0x0F000000),
                            RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00D084)),
                        )
                        Box(
                            Modifier
                                .width(38.dp)
                                .height(2.5.dp)
                                .background(if (isDarkMockup) Color(0xFF6B6E7A) else Color(0xFF94A3B8)),
                        )
                    }
                }
            }
        }

        Text(
            title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = IsleColors.Text,
        )

        // Radio button tròn Teal
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(
                    width = if (selected) 2.dp else 1.2.dp,
                    color = if (selected) IsleColors.Accent else IsleColors.Divider,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(IsleColors.Accent),
                )
            }
        }
    }
}

@Composable
private fun MenuItem(text: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = IsleColors.Text) },
        onClick = onClick,
        modifier = Modifier.widthIn(min = 200.dp).heightIn(min = 48.dp),
    )
}

/** Island preview + sample picker + send button: fires any [IslandSamples] card on demand. */
@Composable
private fun SampleTester(context: Context) {
    var selected by remember { mutableIntStateOf(0) }
    val sample = IslandSamples.all[selected]
    var sentAt by remember { mutableLongStateOf(0L) }
    LaunchedEffect(sentAt) {
        if (sentAt != 0L) { delay(1600); sentAt = 0L }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(IsleColors.Surface)
            .border(0.8.dp, IsleColors.BorderHairline, RoundedCornerShape(24.dp))
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .heightIn(min = 104.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(IsleColors.PreviewBg)
                .border(
                    width = 0.8.dp,
                    color = IsleColors.BorderHairline,
                    shape = RoundedCornerShape(18.dp),
                )
                .padding(start = 10.dp, end = 10.dp, top = 14.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            IslandPreviewView(sample.preview)
            Text(
                sample.summary,
                modifier = Modifier.padding(top = 12.dp),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = IsleColors.PreviewLabel,
                textAlign = TextAlign.Center,
            )
        }
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(IslandSamples.all) { i, s ->
                ChoiceChip(
                    s.name,
                    selected = i == selected,
                    onClick = { selected = i },
                    selectedContainer = IsleColors.Accent,
                    selectedContent = Color.White,
                    shape = RoundedCornerShape(18.dp),
                    height = 36.dp,
                    horizontalPadding = 14.dp,
                )
            }
        }
        PillButton(
            if (sentAt != 0L) "Đã gửi lên đảo" else "Thử trên đảo",
            onClick = { sample.post(context); sentAt = System.currentTimeMillis() },
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
            icon = IsleIcons.Send,
            container = IsleColors.Accent,
            content = Color.White,
            height = 48.dp,
        )
    }
}

/** The platform time picker, in minutes since midnight, honouring the device's 12/24h setting. */
private fun pickTime(context: Context, initial: Int, onPicked: (Int) -> Unit) {
    android.app.TimePickerDialog(
        context,
        { _, hour, minute -> onPicked(hour * 60 + minute) },
        initial / 60, initial % 60,
        android.text.format.DateFormat.is24HourFormat(context),
    ).show()
}

private fun stopAll(context: Context) {
    context.startService(Intent(context, PlaygroundService::class.java).setAction(PlaygroundService.ACTION_STOP))
}

private fun recastAll(context: Context) {
    PlaygroundService.keepAlive(context)
    val listener = NotificationCastListener.instance
    if (listener == null) {
        val message = when (NotificationCastListener.forceRebind(context)) {
            NotificationCastListener.RebindResult.NO_ACCESS ->
                "Chưa cấp quyền truy cập thông báo — vui lòng bật trong Cài đặt rồi thử lại."
            else ->
                "Bộ lắng nghe chưa kết nối — đang kết nối lại. Vui lòng chờ vài giây rồi thử lại."
        }
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
        return
    }
    val count = listener.recastAll()
    android.widget.Toast.makeText(
        context, "Đã phát lại $count thông báo lên đảo.", android.widget.Toast.LENGTH_SHORT,
    ).show()
}
