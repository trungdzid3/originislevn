package com.originisle.android.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
 * Tab 1 — Trang chủ Đảo Origin theo chuẩn OriginUI ROM 16.0:
 * - OriginCollapsingScaffold (28.sp -> 18.sp, hasBottomNav = true)
 * - HeroCard (30.dp G2) hiển thị trạng thái Đảo & nút PillButton
 * - VTipCard (12.dp G2) khi thiếu quyền quan trọng
 * - Lưới Bento 2 cột (24.dp G2) cho lối tắt điều khiển nhanh
 * - Khối thử nghiệm mẫu Đảo (IslandSamples) trong GroupCard (24.dp G2)
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
    var mediaOn by remember { mutableStateOf(prefs.getBoolean("cast_media_sessions", true)) }
    var flashlightOn by remember { mutableStateOf(prefs.getBoolean("cast_flashlight", true)) }
    var btConnectPopup by remember { mutableStateOf(prefs.getBoolean(PREF_BT_CONNECT_POPUP, false)) }

    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
            when (key) {
                "cast_notifications" -> castOn = sp.getBoolean("cast_notifications", true)
                "cast_media_sessions" -> mediaOn = sp.getBoolean("cast_media_sessions", true)
                "cast_flashlight" -> flashlightOn = sp.getBoolean("cast_flashlight", true)
                PREF_BT_CONNECT_POPUP -> btConnectPopup = sp.getBoolean(PREF_BT_CONNECT_POPUP, false)
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
    var btPopupSheetOpen by remember { mutableStateOf(false) }
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

    if (btPopupSheetOpen) {
        BluetoothPopupSheet(context = context, onDismiss = { btPopupSheetOpen = false })
    }

    if (testerSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { testerSheetOpen = false },
            modifier = Modifier.statusBarsPadding(),
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = G2RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp, bottomEnd = 0.dp, bottomStart = 0.dp),
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
                SampleTesterCard(context)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButton(
                        text = "Hiện lại tất cả",
                        onClick = { recastAll(context) },
                        variant = PillButtonVariant.ALPHA_BLUE,
                        modifier = Modifier.weight(1f),
                        icon = IsleIcons.Refresh,
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

    val missingPermissions = !listenerOk || !a11yOk || !overlayOk || !connected || !batteryOk
    val permissionReason = when {
        !listenerOk -> "Chưa cấp quyền Truy cập thông báo để Đảo đọc thông báo."
        !connected -> "Dịch vụ lắng nghe chưa liên kết — kiểm tra lại quyền nền."
        !a11yOk -> "Chưa bật Hỗ trợ tiếp cận cho tiến trình tải xuống."
        !overlayOk -> "Chưa cấp quyền Hiển thị trên ứng dụng khác."
        !batteryOk -> "Cần tắt hạn chế pin để dịch vụ Đảo chạy ổn định."
        else -> ""
    }

    val toggleCast: (Boolean) -> Unit = { enabled ->
        castOn = enabled
        prefs.edit().putBoolean("cast_notifications", enabled).apply()
        if (enabled) PlaygroundService.keepAlive(context)
        FlashlightCard.refresh()
        EarbudsCard.onSettingsChanged(context, NotificationCastListener.instance != null)
    }

    OriginCollapsingScaffold(
        title = "Đảo Origin",
        hasBottomNav = true,
        trailing = {
            Box {
                CircleIconButton(IsleIcons.More, "Tùy chọn khác", onClick = { menuOpen = true })
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    offset = DpOffset(0.dp, 8.dp),
                    shape = G2RoundedCornerShape(18.dp),
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
        },
    ) {
        item(key = "home_hero_card") {
            HomeHeroCard(
                castOn = castOn,
                connected = connected,
                missingPermissions = missingPermissions,
                deviceName = displayModel,
                islandCount = CastLog.entries.size,
                onToggleCast = { toggleCast(!castOn) },
                onOpenPermissions = { permissionsOpen = true },
            )
        }

        if (missingPermissions) {
            item(key = "home_permissions_tip") {
                VTipCard(
                    title = "Thiếu quyền hệ thống quan trọng",
                    body = permissionReason,
                    icon = IsleIcons.Shield,
                    iconTint = if (!connected || !listenerOk) IsleColors.Bad else IsleColors.Warn,
                    actionLabel = "Mở bảng quyền",
                    actionColor = IsleColors.Link,
                    onAction = { permissionsOpen = true },
                )
            }
        }

        item(key = "home_bento_section") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Điều khiển nhanh",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = IsleColors.TextSub,
                    modifier = Modifier.padding(start = 16.dp, bottom = 2.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        BentoShortcutCard(
                            icon = IsleIcons.Sun,
                            title = "Đèn pin",
                            subtitle = if (flashlightOn) "Đang bật trên Đảo" else "Đang tắt",
                            isActive = flashlightOn,
                            onClick = {
                                val next = !flashlightOn
                                flashlightOn = next
                                prefs.edit().putBoolean("cast_flashlight", next).apply()
                                FlashlightCard.refresh()
                            },
                            modifier = Modifier.weight(1f),
                        )
                        BentoShortcutCard(
                            icon = IsleIcons.Headphones,
                            title = "Popup Bluetooth",
                            subtitle = if (btConnectPopup) "Đang bật popup" else "Tùy chỉnh tai nghe",
                            isActive = btConnectPopup,
                            onClick = { btPopupSheetOpen = true },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        BentoShortcutCard(
                            icon = IsleIcons.Grid,
                            title = "Ứng dụng",
                            subtitle = if (appsOff == 0) "Tất cả đang bật" else "Đang tắt $appsOff",
                            isActive = true,
                            onClick = onOpenApps,
                            modifier = Modifier.weight(1f),
                        )
                        BentoShortcutCard(
                            icon = IsleIcons.Pulse,
                            title = "Nhật ký Hoạt động",
                            subtitle = "${CastLog.entries.size} lượt Đảo",
                            isActive = connected,
                            onClick = onOpenActivity,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        item(key = "home_samples_section") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Thử nghiệm mẫu Đảo",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = IsleColors.TextSub,
                    modifier = Modifier.padding(start = 16.dp, bottom = 2.dp),
                )
                SampleTesterCard(context)
            }
        }
    }
}

@Composable
private fun HomeHeroCard(
    castOn: Boolean,
    connected: Boolean,
    missingPermissions: Boolean,
    deviceName: String,
    islandCount: Int,
    onToggleCast: () -> Unit,
    onOpenPermissions: () -> Unit,
) {
    val statusColor = when {
        !castOn -> IsleColors.TextTertiary
        connected -> IsleColors.Good
        else -> IsleColors.Warn
    }
    val statusLabel = when {
        !castOn -> "Origin Island đang tắt"
        connected -> "Origin Island đang hoạt động"
        else -> "Đang chờ kết nối dịch vụ"
    }
    val summaryText = when {
        !castOn -> "Chế độ tạm nghỉ · $deviceName"
        missingPermissions -> "Cần hoàn tất quyền · $islandCount lượt hôm nay"
        else -> "Sẵn sàng kết nối · $islandCount lượt hôm nay"
    }

    HeroCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.14f))
                        .border(0.6.dp, statusColor.copy(alpha = 0.28f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
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
                        text = if (!castOn) "Đã tắt" else if (connected) "Đã kết nối" else "Chờ liên kết",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                    )
                }

                Text(
                    text = "v${BuildConfig.VERSION_NAME}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = IsleColors.TextSub,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = statusLabel,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = IsleColors.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = summaryText,
                    fontSize = 13.sp,
                    color = IsleColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PillButton(
                    text = if (castOn) "Tắt Đảo" else "Bật Origin Island",
                    onClick = onToggleCast,
                    variant = if (castOn) PillButtonVariant.ALPHA_BLUE else PillButtonVariant.PRIMARY,
                    icon = IsleIcons.Power,
                    modifier = Modifier.weight(1f),
                    height = 50.dp,
                )
                PillButton(
                    text = if (missingPermissions) "Cấp quyền" else "Quyền & Kết nối",
                    onClick = onOpenPermissions,
                    variant = if (missingPermissions) PillButtonVariant.PRIMARY else PillButtonVariant.ALPHA_BLUE,
                    icon = IsleIcons.Shield,
                    modifier = Modifier.weight(1f),
                    height = 50.dp,
                )
            }
        }
    }
}

@Composable
private fun BentoShortcutCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "bentoScale",
    )
    val cardShape = G2RoundedCornerShape(24.dp)
    val isDark = IsleColors.isDark
    val iconBg = if (isActive) IsleColors.AccentSoft else IsleColors.SurfaceHigher
    val iconTint = if (isActive) IsleColors.Accent else IsleColors.TextSecondary

    Column(
        modifier = modifier
            .scale(scale)
            .heightIn(min = 108.dp)
            .clip(cardShape)
            .background(IsleColors.Surface)
            .then(
                if (isDark) {
                    Modifier.border(
                        width = 0.8.dp,
                        brush = Brush.verticalGradient(listOf(IsleColors.EdgeLightTop, IsleColors.EdgeLightBottom)),
                        shape = cardShape,
                    )
                } else {
                    Modifier
                },
            )
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp),
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
                color = IsleColors.TextSub,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
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
private fun SampleTesterCard(context: Context) {
    var selected by remember { mutableIntStateOf(0) }
    val sample = IslandSamples.all[selected]
    var sentAt by remember { mutableLongStateOf(0L) }
    LaunchedEffect(sentAt) {
        if (sentAt != 0L) {
            delay(1600)
            sentAt = 0L
        }
    }

    GroupCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 88.dp)
                    .clip(G2RoundedCornerShape(18.dp))
                    .background(IsleColors.PreviewBg)
                    .border(0.5.dp, IsleColors.Divider, G2RoundedCornerShape(18.dp))
                    .padding(horizontal = 10.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IslandPreviewView(sample.preview)
                    Text(
                        text = sample.summary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = IsleColors.PreviewLabel,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        GroupDivider(inset = 0.dp)

        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(IslandSamples.all) { i, s ->
                ChoiceChip(
                    label = s.name,
                    selected = i == selected,
                    onClick = { selected = i },
                    selectedContainer = IsleColors.Accent,
                    selectedContent = Color.White,
                    shape = G2RoundedCornerShape(18.dp),
                    height = 36.dp,
                    horizontalPadding = 14.dp,
                )
            }
        }

        GroupDivider(inset = 0.dp)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            PillButton(
                text = if (sentAt != 0L) "Đã gửi lên đảo" else "Thử trên đảo",
                onClick = {
                    sample.post(context)
                    sentAt = System.currentTimeMillis()
                },
                variant = PillButtonVariant.PRIMARY,
                modifier = Modifier.fillMaxWidth(),
                icon = IsleIcons.Send,
                height = 50.dp,
            )
        }
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
