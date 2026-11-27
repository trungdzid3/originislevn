package com.originisle.android.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.originisle.android.cards.EarbudsCard
import com.originisle.android.cards.FlashlightCard
import com.originisle.android.island.PlaygroundService
import com.originisle.android.log.CastLog
import com.originisle.android.service.NotificationCastListener
import com.originisle.android.service.ShizukuKeepAliveHelper
import com.originisle.android.ui.samples.IslandSamples
import kotlinx.coroutines.delay

/**
 * Tab 1 — Trang chủ Đảo Origin theo chuẩn OriginUI ROM 16.0:
 * - OriginCollapsingScaffold (28.sp -> 18.sp, hasBottomNav = true)
 * - HeroCard (30.dp G2) gọn 1 hàng với IsleSwitch bật/tắt trực tiếp
 * - VTipCard (12.dp G2) khi thiếu quyền quan trọng
 * - GroupCard (24.dp G2) 3 hàng điều hướng cốt lõi
 * - Khối thử nhanh mẫu Đảo (IslandSamples) duy nhất trong GroupCard (24.dp G2)
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

    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
            if (key == "cast_notifications") {
                castOn = sp.getBoolean("cast_notifications", true)
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
    val shizukuReady = remember(tick.intValue) {
        ShizukuKeepAliveHelper.getState(context) == ShizukuKeepAliveHelper.STATUS_READY
    }

    var connected by remember { mutableStateOf(NotificationCastListener.instance != null) }
    var deviceSnapshot by remember { mutableStateOf(DeviceInfoHelper.collect(context)) }
    var permissionsOpen by remember { mutableStateOf(false) }
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

    val sysStatusValue = when {
        missingPermissions -> "Thiếu quyền"
        shizukuReady -> "Đủ quyền · Shizuku"
        else -> "Đủ quyền"
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
                deviceName = displayModel,
                islandCount = CastLog.entries.size,
                onToggleCast = toggleCast,
            )
        }

        if (missingPermissions) {
            item(key = "home_permissions_tip") {
                VTipCard(
                    title = "Thiếu quyền hệ thống quan trọng",
                    body = permissionReason,
                    icon = IsleIcons.Shield,
                    iconTint = if (!connected || !listenerOk) IsleColors.Bad else IsleColors.Warn,
                    actionLabel = "Cấp quyền",
                    actionColor = IsleColors.Link,
                    onAction = { permissionsOpen = true },
                )
            }
        }

        item(key = "home_nav_group") {
            GroupCard {
                NavRow(
                    icon = IsleIcons.Grid,
                    tileColor = IsleColors.Accent,
                    title = "Ứng dụng được phép",
                    value = if (appsOff == 0) "Tất cả đang bật" else "Đang tắt $appsOff",
                    onClick = onOpenApps,
                )
                GroupDivider()
                NavRow(
                    icon = IsleIcons.Pulse,
                    tileColor = IsleColors.TileGreen,
                    title = "Nhật ký Hoạt động",
                    value = "${CastLog.entries.size} lượt Đảo",
                    onClick = onOpenActivity,
                )
                GroupDivider()
                NavRow(
                    icon = IsleIcons.Shield,
                    tileColor = if (missingPermissions) IsleColors.Warn else IsleColors.TileBlue,
                    title = "Quyền & Kết nối hệ thống",
                    value = sysStatusValue,
                    onClick = { permissionsOpen = true },
                )
            }
        }

        item(key = "home_samples_section") {
            SampleTesterCard(context)
        }
    }
}

@Composable
private fun HomeHeroCard(
    castOn: Boolean,
    connected: Boolean,
    deviceName: String,
    islandCount: Int,
    onToggleCast: (Boolean) -> Unit,
) {
    val statusColor = when {
        !castOn -> IsleColors.TextTertiary
        connected -> IsleColors.Good
        else -> IsleColors.Warn
    }
    val statusTitle = if (castOn) "Origin Island đang bật" else "Đang tắt"

    HeroCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            IconTile(
                icon = IsleIcons.Pulse,
                color = statusColor,
                size = 44.dp,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = statusTitle,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = IsleColors.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "$deviceName · $islandCount lượt",
                    fontSize = 12.5.sp,
                    color = IsleColors.TextSub,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IsleSwitch(
                checked = castOn,
                onCheckedChange = onToggleCast,
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Thử nhanh mẫu Đảo",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = IsleColors.Text,
            )
            Text(
                text = "Đóng hết",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = IsleColors.Link,
                modifier = Modifier
                    .clip(G2RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button) {
                        stopAll(context)
                        Toast.makeText(context, "Đã đóng tất cả thẻ Đảo", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            )
        }

        GroupDivider(inset = 0.dp)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 76.dp)
                    .clip(G2RoundedCornerShape(16.dp))
                    .background(IsleColors.PreviewBg)
                    .border(0.5.dp, IsleColors.Divider, G2RoundedCornerShape(16.dp))
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
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

        LazyRow(
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(IslandSamples.all) { i, s ->
                ChoiceChip(
                    label = s.name,
                    selected = i == selected,
                    onClick = { selected = i },
                    selectedContainer = IsleColors.Accent,
                    selectedContent = Color.White,
                    shape = G2RoundedCornerShape(16.dp),
                    height = 34.dp,
                    horizontalPadding = 12.dp,
                )
            }
        }

        GroupDivider(inset = 0.dp)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
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
                height = 44.dp,
            )
        }
    }
}

private fun stopAll(context: Context) {
    context.startService(Intent(context, PlaygroundService::class.java).setAction(PlaygroundService.ACTION_STOP))
}
