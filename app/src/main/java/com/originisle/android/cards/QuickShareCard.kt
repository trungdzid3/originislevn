package com.originisle.android.cards

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.Icon
import android.os.Handler
import android.os.Looper
import android.service.notification.StatusBarNotification
import com.originisle.android.R
import com.originisle.android.island.OriginIslandConstants
import com.originisle.android.island.PlaygroundService
import com.originisle.android.quickshare.QuickShareInfo
import com.originisle.android.service.IconCache
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

object QuickShareCard {

    private val handler = Handler(Looper.getMainLooper())
    private val lastPostTimeMap: MutableMap<Int, Long> = ConcurrentHashMap()
    private val lastProgressMap: MutableMap<Int, Int> = ConcurrentHashMap()
    private val finishedIds = ConcurrentHashMap.newKeySet<Int>()

    @Volatile
    private var cachedOfficialIcon: Icon? = null

    /**
     * Tạo biểu tượng Quick Share chính thức dạng Canvas Bitmap:
     * - Nền tròn màu xanh dương Quick Share #1B72E8.
     * - Vẽ 2 mũi tên chia sẻ lồng nhau dạng vòng cung xoay đối xứng 180 độ màu trắng chuẩn xác.
     */
    fun createQuickShareOfficialIcon(context: Context): Icon {
        cachedOfficialIcon?.let { return it }
        val size = 144
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = size / 2f
        val cy = size / 2f

        // Nền tròn màu xanh Quick Share
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1B72E8")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, cx, bgPaint)

        // Bút vẽ nét vòng cung màu trắng
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 11f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        // Bút vẽ đầu mũi tên màu trắng
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        val arcRadius = 34f
        val arcRect = RectF(cx - arcRadius, cy - arcRadius, cx + arcRadius, cy + arcRadius)

        // Nhánh 1: Cung tròn quét từ 170° qua đỉnh 270° đến 325°
        val endAngleDeg = 325.0
        val endAngleRad = Math.toRadians(endAngleDeg)
        val endX = cx + arcRadius * Math.cos(endAngleRad).toFloat()
        val endY = cy + arcRadius * Math.sin(endAngleRad).toFloat()

        // Hướng tiếp tuyến theo chiều kim đồng hồ (+90°)
        val tangentAngleRad = Math.toRadians(endAngleDeg + 90.0)
        val dirX = Math.cos(tangentAngleRad).toFloat()
        val dirY = Math.sin(tangentAngleRad).toFloat()
        val normX = -dirY
        val normY = dirX

        // Tạo hình tam giác đầu mũi tên
        val tipOffset = 3f
        val tipX = endX + dirX * tipOffset
        val tipY = endY + dirY * tipOffset
        val arrowLen = 16f
        val arrowHalfWidth = 8f
        val baseX = tipX - dirX * arrowLen
        val baseY = tipY - dirY * arrowLen

        val arrowPath = Path().apply {
            moveTo(tipX, tipY)
            lineTo(baseX + normX * arrowHalfWidth, baseY + normY * arrowHalfWidth)
            lineTo(baseX - normX * arrowHalfWidth, baseY - normY * arrowHalfWidth)
            close()
        }

        // Vẽ cánh trên (nhánh 1)
        canvas.drawArc(arcRect, 170f, 155f, false, strokePaint)
        canvas.drawPath(arrowPath, fillPaint)

        // Vẽ cánh dưới (nhánh 2) xoay đối xứng 180° qua tâm
        canvas.save()
        canvas.rotate(180f, cx, cy)
        canvas.drawArc(arcRect, 170f, 155f, false, strokePaint)
        canvas.drawPath(arrowPath, fillPaint)
        canvas.restore()

        val icon = Icon.createWithBitmap(bitmap)
        cachedOfficialIcon = icon
        return icon
    }

    private fun resolveShareIcon(context: Context, packageName: String): Icon {
        return if (packageName == "com.vivo.easyshare") {
            IconCache.safeAppIcon(context, packageName)
        } else {
            createQuickShareOfficialIcon(context)
        }
    }

    fun post(context: Context, sbn: StatusBarNotification, info: QuickShareInfo) = post(context, info, sbn)

    fun post(context: Context, info: QuickShareInfo, sbn: StatusBarNotification) {
        val id = IconCache.castIdFor(sbn)
        val now = System.currentTimeMillis()
        val lastTime = lastPostTimeMap[id] ?: 0L
        val lastProg = lastProgressMap[id] ?: -1

        val isTerminal = info.isFinished || info.isCanceled

        // Throttle chống spam thông báo khi đang truyền tệp:
        // Đăng ngay nếu là yêu cầu (REQUEST), hoặc hoàn tất/hủy, hoặc sau 500ms, hoặc tiến độ đổi >= 2%
        val shouldPost = info.isRequest || isTerminal ||
            (now - lastTime >= 500L) ||
            (abs(info.progress - lastProg) >= 2)

        if (!shouldPost) {
            return
        }

        if (info.isFinished) {
            finishedIds.add(id)
            lastPostTimeMap.remove(id)
            lastProgressMap[id] = 100
        } else if (info.isCanceled) {
            finishedIds.remove(id)
            lastPostTimeMap.remove(id)
            lastProgressMap[id] = 0
        } else {
            lastPostTimeMap[id] = now
            lastProgressMap[id] = info.progress
        }

        val n = sbn.notification
        // Quản lý Icon chuẩn Resource theo GenericCard:
        n.smallIcon?.let { IconCache.activeSmallIcons[id] = it } ?: IconCache.activeSmallIcons.remove(id)
        val appIcon: Icon = resolveShareIcon(context, sbn.packageName)
        IconCache.activeLargeIcons[id] = appIcon

        val clickPi = info.contentIntent
            ?: n.contentIntent
            ?: fallbackClickIntent(context, sbn.packageName)

        val intent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", id)
            putExtra("oi_scene", "NAVIGATION")
            putExtra("source_pkg", sbn.packageName)
            putExtra("icon_res", R.drawable.ic_download)
            putExtra("oi_left_icon", appIcon)
            putExtra("oi_large_icon", appIcon)
            putExtra("source_content_intent", clickPi)
            putExtra("click_resp", clickPi)
            putExtra("oi_force_show", true)
            putExtra("is_high_priority", true)
            putExtra("oi_dismiss_when_kill", true)
            putExtra("oi_show_notify", false)

            // Cấu hình SuperX chuẩn Vivo OriginOS 6 theo quy ước Animation
            when {
                info.isCanceled -> {
                    // Trạng thái hủy / thất bại (X đỏ)
                    putExtra("oi_template", OriginIslandConstants.TEMPLATE_PRIORITY_INFO) // 1
                    putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT) // 6
                    putExtra("oi_right_content", "✕")
                    putExtra("oi_left_content", "✕")
                    putExtra("oi_icon_status_type", OriginIslandConstants.ICON_STATUS_FAIL) // 1
                    putExtra("oi_light_color", "#FF5247")
                    putExtra("oi_bg_color", "#FF5247")
                    putExtra("oi_fg_color", "#FF5247")
                    putExtra("title", "Đã hủy")
                    putExtra("text", "✕")
                    putExtra("is_ongoing", false)
                }
                info.isFinished -> {
                    // Trạng thái hoàn tất truyền tệp (tích xanh)
                    putExtra("oi_template", OriginIslandConstants.TEMPLATE_PRIORITY_INFO) // 1
                    putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT) // 6
                    putExtra("oi_right_content", "✓")
                    putExtra("oi_left_content", "Xong")
                    putExtra("oi_icon_status_type", OriginIslandConstants.ICON_STATUS_SUCCESS) // 0
                    putExtra("oi_light_color", "#00D084")
                    putExtra("oi_bg_color", "#00D084")
                    putExtra("oi_fg_color", "#00D084")
                    putExtra("title", "Quick Share · Hoàn tất")
                    putExtra("text", "Đã nhận tệp")
                    putExtra("is_ongoing", false)
                }
                info.isRequest -> {
                    // Trạng thái yêu cầu nhận tệp (REQUEST)
                    val hasActions = info.actions.isNotEmpty()
                    putExtra("oi_template", if (hasActions) OriginIslandConstants.TEMPLATE_BUTTONS else OriginIslandConstants.TEMPLATE_BASE)
                    putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT) // 6
                    putExtra("oi_left_content", "")
                    putExtra("oi_right_content", "Nhận tệp")
                    putExtra("status_chip_text", "Nhận tệp")
                    putExtra("oi_island_show_time", 15)
                    putExtra("is_ongoing", true)
                    putExtra("title", info.title)
                    putExtra("text", info.text)
                    if (hasActions) {
                        putParcelableArrayListExtra("actions", ArrayList(info.actions))
                        val btnTitles = ArrayList(info.actions.mapNotNull { it.title?.toString() })
                        putStringArrayListExtra("oi_button_titles", if (btnTitles.isNotEmpty()) btnTitles else arrayListOf("Từ chối", "Chấp nhận"))
                    } else {
                        putStringArrayListExtra("oi_button_titles", arrayListOf("Từ chối", "Chấp nhận"))
                    }
                }
                else -> {
                    // Trạng thái đang truyền/kết nối tệp (PROGRESS / INDETERMINATE)
                    val pct = if (info.isIndeterminate) 10 else info.progress.coerceIn(1, 99)
                    val hasActions = info.actions.isNotEmpty()
                    if (hasActions) {
                        putExtra("oi_template", OriginIslandConstants.TEMPLATE_BUTTONS) // 8
                        putParcelableArrayListExtra("actions", ArrayList(info.actions))
                        val titles = ArrayList(info.actions.mapNotNull { it.title?.toString() })
                        putStringArrayListExtra("oi_button_titles", titles)
                    } else {
                        putExtra("oi_template", OriginIslandConstants.TEMPLATE_BASE) // 4
                        putExtra("oi_card_ring", pct)
                    }
                    putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_PROGRESS) // 2
                    putExtra("oi_progress", pct)
                    putExtra("show_progress", true)
                    putExtra("progress", pct)
                    putExtra("progress_max", 100)
                    if (info.isIndeterminate) putExtra("progress_indeterminate", true)
                    putExtra("oi_left_content", "$pct%")
                    putExtra("oi_right_content", "")
                    putExtra("status_chip_text", "$pct%")
                    putExtra("title", "Quick Share · ${if (info.isIndeterminate) "Đang kết nối" else "$pct%"}")
                    putExtra("text", "${info.title.take(22)} · ${info.text.ifBlank { "Đang truyền tệp" }}")
                    if (!info.speedText.isNullOrBlank()) {
                        putExtra("subtext", "Tốc độ: ${info.speedText}")
                    }
                    putExtra("is_ongoing", true)
                }
            }
        }

        context.startService(intent)

        // Tự động dọn dẹp đảo sau khi xong (3.5s) hoặc hủy (2.5s)
        if (info.isFinished) {
            handler.postDelayed({
                context.startService(
                    Intent(context, PlaygroundService::class.java)
                        .setAction(PlaygroundService.ACTION_CANCEL)
                        .putExtra("id", id)
                        .putExtra("oi_scene", "NAVIGATION")
                )
            }, 3500L)
        } else if (info.isCanceled) {
            handler.postDelayed({
                context.startService(
                    Intent(context, PlaygroundService::class.java)
                        .setAction(PlaygroundService.ACTION_CANCEL)
                        .putExtra("id", id)
                        .putExtra("oi_scene", "NAVIGATION")
                )
            }, 2500L)
        }
    }

    /**
     * Phát hoạt ảnh hoàn tất (tích xanh lá) khi notification bị gỡ bỏ lúc đã truyền xong (>= 80%).
     */
    fun postFinished(context: Context, id: Int, appPkg: String, title: String = "Quick Share") {
        clear(id)
        val appIcon: Icon = resolveShareIcon(context, appPkg)
        IconCache.activeLargeIcons[id] = appIcon
        val clickPi = fallbackClickIntent(context, appPkg)
        val intent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", id)
            putExtra("oi_scene", "NAVIGATION")
            putExtra("source_pkg", appPkg)
            putExtra("icon_res", R.drawable.ic_download)
            putExtra("oi_left_icon", appIcon)
            putExtra("oi_large_icon", appIcon)
            putExtra("source_content_intent", clickPi)
            putExtra("click_resp", clickPi)
            putExtra("oi_force_show", true)
            putExtra("is_high_priority", true)
            putExtra("oi_dismiss_when_kill", true)
            putExtra("oi_show_notify", false)
            putExtra("is_ongoing", false)
            putExtra("oi_template", OriginIslandConstants.TEMPLATE_PRIORITY_INFO) // 1
            putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT) // 6
            putExtra("oi_right_content", "✓")
            putExtra("oi_left_content", "Xong")
            putExtra("oi_icon_status_type", OriginIslandConstants.ICON_STATUS_SUCCESS) // 0
            putExtra("oi_light_color", "#00D084")
            putExtra("oi_bg_color", "#00D084")
            putExtra("oi_fg_color", "#00D084")
            putExtra("title", "Quick Share · Hoàn tất")
            putExtra("text", "Đã nhận tệp")
        }
        context.startService(intent)
        handler.postDelayed({
            context.startService(
                Intent(context, PlaygroundService::class.java)
                    .setAction(PlaygroundService.ACTION_CANCEL)
                    .putExtra("id", id)
                    .putExtra("oi_scene", "NAVIGATION")
            )
        }, 3500L)
    }

    /**
     * Phát hoạt ảnh hủy / lỗi (X đỏ) khi notification bị gỡ bỏ khi đang truyền dở (< 80%).
     */
    fun postCanceled(context: Context, id: Int, appPkg: String) {
        clear(id)
        val appIcon: Icon = resolveShareIcon(context, appPkg)
        IconCache.activeLargeIcons[id] = appIcon
        val clickPi = fallbackClickIntent(context, appPkg)
        val intent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", id)
            putExtra("oi_scene", "NAVIGATION")
            putExtra("source_pkg", appPkg)
            putExtra("icon_res", R.drawable.ic_download)
            putExtra("oi_left_icon", appIcon)
            putExtra("oi_large_icon", appIcon)
            putExtra("source_content_intent", clickPi)
            putExtra("click_resp", clickPi)
            putExtra("oi_force_show", true)
            putExtra("is_high_priority", true)
            putExtra("oi_dismiss_when_kill", true)
            putExtra("oi_show_notify", false)
            putExtra("is_ongoing", false)
            putExtra("oi_template", OriginIslandConstants.TEMPLATE_PRIORITY_INFO) // 1
            putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT) // 6
            putExtra("oi_right_content", "✕")
            putExtra("oi_left_content", "✕")
            putExtra("oi_icon_status_type", OriginIslandConstants.ICON_STATUS_FAIL) // 1
            putExtra("oi_light_color", "#FF5247")
            putExtra("oi_bg_color", "#FF5247")
            putExtra("oi_fg_color", "#FF5247")
            putExtra("title", "Đã hủy")
            putExtra("text", "✕")
        }
        context.startService(intent)
        handler.postDelayed({
            context.startService(
                Intent(context, PlaygroundService::class.java)
                    .setAction(PlaygroundService.ACTION_CANCEL)
                    .putExtra("id", id)
                    .putExtra("oi_scene", "NAVIGATION")
            )
        }, 2500L)
    }

    fun getLastProgress(id: Int): Int = lastProgressMap[id] ?: -1

    fun isFinished(id: Int): Boolean = finishedIds.contains(id)

    fun clear(id: Int) {
        lastPostTimeMap.remove(id)
        lastProgressMap.remove(id)
        finishedIds.remove(id)
    }
}
