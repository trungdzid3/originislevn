package com.originisle.android.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.originisle.android.cards.BatteryCanvasRenderer
import com.originisle.android.cards.createGreenTickCircleIcon
import com.originisle.android.cards.TikTokStreakCard
import com.originisle.android.cards.ZaloCallCard
import com.originisle.android.island.OriginIslandConstants
import com.originisle.android.island.PlaygroundService
import com.originisle.android.log.CastLog
import com.originisle.android.service.NotificationCastListener
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Listener health plus the live log of what was cast or skipped, and why. */
@Composable
fun ActivityScreen(context: Context, onBack: () -> Unit) {
    // Ensure persisted logs are loaded into memory
    LaunchedEffect(Unit) {
        CastLog.ensureLoaded(context)
    }

    // Tick once a second so "last event / connected N ago" and the live status stay fresh.
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { now = System.currentTimeMillis(); delay(1000) }
    }

    val hasAccess = remember(now) {
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    }
    val isBound = NotificationCastListener.instance != null
    val connected = isBound && hasAccess

    // 0 = all, 1 = cast only, 2 = skipped only.
    var filter by remember { mutableIntStateOf(0) }
    val entries = CastLog.entries.filter {
        when (filter) {
            1 -> it.cast
            2 -> !it.cast
            else -> true
        }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = ScreenPadding) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                ScreenHeader("Nhật ký hoạt động", onBack) {
                    CircleIconButton(IsleIcons.Trash, "Xóa nhật ký", onClick = { CastLog.clear(context) }, iconSize = 19.dp)
                }

                // Cảnh báo nếu dịch vụ bị ngắt hoặc chưa cấp quyền trên Vivo OriginOS
                if (!connected) {
                    VivoDiagnosticAlertCard(
                        context = context,
                        hasAccess = hasAccess,
                        isBound = isBound,
                        onReconnect = { reconnectListener(context) },
                    )
                }

                ListenerCard(connected, now, onReconnect = { reconnectListener(context) })

                // Cài đặt chuỗi TikTok
                TikTokStreakSettingsCard(context = context)

                // Thẻ công cụ gửi thông báo thử nghiệm
                TestNotificationCard(context = context, connected = connected)

                SegmentedControl(
                    listOf("Tất cả", "Đã hiện", "Bỏ qua"), filter, { filter = it },
                    track = IsleColors.Surface, height = 40.dp,
                )
            }
        }

        if (entries.isEmpty()) {
            item {
                Box(Modifier.clip(RoundedCornerShape(24.dp)).background(IsleColors.Surface)) {
                    EmptyMessage(
                        if (CastLog.entries.isEmpty()) {
                            "Chưa có hoạt động nào. Hãy thử bấm các nút 'Kiểm tra thông báo thử nghiệm' ở trên để kiểm tra kết nối và hiển thị log ngay lập tức."
                        } else {
                            "Không có dữ liệu phù hợp với bộ lọc."
                        },
                    )
                }
            }
        }
        itemsIndexed(entries) { i, e ->
            Column(Modifier.clip(groupItemShape(i, entries.size)).background(IsleColors.Surface)) {
                if (i > 0) GroupDivider(inset = 0.dp)
                LogRow(e)
            }
        }
    }
}

/** Thẻ cảnh báo nổi bật màu đỏ khi listener chưa kết nối trên Vivo OriginOS */
@Composable
private fun VivoDiagnosticAlertCard(
    context: Context,
    hasAccess: Boolean,
    isBound: Boolean,
    onReconnect: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(IsleColors.BadBg)
            .border(1.2.dp, IsleColors.Bad.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier.size(32.dp).clip(CircleShape).background(IsleColors.Bad.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(IsleIcons.Shield, contentDescription = null, tint = IsleColors.Bad, modifier = Modifier.size(18.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "HỆ THỐNG THÔNG BÁO CHƯA KẾT NỐI!",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = IsleColors.BadText,
                )
                Text(
                    if (!hasAccess) "Chưa bật quyền Truy cập thông báo" else "Dịch vụ nền bị hệ thống Vivo tạm dừng",
                    fontSize = 12.sp,
                    color = IsleColors.BadSubtle,
                )
            }
        }

        Text(
            "Trên Vivo OriginOS, để đảo luôn nhận được thông báo ổn định, bạn cần thực hiện:\n" +
                "1. Cấp quyền 'Truy cập thông báo'.\n" +
                "2. Mở Cài đặt ứng dụng -> dấu 3 chấm góc trên -> Cho phép cài đặt bị hạn chế (Restricted Settings).\n" +
                "3. Bật Tự khởi chạy và Đặt pin chạy dưới nền Không hạn chế.",
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = IsleColors.Text,
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PillButton(
                text = "1. Quyền thông báo",
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                },
                modifier = Modifier.weight(1f),
                container = IsleColors.BadButton,
                content = IsleColors.BadText,
                height = 38.dp,
            )
            PillButton(
                text = "2. Cài đặt ứng dụng",
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                },
                modifier = Modifier.weight(1f),
                container = IsleColors.BadButton,
                content = IsleColors.BadText,
                height = 38.dp,
            )
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PillButton(
                text = "3. Mở Tự khởi chạy",
                onClick = { openAutoStartSettings(context) },
                modifier = Modifier.weight(1f),
                container = IsleColors.BadButton,
                content = IsleColors.BadText,
                height = 38.dp,
            )
            PillButton(
                text = "Kết nối lại ngay",
                onClick = onReconnect,
                modifier = Modifier.weight(1f),
                icon = IsleIcons.Refresh,
                container = IsleColors.Bad,
                content = Color.White,
                height = 38.dp,
            )
        }
    }
}

/** Thẻ công cụ gửi thông báo thử nghiệm giả lập Cuộc gọi Zalo và biến động số dư */
@Composable
private fun TestNotificationCard(context: Context, connected: Boolean) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(IsleColors.Surface)
            .border(0.8.dp, IsleColors.BorderHairline, RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier.size(32.dp).clip(CircleShape).background(IsleColors.Accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(IsleIcons.Send, contentDescription = null, tint = IsleColors.Accent, modifier = Modifier.size(16.dp))
            }
            Column(Modifier.weight(1f)) {
                Text("Kiểm tra gửi thông báo thử nghiệm", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = IsleColors.Text)
                Text("Gửi giả lập Cuộc gọi Zalo, Số dư & Tải về để kiểm tra đảo và nhật ký", fontSize = 12.sp, color = IsleColors.TextSecondary)
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PillButton(
                text = "Thử Số dư",
                onClick = { sendTestBankNotification(context, connected) },
                modifier = Modifier.weight(1f),
                icon = IsleIcons.CreditCard,
                container = IsleColors.SurfaceHigh,
                content = IsleColors.Text,
                height = 40.dp,
            )
            PillButton(
                text = "Thử Gọi Zalo",
                onClick = { sendTestZaloCallNotification(context) },
                modifier = Modifier.weight(1f),
                icon = IsleIcons.Pulse,
                container = IsleColors.SurfaceHigh,
                content = IsleColors.Text,
                height = 40.dp,
            )
            PillButton(
                text = "Thử Tải về",
                onClick = { sendTestDownloadNotification(context, connected) },
                modifier = Modifier.weight(1f),
                icon = IsleIcons.Download,
                container = IsleColors.SurfaceHigh,
                content = IsleColors.Text,
                height = 40.dp,
            )
        }

        PillButton(
            text = "Thử Chuỗi TikTok",
            onClick = { sendTestTikTokStreakNotification(context) },
            modifier = Modifier.fillMaxWidth(),
            icon = IsleIcons.Bolt,
            container = IsleColors.SurfaceHigh,
            content = Color(0xFFFF5E00),
            height = 40.dp,
        )
    }
}

/** Thẻ cài đặt tính năng Nhắc nhở chuỗi TikTok và tin nhắn tùy chỉnh */
@Composable
private fun TikTokStreakSettingsCard(context: Context) {
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var streakOn by remember { mutableStateOf(prefs.getBoolean("cast_tiktok_streak", true)) }
    var streakMessage by remember {
        mutableStateOf(prefs.getString("tiktok_streak_custom_message", "🔥") ?: "🔥")
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(IsleColors.Surface)
            .border(0.8.dp, IsleColors.BorderHairline, RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .toggleable(
                    value = streakOn,
                    role = Role.Switch,
                    onValueChange = {
                        streakOn = it
                        prefs.edit().putBoolean("cast_tiktok_streak", it).apply()
                    },
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFFF5E00).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(IsleIcons.Bolt, contentDescription = null, tint = Color(0xFFFF5E00), modifier = Modifier.size(16.dp))
            }
            Column(Modifier.weight(1f)) {
                Text("Nhắc nhở chuỗi TikTok", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = IsleColors.Text)
                Text(
                    "Bắt thông báo nhắc chuỗi TikTok, hiện đảo ngọn lửa để gửi tin nhắn duy trì nhanh",
                    fontSize = 12.sp,
                    color = IsleColors.TextSecondary,
                    lineHeight = 16.sp,
                )
            }
            IsleSwitch(checked = streakOn, onCheckedChange = null)
        }

        if (streakOn) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(IsleColors.SurfaceHigh)
                    .border(0.8.dp, IsleColors.BorderHairline, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("Nội dung tin nhắn gửi chuỗi:", fontSize = 12.sp, color = IsleColors.TextSecondary)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(IsleColors.Surface)
                        .border(0.8.dp, IsleColors.BorderHairline, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicTextField(
                        value = streakMessage,
                        onValueChange = {
                            streakMessage = it
                            prefs.edit().putString("tiktok_streak_custom_message", it).apply()
                        },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 14.sp, color = IsleColors.Text),
                        cursorBrush = SolidColor(Color(0xFFFF5E00)),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Text("Tự động sao chép vào bộ nhớ tạm khi nhấn nút ngọn lửa", fontSize = 11.sp, color = IsleColors.TextSecondary)
            }
        }
    }
}

private const val TEST_CHANNEL_ID = "origin_isle_test_channel"

private fun ensureTestChannel(context: Context): NotificationManager? {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return null
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        if (nm.getNotificationChannel(TEST_CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                TEST_CHANNEL_ID,
                "Origin Isle Thử nghiệm",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Kênh thông báo kiểm tra hiển thị đảo và nhật ký hoạt động"
            }
            nm.createNotificationChannel(channel)
        }
    }
    return nm
}

private fun sendTestBankNotification(context: Context, connected: Boolean) {
    runCatching {
        com.originisle.android.cards.PaymentCard.post(
            context = context,
            id = 9902,
            appIcon = android.graphics.drawable.Icon.createWithResource(context, com.originisle.android.R.mipmap.ic_launcher_round),
            amount = "+2.500.000 đ",
            description = "Chuyen tien thu nghiem Dao Origin",
            bankName = "Vietcombank",
            isIncome = true,
            clickResp = null,
        )
    }

    CastLog.add(
        app = "Vietcombank",
        title = "Vietcombank: +2.500.000 đ",
        outcome = "hiển thị — biến động số dư cộng tiền: +2.500.000 đ (Vietcombank)",
        cast = true,
        context = context,
    )
    Toast.makeText(context, "Đã phát thẻ Biến động số dư Vietcombank lên Đảo Origin!", Toast.LENGTH_SHORT).show()
}

private fun sendTestZaloCallNotification(context: Context) {
    runCatching {
        ZaloCallCard.postSample(context)
    }
    Toast.makeText(context, "Đã phát thẻ Cuộc gọi Zalo lên Đảo Origin!", Toast.LENGTH_SHORT).show()
}

private fun sendTestDownloadNotification(context: Context, connected: Boolean) {
    val app = context.applicationContext
    runCatching {
        val intent = Intent(app, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", 9903)
            putExtra("oi_scene", "NAVIGATION")
            putExtra("oi_template", OriginIslandConstants.TEMPLATE_BASE)
            putExtra("oi_card_ring", 75)
            putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_PROGRESS)
            putExtra("title", "Edge Canary")
            putExtra("text", "Đang tải xuống 75% · 2.4 MB/s")
            putExtra("subtext", "Tốc độ: 2.4 MB/s")
            putExtra("oi_left_content", "")
            putExtra("oi_right_content", "75%")
            putExtra("status_chip_text", "75%")
            putExtra("show_progress", true)
            putExtra("progress", 75)
            putExtra("progress_max", 100)
            putExtra("icon_res", com.originisle.android.R.mipmap.ic_launcher_round)
            putExtra("oi_force_show", true)
            putExtra("oi_dismiss_when_kill", true)
            putExtra("is_ongoing", true)
        }
        app.startService(intent)
    }

    // Sau 1500ms tự động phát thông báo hoàn tất 100% với biểu tượng tích xanh viền rỗng
    Handler(Looper.getMainLooper()).postDelayed({
        runCatching {
            val completeIntent = Intent(app, PlaygroundService::class.java).apply {
                action = PlaygroundService.ACTION_START
                putExtra("id", 9903)
                putExtra("oi_scene", "NAVIGATION")
                putExtra("oi_template", OriginIslandConstants.TEMPLATE_BASE)
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_TEXT_ICON)
                putExtra("oi_show_right_icon", true)
                putExtra("oi_right_icon", BatteryCanvasRenderer.createGreenTickCircleIcon(context))
                putExtra("icon_res", com.originisle.android.R.mipmap.ic_launcher_round)
                putExtra("oi_left_content", "")
                putExtra("oi_right_content", "")
                putExtra("status_chip_text", "")
                putExtra("oi_icon_status_type", OriginIslandConstants.ICON_STATUS_SUCCESS)
                putExtra("oi_light_color", "#00D084")
                putExtra("oi_bg_color", "#4C00D084")
                putExtra("oi_fg_color", "#00D084")
                putExtra("title", "Edge Canary · Đã tải xong")
                putExtra("text", "Tập tin cập nhật đã sẵn sàng")
                putExtra("subtext", "")
                putExtra("oi_force_show", true)
                putExtra("oi_dismiss_when_kill", true)
                putExtra("is_ongoing", false)
            }
            app.startService(completeIntent)
        }

        // Tự động hủy sau 4000ms
        Handler(Looper.getMainLooper()).postDelayed({
            runCatching {
                app.startService(
                    Intent(app, PlaygroundService::class.java).apply {
                        action = PlaygroundService.ACTION_CANCEL
                        putExtra("id", 9903)
                        putExtra("oi_scene", "NAVIGATION")
                    }
                )
            }
        }, 4000L)
    }, 1500L)

    CastLog.add(
        app = "Edge Canary",
        title = "Tải tệp tin: 75%",
        outcome = "hiển thị — tiến độ tải xuống 75% -> hoàn tất (Edge Canary)",
        cast = true,
        context = context,
    )
    Toast.makeText(context, "Đã phát thẻ Tiến trình tải xuống lên Đảo Origin!", Toast.LENGTH_SHORT).show()
}

private fun sendTestTikTokStreakNotification(context: Context) {
    runCatching {
        TikTokStreakCard.postSample(context)
    }
    Toast.makeText(context, "Đã phát thẻ Chuỗi TikTok lên Đảo Origin!", Toast.LENGTH_SHORT).show()
}

@Composable
private fun ListenerCard(connected: Boolean, now: Long, onReconnect: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(if (connected) IsleColors.GoodBg else IsleColors.BadBg)
            .padding(start = 18.dp, end = 12.dp, top = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val dot = if (connected) IsleColors.Good else IsleColors.Bad
        Box(
            Modifier.size(24.dp).clip(CircleShape).background(dot.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) { Box(Modifier.size(12.dp).clip(CircleShape).background(dot)) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                if (connected) "Đang hoạt động" else "Chưa kết nối dịch vụ",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = IsleColors.Text,
            )
            val subtle = if (connected) IsleColors.GoodSubtle else IsleColors.BadSubtle
            Text(
                "Đã kết nối ${agoText(NotificationCastListener.connectedAt, now)}",
                fontSize = 13.sp, color = subtle,
            )
            Text(
                "Sự kiện gần nhất: ${agoText(NotificationCastListener.lastEventAt, now)}",
                fontSize = 13.sp, color = subtle,
            )
        }
        CircleIconButton(
            IsleIcons.Refresh, "Kết nối lại", onReconnect,
            background = if (connected) IsleColors.GoodButton else IsleColors.BadButton,
            tint = if (connected) IsleColors.GoodText else IsleColors.BadText,
            iconSize = 18.dp,
        )
    }
}

@Composable
private fun LogRow(e: CastLog.Entry) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 68.dp).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (e.cast) IsleColors.GoodIconBg else IsleColors.SurfaceHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (e.cast) IsleIcons.Check else IsleIcons.Dash,
                if (e.cast) "Đã hiện" else "Bỏ qua",
                Modifier.size(16.dp),
                tint = if (e.cast) IsleColors.GoodText else IsleColors.TextSecondary,
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                e.title.ifBlank { e.app },
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (e.cast) IsleColors.Text else IsleColors.TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${e.app} · ${clockText(e.time)}",
                fontSize = 12.sp,
                color = IsleColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            outcomeTag(e.outcome),
            modifier = Modifier
                .widthIn(max = 116.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(if (e.cast) IsleColors.CastTagBg else IsleColors.SurfaceHigh)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 14.sp,
            color = if (e.cast) IsleColors.CastTagFg else IsleColors.TextTertiary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** "skipped — silent notification" → "Silent notification"; the icon already says cast/skipped. */
private fun outcomeTag(outcome: String): String =
    outcome.substringAfter(" — ").replaceFirstChar { it.uppercase() }

private fun reconnectListener(context: Context) {
    PlaygroundService.keepAlive(context)
    val message = when (NotificationCastListener.forceRebind(context)) {
        NotificationCastListener.RebindResult.REBINDING ->
            "Đang kết nối lại bộ lắng nghe… vui lòng chờ vài giây."
        NotificationCastListener.RebindResult.ALREADY_CONNECTED ->
            "Bộ lắng nghe thông báo đã được kết nối."
        NotificationCastListener.RebindResult.NO_ACCESS ->
            "Chưa cấp quyền truy cập thông báo — vui lòng bật trong Cài đặt."
        NotificationCastListener.RebindResult.FAILED ->
            "Không thể kết nối lại. Hãy kiểm tra 'Cho phép cài đặt bị hạn chế' trong Cài đặt ứng dụng của Vivo."
    }
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}

private fun clockText(ts: Long): String =
    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(ts))

private fun agoText(ts: Long, now: Long): String {
    if (ts <= 0L) return "chưa từng"
    val s = ((now - ts) / 1000).coerceAtLeast(0)
    return when {
        s < 2 -> "vừa xong"
        s < 60 -> "${s} giây trước"
        s < 3600 -> "${s / 60} phút trước"
        else -> "${s / 3600} giờ trước"
    }
}
