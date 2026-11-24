package com.originisle.android.ui

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.originisle.android.BuildConfig
import com.originisle.android.service.QuietHours
import com.originisle.android.service.ShizukuKeepAliveHelper
import kotlinx.coroutines.launch

/**
 * Tab 3 — Cài đặt:
 * 1. Quy tắc & Riêng tư
 * 2. Giữ nền (Shizuku)
 * 3. Hệ thống & Cập nhật
 */
@Composable
fun SettingsScreen(
    context: Context,
    prefs: SharedPreferences,
    onRedoSetup: () -> Unit,
) {
    var castOn by remember { mutableStateOf(prefs.getBoolean("cast_notifications", true)) }

    // Nhóm 1: Quy tắc & Riêng tư
    var includeMessages by remember { mutableStateOf(prefs.getBoolean("cast_include_messages", false)) }
    var autoDismissSec by remember { mutableIntStateOf(prefs.getInt("cast_auto_dismiss_seconds", 5)) }
    var islandDurationSec by remember {
        mutableIntStateOf(prefs.getInt(PREF_CAST_ISLAND_DURATION_SEC, DEFAULT_ISLAND_DURATION_SEC))
    }
    var ignoreSilent by remember { mutableStateOf(prefs.getBoolean("cast_ignore_silent", true)) }
    var lockscreenLiveCard by remember { mutableStateOf(prefs.getBoolean("cast_lockscreen_live_card", false)) }
    var lockscreenRedactSensitive by remember {
        mutableStateOf(prefs.getBoolean("cast_lockscreen_redact_sensitive", true))
    }
    var quietDnd by remember { mutableStateOf(prefs.getBoolean("cast_quiet_dnd", false)) }
    var quietSchedule by remember { mutableStateOf(prefs.getBoolean("cast_quiet_schedule", false)) }
    var quietStart by remember { mutableIntStateOf(prefs.getInt("cast_quiet_start", QuietHours.DEFAULT_START)) }
    var quietEnd by remember { mutableIntStateOf(prefs.getInt("cast_quiet_end", QuietHours.DEFAULT_END)) }
    var actionsOn by remember { mutableStateOf(prefs.getBoolean("cast_show_actions", true)) }

    // Nhóm 2: Shizuku
    var shizukuAutoKeepAlive by remember { mutableStateOf(prefs.getBoolean(PREF_SHIZUKU_AUTO_KEEPALIVE, false)) }

    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
            when (key) {
                "cast_notifications" -> castOn = sp.getBoolean("cast_notifications", true)
                PREF_CAST_ISLAND_DURATION_SEC -> {
                    islandDurationSec = sp.getInt(PREF_CAST_ISLAND_DURATION_SEC, DEFAULT_ISLAND_DURATION_SEC)
                }
                PREF_SHIZUKU_AUTO_KEEPALIVE -> {
                    shizukuAutoKeepAlive = sp.getBoolean(PREF_SHIZUKU_AUTO_KEEPALIVE, false)
                }
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    val tick = rememberResumeTick()
    val listenerOk = remember(tick.intValue) { isListenerEnabled(context) }
    val batteryOk = remember(tick.intValue) { isBatteryUnrestricted(context) }
    val a11yOk = remember(tick.intValue) { isAccessibilityEnabled(context) }
    val overlayOk = remember(tick.intValue) { isOverlayPermissionGranted(context) }

    var shizukuState by remember(tick.intValue) { mutableIntStateOf(ShizukuKeepAliveHelper.getState(context)) }
    var shizukuSummary by remember(tick.intValue) { mutableStateOf(ShizukuKeepAliveHelper.getLastSummary(context)) }
    var shizukuBusy by remember { mutableStateOf(false) }

    var permissionsOpen by remember { mutableStateOf(false) }
    var updateStatus by remember { mutableStateOf<String?>(null) }
    var updateDialogRelease by remember { mutableStateOf<UpdateChecker.ReleaseInfo?>(null) }
    val coroutineScope = rememberCoroutineScope()

    if (permissionsOpen) {
        PermissionsSheet(context, prefs, onDismiss = { permissionsOpen = false }, onRedoSetup = onRedoSetup)
    }

    updateDialogRelease?.let { release ->
        UpdateDialog(
            release = release,
            onDismiss = { updateDialogRelease = null },
        )
    }

    val checkForUpdates: () -> Unit = {
        coroutineScope.launch {
            updateStatus = "Đang kiểm tra…"
            when (val result = UpdateChecker.check(context)) {
                is UpdateChecker.Result.UpdateAvailable -> {
                    updateStatus = "Có v${result.release.versionName}"
                    updateDialogRelease = result.release
                }
                is UpdateChecker.Result.UpToDate -> {
                    updateStatus = "v${BuildConfig.VERSION_NAME}"
                    Toast.makeText(
                        context,
                        "Bạn đang dùng phiên bản mới nhất (v${result.current})",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
                is UpdateChecker.Result.Error -> {
                    updateStatus = "Lỗi kết nối"
                    Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = ScreenPadding,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            MainScreenHeader(title = "Cài đặt")
        }

        // Nhóm 1 — Quy tắc & Riêng tư
        item {
            Column(
                modifier = Modifier.alpha(if (castOn) 1f else 0.45f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SectionLabel("Quy tắc & Riêng tư")
                GroupCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "Mức lọc thông báo",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IsleColors.Text,
                        )
                        SegmentedControl(
                            options = listOf("Quan trọng", "Tất cả"),
                            selected = if (includeMessages) 1 else 0,
                            onSelect = { idx ->
                                includeMessages = idx == 1
                                prefs.edit().putBoolean("cast_include_messages", includeMessages).apply()
                            },
                        )
                    }

                    NestedSettingContainer(visible = includeMessages) {
                        Text(
                            "Tự ẩn thẻ tin nhắn thường sau:",
                            fontSize = 13.sp,
                            color = IsleColors.TextSecondary,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(0 to "Không", 5 to "5s", 10 to "10s", 15 to "15s", 30 to "30s").forEach { (sec, label) ->
                                ChoiceChip(
                                    label = label,
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

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "Thời lượng viên thuốc",
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
                        icon = IsleIcons.Moon,
                        tileColor = IsleColors.Accent,
                        title = "Bỏ qua thông báo im lặng",
                        checked = ignoreSilent,
                        subtitle = "Không hiện thông báo không chuông/rung",
                    ) {
                        ignoreSilent = it
                        prefs.edit().putBoolean("cast_ignore_silent", it).apply()
                    }

                    GroupDivider()

                    ToggleRow(
                        icon = IsleIcons.Lock,
                        tileColor = IsleColors.Accent,
                        title = "Hiển thị trên màn hình khóa",
                        checked = lockscreenLiveCard,
                        subtitle = "Giữ thẻ Đảo khi thiết bị đang khóa",
                    ) {
                        lockscreenLiveCard = it
                        prefs.edit().putBoolean("cast_lockscreen_live_card", it).apply()
                    }

                    GroupDivider()

                    ToggleRow(
                        icon = IsleIcons.Shield,
                        tileColor = IsleColors.Accent,
                        title = "Ẩn số dư & OTP khi khóa máy",
                        checked = lockscreenRedactSensitive,
                        subtitle = "Che tiền chuyển khoản & mã OTP trên khóa",
                    ) {
                        lockscreenRedactSensitive = it
                        prefs.edit().putBoolean("cast_lockscreen_redact_sensitive", it).apply()
                    }

                    GroupDivider()

                    ToggleRow(
                        icon = IsleIcons.Moon,
                        tileColor = IsleColors.Accent,
                        title = "Theo Không làm phiền",
                        checked = quietDnd,
                        subtitle = "Tự tắt Đảo khi bật chế độ DND",
                    ) {
                        quietDnd = it
                        prefs.edit().putBoolean("cast_quiet_dnd", it).apply()
                    }

                    GroupDivider()

                    ToggleRow(
                        icon = IsleIcons.Clock,
                        tileColor = IsleColors.Accent,
                        title = "Lịch giờ yên tĩnh",
                        checked = quietSchedule,
                        subtitle = "${QuietHours.format(quietStart)} – ${QuietHours.format(quietEnd)} hàng ngày",
                    ) {
                        quietSchedule = it
                        prefs.edit().putBoolean("cast_quiet_schedule", it).apply()
                    }

                    NestedSettingContainer(visible = quietSchedule) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ChoiceChip(
                                label = "Từ ${QuietHours.format(quietStart)}",
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
                                label = "Đến ${QuietHours.format(quietEnd)}",
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
                        icon = IsleIcons.Buttons,
                        tileColor = IsleColors.Accent,
                        title = "Nút thao tác trên thẻ",
                        checked = actionsOn,
                        subtitle = "Hiện nút trả lời nhanh, sao chép, hủy",
                    ) {
                        actionsOn = it
                        prefs.edit().putBoolean("cast_show_actions", it).apply()
                    }
                }
            }
        }

        // Nhóm 2 — Giữ nền (Shizuku)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("Giữ nền (Shizuku)")
                GroupCard {
                    SettingRow(
                        icon = IsleIcons.Bolt,
                        tileColor = IsleColors.Accent,
                        title = ShizukuKeepAliveHelper.getStateLabel(context),
                        subtitle = if (shizukuSummary.isNotBlank()) {
                            shizukuSummary
                        } else {
                            "Tối ưu whitelist pin không cần root"
                        },
                    ) {
                        val canTap = !shizukuBusy &&
                            shizukuState != ShizukuKeepAliveHelper.STATUS_NOT_INSTALLED &&
                            shizukuState != ShizukuKeepAliveHelper.STATUS_NOT_RUNNING
                        PillButton(
                            text = if (shizukuBusy) "Đang chạy..." else "Áp dụng",
                            onClick = {
                                if (shizukuState == ShizukuKeepAliveHelper.STATUS_NO_PERMISSION) {
                                    ShizukuKeepAliveHelper.requestPermission { granted ->
                                        shizukuState = ShizukuKeepAliveHelper.getState(context)
                                        if (granted) {
                                            shizukuBusy = true
                                            ShizukuKeepAliveHelper.applyKeepAliveAsync(context) { _, msg ->
                                                shizukuBusy = false
                                                shizukuSummary = ShizukuKeepAliveHelper.getLastSummary(context)
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                } else if (shizukuState == ShizukuKeepAliveHelper.STATUS_READY) {
                                    shizukuBusy = true
                                    ShizukuKeepAliveHelper.applyKeepAliveAsync(context) { _, msg ->
                                        shizukuBusy = false
                                        shizukuSummary = ShizukuKeepAliveHelper.getLastSummary(context)
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = canTap,
                            height = 36.dp,
                        )
                    }

                    GroupDivider()

                    ToggleRow(
                        icon = IsleIcons.Power,
                        tileColor = IsleColors.Accent,
                        title = "Tự áp dụng sau khi khởi động lại",
                        checked = shizukuAutoKeepAlive,
                        subtitle = "Tự chạy lại giữ nền Shizuku khi bật máy",
                    ) {
                        shizukuAutoKeepAlive = it
                        prefs.edit().putBoolean(PREF_SHIZUKU_AUTO_KEEPALIVE, it).apply()
                    }
                }
            }
        }

        // Nhóm 3 — Hệ thống & Cập nhật
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("Hệ thống & Cập nhật")
                GroupCard {
                    val allPermsOk = listenerOk && batteryOk && a11yOk && overlayOk
                    NavRow(
                        icon = IsleIcons.Shield,
                        tileColor = if (allPermsOk) IsleColors.Accent else IsleColors.Warn,
                        title = "Quyền hệ thống",
                        value = if (allPermsOk) "Đủ" else "Thiếu",
                        onClick = { permissionsOpen = true },
                    )

                    GroupDivider()

                    NavRow(
                        icon = IsleIcons.Refresh,
                        tileColor = IsleColors.Accent,
                        title = "Kiểm tra cập nhật",
                        value = updateStatus ?: "v${BuildConfig.VERSION_NAME}",
                        onClick = checkForUpdates,
                    )
                }
            }
        }
    }
}

private fun pickTime(context: Context, initial: Int, onPicked: (Int) -> Unit) {
    android.app.TimePickerDialog(
        context,
        { _, hour, minute -> onPicked(hour * 60 + minute) },
        initial / 60,
        initial % 60,
        android.text.format.DateFormat.is24HourFormat(context),
    ).show()
}
