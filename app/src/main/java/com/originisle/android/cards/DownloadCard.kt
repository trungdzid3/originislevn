package com.originisle.android.cards

import android.content.Context
import android.content.Intent
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

object DownloadCard {

    private val handler = Handler(Looper.getMainLooper())
    private val lastPostTimeMap: MutableMap<Int, Long> = ConcurrentHashMap()
    private val lastProgressMap: MutableMap<Int, Int> = ConcurrentHashMap()

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
        } else {
            lastPostTimeMap[id] = now
            lastProgressMap[id] = info.progress
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

        val intent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", id)
            putExtra("oi_scene", "NAVIGATION")
            putExtra("source_pkg", sbn.packageName)
            putExtra("source_app", appLabel)
            putExtra("icon_res", R.mipmap.ic_launcher_round)
            putExtra("oi_left_icon", leftIcon)
            putExtra("oi_large_icon", leftIcon)
            putExtra("source_content_intent", clickPi)
            putExtra("click_resp", clickPi)
            putExtra("oi_force_show", true)
            putExtra("is_high_priority", true)
            putExtra("oi_dismiss_when_kill", true)

            when {
                // 3. Khi hoàn tất (info.isFinished)
                isCompleted -> {
                    putExtra("is_ongoing", false)
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
                    putExtra("oi_left_content", "$pct%")
                    putExtra("oi_right_content", "")
                    putExtra("title", "${info.title.take(26)} ($pct%)")
                    putExtra("text", subText.ifBlank { "$appLabel · Đang tải xuống" })
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
                        .putExtra("oi_scene", "NAVIGATION")
                )
            }, 3500L)
        } else if (isCanceled) {
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

    fun clear(id: Int) {
        lastPostTimeMap.remove(id)
        lastProgressMap.remove(id)
    }
}
