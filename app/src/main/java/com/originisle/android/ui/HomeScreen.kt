package com.originisle.android.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.originisle.android.cards.ChargingCard
import com.originisle.android.cards.EarbudsCard
import com.originisle.android.cards.FlashlightCard
import com.originisle.android.cards.PrivacyCard
import com.originisle.android.island.PlaygroundService
import com.originisle.android.log.CastLog
import com.originisle.android.service.NotificationCastListener
import com.originisle.android.service.QuietHours
import com.originisle.android.ui.samples.IslandSamples
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.util.Locale

/**
 * OriginOS 6 Settings Home (Đại tu giao diện chuẩn Vivo OriginOS 6):
 * - Phân nhóm cấu trúc 5 tầng chuẩn Cài đặt Vivo OriginOS
 * - Tối ưu hóa kích thước hiển thị và khoảng cách padding / hairline 0.8dp
 * - Sử dụng NestedSettingContainer cho các cài đặt con mở rộng mượt mà
 */
@Composable
fun HomeScreen(
    context: Context,
    prefs: SharedPreferences,
    onRedoSetup: () -> Unit,
    onOpenApps: () -> Unit,
    onOpenActivity: () -> Unit,
    autoUpdateRelease: UpdateChecker.ReleaseInfo? = null,
    onDismissUpdate: () -> Unit = {},
) {
    // 1. Quản lý trạng thái Theme (Sáng / Tối / Hệ thống) & Tùy biến Đảo 2.0.0
    var themePref by remember { mutableStateOf(prefs.getString("app_theme", "SYSTEM") ?: "SYSTEM") }
    var islandPosition by remember { mutableStateOf(prefs.getString("island_position", "CENTER") ?: "CENTER") }
    var suppressHeadsUp by remember { mutableStateOf(prefs.getBoolean(PREF_CAST_SUPPRESS_HEADS_UP, false)) }
    var suppressMode by remember {
        mutableStateOf(prefs.getString(PREF_CAST_SUPPRESS_MODE, SUPPRESS_MODE_SNOOZE) ?: SUPPRESS_MODE_SNOOZE)
    }
    var customLightColor by remember {
        mutableStateOf(prefs.getString(PREF_CAST_CUSTOM_LIGHT_COLOR, LIGHT_COLOR_AUTO) ?: LIGHT_COLOR_AUTO)
    }
    var islandDurationSec by remember {
        mutableIntStateOf(prefs.getInt(PREF_CAST_ISLAND_DURATION_SEC, DEFAULT_ISLAND_DURATION_SEC))
    }
    var tapExpandCard by remember { mutableStateOf(prefs.getBoolean(PREF_CAST_TAP_EXPAND_CARD, true)) }
    var searchQuery by remember { mutableStateOf("") }

    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
            when (key) {
                "app_theme" -> themePref = sp.getString("app_theme", "SYSTEM") ?: "SYSTEM"
                "island_position" -> islandPosition = sp.getString("island_position", "CENTER") ?: "CENTER"
                PREF_CAST_SUPPRESS_HEADS_UP -> suppressHeadsUp = sp.getBoolean(PREF_CAST_SUPPRESS_HEADS_UP, false)
                PREF_CAST_SUPPRESS_MODE -> {
                    suppressMode = sp.getString(PREF_CAST_SUPPRESS_MODE, SUPPRESS_MODE_SNOOZE) ?: SUPPRESS_MODE_SNOOZE
                }
                PREF_CAST_CUSTOM_LIGHT_COLOR -> {
                    customLightColor = sp.getString(PREF_CAST_CUSTOM_LIGHT_COLOR, LIGHT_COLOR_AUTO) ?: LIGHT_COLOR_AUTO
                }
                PREF_CAST_ISLAND_DURATION_SEC -> {
                    islandDurationSec = sp.getInt(PREF_CAST_ISLAND_DURATION_SEC, DEFAULT_ISLAND_DURATION_SEC)
                }
                PREF_CAST_TAP_EXPAND_CARD -> tapExpandCard = sp.getBoolean(PREF_CAST_TAP_EXPAND_CARD, true)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    // 2. Nhóm công tắc dịch vụ cốt lõi & Việt Nam
    var castOn by remember { mutableStateOf(prefs.getBoolean("cast_notifications", true)) }
    var ridesOn by remember { mutableStateOf(prefs.getBoolean("cast_rides", true)) }
    var paymentOn by remember { mutableStateOf(prefs.getBoolean("cast_payment", prefs.getBoolean("cast_vietnam_banks", true))) }
    var zaloCallOn by remember { mutableStateOf(prefs.getBoolean("cast_zalo_call", true)) }
    var tiktokStreakOn by remember { mutableStateOf(prefs.getBoolean("cast_tiktok_streak", true)) }
    var tiktokStreakMessage by remember {
        mutableStateOf(prefs.getString("tiktok_streak_custom_message", "🔥") ?: "🔥")
    }
    var downloadOn by remember { mutableStateOf(prefs.getBoolean("cast_downloads", true)) }
    var castCalendarEvents by remember { mutableStateOf(prefs.getBoolean("cast_calendar_events", true)) }
    var callsOn by remember { mutableStateOf(prefs.getBoolean("cast_calls", true)) }

    // 3. Nhóm tiện ích hệ thống & phần cứng
    var mediaOn by remember { mutableStateOf(prefs.getBoolean("cast_media_sessions", true)) }
    var earbudsOn by remember {
        mutableStateOf(prefs.getBoolean("cast_earbuds", true) && EarbudsCard.hasPermission(context))
    }
    var chargingOn by remember { mutableStateOf(prefs.getBoolean("cast_charging", true)) }
    var chargingKeep by remember { mutableStateOf(prefs.getBoolean("cast_charging_keep", false)) }
    var ringerModeOn by remember { mutableStateOf(prefs.getBoolean("cast_ringer_mode", true)) }
    var flashlightOn by remember { mutableStateOf(prefs.getBoolean("cast_flashlight", true)) }
    var privacyDotOn by remember { mutableStateOf(prefs.getBoolean("cast_privacy_dot", true)) }

    // 4. Nhóm quy tắc hiển thị & Giờ yên tĩnh
    var includeMessages by remember { mutableStateOf(prefs.getBoolean("cast_include_messages", false)) }
    var autoDismissSec by remember { mutableStateOf(prefs.getInt("cast_auto_dismiss_seconds", 5)) }
    var ignoreSilent by remember { mutableStateOf(prefs.getBoolean("cast_ignore_silent", true)) }
    var lockscreenLiveCard by remember { mutableStateOf(prefs.getBoolean("cast_lockscreen_live_card", false)) }
    var lockscreenRedactSensitive by remember { mutableStateOf(prefs.getBoolean("cast_lockscreen_redact_sensitive", true)) }
    var quietDnd by remember { mutableStateOf(prefs.getBoolean("cast_quiet_dnd", false)) }
    var quietSchedule by remember { mutableStateOf(prefs.getBoolean("cast_quiet_schedule", false)) }
    var quietStart by remember { mutableIntStateOf(prefs.getInt("cast_quiet_start", QuietHours.DEFAULT_START)) }
    var quietEnd by remember { mutableIntStateOf(prefs.getInt("cast_quiet_end", QuietHours.DEFAULT_END)) }
    var actionsOn by remember { mutableStateOf(prefs.getBoolean("cast_show_actions", true)) }

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
    var updateDialogRelease by remember { mutableStateOf<UpdateChecker.ReleaseInfo?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(autoUpdateRelease) {
        if (autoUpdateRelease != null) {
            updateDialogRelease = autoUpdateRelease
        }
    }

    val checkForUpdates = {
        coroutineScope.launch {
            updateStatus = "Đang kiểm tra…"
            when (val result = UpdateChecker.check(context)) {
                is UpdateChecker.Result.UpdateAvailable -> {
                    updateStatus = "Có bản cập nhật mới: v${result.release.versionName}"
                    updateDialogRelease = result.release
                }
                is UpdateChecker.Result.UpToDate -> {
                    val msg = "Bạn đang dùng phiên bản mới nhất (v${result.current})"
                    updateStatus = msg
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
                is UpdateChecker.Result.Error -> {
                    val msg = result.message
                    updateStatus = msg
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    var connected by remember { mutableStateOf(NotificationCastListener.instance != null) }
    var deviceSnapshot by remember { mutableStateOf(DeviceInfoHelper.collect(context)) }
    val superXSupported = remember { DeviceInfoHelper.isSuperXSupported(context) }

    LaunchedEffect(tick.intValue) {
        deviceSnapshot = DeviceInfoHelper.collect(context)
    }

    LaunchedEffect(Unit) {
        CastLog.ensureLoaded(context)
        deviceSnapshot = DeviceInfoHelper.collect(context)
        while (true) {
            connected = NotificationCastListener.instance != null
            delay(1000)
        }
    }

    if (permissionsOpen) {
        PermissionsSheet(context, prefs, onDismiss = { permissionsOpen = false }, onRedoSetup = onRedoSetup)
    }

    updateDialogRelease?.let { release ->
        UpdateDialog(
            release = release,
            onDismiss = {
                updateDialogRelease = null
                onDismissUpdate()
            },
        )
    }

    val showHero = matchesSearchQuery(
        searchQuery,
        "thông tin thiết bị", "trạng thái", "superx", "ram", "pin", "chip", "soc",
        deviceSnapshot.modelName, deviceSnapshot.originOsVersion, deviceSnapshot.socName,
    )
    val showGroup1 = matchesSearchQuery(
        searchQuery,
        "trạng thái", "dịch vụ cốt lõi", "đảo origin", "giao diện", "sáng", "tối", "vị trí đảo", "trái", "giữa", "phải",
    )
    val showGroupCustom20 = matchesSearchQuery(
        searchQuery,
        "tùy biến đảo", "chặn pop-up", "popup", "viên thuốc", "heads-up", "snooze", "màu viền",
        "phát sáng", "thời lượng", "mở rộng thẻ", "chạm viên thuốc",
    )
    val showGroup2 = matchesSearchQuery(
        searchQuery,
        "dịch vụ trực tiếp", "tiện ích việt nam", "gọi xe", "grab", "be", "xanh sm", "shopeefood",
        "ngân hàng", "biến động số dư", "zalo", "tiktok", "chuỗi", "tải xuống", "quick share", "lịch", "cuộc gọi",
    )
    val showGroup3 = matchesSearchQuery(
        searchQuery,
        "tiện ích hệ thống", "phần cứng", "nhạc", "spotify", "tai nghe", "earbuds", "pin", "sạc",
        "âm thanh", "chuông", "rung", "im lặng", "đèn pin", "camera", "micro", "bảo mật",
    )
    val showGroup4 = matchesSearchQuery(
        searchQuery,
        "quy tắc hiển thị", "giờ yên tĩnh", "lọc thông báo", "tự ẩn", "màn hình khóa",
        "otp", "số dư", "dnd", "không làm phiền", "nút thao tác",
    )
    val showGroup5 = matchesSearchQuery(
        searchQuery,
        "quản lý", "hỗ trợ", "ứng dụng được phép", "quyền hệ thống", "nhật ký hoạt động", "cập nhật",
    )
    val showTester = matchesSearchQuery(
        searchQuery,
        "phòng thử nghiệm", "mẫu đảo", "hiện lại tất cả", "dừng tất cả",
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = ScreenPadding,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Tiêu đề "Cài đặt" + Menu More + Thanh tìm kiếm Pill
        item {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Cài đặt",
                        fontSize = 26.sp,
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
                                checkForUpdates()
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

                // Thanh tìm kiếm Pill shape bo tròn chuẩn Vivo Settings
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(IsleColors.SurfaceHigh)
                        .border(0.8.dp, IsleColors.BorderHairline, RoundedCornerShape(22.dp))
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        IsleIcons.Search,
                        contentDescription = null,
                        tint = IsleColors.TextSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Tìm kiếm cài đặt Đảo Origin",
                                color = IsleColors.TextSecondary,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 14.sp, color = IsleColors.Text),
                            cursorBrush = SolidColor(IsleColors.Accent),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .clickable(role = Role.Button, onClick = { searchQuery = "" }),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                IsleIcons.Close,
                                contentDescription = "Xóa tìm kiếm",
                                tint = IsleColors.TextSecondary,
                                modifier = Modifier.size(15.dp),
                            )
                        }
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

        // BANNER QUYỀN THÔNG MINH: Chỉ hiển thị khi thiếu quyền hoặc bị chặn bởi OriginOS
        val missingPermissions = !connected || !listenerOk || !batteryOk
        if (missingPermissions && searchQuery.isBlank()) {
            item {
                VivoOriginAttentionBanner(
                    connected = connected,
                    listenerOk = listenerOk,
                    batteryOk = batteryOk,
                    onOpenPermissions = { permissionsOpen = true },
                    onOpenSettings = {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }
                    },
                    onReconnect = {
                        recastAll(context)
                    },
                )
            }
        }

        // THẺ HERO CHÍNH: THÔNG TIN THIẾT BỊ & TRẠNG THÁI ORIGIN ISLAND 2.0.0
        if (showHero) {
            item {
                DeviceHeroCard(
                    snapshot = deviceSnapshot,
                    castOn = castOn,
                    connected = connected,
                    superXSupported = superXSupported,
                    customLightColor = customLightColor,
                    islandPosition = islandPosition,
                    islandDurationSec = islandDurationSec,
                    onRefresh = {
                        CastLog.ensureLoaded(context)
                        deviceSnapshot = DeviceInfoHelper.collect(context)
                    },
                )
            }
        }

        // ==========================================
        // NHÓM 1: TRẠNG THÁI & DỊCH VỤ CỐT LÕI
        // ==========================================
        if (showGroup1) item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("Trạng thái & Dịch vụ cốt lõi")
                GroupCard {
                    // Thẻ công tắc Đảo tổng & trạng thái kết nối
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (castOn) IsleColors.Accent else IsleColors.SwitchOff),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(22.dp)
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(Color.White),
                            )
                        }

                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                "Dịch vụ Đảo Origin",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = IsleColors.Text,
                            )
                            Text(
                                if (!castOn) "Đang tắt tất cả thẻ hiển thị"
                                else if (connected) "Trạng thái: Hoạt động · Đã kết nối"
                                else "Trạng thái: Chưa liên kết dịch vụ",
                                fontSize = 13.sp,
                                color = if (!castOn) IsleColors.TextSecondary else if (connected) IsleColors.GoodText else IsleColors.Danger,
                            )
                        }

                        IsleSwitch(
                            checked = castOn,
                            onCheckedChange = {
                                castOn = it
                                prefs.edit().putBoolean("cast_notifications", it).apply()
                                if (it) PlaygroundService.keepAlive(context)
                                PrivacyCard.refresh()
                                FlashlightCard.refresh()
                                EarbudsCard.onSettingsChanged(context, NotificationCastListener.instance != null)
                            },
                        )
                    }

                    GroupDivider()

                    // Tùy chọn Giao diện Sáng / Tối / Theo hệ thống tinh gọn chuẩn Vivo
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "Giao diện ứng dụng & Đảo",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IsleColors.Text,
                        )
                        val themeIndex = when (themePref) {
                            "LIGHT" -> 1
                            "DARK" -> 2
                            else -> 0
                        }
                        SegmentedControl(
                            options = listOf("Theo hệ thống", "Sáng", "Tối"),
                            selected = themeIndex,
                            onSelect = { idx ->
                                val selectedTheme = when (idx) {
                                    1 -> "LIGHT"
                                    2 -> "DARK"
                                    else -> "SYSTEM"
                                }
                                themePref = selectedTheme
                                prefs.edit().putString("app_theme", selectedTheme).apply()
                            },
                        )
                    }

                    GroupDivider()

                    // Tùy chọn Vị trí Đảo (Trái / Giữa / Phải)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "Vị trí Đảo trên màn hình",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IsleColors.Text,
                        )
                        val posIndex = when (islandPosition) {
                            "LEFT" -> 0
                            "RIGHT" -> 2
                            else -> 1
                        }
                        SegmentedControl(
                            options = listOf("Bên trái", "Ở giữa", "Bên phải"),
                            selected = posIndex,
                            onSelect = { idx ->
                                val selectedPos = when (idx) {
                                    0 -> "LEFT"
                                    2 -> "RIGHT"
                                    else -> "CENTER"
                                }
                                islandPosition = selectedPos
                                prefs.edit().putString("island_position", selectedPos).apply()
                            },
                        )
                    }
                }
            }
        }

        // ==========================================
        // NHÓM TÙY BIẾN ĐẢO 2.0.0 & CHẶN POP-UP
        // ==========================================
        if (showGroupCustom20) item {
            Column(
                modifier = Modifier.alpha(if (castOn) 1f else 0.4f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SectionLabel("Tùy biến Đảo 2.0.0 & Chặn Pop-up")
                GroupCard {
                    ToggleRow(
                        IsleIcons.Shield,
                        IsleColors.Accent,
                        title = "Chỉ hiện trên viên thuốc (Chặn Pop-up nổi)",
                        checked = suppressHeadsUp,
                        subtitle = "Ngăn biểu ngữ thả xuống che màn hình, giữ trải nghiệm tập trung trên Đảo",
                    ) {
                        suppressHeadsUp = it
                        prefs.edit().putBoolean(PREF_CAST_SUPPRESS_HEADS_UP, it).apply()
                    }

                    NestedSettingContainer(visible = suppressHeadsUp) {
                        Text(
                            "Chế độ xử lý thông báo gốc:",
                            fontSize = 13.sp,
                            color = IsleColors.TextSecondary,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                SUPPRESS_MODE_SNOOZE to "Ẩn Pop-up nổi (Giữ trong khay)",
                                SUPPRESS_MODE_ISLAND_ONLY to "Chỉ hiện trên Đảo (Ẩn thông báo phụ)",
                            ).forEach { (mode, label) ->
                                ChoiceChip(
                                    label = label,
                                    selected = suppressMode == mode,
                                    onClick = {
                                        suppressMode = mode
                                        prefs.edit().putString(PREF_CAST_SUPPRESS_MODE, mode).apply()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalPadding = 12.dp,
                                )
                            }
                        }
                        Text(
                            if (suppressMode == SUPPRESS_MODE_ISLAND_ONLY) {
                                "Thông báo đã phát lên Đảo sẽ không lưu thêm bản phụ trong thanh thông báo."
                            } else {
                                "Chỉ chặn biểu ngữ pop-up bật xuống, vẫn lưu thông báo trong khay hệ thống."
                            },
                            fontSize = 12.sp,
                            color = IsleColors.TextSecondary,
                            lineHeight = 16.sp,
                        )
                    }

                    GroupDivider()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "Màu viền phát sáng Đảo",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = IsleColors.Text,
                            )
                            Text(
                                "Chọn màu hào quang xung quanh viên thuốc Origin Island 2.0.0",
                                fontSize = 13.sp,
                                color = IsleColors.TextSecondary,
                            )
                        }
                        val lightOptions = listOf(
                            Triple(LIGHT_COLOR_AUTO, "Tự động", null),
                            Triple(LIGHT_COLOR_NONE, "Tắt viền", null),
                            Triple(LIGHT_COLOR_EMERALD, "Ngọc lục", Color(0xFF00D084)),
                            Triple(LIGHT_COLOR_BLUE, "Xanh lam", Color(0xFF0A59F7)),
                            Triple(LIGHT_COLOR_PURPLE, "Tím Neon", Color(0xFF9D4EDD)),
                            Triple(LIGHT_COLOR_AMBER, "Hổ phách", Color(0xFFFFB020)),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            lightOptions.chunked(3).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    rowItems.forEach { (value, label, color) ->
                                        ColorSwatchPill(
                                            label = label,
                                            swatchColor = color,
                                            isAuto = value == LIGHT_COLOR_AUTO,
                                            selected = customLightColor == value,
                                            onClick = {
                                                customLightColor = value
                                                prefs.edit().putString(PREF_CAST_CUSTOM_LIGHT_COLOR, value).apply()
                                            },
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    GroupDivider()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "Thời lượng hiển thị viên thuốc",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IsleColors.Text,
                        )
                        val durationValues = listOf(3, 5, 7, 10)
                        val durationIdx = durationValues.indexOf(islandDurationSec).let { if (it >= 0) it else 1 }
                        SegmentedControl(
                            options = listOf("3s", "5s", "7s", "10s"),
                            selected = durationIdx,
                            onSelect = { idx ->
                                val sec = durationValues.getOrElse(idx) { DEFAULT_ISLAND_DURATION_SEC }
                                islandDurationSec = sec
                                prefs.edit().putInt(PREF_CAST_ISLAND_DURATION_SEC, sec).apply()
                            },
                        )
                    }

                    GroupDivider()

                    ToggleRow(
                        IsleIcons.Buttons,
                        IsleColors.Accent,
                        title = "Chạm viên thuốc để mở rộng thẻ",
                        checked = tapExpandCard,
                        subtitle = "Nhấn vào viên thuốc thu gọn để bung thẻ thông báo chi tiết trên Đảo",
                    ) {
                        tapExpandCard = it
                        prefs.edit().putBoolean(PREF_CAST_TAP_EXPAND_CARD, it).apply()
                    }
                }
            }
        }

        // ==========================================
        // NHÓM 2: DỊCH VỤ TRỰC TIẾP & TIỆN ÍCH VIỆT NAM
        // ==========================================
        if (showGroup2) item {
            Column(
                modifier = Modifier.alpha(if (castOn) 1f else 0.4f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SectionLabel("Dịch vụ Trực tiếp & Tiện ích Việt Nam")
                GroupCard {
                    ToggleRow(
                        IsleIcons.Smartphone, IsleColors.Accent,
                        title = "Gọi xe & Giao đồ ăn Việt Nam",
                        checked = ridesOn,
                        subtitle = "Grab, Be, Xanh SM, ShopeeFood (tiến trình chuyến đi & giao hàng)",
                    ) {
                        ridesOn = it
                        prefs.edit().putBoolean("cast_rides", it).apply()
                    }

                    GroupDivider()

                    ToggleRow(
                        IsleIcons.CreditCard, IsleColors.Accent,
                        title = "Biến động số dư Ngân hàng",
                        checked = paymentOn,
                        subtitle = "Thông báo tiền vào (+), tiền ra (-) các ngân hàng Việt Nam",
                    ) {
                        paymentOn = it
                        prefs.edit().putBoolean("cast_payment", it).putBoolean("cast_vietnam_banks", it).apply()
                    }

                    GroupDivider()

                    ToggleRow(
                        IsleIcons.Pulse, IsleColors.Accent,
                        title = "Báo cuộc gọi Zalo trên Đảo",
                        checked = zaloCallOn,
                        subtitle = "Hiển thị người gọi, trạng thái & nút Từ chối / Trả lời cho cuộc gọi Zalo",
                    ) {
                        zaloCallOn = it
                        prefs.edit().putBoolean("cast_zalo_call", it).apply()
                    }

                    GroupDivider()

                    ToggleRow(
                        IsleIcons.Bolt, Color(0xFFFF5E00),
                        title = "Nhắc nhở chuỗi TikTok",
                        checked = tiktokStreakOn,
                        subtitle = "Bắt thông báo nhắc chuỗi TikTok, hiện đảo ngọn lửa để gửi tin nhắn duy trì nhanh",
                    ) {
                        tiktokStreakOn = it
                        prefs.edit().putBoolean("cast_tiktok_streak", it).apply()
                    }

                    NestedSettingContainer(visible = tiktokStreakOn) {
                        Text(
                            "Nội dung tin nhắn gửi chuỗi:",
                            fontSize = 13.sp,
                            color = IsleColors.TextSecondary,
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
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
                                textStyle = TextStyle(
                                    fontSize = 14.sp,
                                    color = IsleColors.Text,
                                ),
                                cursorBrush = SolidColor(Color(0xFFFF5E00)),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Text(
                            "Tin nhắn trên sẽ tự động sao chép vào bộ nhớ tạm (Clipboard) khi nhấn nút ngọn lửa.",
                            fontSize = 12.sp,
                            color = IsleColors.TextSecondary,
                            lineHeight = 16.sp,
                        )
                    }

                    GroupDivider()

                    ToggleRow(
                        IsleIcons.Download, IsleColors.Accent,
                        title = "Tiến trình Tải xuống & Quick Share",
                        checked = downloadOn,
                        subtitle = "Hiển thị phần trăm tải tệp trình duyệt và chia sẻ nhanh",
                    ) {
                        downloadOn = it
                        prefs.edit().putBoolean("cast_downloads", it).apply()
                    }

                    GroupDivider()

                    ToggleRow(
                        IsleIcons.Clock, IsleColors.Accent,
                        title = "Nhắc lịch & Sự kiện sắp tới",
                        checked = castCalendarEvents,
                        subtitle = "Đếm ngược cuộc họp, lịch hẹn từ Google Calendar, Lịch Vivo, Outlook",
                    ) {
                        castCalendarEvents = it
                        prefs.edit().putBoolean("cast_calendar_events", it).apply()
                    }

                    GroupDivider()

                    ToggleRow(
                        IsleIcons.Pulse, IsleColors.Accent,
                        title = "Trạng thái Cuộc gọi & Đàm thoại",
                        checked = callsOn,
                        subtitle = "Hiển thị cuộc gọi đến, thời gian đàm thoại trực tiếp trên Đảo",
                    ) {
                        callsOn = it
                        prefs.edit().putBoolean("cast_calls", it).apply()
                    }
                }
            }
        }

        // ==========================================
        // NHÓM 3: TIỆN ÍCH HỆ THỐNG & PHẦN CỨNG
        // ==========================================
        if (showGroup3) item {
            Column(
                modifier = Modifier.alpha(if (castOn) 1f else 0.4f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SectionLabel("Tiện ích Hệ thống & Phần cứng")
                GroupCard {
                    ToggleRow(
                        IsleIcons.Music, IsleColors.Accent,
                        title = "Trình phát nhạc Đảo gốc OriginOS 6",
                        checked = mediaOn,
                        subtitle = "Hiển thị sóng nhạc động và điều khiển (Spotify, YouTube Music, Apple Music...)",
                    ) {
                        mediaOn = it
                        prefs.edit().putBoolean("cast_media_sessions", it).apply()
                    }

                    GroupDivider()

                    ToggleRow(
                        IsleIcons.Headphones, IsleColors.Accent,
                        title = "Tai nghe & Thiết bị đeo",
                        checked = earbudsOn,
                        subtitle = "Samsung Galaxy Fit 3, Galaxy Watch, Buds... hiển thị pin chi tiết",
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
                        IsleIcons.Bolt, IsleColors.Accent,
                        title = "Pin & Trạng thái sạc",
                        checked = chargingOn,
                        subtitle = "Hiển thị thẻ sạc pin trực quan chuẩn Vivo",
                    ) {
                        chargingOn = it
                        prefs.edit().putBoolean("cast_charging", it).apply()
                        ChargingCard.onSettingsChanged(context)
                    }

                    // Tùy chọn con lồng nhau mượt mà cho Sạc pin
                    NestedSettingContainer(visible = chargingOn) {
                        Text(
                            "Thời gian hiển thị thẻ sạc trên Đảo:",
                            fontSize = 13.sp,
                            color = IsleColors.TextSecondary,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(false to "Thoáng qua (5s)", true to "Suốt thời gian sạc").forEach { (keep, label) ->
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

                    // Công tắc cho Chế độ Âm thanh (Chuông / Rung / Im lặng / DND)
                    ToggleRow(
                        IsleIcons.Bell, IsleColors.Accent,
                        title = "Chế độ Âm thanh Chuông / Rung / Im lặng",
                        checked = ringerModeOn,
                        subtitle = "Hiển thị thẻ đảo khi chuyển đổi chuông, rung hoặc chế độ im lặng",
                    ) {
                        ringerModeOn = it
                        prefs.edit().putBoolean("cast_ringer_mode", it).apply()
                    }

                    GroupDivider()

                    // Công tắc cho Đèn pin
                    ToggleRow(
                        IsleIcons.Sun, IsleColors.Accent,
                        title = "Đèn pin",
                        checked = flashlightOn,
                        subtitle = "Thẻ điều khiển tắt đèn pin trực tiếp nhanh chóng từ Đảo",
                    ) {
                        flashlightOn = it
                        prefs.edit().putBoolean("cast_flashlight", it).apply()
                        FlashlightCard.refresh()
                    }

                    GroupDivider()

                    ToggleRow(
                        IsleIcons.Camera, IsleColors.Accent,
                        title = "Chỉ báo bảo mật Camera & Micro",
                        checked = privacyDotOn,
                        subtitle = "Chấm bảo mật quyền riêng tư: Camera đỏ, Micro xanh lá bo góc",
                    ) {
                        privacyDotOn = it
                        prefs.edit().putBoolean("cast_privacy_dot", it).apply()
                        PrivacyCard.refresh()
                    }
                }
            }
        }

        // ==========================================
        // NHÓM 4: QUY TẮC HIỂN THỊ & GIỜ YÊN TĨNH
        // ==========================================
        if (showGroup4) item {
            Column(
                modifier = Modifier.alpha(if (castOn) 1f else 0.4f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SectionLabel("Quy tắc Hiển thị & Giờ yên tĩnh")
                GroupCard {
                    // Chế độ lọc thông báo
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "Chế độ lọc thông báo",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IsleColors.Text,
                        )
                        SegmentedControl(
                            options = listOf("Chỉ thẻ động trực tiếp", "Tất cả thông báo"),
                            selected = if (includeMessages) 1 else 0,
                            onSelect = { idx ->
                                includeMessages = idx == 1
                                prefs.edit().putBoolean("cast_include_messages", includeMessages).apply()
                            },
                        )
                    }

                    // Tùy chọn con lồng nhau: Thời gian tự ẩn cho thẻ tin nhắn thường
                    NestedSettingContainer(visible = includeMessages) {
                        Text(
                            "Tự ẩn sau (dành cho thẻ thông báo tin nhắn thường):",
                            fontSize = 13.sp,
                            color = IsleColors.TextSecondary,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                        IsleIcons.Moon, IsleColors.Accent,
                        title = "Bỏ qua thông báo im lặng",
                        checked = ignoreSilent,
                        subtitle = "Không hiển thị thông báo không có chuông hoặc rung",
                    ) {
                        ignoreSilent = it
                        prefs.edit().putBoolean("cast_ignore_silent", it).apply()
                    }

                    GroupDivider()

                    ToggleRow(
                        IsleIcons.Lock, IsleColors.Accent,
                        title = "Hiển thị trên màn hình khóa",
                        checked = lockscreenLiveCard,
                        subtitle = "Hiển thị thẻ đảo động ngay cả khi thiết bị đang khóa màn hình",
                    ) {
                        lockscreenLiveCard = it
                        prefs.edit().putBoolean("cast_lockscreen_live_card", it).apply()
                    }

                    GroupDivider()

                    ToggleRow(
                        IsleIcons.Lock, IsleColors.Accent,
                        title = "Ẩn số dư & OTP khi khóa máy",
                        checked = lockscreenRedactSensitive,
                        subtitle = "Che số tiền chuyển khoản và mã xác thực OTP trên Đảo khi màn hình đang khóa",
                    ) {
                        lockscreenRedactSensitive = it
                        prefs.edit().putBoolean("cast_lockscreen_redact_sensitive", it).apply()
                    }

                    GroupDivider()

                    ToggleRow(
                        IsleIcons.Moon, IsleColors.Accent,
                        title = "Theo chế độ Không làm phiền (DND)",
                        checked = quietDnd,
                        subtitle = "Tự động tắt Đảo khi thiết bị bật chế độ Không làm phiền",
                    ) {
                        quietDnd = it
                        prefs.edit().putBoolean("cast_quiet_dnd", it).apply()
                    }

                    GroupDivider()

                    ToggleRow(
                        IsleIcons.Clock, IsleColors.Accent,
                        title = "Lịch đặt giờ yên tĩnh",
                        checked = quietSchedule,
                        subtitle = "${QuietHours.format(quietStart)} – ${QuietHours.format(quietEnd)} hàng ngày",
                    ) {
                        quietSchedule = it
                        prefs.edit().putBoolean("cast_quiet_schedule", it).apply()
                    }

                    // Tùy chọn con lồng nhau: Chọn khung giờ yên tĩnh
                    NestedSettingContainer(visible = quietSchedule) {
                        Text(
                            "Khung giờ tắt thông báo Đảo hàng ngày:",
                            fontSize = 13.sp,
                            color = IsleColors.TextSecondary,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ChoiceChip(
                                "Từ ${QuietHours.format(quietStart)}",
                                selected = false,
                                onClick = {
                                    pickTime(context, quietStart) {
                                        quietStart = it
                                        prefs.edit().putInt("cast_quiet_start", it).apply()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            )
                            ChoiceChip(
                                "Đến ${QuietHours.format(quietEnd)}",
                                selected = false,
                                onClick = {
                                    pickTime(context, quietEnd) {
                                        quietEnd = it
                                        prefs.edit().putInt("cast_quiet_end", it).apply()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    GroupDivider()

                    ToggleRow(
                        IsleIcons.Buttons, IsleColors.Accent,
                        title = "Nút thao tác trên thẻ",
                        checked = actionsOn,
                        subtitle = "Hiển thị các nút trả lời nhanh, sao chép, hủy bỏ",
                    ) {
                        actionsOn = it
                        prefs.edit().putBoolean("cast_show_actions", it).apply()
                    }
                }
            }
        }

        // ==========================================
        // NHÓM 5: QUẢN LÝ & TRẢI NGHIỆM ĐẢO
        // ==========================================
        if (showGroup5) item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("Quản lý & Hỗ trợ")
                GroupCard {
                    NavRow(
                        IsleIcons.Grid, IsleColors.Accent,
                        title = "Ứng dụng được phép",
                        value = if (appsOff == 0) "Đang bật tất cả" else "Đã tắt $appsOff",
                        onClick = onOpenApps,
                    )
                    GroupDivider()
                    NavRow(
                        IsleIcons.Shield, IsleColors.Accent,
                        title = "Quyền hệ thống",
                        value = if (listenerOk && batteryOk) "Đã hoàn tất" else "Cần cấp quyền",
                        onClick = { permissionsOpen = true },
                    )
                    GroupDivider()
                    NavRow(
                        IsleIcons.Pulse, IsleColors.Accent,
                        title = "Nhật ký hoạt động",
                        onClick = onOpenActivity,
                    )
                    GroupDivider()
                    NavRow(
                        IsleIcons.Refresh, IsleColors.Accent,
                        title = "Kiểm tra cập nhật",
                        value = updateStatus,
                        onClick = { checkForUpdates() },
                    )
                }
            }
        }

        // Phòng thử nghiệm mẫu Đảo & Nút điều khiển
        if (showTester) item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionLabel("Phòng thử nghiệm mẫu Đảo")
                SampleTester(context)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButton(
                        "Hiện lại tất cả",
                        onClick = { recastAll(context) },
                        modifier = Modifier.weight(1f),
                        icon = IsleIcons.Refresh,
                        enabled = castOn || mediaOn,
                    )
                    PillButton(
                        "Dừng tất cả",
                        onClick = { stopAll(context) },
                        modifier = Modifier.weight(1f),
                        icon = IsleIcons.Stop,
                        iconSize = 16.dp,
                        content = IsleColors.Danger,
                    )
                }
            }
        }

        if (
            searchQuery.isNotBlank() &&
            !showHero && !showGroup1 && !showGroupCustom20 &&
            !showGroup2 && !showGroup3 && !showGroup4 && !showGroup5 && !showTester
        ) {
            item {
                EmptyMessage("Không tìm thấy cài đặt phù hợp.")
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

@Composable
private fun VivoOriginAttentionBanner(
    connected: Boolean,
    listenerOk: Boolean,
    batteryOk: Boolean,
    onOpenPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
    onReconnect: () -> Unit,
) {
    val isRed = !connected || !listenerOk
    val bg = if (isRed) IsleColors.BadBg else IsleColors.WarnBg
    val borderCol = if (isRed) IsleColors.Bad.copy(alpha = 0.5f) else IsleColors.Warn.copy(alpha = 0.5f)
    val textCol = if (isRed) IsleColors.BadText else IsleColors.Warn
    val iconTileCol = if (isRed) IsleColors.Bad else IsleColors.TileOrange

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(bg)
            .border(1.dp, borderCol, RoundedCornerShape(22.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onOpenPermissions),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconTile(IsleIcons.Shield, iconTileCol)
            Column(Modifier.weight(1f)) {
                Text(
                    if (!connected) "Dịch vụ thông báo chưa kết nối" else "Cài đặt cần lưu ý trên Vivo OriginOS",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = IsleColors.Text,
                )
                Text(
                    if (!listenerOk) {
                        "Chưa cấp quyền Truy cập thông báo"
                    } else if (!connected) {
                        "Dịch vụ bị OriginOS chặn hoặc cần mở khóa Cài đặt bị hạn chế"
                    } else {
                        "Cần tắt hạn chế pin để chạy nền ổn định"
                    },
                    fontSize = 13.sp,
                    color = textCol,
                )
            }
            Icon(IsleIcons.ChevronRight, null, Modifier.size(18.dp), tint = textCol)
        }

        if (!connected) {
            Text(
                "Để khắc phục trên OriginOS: Vào Cài đặt ứng dụng -> dấu 3 chấm góc trên -> Cho phép cài đặt bị hạn chế (Restricted Settings) & bật Tự khởi chạy.",
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = IsleColors.TextSecondary,
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PillButton(
                    text = "Cài đặt ứng dụng",
                    onClick = onOpenSettings,
                    modifier = Modifier.weight(1f),
                    container = IsleColors.SurfaceHigh,
                    content = IsleColors.Text,
                    height = 36.dp,
                )
                PillButton(
                    text = "Thử kết nối lại",
                    onClick = onReconnect,
                    modifier = Modifier.weight(1f),
                    icon = IsleIcons.Refresh,
                    container = if (isRed) IsleColors.Bad else IsleColors.Accent,
                    content = Color.White,
                    height = 36.dp,
                )
            }
        }
    }
}

@Composable
private fun DeviceHeroCard(
    snapshot: DeviceSnapshot,
    castOn: Boolean,
    connected: Boolean,
    superXSupported: Boolean,
    customLightColor: String,
    islandPosition: String,
    islandDurationSec: Int,
    onRefresh: () -> Unit,
) {
    val targetGlowColor = resolveGlowColor(customLightColor, IsleColors.GlowAccent)
    val animatedGlowColor by animateColorAsState(
        targetValue = if (castOn) targetGlowColor else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "heroIslandGlow",
    )
    val hasGlow = castOn && customLightColor != LIGHT_COLOR_NONE
    var spinDegrees by remember { mutableFloatStateOf(0f) }
    val animatedSpin by animateFloatAsState(
        targetValue = spinDegrees,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "heroRefreshSpin",
    )

    val displayModel = remember(snapshot.marketBrand, snapshot.modelName) {
        if (snapshot.modelName.startsWith(snapshot.marketBrand, ignoreCase = true)) {
            snapshot.modelName
        } else {
            "${snapshot.marketBrand} ${snapshot.modelName}"
        }
    }

    GroupCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Mô phỏng khung màn hình & viên thuốc Origin Island 2.0.0 thu nhỏ
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(IsleColors.PreviewBg)
                    .border(0.8.dp, IsleColors.BorderHairline, RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val pillAlignment = when (islandPosition) {
                    "LEFT" -> Alignment.CenterStart
                    "RIGHT" -> Alignment.CenterEnd
                    else -> Alignment.Center
                }
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = pillAlignment,
                ) {
                    if (islandPosition == "CENTER") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "09:41",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = IsleColors.PreviewLabel,
                            )
                            Text(
                                text = "${snapshot.batteryPct}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = IsleColors.PreviewLabel,
                            )
                        }
                    }

                    // Viên thuốc Origin Island 2.0.0 có viền hào quang phát sáng
                    Row(
                        modifier = Modifier
                            .width(164.dp)
                            .height(30.dp)
                            .drawBehind {
                                if (hasGlow) {
                                    val radius = size.height / 2f
                                    drawRoundRect(
                                        color = animatedGlowColor.copy(alpha = 0.14f),
                                        topLeft = Offset(-5.dp.toPx(), -5.dp.toPx()),
                                        size = Size(size.width + 10.dp.toPx(), size.height + 10.dp.toPx()),
                                        cornerRadius = CornerRadius(radius + 5.dp.toPx(), radius + 5.dp.toPx()),
                                    )
                                    drawRoundRect(
                                        color = animatedGlowColor.copy(alpha = 0.30f),
                                        topLeft = Offset(-2.dp.toPx(), -2.dp.toPx()),
                                        size = Size(size.width + 4.dp.toPx(), size.height + 4.dp.toPx()),
                                        cornerRadius = CornerRadius(radius + 2.dp.toPx(), radius + 2.dp.toPx()),
                                        style = Stroke(width = 2.dp.toPx()),
                                    )
                                }
                            }
                            .clip(CircleShape)
                            .background(Color.Black)
                            .then(
                                if (hasGlow) {
                                    Modifier.border(
                                        width = 1.4.dp,
                                        brush = Brush.horizontalGradient(
                                            listOf(
                                                animatedGlowColor,
                                                animatedGlowColor.copy(alpha = 0.65f),
                                                animatedGlowColor,
                                            ),
                                        ),
                                        shape = CircleShape,
                                    )
                                } else {
                                    Modifier.border(1.dp, Color(0xFF2E2E32), CircleShape)
                                },
                            )
                            .padding(horizontal = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (!castOn) Color(0xFF5A5D66)
                                        else if (hasGlow) animatedGlowColor
                                        else IsleColors.Accent,
                                    ),
                            )
                            Text(
                                text = if (castOn) "Origin" else "Tạm tắt",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = if (castOn) 0.92f else 0.55f),
                                maxLines = 1,
                            )
                        }

                        // Camera punch-hole
                        Box(
                            modifier = Modifier
                                .size(11.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0C0D12))
                                .border(0.8.dp, Color(0xFF252832), CircleShape),
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            listOf(0.5f, 0.95f, 0.65f).forEach { ratio ->
                                Box(
                                    modifier = Modifier
                                        .width(2.5.dp)
                                        .height((11f * ratio).dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (castOn && hasGlow) animatedGlowColor else Color.White.copy(alpha = 0.7f),
                                        ),
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Viền sáng: ${lightColorLabel(customLightColor)} · Giữ ${islandDurationSec}s",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = IsleColors.PreviewLabel,
                    )
                    Text(
                        text = if (castOn) "2.0.0 LIVE" else "CHỜ BẬT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (castOn) IsleColors.AccentText else IsleColors.PreviewLabel,
                    )
                }
            }

            // Tiêu đề thiết bị + Hệ điều hành + Nút làm mới thông số nhanh
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(IsleColors.Accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        IsleIcons.Smartphone,
                        contentDescription = null,
                        tint = IsleColors.Accent,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = displayModel,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = IsleColors.Text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${snapshot.originOsVersion} · Android ${snapshot.androidVersion}",
                        fontSize = 13.sp,
                        color = IsleColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(IsleColors.SurfaceHigh)
                        .border(0.8.dp, IsleColors.BorderHairline, CircleShape)
                        .clickable(
                            role = Role.Button,
                            onClick = {
                                spinDegrees += 360f
                                onRefresh()
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        IsleIcons.Refresh,
                        contentDescription = "Làm mới thông số thiết bị",
                        tint = IsleColors.Accent,
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(animatedSpin),
                    )
                }
            }

            // Lưới thông số phần cứng (Squircle 16dp SurfaceHigh + Hairline 0.8dp)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HeroSpecTile(
                        label = "Vi xử lý (SoC)",
                        value = snapshot.socName,
                        modifier = Modifier.weight(1f),
                    )
                    HeroSpecTile(
                        label = "RAM khả dụng / Tổng",
                        value = "${snapshot.availRamGb} / ${snapshot.totalRamGb}",
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val tempText = String.format(Locale.US, "%.1f°C", snapshot.batteryTempC)
                    val batteryText = buildString {
                        append("${snapshot.batteryPct}% · $tempText")
                        if (snapshot.isCharging) append(" · Sạc")
                    }
                    HeroSpecTile(
                        label = "Pin & Nhiệt độ",
                        value = batteryText,
                        valueColor = when {
                            snapshot.batteryTempC >= 42f -> IsleColors.Warn
                            snapshot.isCharging -> IsleColors.GoodText
                            else -> IsleColors.Text
                        },
                        modifier = Modifier.weight(1f),
                    )
                    HeroSpecTile(
                        label = "Lượt Đảo hôm nay",
                        value = "${snapshot.todayCastCount} thẻ đã phát",
                        valueColor = IsleColors.AccentText,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Thanh trạng thái SuperX Engine & Phiên bản 2.0.0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(IsleColors.SurfaceHigh)
                    .border(0.8.dp, IsleColors.BorderHairline, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val engineColor = when {
                        superXSupported && connected && castOn -> IsleColors.Good
                        superXSupported -> IsleColors.Warn
                        else -> IsleColors.Accent
                    }
                    val engineLabel = when {
                        superXSupported && connected && castOn -> "SuperX Engine: Hoạt động"
                        superXSupported && !castOn -> "SuperX Engine: Đang tạm tắt"
                        superXSupported -> "SuperX Engine: Chờ kết nối"
                        else -> "SuperX Engine: Chế độ tương thích"
                    }
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(engineColor),
                    )
                    Text(
                        text = engineLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = IsleColors.Text,
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(IsleColors.Accent.copy(alpha = 0.14f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "v2.0.0",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = IsleColors.AccentText,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroSpecTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = IsleColors.Text,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(IsleColors.SurfaceHigh)
            .border(0.8.dp, IsleColors.BorderHairline, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = IsleColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun resolveGlowColor(prefValue: String, autoFallback: Color): Color = when (prefValue) {
    LIGHT_COLOR_NONE -> Color.Transparent
    LIGHT_COLOR_EMERALD -> Color(0xFF00D084)
    LIGHT_COLOR_BLUE -> Color(0xFF0A59F7)
    LIGHT_COLOR_PURPLE -> Color(0xFF9D4EDD)
    LIGHT_COLOR_AMBER -> Color(0xFFFFB020)
    else -> autoFallback
}

private fun lightColorLabel(prefValue: String): String = when (prefValue) {
    LIGHT_COLOR_NONE -> "Tắt"
    LIGHT_COLOR_EMERALD -> "Ngọc lục"
    LIGHT_COLOR_BLUE -> "Xanh lam"
    LIGHT_COLOR_PURPLE -> "Tím Neon"
    LIGHT_COLOR_AMBER -> "Hổ phách"
    else -> "Tự động"
}

private fun matchesSearchQuery(query: String, vararg keywords: String): Boolean {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return true
    val normalizedQuery = normalizeForSearch(trimmed)
    return keywords.any { normalizeForSearch(it).contains(normalizedQuery) }
}

private fun normalizeForSearch(input: String): String {
    val decomposed = Normalizer.normalize(input, Normalizer.Form.NFD)
    return decomposed
        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        .replace('đ', 'd')
        .replace('Đ', 'd')
        .lowercase(Locale.ROOT)
}
