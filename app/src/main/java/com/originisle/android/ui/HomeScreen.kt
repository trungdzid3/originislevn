package com.originisle.android.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.originisle.android.BuildConfig
import com.originisle.android.cards.EarbudsCard
import com.originisle.android.cards.FlashlightCard
import com.originisle.android.island.PlaygroundService
import com.originisle.android.log.CastLog
import com.originisle.android.service.NotificationCastListener
import com.originisle.android.ui.samples.IslandSamples
import kotlinx.coroutines.delay

/**
 * Tab 1 — Trang chủ Đảo Origin:
 * Bố cục tối giản chuẩn Material Design 3 (~1.5 màn hình), gồm:
 * - CompactStatusCard (trạng thái + tên máy + công tắc tổng)
 * - Banner cảnh báo quyền (tự ẩn khi đủ quyền)
 * - Nhóm 1: Giao diện & Tương tác
 * - Lối tắt nhanh: Ứng dụng được phép & Nhật ký hoạt động
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    context: Context,
    prefs: SharedPreferences,
    onRedoSetup: () -> Unit,
    onOpenApps: () -> Unit,
    onOpenActivity: () -> Unit,
) {
    var castOn by remember { mutableStateOf(prefs.getBoolean("cast_notifications", true)) }
    var themePref by remember { mutableStateOf(prefs.getString(PREF_APP_THEME, "SYSTEM") ?: "SYSTEM") }
    var themeColorPref by remember {
        mutableStateOf(prefs.getString(PREF_APP_THEME_COLOR, AppThemeColor.DYNAMIC.key) ?: AppThemeColor.DYNAMIC.key)
    }
    var suppressHeadsUp by remember { mutableStateOf(prefs.getBoolean(PREF_CAST_SUPPRESS_HEADS_UP, false)) }
    var suppressMode by remember {
        mutableStateOf(prefs.getString(PREF_CAST_SUPPRESS_MODE, SUPPRESS_MODE_SNOOZE) ?: SUPPRESS_MODE_SNOOZE)
    }
    var tapExpandCard by remember { mutableStateOf(prefs.getBoolean(PREF_CAST_TAP_EXPAND_CARD, true)) }
    var quickReplyOverlay by remember { mutableStateOf(prefs.getBoolean(PREF_QUICK_REPLY_OVERLAY, false)) }
    var mediaOn by remember { mutableStateOf(prefs.getBoolean("cast_media_sessions", true)) }

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
                PREF_CAST_TAP_EXPAND_CARD -> tapExpandCard = sp.getBoolean(PREF_CAST_TAP_EXPAND_CARD, true)
                PREF_QUICK_REPLY_OVERLAY -> quickReplyOverlay = sp.getBoolean(PREF_QUICK_REPLY_OVERLAY, false)
                "cast_media_sessions" -> mediaOn = sp.getBoolean("cast_media_sessions", true)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    val tick = rememberResumeTick()
    val appsOff = remember(tick.intValue) {
        prefs.getStringSet("cast_ignored_apps", emptySet()).orEmpty().size
    }
    val listenerOk = remember(tick.intValue) { isListenerEnabled(context) }
    val batteryOk = remember(tick.intValue) { isBatteryUnrestricted(context) }
    val a11yOk = remember(tick.intValue) { isAccessibilityEnabled(context) }
    val overlayOk = remember(tick.intValue) { isOverlayPermissionGranted(context) }

    var connected by remember { mutableStateOf(NotificationCastListener.instance != null) }
    var deviceSnapshot by remember { mutableStateOf(DeviceInfoHelper.collect(context)) }
    var permissionsOpen by remember { mutableStateOf(false) }
    var testerSheetOpen by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(tick.intValue) {
        if (tick.intValue > 0) {
            deviceSnapshot = DeviceInfoHelper.collect(context)
        }
    }

    val lifecycle = (context as? ComponentActivity)?.lifecycle
    LaunchedEffect(lifecycle) {
        CastLog.ensureLoaded(context)
        val pollLoop: suspend () -> Unit = {
            while (true) {
                val nowConnected = NotificationCastListener.instance != null
                if (nowConnected != connected) connected = nowConnected
                delay(3000L)
            }
        }
        if (lifecycle != null) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { pollLoop() }
        } else {
            pollLoop()
        }
    }

    if (permissionsOpen) {
        PermissionsSheet(context, prefs, onDismiss = { permissionsOpen = false }, onRedoSetup = onRedoSetup)
    }

    if (testerSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { testerSheetOpen = false },
            modifier = Modifier.statusBarsPadding(),
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
            containerColor = IsleColors.Menu,
            contentColor = IsleColors.Text,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Phòng thử nghiệm mẫu Đảo",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = IsleColors.Text,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                SampleTester(context)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButton(
                        text = "Hiện lại tất cả",
                        onClick = { recastAll(context) },
                        modifier = Modifier.weight(1f),
                        icon = IsleIcons.Refresh,
                        container = IsleColors.Accent.copy(alpha = if (IsleColors.isDark) 0.20f else 0.12f),
                        content = IsleColors.Accent,
                        enabled = castOn || mediaOn,
                        height = 50.dp,
                    )
                    PillButton(
                        text = "Đóng tất cả",
                        onClick = {
                            stopAll(context)
                            testerSheetOpen = false
                        },
                        modifier = Modifier.weight(1f),
                        icon = IsleIcons.Stop,
                        iconSize = 16.dp,
                        container = MaterialTheme.colorScheme.errorContainer,
                        content = MaterialTheme.colorScheme.onErrorContainer,
                        height = 50.dp,
                    )
                }
            }
        }
    }

    val displayModel = remember(deviceSnapshot.marketBrand, deviceSnapshot.modelName) {
        if (deviceSnapshot.modelName.startsWith(deviceSnapshot.marketBrand, ignoreCase = true)) {
            deviceSnapshot.modelName
        } else {
            "${deviceSnapshot.marketBrand} ${deviceSnapshot.modelName}"
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Header + menu 3 chấm
        item {
            MainScreenHeader(title = "Đảo Origin") {
                Box {
                    CircleIconButton(IsleIcons.More, "Tùy chọn khác", onClick = { menuOpen = true })
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                        offset = DpOffset(0.dp, 8.dp),
                        shape = MaterialTheme.shapes.medium,
                        containerColor = IsleColors.Menu,
                    ) {
                        MenuItem("Thử nghiệm mẫu Đảo") {
                            menuOpen = false
                            testerSheetOpen = true
                        }
                        MenuItem("Đóng tất cả thẻ đang hiện") {
                            menuOpen = false
                            stopAll(context)
                            Toast.makeText(context, "Đã đóng tất cả thẻ Đảo", Toast.LENGTH_SHORT).show()
                        }
                        MenuItem("Chạy lại hướng dẫn") {
                            menuOpen = false
                            onRedoSetup()
                        }
                    }
                }
            }
        }

        // Banner cảnh báo quyền — tự ẩn hoàn toàn khi đã cấp đủ
        val missingPermissions = !listenerOk || !a11yOk || !overlayOk || !connected || !batteryOk
        if (missingPermissions) {
            item {
                VivoOriginAttentionBanner(
                    connected = connected,
                    listenerOk = listenerOk,
                    batteryOk = batteryOk,
                    a11yOk = a11yOk,
                    overlayOk = overlayOk,
                    onOpenPermissions = { permissionsOpen = true },
                )
            }
        }

        // Thẻ trạng thái nguyên tử Lưới Hoa Dung (Huarong Grid Atomic Status Widget)
        item {
            CompactStatusCard(
                castOn = castOn,
                connected = connected,
                deviceName = displayModel,
                islandCount = CastLog.entries.size,
                onToggle = { enabled ->
                    castOn = enabled
                    prefs.edit().putBoolean("cast_notifications", enabled).apply()
                    if (enabled) PlaygroundService.keepAlive(context)
                    FlashlightCard.refresh()
                    EarbudsCard.onSettingsChanged(context, NotificationCastListener.instance != null)
                },
            )
        }

        // Nhóm 1 — Giao diện & Tương tác
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("Giao diện & Tương tác")
                GroupCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Giao diện",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
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

                        Text(
                            text = "Màu chủ đề",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = IsleColors.TextSecondary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(AppThemeColor.entries) { option ->
                                val selected = themeColorPref == option.key
                                Row(
                                    modifier = Modifier
                                        .heightIn(min = 40.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            if (selected) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else {
                                                IsleColors.SurfaceHigh
                                            },
                                        )
                                        .border(
                                            width = if (selected) 1.5.dp else 0.5.dp,
                                            color = if (selected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                IsleColors.BorderHairline
                                            },
                                            shape = RoundedCornerShape(50),
                                        )
                                        .selectable(
                                            selected = selected,
                                            role = Role.RadioButton,
                                            onClick = {
                                                themeColorPref = option.key
                                                prefs.edit().putString(PREF_APP_THEME_COLOR, option.key).apply()
                                            },
                                        )
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(option.swatch),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (selected) {
                                            Icon(
                                                imageVector = IsleIcons.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(10.dp),
                                                tint = Color.White,
                                            )
                                        }
                                    }
                                    Text(
                                        text = option.label,
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            IsleColors.Text
                                        },
                                    )
                                }
                            }
                        }
                    }

                    GroupDivider()

                    Column(modifier = Modifier.alpha(if (castOn) 1f else 0.45f)) {
                        ToggleRow(
                            icon = IsleIcons.Shield,
                            tileColor = IsleColors.Accent,
                            title = "Chặn Pop-up nổi",
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
                    }
                }
            }
        }

        // Lối tắt nhanh: 2 ô lưới Atomic Grid 1x2 cân đối
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AtomicShortcutTile(
                    icon = IsleIcons.Grid,
                    title = "Ứng dụng",
                    subtitle = if (appsOff == 0) "Tất cả đang bật" else "Đang tắt $appsOff",
                    onClick = onOpenApps,
                    modifier = Modifier.weight(1f),
                )
                AtomicShortcutTile(
                    icon = IsleIcons.Pulse,
                    title = "Nhật ký",
                    subtitle = "${CastLog.entries.size} hoạt động Đảo",
                    onClick = onOpenActivity,
                    modifier = Modifier.weight(1f),
                )
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

@Composable
private fun CompactStatusCard(
    castOn: Boolean,
    connected: Boolean,
    deviceName: String,
    islandCount: Int,
    onToggle: (Boolean) -> Unit,
) {
    val statusColor = when {
        !castOn -> IsleColors.TextTertiary
        connected -> IsleColors.Good
        else -> IsleColors.Warn
    }
    val statusTitle = when {
        !castOn -> "Đã tắt"
        connected -> "Đang hoạt động"
        else -> "Chờ kết nối"
    }
    val connectionSummary = when {
        !castOn -> "Chế độ tạm nghỉ"
        connected -> "Dịch vụ đã liên kết"
        else -> "Đang chờ liên kết"
    }

    GroupCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // Connection status pill (OriginOS Atomic zero-tier badge)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(statusColor.copy(alpha = 0.14f))
                            .border(0.6.dp, statusColor.copy(alpha = 0.28f), RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(statusColor),
                        )
                        Text(
                            text = statusTitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Text(
                        text = "$deviceName · v${BuildConfig.VERSION_NAME}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = IsleColors.Text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                IsleSwitch(
                    checked = castOn,
                    onCheckedChange = onToggle,
                )
            }

            // Huarong Grid zero-tier metric strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(IsleColors.SurfaceHigh.copy(alpha = 0.65f))
                    .border(
                        width = 0.6.dp,
                        brush = Brush.verticalGradient(
                            listOf(IsleColors.EdgeLightTop, IsleColors.EdgeLightBottom),
                        ),
                        shape = RoundedCornerShape(18.dp),
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = "Thẻ Đảo hôm nay",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = IsleColors.TextSecondary,
                    )
                    Text(
                        text = "$islandCount lượt",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = IsleColors.Text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFeatureSettings = "tnum",
                        ),
                    )
                }

                Text(
                    text = connectionSummary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = IsleColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * OriginOS Huarong Grid 1x2 Atomic Shortcut Tile with concentric squircle radii
 * (26.dp outer card, 14.dp inner icon tile) and Touch Light spring feedback.
 */
@Composable
private fun AtomicShortcutTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tileColor: Color = IsleColors.Accent,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "atomicTileScale",
    )
    val touchLightAlpha by animateFloatAsState(
        targetValue = if (pressed) 0.08f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "atomicTouchLight",
    )
    val outerShape = RoundedCornerShape(24.dp)
    val innerIconShape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .scale(scale)
            .heightIn(min = 114.dp)
            .clip(outerShape)
            .background(IsleColors.Surface)
            .border(
                width = 0.8.dp,
                brush = Brush.verticalGradient(listOf(IsleColors.EdgeLightTop, IsleColors.EdgeLightBottom)),
                shape = outerShape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
            ),
    ) {
        if (touchLightAlpha > 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.White.copy(alpha = touchLightAlpha)),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(innerIconShape)
                    .background(tileColor.copy(alpha = 0.14f))
                    .border(
                        width = 0.6.dp,
                        brush = Brush.verticalGradient(
                            listOf(tileColor.copy(alpha = 0.28f), tileColor.copy(alpha = 0.06f)),
                        ),
                        shape = innerIconShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = tileColor,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = IsleColors.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = IsleColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun VivoOriginAttentionBanner(
    connected: Boolean,
    listenerOk: Boolean,
    batteryOk: Boolean,
    a11yOk: Boolean,
    overlayOk: Boolean,
    onOpenPermissions: () -> Unit,
) {
    val isRed = !connected || !listenerOk
    val bg = if (isRed) IsleColors.BadBg else IsleColors.WarnBg
    val borderCol = if (isRed) IsleColors.Bad.copy(alpha = 0.45f) else IsleColors.Warn.copy(alpha = 0.45f)
    val textCol = if (isRed) IsleColors.BadText else IsleColors.Warn
    val iconCol = if (isRed) IsleColors.Bad else IsleColors.Warn

    val reason = when {
        !listenerOk -> "Chưa cấp quyền Truy cập thông báo"
        !connected -> "Dịch vụ chưa liên kết — chạm để kiểm tra quyền"
        !a11yOk -> "Chưa bật Hỗ trợ tiếp cận cho tiến trình tải"
        !overlayOk -> "Chưa cấp quyền Hiển thị trên ứng dụng khác"
        !batteryOk -> "Cần tắt hạn chế pin để chạy nền ổn định"
        else -> "Chạm để hoàn tất quyền hệ thống"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(0.8.dp, borderCol, RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onOpenPermissions)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconTile(IsleIcons.Shield, color = iconCol, size = 36.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Thiếu quyền hệ thống",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = IsleColors.Text,
            )
            Text(
                text = reason,
                fontSize = 12.sp,
                color = textCol,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(IsleIcons.ChevronRight, null, Modifier.size(18.dp), tint = textCol)
    }
}

@Composable
private fun MenuItem(text: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = IsleColors.Text) },
        onClick = onClick,
        modifier = Modifier.widthIn(min = 210.dp).heightIn(min = 48.dp),
    )
}

@Composable
private fun SampleTester(context: Context) {
    var selected by remember { mutableIntStateOf(0) }
    val sample = IslandSamples.all[selected]
    var sentAt by remember { mutableLongStateOf(0L) }
    LaunchedEffect(sentAt) {
        if (sentAt != 0L) {
            delay(1600)
            sentAt = 0L
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(IsleColors.Surface)
            .border(0.8.dp, IsleColors.BorderHairline, RoundedCornerShape(24.dp))
            .padding(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            Modifier
                .padding(horizontal = 14.dp)
                .fillMaxWidth()
                .heightIn(min = 96.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(IsleColors.PreviewBg)
                .border(0.8.dp, IsleColors.BorderHairline, RoundedCornerShape(18.dp))
                .padding(start = 10.dp, end = 10.dp, top = 12.dp, bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            IslandPreviewView(sample.preview)
            Text(
                sample.summary,
                modifier = Modifier.padding(top = 10.dp),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = IsleColors.PreviewLabel,
                textAlign = TextAlign.Center,
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
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
            onClick = {
                sample.post(context)
                sentAt = System.currentTimeMillis()
            },
            modifier = Modifier.padding(horizontal = 14.dp).fillMaxWidth(),
            icon = IsleIcons.Send,
            container = IsleColors.Accent,
            content = Color.White,
            height = 50.dp,
        )
    }
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
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        return
    }
    val count = listener.recastAll()
    Toast.makeText(context, "Đã phát lại $count thông báo lên đảo.", Toast.LENGTH_SHORT).show()
}
