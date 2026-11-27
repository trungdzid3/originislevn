package com.originisle.android.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
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
    LaunchedEffect(Unit) {
        CastLog.ensureLoaded(context)
    }

    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    var hasAccess by remember {
        mutableStateOf(
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName),
        )
    }
    var isBound by remember { mutableStateOf(NotificationCastListener.instance != null) }

    val lifecycle = (context as? ComponentActivity)?.lifecycle
    LaunchedEffect(lifecycle) {
        val pollLoop: suspend () -> Unit = {
            while (true) {
                val nextNow = System.currentTimeMillis()
                if (nextNow != now) now = nextNow
                val nextAccess = NotificationManagerCompat
                    .getEnabledListenerPackages(context)
                    .contains(context.packageName)
                if (nextAccess != hasAccess) hasAccess = nextAccess
                val nextBound = NotificationCastListener.instance != null
                if (nextBound != isBound) isBound = nextBound
                delay(3000L)
            }
        }
        if (lifecycle != null) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { pollLoop() }
        } else {
            pollLoop()
        }
    }

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

    OriginCollapsingScaffold(
        title = "Nhật ký hoạt động",
        onBack = onBack,
        verticalArrangement = Arrangement.spacedBy(0.dp),
        trailing = {
            PillButton(
                text = "Xóa",
                onClick = { CastLog.clear(context) },
                variant = PillButtonVariant.ALPHA_GRAY,
                icon = IsleIcons.Trash,
                iconSize = 16.dp,
                height = 38.dp,
            )
        },
    ) {
        item(key = "activity_header_cards") {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
            ) {
                if (!connected) {
                    VivoDiagnosticAlertCard(
                        context = context,
                        hasAccess = hasAccess,
                        onReconnect = { reconnectListener(context) },
                    )
                }

                ListenerCard(connected, now, onReconnect = { reconnectListener(context) })

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("Tất cả", "Đã hiện", "Bỏ qua").forEachIndexed { index, label ->
                        ChoiceChip(
                            label = label,
                            selected = filter == index,
                            onClick = { filter = index },
                            selectedContainer = IsleColors.AccentSoft,
                            selectedContent = IsleColors.Accent,
                            shape = G2RoundedCornerShape(18.dp),
                            height = 38.dp,
                            horizontalPadding = 14.dp,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        if (entries.isEmpty()) {
            item(key = "activity_empty") {
                GroupCard {
                    EmptyMessage(
                        if (CastLog.entries.isEmpty()) {
                            "Chưa có hoạt động nào được ghi nhận."
                        } else {
                            "Không có dữ liệu phù hợp với bộ lọc."
                        },
                    )
                }
            }
        } else {
            itemsIndexed(entries) { i, e ->
                val shape = groupItemShape(i, entries.size)
                val isDark = IsleColors.isDark
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(IsleColors.Surface)
                        .then(
                            if (isDark) {
                                Modifier.border(
                                    width = 0.8.dp,
                                    brush = Brush.verticalGradient(listOf(IsleColors.EdgeLightTop, IsleColors.EdgeLightBottom)),
                                    shape = shape,
                                )
                            } else {
                                Modifier
                            },
                        ),
                ) {
                    if (i > 0) GroupDivider(inset = 60.dp)
                    LogRow(e)
                }
            }
        }
    }
}

/** Thẻ cảnh báo nổi bật màu đỏ khi listener chưa kết nối trên Vivo OriginOS */
@Composable
private fun VivoDiagnosticAlertCard(
    context: Context,
    hasAccess: Boolean,
    onReconnect: () -> Unit,
) {
    val alertShape = G2RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(alertShape)
            .background(IsleColors.BadBg)
            .border(1.0.dp, IsleColors.Bad.copy(alpha = 0.4f), alertShape)
            .padding(16.dp),
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

@Composable
private fun ListenerCard(connected: Boolean, now: Long, onReconnect: () -> Unit) {
    val cardShape = G2RoundedCornerShape(24.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(if (connected) IsleColors.GoodBg else IsleColors.BadBg)
            .border(
                width = 0.8.dp,
                brush = Brush.verticalGradient(listOf(IsleColors.EdgeLightTop, IsleColors.EdgeLightBottom)),
                shape = cardShape,
            )
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
                fontSize = 13.sp,
                color = subtle,
            )
            Text(
                "Sự kiện gần nhất: ${agoText(NotificationCastListener.lastEventAt, now)}",
                fontSize = 13.sp,
                color = subtle,
            )
        }
        CircleIconButton(
            IsleIcons.Refresh,
            "Kết nối lại",
            onReconnect,
            background = if (connected) IsleColors.GoodButton else IsleColors.BadButton,
            tint = if (connected) IsleColors.GoodText else IsleColors.BadText,
            iconSize = 18.dp,
        )
    }
}

@Composable
private fun LogRow(e: CastLog.Entry) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 66.dp).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (e.cast) IsleColors.Good.copy(alpha = 0.14f) else IsleColors.SurfaceHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (e.cast) IsleIcons.Check else IsleIcons.Dash,
                if (e.cast) "Đã hiện" else "Bỏ qua",
                Modifier.size(16.dp),
                tint = if (e.cast) IsleColors.Good else IsleColors.TextSub,
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                e.title.ifBlank { e.app },
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (e.cast) IsleColors.Text else IsleColors.TextSub,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${e.app} · ${clockText(e.time)}",
                fontSize = 12.sp,
                color = IsleColors.TextSub,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        val badgeShape = G2RoundedCornerShape(18.dp)
        Text(
            outcomeTag(e.outcome),
            modifier = Modifier
                .widthIn(max = 116.dp)
                .clip(badgeShape)
                .background(if (e.cast) IsleColors.Good.copy(alpha = 0.14f) else IsleColors.SurfaceHigher)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 14.sp,
            color = if (e.cast) IsleColors.Good else IsleColors.TextSub,
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
            "Đang kết nối lại bộ lắng nghe... vui lòng chờ vài giây."
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
