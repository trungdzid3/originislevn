package com.originisle.android.ui

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
 * Tab 3 — Cài đặt theo chuẩn OriginUI ROM 16.0:
 * 1. Giao diện (Sáng/Tối/Hệ thống & Bảng màu)
 * 2. Hành vi Đảo & Quy tắc (Chặn popup, Thời lượng đảo, Trả lời nhanh nổi, Lọc thông báo, Màn hình khóa, Giờ yên tĩnh)
 * 3. Hệ thống & Cập nhật (Quyền hệ thống, Giữ sống Shizuku, Kiểm tra cập nhật)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    context: Context,
    prefs: SharedPreferences,
    onRedoSetup: () -> Unit,
) {
    var castOn by remember { mutableStateOf(prefs.getBoolean("cast_notifications", true)) }

    // Nhóm Giao diện
    var themePref by remember { mutableStateOf(prefs.getString(PREF_APP_THEME, "SYSTEM") ?: "SYSTEM") }
    var themeColorPref by remember {
        mutableStateOf(prefs.getString(PREF_APP_THEME_COLOR, AppThemeColor.DYNAMIC.key) ?: AppThemeColor.DYNAMIC.key)
    }

    // Nhóm Hành vi Đảo & Quy tắc
    var suppressHeadsUp by remember { mutableStateOf(prefs.getBoolean(PREF_CAST_SUPPRESS_HEADS_UP, false)) }
    var suppressMode by remember {
        mutableStateOf(prefs.getString(PREF_CAST_SUPPRESS_MODE, SUPPRESS_MODE_SNOOZE) ?: SUPPRESS_MODE_SNOOZE)
    }
    var islandDurationSec by remember {
        mutableIntStateOf(prefs.getInt(PREF_CAST_ISLAND_DURATION_SEC, DEFAULT_ISLAND_DURATION_SEC))
    }
    var tapExpandCard by remember { mutableStateOf(prefs.getBoolean(PREF_CAST_TAP_EXPAND_CARD, true)) }
    var quickReplyOverlay by remember { mutableStateOf(prefs.getBoolean(PREF_QUICK_REPLY_OVERLAY, false)) }
    var actionsOn by remember { mutableStateOf(prefs.getBoolean("cast_show_actions", true)) }
    var includeMessages by remember { mutableStateOf(prefs.getBoolean("cast_include_messages", false)) }
    var autoDismissSec by remember { mutableIntStateOf(prefs.getInt("cast_auto_dismiss_seconds", 5)) }
    var ignoreSilent by remember { mutableStateOf(prefs.getBoolean("cast_ignore_silent", true)) }
    var lockscreenLiveCard by remember { mutableStateOf(prefs.getBoolean("cast_lockscreen_live_card", false)) }
    var lockscreenRedactSensitive by remember {
        mutableStateOf(prefs.getBoolean("cast_lockscreen_redact_sensitive", true))
    }
    var quietDnd by remember { mutableStateOf(prefs.getBoolean("cast_quiet_dnd", false)) }
    var quietSchedule by remember { mutableStateOf(prefs.getBoolean("cast_quiet_schedule", false)) }
    var quietStart by remember { mutableIntStateOf(prefs.getInt("cast_quiet_start", QuietHours.DEFAULT_START)) }
    var quietEnd by remember { mutableIntStateOf(prefs.getInt("cast_quiet_end", QuietHours.DEFAULT_END)) }

    // Nhóm Hệ thống & Shizuku
    var shizukuAutoKeepAlive by remember { mutableStateOf(prefs.getBoolean(PREF_SHIZUKU_AUTO_KEEPALIVE, false)) }

    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
            when (key) {
                "cast_notifications" -> castOn = sp.getBoolean("cast_notifications", true)
                PREF_APP_THEME -> themePref = sp.getString(PREF_APP_THEME, "SYSTEM") ?: "SYSTEM"
                PREF_APP_THEME_COLOR -> {
                    themeColorPref = sp.getString(PREF_APP_THEME_COLOR, AppThemeColor.DYNAMIC.key)
                        ?: AppThemeColor.DYNAMIC.key
                }
                PREF_CAST_SUPPRESS_HEADS_UP -> suppressHeadsUp = sp.getBoolean(PREF_CAST_SUPPRESS_HEADS_UP, false)
                PREF_CAST_SUPPRESS_MODE -> {
                    suppressMode = sp.getString(PREF_CAST_SUPPRESS_MODE, SUPPRESS_MODE_SNOOZE) ?: SUPPRESS_MODE_SNOOZE
                }
                PREF_CAST_ISLAND_DURATION_SEC -> {
                    islandDurationSec = sp.getInt(PREF_CAST_ISLAND_DURATION_SEC, DEFAULT_ISLAND_DURATION_SEC)
                }
                PREF_CAST_TAP_EXPAND_CARD -> tapExpandCard = sp.getBoolean(PREF_CAST_TAP_EXPAND_CARD, true)
                PREF_QUICK_REPLY_OVERLAY -> quickReplyOverlay = sp.getBoolean(PREF_QUICK_REPLY_OVERLAY, false)
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
    var shizukuSheetOpen by remember { mutableStateOf(false) }
    var updateStatus by remember { mutableStateOf<String?>(null) }
    var updateDialogRelease by remember { mutableStateOf<UpdateChecker.ReleaseInfo?>(null) }
    val coroutineScope = rememberCoroutineScope()

    if (permissionsOpen) {
        PermissionsSheet(context, prefs, onDismiss = { permissionsOpen = false }, onRedoSetup = onRedoSetup)
    }

    if (shizukuSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { shizukuSheetOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = G2RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp, bottomEnd = 0.dp, bottomStart = 0.dp),
            containerColor = IsleColors.Menu,
            contentColor = IsleColors.Text,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "Giữ sống Shizuku",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = IsleColors.Text,
                )
                GroupCard {
                    SettingRow(
                        icon = IsleIcons.Bolt,
                        tileColor = IsleColors.Accent,
                        title = ShizukuKeepAliveHelper.getStateLabel(context),
                        subtitle = shizukuSummary.takeIf { it.isNotBlank() },
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
                            variant = PillButtonVariant.PRIMARY,
                            enabled = canTap,
                            height = 38.dp,
                        )
                    }

                    if (shizukuState != ShizukuKeepAliveHelper.STATUS_NOT_INSTALLED) {
                        GroupDivider()

                        ToggleRow(
                            icon = IsleIcons.Power,
                            tileColor = IsleColors.Accent,
                            title = "Tự áp dụng sau khi khởi động lại",
                            checked = shizukuAutoKeepAlive,
                        ) {
                            shizukuAutoKeepAlive = it
                            prefs.edit().putBoolean(PREF_SHIZUKU_AUTO_KEEPALIVE, it).apply()
                        }
                    }
                }
            }
        }
    }

    updateDialogRelease?.let { release ->
        UpdateDialog(
            release = release,
            onDismiss = { updateDialogRelease = null },
        )
    }

    val checkForUpdates: () -> Unit = {
        coroutineScope.launch {
            updateStatus = "Đang kiểm tra..."
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

    OriginCollapsingScaffold(
        title = "Cài đặt",
        hasBottomNav = true,
    ) {
        // Nhóm 1: Giao diện
        item(key = "settings_group_appearance") {
            Column {
                SettingsGroupHeader("Giao diện")
                GroupCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "Chế độ hiển thị",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = IsleColors.Text,
                        )
                        val themeIndex = when (themePref) {
                            "LIGHT" -> 0
                            "DARK" -> 2
                            else -> 1
                        }
                        SegmentedControl(
                            options = listOf("Sáng", "Hệ thống", "Tối"),
                            selected = themeIndex,
                            onSelect = { idx ->
                                val selectedTheme = when (idx) {
                                    0 -> "LIGHT"
                                    2 -> "DARK"
                                    else -> "SYSTEM"
                                }
                                themePref = selectedTheme
                                prefs.edit().putString(PREF_APP_THEME, selectedTheme).apply()
                            },
                        )
                    }

                    GroupDivider(inset = 16.dp)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "Bảng màu chủ đề",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = IsleColors.Text,
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(AppThemeColor.entries) { option ->
                                ColorSwatchPill(
                                    label = option.label,
                                    swatchColor = option.swatch,
                                    selected = themeColorPref == option.key,
                                    isAuto = option == AppThemeColor.DYNAMIC,
                                    onClick = {
                                        themeColorPref = option.key
                                        prefs.edit().putString(PREF_APP_THEME_COLOR, option.key).apply()
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        // Nhóm 2: Hành vi Đảo & Quy tắc
        item(key = "settings_group_behavior") {
            Column(modifier = Modifier.alpha(if (castOn) 1f else 0.45f)) {
                SettingsGroupHeader("Hành vi Đảo & Hệ thống")
                GroupCard {
                    ToggleRow(
                        icon = IsleIcons.Shield,
                        tileColor = IsleColors.Accent,
                        title = "Chặn popup thông báo",
                        checked = suppressHeadsUp,
                    ) {
                        suppressHeadsUp = it
                        prefs.edit().putBoolean(PREF_CAST_SUPPRESS_HEADS_UP, it).apply()
                    }

                    NestedSettingContainer(visible = suppressHeadsUp) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                SUPPRESS_MODE_SNOOZE to "Giữ trong khay",
                                SUPPRESS_MODE_ISLAND_ONLY to "Chỉ hiện trên Đảo",
                            ).forEach { (mode, label) ->
                                ChoiceChip(
                                    label = label,
                                    selected = suppressMode == mode,
                                    onClick = {
                                        suppressMode = mode
                                        prefs.edit().putString(PREF_CAST_SUPPRESS_MODE, mode).apply()
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
                            text = "Thời lượng đảo",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
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
                        icon = IsleIcons.Send,
                        tileColor = IsleColors.Accent,
                        title = "Trả lời nhanh nổi",
                        checked = quickReplyOverlay,
                    ) {
                        quickReplyOverlay = it
                        prefs.edit().putBoolean(PREF_QUICK_REPLY_OVERLAY, it).apply()
                        if (it && !isOverlayPermissionGranted(context)) {
                            openOverlayPermissionSettings(context)
                        }
                    }

                    GroupDivider()

                    ToggleRow(
                        icon = IsleIcons.Buttons,
                        tileColor = IsleColors.Accent,
                        title = "Chạm viên thuốc mở rộng",
                        checked = tapExpandCard,
                    ) {
                        tapExpandCard = it
                        prefs.edit().putBoolean(PREF_CAST_TAP_EXPAND_CARD, it).apply()
                    }

                    GroupDivider()

                    ToggleRow(
                        icon = IsleIcons.Buttons,
                        tileColor = IsleColors.Accent,
                        title = "Nút thao tác trên thẻ",
                        checked = actionsOn,
                    ) {
                        actionsOn = it
                        prefs.edit().putBoolean("cast_show_actions", it).apply()
                    }

                    GroupDivider()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Mức lọc thông báo",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
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
                            text = "Tự ẩn sau:",
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

                    ToggleRow(
                        icon = IsleIcons.Moon,
                        tileColor = IsleColors.Accent,
                        title = "Bỏ qua thông báo im lặng",
                        checked = ignoreSilent,
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
                        subtitle = if (quietSchedule) {
                            "${QuietHours.format(quietStart)} – ${QuietHours.format(quietEnd)}"
                        } else {
                            null
                        },
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
                }
            }
        }

        // Nhóm 3: Hệ thống & Cập nhật
        item(key = "settings_group_system") {
            Column {
                SettingsGroupHeader("Hệ thống & Cập nhật")
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
                        icon = IsleIcons.Bolt,
                        tileColor = IsleColors.Accent,
                        title = "Giữ sống Shizuku",
                        value = if (shizukuSummary.isNotBlank()) "Đã tối ưu" else ShizukuKeepAliveHelper.getStateLabel(context),
                        onClick = { shizukuSheetOpen = true },
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

@Composable
private fun SettingsGroupHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = IsleColors.TextSub,
        modifier = Modifier.padding(start = 16.dp, bottom = 6.dp),
    )
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
