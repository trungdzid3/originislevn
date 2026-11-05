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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
 * The main screen: listener status, the sample-card tester, casting settings, and links to the Apps
 * and Activity screens. Setup/permissions and the update check live in the ⋯ menu.
 */
@Composable
fun HomeScreen(
    context: Context,
    prefs: SharedPreferences,
    onRedoSetup: () -> Unit,
    onOpenApps: () -> Unit,
    onOpenActivity: () -> Unit,
) {
    var castOn by remember { mutableStateOf(prefs.getBoolean("cast_notifications", true)) }
    var mediaOn by remember { mutableStateOf(prefs.getBoolean("cast_media_sessions", false)) }
    var includeMessages by remember { mutableStateOf(prefs.getBoolean("cast_include_messages", false)) }
    var ignoreSilent by remember { mutableStateOf(prefs.getBoolean("cast_ignore_silent", true)) }
    var lockscreenLiveCard by remember { mutableStateOf(prefs.getBoolean("cast_lockscreen_live_card", false)) }
    var autoDismissSec by remember { mutableStateOf(prefs.getInt("cast_auto_dismiss_seconds", 0)) }
    var otpOn by remember { mutableStateOf(prefs.getBoolean("cast_otp", true)) }
    var downloadOn by remember { mutableStateOf(prefs.getBoolean("cast_downloads", true)) }
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
            // Denied, or the dialog was skipped (Android stops showing it after a refusal): the
            // app's own page is where it can still be turned on. Back here, the resume check
            // below picks it up — the switch stays wanted in prefs.
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
    // "Nearby devices" may have been turned on (or off) in Settings while we were away.
    LaunchedEffect(tick.intValue) {
        val now = prefs.getBoolean("cast_earbuds", false) && EarbudsCard.hasPermission(context)
        if (now != earbudsOn) {
            earbudsOn = now
            EarbudsCard.onSettingsChanged(context, NotificationCastListener.instance != null)
        }
    }
    // Setup only needs attention again if something actually broke (OriginOS revoking notification
    // access or re-restricting battery is a known failure mode) — then Home shows a one-line banner;
    // otherwise the permissions stay behind the ⋯ menu so they're not in the way on every open.
    val listenerOk = remember(tick.intValue) { isListenerEnabled(context) }
    val batteryOk = remember(tick.intValue) { isBatteryUnrestricted(context) }
    var permissionsOpen by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var updateStatus by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Poll the listener once a second so the status pill stays live.
    var connected by remember { mutableStateOf(NotificationCastListener.instance != null) }
    LaunchedEffect(Unit) {
        while (true) { connected = NotificationCastListener.instance != null; delay(1000) }
    }

    if (permissionsOpen) {
        PermissionsSheet(context, prefs, onDismiss = { permissionsOpen = false }, onRedoSetup = onRedoSetup)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = ScreenPadding,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Origin Isle",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.7).sp,
                        lineHeight = 38.sp,
                        color = IsleColors.Text,
                    )
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
                                text = "Tìm kiếm tính năng, cài đặt...",
                                color = IsleColors.TextSecondary,
                                fontSize = 15.sp,
                            )
                        }
                    }
                    StatusPill(connected, onClick = onOpenActivity)
                }
                Box {
                    CircleIconButton(IsleIcons.More, "More", onClick = { menuOpen = true })
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
                                        "Có bản cập nhật mới: v${result.latest} — Đang mở GitHub…"
                                    }
                                    is UpdateChecker.Result.Error -> result.message
                                }
                            }
                        }
                        MenuItem("Quyền ứng dụng") {
                            menuOpen = false
                            permissionsOpen = true
                        }
                    }
                }
            }
            updateStatus?.let {
                Text(
                    it,
                    modifier = Modifier.padding(start = 4.dp, top = 12.dp),
                    fontSize = 13.sp,
                    color = IsleColors.TextSecondary,
                )
            }
        }

        if (!listenerOk || !batteryOk) {
            item { SetupAttentionBanner(onClick = { permissionsOpen = true }) }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("Hiện trên đảo")
                GroupCard {
                    ToggleRow(IsleIcons.Bell, IsleColors.Accent, "Thông báo", castOn) {
                        castOn = it; prefs.edit().putBoolean("cast_notifications", it).apply()
                        if (it) PlaygroundService.keepAlive(context)
                        PrivacyCard.refresh()
                        FlashlightCard.refresh()
                        EarbudsCard.onSettingsChanged(context, NotificationCastListener.instance != null)
                    }
                    GroupDivider()
                    ToggleRow(
                        IsleIcons.Download, IsleColors.Accent, "Tiến trình tải xuống", downloadOn,
                        subtitle = "Chrome, Edge, Canary...",
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
                        IsleIcons.Music, IsleColors.Accent, "Âm nhạc", mediaOn,
                        subtitle = "Trình phát nhạc trên đảo",
                    ) {
                        mediaOn = it; prefs.edit().putBoolean("cast_media_sessions", it).apply()
                    }
                }
            }
        }

        item {
            Column(
                Modifier.alpha(if (castOn) 1f else 0.4f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SectionLabel("Tùy chọn thông báo")
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
                        SettingRow(IsleIcons.Clock, IsleColors.TileTeal, "Tự ẩn sau", subtitle = "Dành cho thẻ thông báo thường")
                        Row(
                            Modifier.padding(start = 62.dp, end = 16.dp, bottom = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            listOf(0 to "Không ẩn", 5 to "5 giây", 10 to "10 giây", 15 to "15 giây", 30 to "30 giây").forEach { (sec, label) ->
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
                        IsleIcons.Headphones, IsleColors.TilePurple, "Tai nghe Bluetooth", earbudsOn,
                        subtitle = "Hiện phần trăm pin khi kết nối",
                    ) {
                        prefs.edit().putBoolean("cast_earbuds", it).apply()
                        if (it && !EarbudsCard.hasPermission(context)) {
                            // "Nearby devices": asked for here, only by this switch.
                            bluetoothPermission.launch(Manifest.permission.BLUETOOTH_CONNECT)
                        } else {
                            earbudsOn = it
                            EarbudsCard.onSettingsChanged(context, NotificationCastListener.instance != null)
                        }
                    }
                    GroupDivider()
                    ToggleRow(
                        IsleIcons.Moon, IsleColors.TilePurple, "Bỏ qua thông báo im lặng", ignoreSilent,
                        subtitle = "Không hiển thị thông báo không có chuông/rung",
                    ) {
                        ignoreSilent = it; prefs.edit().putBoolean("cast_ignore_silent", it).apply()
                    }
                    GroupDivider()
                    ToggleRow(
                        IsleIcons.Lock, IsleColors.TileGreen, "Màn hình khóa", lockscreenLiveCard,
                        subtitle = "Hiển thị thẻ động trên màn hình khóa",
                    ) {
                        lockscreenLiveCard = it
                        prefs.edit().putBoolean("cast_lockscreen_live_card", it).apply()
                    }
                }
            }
        }

        item {
            Column(
                Modifier.alpha(if (castOn) 1f else 0.4f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SectionLabel("Các loại thẻ trên đảo")
                GroupCard {
                    ToggleRow(
                        IsleIcons.Buttons, IsleColors.TileTeal, "Nút hành động", actionsOn,
                        subtitle = "Hiển thị các nút thao tác của thông báo trên thẻ",
                    ) {
                        actionsOn = it; prefs.edit().putBoolean("cast_show_actions", it).apply()
                    }
                    GroupDivider()
                    ToggleRow(
                        IsleIcons.Bolt, IsleColors.TileGreen, "Trạng thái sạc pin", chargingOn,
                        subtitle = "Hiển thị thẻ pin khi cắm sạc",
                    ) {
                        chargingOn = it; prefs.edit().putBoolean("cast_charging", it).apply()
                        ChargingCard.onSettingsChanged(context)
                    }
                    if (chargingOn) {
                        Row(
                            Modifier.padding(start = 62.dp, end = 16.dp, bottom = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                        IsleIcons.Camera, IsleColors.TileAmber, "Chỉ báo Camera & Mic", privacyDotOn,
                        subtitle = "Chấm màu khi đang dùng quyền riêng tư",
                    ) {
                        privacyDotOn = it; prefs.edit().putBoolean("cast_privacy_dot", it).apply()
                        PrivacyCard.refresh()
                    }
                }
            }
        }

        item {
            Column(
                Modifier.alpha(if (castOn) 1f else 0.4f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SectionLabel("Giờ yên tĩnh")
                GroupCard {
                    ToggleRow(
                        IsleIcons.Moon, IsleColors.TilePurple, "Theo chế độ Không làm phiền", quietDnd,
                        subtitle = "Tự động tắt đảo khi bật Không làm phiền",
                    ) {
                        quietDnd = it; prefs.edit().putBoolean("cast_quiet_dnd", it).apply()
                    }
                    GroupDivider()
                    ToggleRow(
                        IsleIcons.Clock, IsleColors.TileGray, "Lịch đặt giờ", quietSchedule,
                        subtitle = "${QuietHours.format(quietStart)} – ${QuietHours.format(quietEnd)} hàng ngày",
                    ) {
                        quietSchedule = it; prefs.edit().putBoolean("cast_quiet_schedule", it).apply()
                    }
                    if (quietSchedule) {
                        Row(
                            Modifier.padding(start = 62.dp, end = 16.dp, bottom = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                Text(
                    "Trong giờ yên tĩnh, chỉ có cuộc gọi, điều hướng, hẹn giờ, mã OTP, âm nhạc và sạc pin mới được hiển thị.",
                    modifier = Modifier.padding(horizontal = 4.dp),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = IsleColors.TextSecondary,
                )
            }
        }

        item {
            GroupCard {
                NavRow(
                    IsleIcons.Grid, IsleColors.TileAmber, "Ứng dụng",
                    value = if (appsOff == 0) "Đang bật tất cả" else "Đã tắt $appsOff",
                    onClick = onOpenApps,
                )
                GroupDivider()
                NavRow(IsleIcons.Pulse, IsleColors.TileGray, "Nhật ký hoạt động", onClick = onOpenActivity)
            }
        }
    }
}

/** Green "Listening" / red "Not listening" pill under the title; opens the Activity screen. */
@Composable
private fun StatusPill(connected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(if (connected) IsleColors.GoodBg else IsleColors.BadBg)
            .border(
                width = 0.8.dp,
                color = if (connected) IsleColors.Good.copy(alpha = 0.3f) else IsleColors.Bad.copy(alpha = 0.3f),
                shape = RoundedCornerShape(17.dp),
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(if (connected) IsleColors.Good else IsleColors.Bad))
        Text(
            if (connected) "Đảo OriginOS: Đang hoạt động" else "Đảo OriginOS: Chưa kết nối",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (connected) IsleColors.GoodText else IsleColors.BadText,
        )
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
            .clip(RoundedCornerShape(28.dp))
            .background(IsleColors.Surface)
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .heightIn(min = 104.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(IsleColors.PreviewBg)
                .border(
                    width = 0.8.dp,
                    color = IsleColors.BorderHairline,
                    shape = RoundedCornerShape(20.dp),
                )
                .padding(start = 10.dp, end = 10.dp, top = 14.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // What the compact island will look like once this sample is sent.
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
                    selectedContainer = IsleColors.Text,
                    selectedContent = IsleColors.Background,
                    shape = RoundedCornerShape(18.dp),
                    height = 36.dp,
                    horizontalPadding = 14.dp,
                )
            }
        }
        PillButton(
            if (sentAt != 0L) "Đã gửi" else "Thử trên đảo",
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

/**
 * Ask the bound notification listener to replay every current notification onto the island. If the
 * listener isn't connected (access not granted, or the binding went stale after a kill), keep it
 * alive and request a rebind so the sweep works on the next tap.
 */
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
