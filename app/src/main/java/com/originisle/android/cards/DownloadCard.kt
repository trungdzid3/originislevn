package com.originisle.android.cards

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Icon
import android.os.Handler
import android.os.Looper
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.originisle.android.R
import com.originisle.android.download.DownloadInfo
import com.originisle.android.island.OriginIslandConstants
import com.originisle.android.island.PlaygroundService
import com.originisle.android.service.IconCache
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/**
 * Mở rộng BatteryCanvasRenderer để tạo biểu tượng vòng tròn đỏ có dấu X trắng (Red cross circle badge).
 * Thiết kế chuẩn OriginOS 6: vòng tròn đặc #FF5247, dấu ✕ trắng nét bo tròn 4.5dp (strokeCap ROUND).
 */
fun BatteryCanvasRenderer.createRedCrossCircleIcon(context: Context, size: Int = 96): Icon {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val s = size.toFloat()
    val cx = s / 2f
    val cy = s / 2f
    val r = s * 0.44f

    // 1. Vòng tròn nền đỏ đặc #FF5247
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#FF5247")
    }
    canvas.drawCircle(cx, cy, r, bgPaint)

    // 2. Dấu ✕ trắng bên trong với nét bo tròn Paint.Cap.ROUND
    val crossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = s * 0.12f
        strokeCap = Paint.Cap.ROUND
        color = Color.WHITE
    }
    val offset = r * 0.42f
    canvas.drawLine(cx - offset, cy - offset, cx + offset, cy + offset, crossPaint)
    canvas.drawLine(cx - offset, cy + offset, cx + offset, cy - offset, crossPaint)

    return Icon.createWithBitmap(bitmap)
}

object DownloadCard {

    private val handler = Handler(Looper.getMainLooper())
    private val lastPostTimeMap: MutableMap<Int, Long> = ConcurrentHashMap()
    private val lastProgressMap: MutableMap<Int, Int> = ConcurrentHashMap()
    private val firstSeenTimeMap: MutableMap<Int, Long> = ConcurrentHashMap()

    fun post(context: Context, sbn: StatusBarNotification, info: DownloadInfo) = post(context, info, sbn)

    fun post(context: Context, info: DownloadInfo, sbn: StatusBarNotification) {
        val id = IconCache.castIdFor(sbn)
        val now = System.currentTimeMillis()
        val lastTime = lastPostTimeMap[id] ?: 0L
        val lastProg = lastProgressMap[id] ?: -1

        val isCompleted = info.isFinished
        val isCanceled = info.isCanceled
        val isTerminated = isCompleted || isCanceled
        val isIndeterminate = info.isIndeterminate

        // Throttle chống spam thông báo từ trình duyệt/chợ ứng dụng:
        // Đăng ngay nếu hoàn tất hoặc bị hủy, hoặc trôi qua >= 500ms, hoặc tiến độ thay đổi >= 2%
        val shouldPost = isTerminated ||
            (now - lastTime >= 500L) ||
            (abs(info.progress - lastProg) >= 2)

        if (!shouldPost) {
            return
        }

        if (isTerminated) {
            lastPostTimeMap.remove(id)
            lastProgressMap.remove(id)
            firstSeenTimeMap.remove(id)
        } else {
            lastPostTimeMap[id] = now
            lastProgressMap[id] = info.progress
            if (!firstSeenTimeMap.containsKey(id)) {
                firstSeenTimeMap[id] = now
            }
        }

        val n = sbn.notification
        // Quản lý Icon chuẩn Resource theo GenericCard:
        n.smallIcon?.let { IconCache.activeSmallIcons[id] = it } ?: IconCache.activeSmallIcons.remove(id)

        // 1. Icon bên trái (Logo app): Luôn lấy app icon thực tế của nguồn tải
        val appIcon: Icon? = runCatching {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(sbn.packageName, 0)
            if (appInfo.icon != 0) Icon.createWithResource(sbn.packageName, appInfo.icon) else null
        }.getOrNull() ?: n.getLargeIcon()
        val fallbackIcon = Icon.createWithResource(context, R.mipmap.ic_launcher_round)
        val leftIcon = appIcon ?: fallbackIcon

        IconCache.activeLargeIcons[id] = leftIcon

        val clickPi = info.contentIntent
            ?: n.contentIntent
            ?: fallbackClickIntent(context, sbn.packageName)

        val appLabel = runCatching {
            context.packageManager.getApplicationLabel(
                context.packageManager.getApplicationInfo(sbn.packageName, 0),
            ).toString()
        }.getOrDefault(sbn.packageName)

        val subText = n.extras.getCharSequence(NotificationCompat.EXTRA_SUB_TEXT)?.toString()?.trim().orEmpty()
            .ifBlank { info.statusText }

        val pct = if (isIndeterminate && info.progress <= 0) 5 else info.progress.coerceIn(1, 99)

        val firstSeen = firstSeenTimeMap[id] ?: now
        val isStarting = !isTerminated && (isIndeterminate || pct <= 5 || (now - firstSeen < 1500L && pct <= 10))
        val downloadArrowIcon = Icon.createWithResource(context, R.drawable.ic_download)

        val intent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", id)
            putExtra("oi_scene", "DELIEVERY")
            putExtra("source_pkg", sbn.packageName)
            putExtra("source_app", appLabel)
            putExtra("source_content_intent", clickPi)
            putExtra("click_resp", clickPi)
            putExtra("oi_force_show", true)
            putExtra("is_high_priority", true)
            putExtra("oi_dismiss_when_kill", true)

            when {
                // 3. Khi hoàn tất (info.isFinished)
                isCompleted -> {
                    putExtra("is_ongoing", false)
                    putExtra("icon_res", R.mipmap.ic_launcher_round)
                    putExtra("oi_left_icon", leftIcon)
                    putExtra("oi_large_icon", leftIcon)
                    putExtra("oi_template", OriginIslandConstants.TEMPLATE_PRIORITY_INFO) // 1
                    putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT) // 6
                    putExtra("oi_right_content", "✓")
                    putExtra("oi_left_content", "Xong")
                    putExtra("oi_icon_status_type", OriginIslandConstants.ICON_STATUS_SUCCESS) // 0
                    putExtra("oi_light_color", "#00D084")
                    putExtra("oi_bg_color", "#00D084")
                    putExtra("oi_fg_color", "#00D084")
                    putExtra("title", "Tải xong")
                    putExtra("text", info.title.take(30))
                }
                // 4. Khi hủy / thất bại (info.isCanceled)
                isCanceled -> {
                    putExtra("is_ongoing", false)
                    putExtra("icon_res", R.drawable.ic_download)
                    putExtra("oi_left_icon", leftIcon)
                    putExtra("oi_large_icon", leftIcon)
                    putExtra("oi_template", OriginIslandConstants.TEMPLATE_PRIORITY_INFO) // 1
                    putExtra("oi_right_icon", BatteryCanvasRenderer.createRedCrossCircleIcon(context))
                    putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_TEXT_ICON) // 4
                    putExtra("oi_right_content", "")
                    putExtra("oi_left_content", "Đã hủy")
                    putExtra("oi_icon_status_type", OriginIslandConstants.ICON_STATUS_FAIL) // 1
                    putExtra("oi_light_color", "#FF5247")
                    putExtra("oi_bg_color", "#FF5247")
                    putExtra("oi_fg_color", "#FF5247")
                    putExtra("title", "Đã hủy")
                    putExtra("text", "${info.title.take(28)} · Đã hủy")
                }
                // 2. Khi đang tải
                else -> {
                    putExtra("is_ongoing", true)
                    putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_PROGRESS) // 2
                    putExtra("oi_progress", pct)
                    putExtra("show_progress", true)
                    putExtra("progress", pct)
                    putExtra("progress_max", 100)
                    if (isIndeterminate) {
                        putExtra("progress_indeterminate", true)
                    }

                    if (isStarting) {
                        // Giai đoạn ban đầu: hiển thị biểu tượng tải xuống ban đầu ic_download & nhãn Tải...
                        putExtra("icon_res", R.drawable.ic_download)
                        putExtra("oi_left_icon", downloadArrowIcon)
                        putExtra("oi_large_icon", downloadArrowIcon)
                        putExtra("oi_left_content", if (pct <= 0 || isIndeterminate) "Tải..." else "$pct%")
                        putExtra("oi_right_content", "")
                        putExtra("title", "${info.title.take(26)} · Đang tải")
                        putExtra("text", subText.ifBlank { "$appLabel · Đang tải xuống" })
                    } else {
                        // Giai đoạn có tiến độ: hiển thị logo app nơi đang tải ở bên trái, vòng quanh camera & thanh tiến độ bên phải
                        putExtra("icon_res", R.mipmap.ic_launcher_round)
                        putExtra("oi_left_icon", leftIcon)
                        putExtra("oi_large_icon", leftIcon)
                        putExtra("oi_left_content", "$pct%")
                        putExtra("oi_right_content", "")
                        putExtra("title", "${info.title.take(26)} ($pct%)")
                        putExtra("text", subText.ifBlank { "$appLabel · Đang tải xuống" })
                    }

                    if (!info.speedText.isNullOrBlank()) {
                        putExtra("subtext", "Tốc độ: ${info.speedText}")
                    }

                    // Khi chạm mở rộng (Expanded card):
                    if (info.actions.isNotEmpty()) {
                        putExtra("oi_template", OriginIslandConstants.TEMPLATE_BUTTONS) // 8
                        putParcelableArrayListExtra("actions", ArrayList(info.actions))
                        putStringArrayListExtra("oi_button_titles", ArrayList(info.actions.mapNotNull { it.title?.toString() }))
                    } else {
                        putExtra("oi_template", OriginIslandConstants.TEMPLATE_BASE) // 4
                        putExtra("oi_card_ring", pct)
                    }
                }
            }
        }

        context.startService(intent)

        // Tự động dọn dẹp đảo:
        if (isCompleted) {
            handler.postDelayed({
                context.startService(
                    Intent(context, PlaygroundService::class.java)
                        .setAction(PlaygroundService.ACTION_CANCEL)
                        .putExtra("id", id)
                        .putExtra("oi_scene", "DELIEVERY")
                )
            }, 3500L)
        } else if (isCanceled) {
            handler.postDelayed({
                context.startService(
                    Intent(context, PlaygroundService::class.java)
                        .setAction(PlaygroundService.ACTION_CANCEL)
                        .putExtra("id", id)
                        .putExtra("oi_scene", "DELIEVERY")
                )
            }, 2500L)
        }
    }

    fun clear(id: Int) {
        lastPostTimeMap.remove(id)
        lastProgressMap.remove(id)
        firstSeenTimeMap.remove(id)
    }
}
