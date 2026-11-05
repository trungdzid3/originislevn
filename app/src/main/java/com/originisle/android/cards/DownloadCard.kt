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

    private const val TEMPLATE_ISLAND_TEXT_SWAP = OriginIslandConstants.TEMPLATE_PRIORITY_INFO // 1
    private const val TEMPLATE_RIGHT_ISLAND_NONE = 0

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
        val isIndeterminate = info.isIndeterminate

        // Throttle chống spam thông báo từ trình duyệt/chợ ứng dụng:
        // Chỉ post nếu hoàn tất, hoặc trôi qua >= 500ms, hoặc tiến độ thay đổi >= 2%
        val shouldPost = isCompleted ||
            (now - lastTime >= 500L) ||
            (abs(info.progress - lastProg) >= 2)

        if (!shouldPost) {
            return
        }

        if (isCompleted) {
            lastPostTimeMap.remove(id)
            lastProgressMap.remove(id)
        } else {
            lastPostTimeMap[id] = now
            lastProgressMap[id] = info.progress
        }

        val n = sbn.notification
        // Quản lý Icon chuẩn Resource theo GenericCard:
        n.smallIcon?.let { IconCache.activeSmallIcons[id] = it } ?: IconCache.activeSmallIcons.remove(id)
        val largeIcon: Icon? = try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(sbn.packageName, 0)
            if (appInfo.icon != 0) Icon.createWithResource(sbn.packageName, appInfo.icon)
            else n.getLargeIcon()
        } catch (_: Throwable) {
            n.getLargeIcon()
        }
        if (largeIcon != null) {
            IconCache.activeLargeIcons[id] = largeIcon
        } else {
            IconCache.activeLargeIcons.remove(id)
        }

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

        val pct = info.progress.coerceIn(1, 99)

        val intent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", id)
            putExtra("oi_scene", "NAVIGATION")
            putExtra("source_pkg", sbn.packageName)
            putExtra("source_app", appLabel)
            putExtra("icon_res", R.mipmap.ic_launcher_round)
            putExtra("source_content_intent", clickPi)
            putExtra("click_resp", clickPi)
            putExtra("oi_force_show", true)
            putExtra("is_high_priority", true)
            putExtra("oi_dismiss_when_kill", true)
            putExtra("is_ongoing", !isCompleted)

            when {
                isCompleted -> {
                    putExtra("oi_template", TEMPLATE_ISLAND_TEXT_SWAP) // 1
                    putExtra("oi_right_template", TEMPLATE_RIGHT_ISLAND_NONE) // 0
                    putExtra("oi_left_content", "Xong")
                    putExtra("oi_right_content", info.title.take(10))
                    putExtra("title", "Tải xong · ${info.title.take(22)}")
                    putExtra("text", subText.ifBlank { "Chạm để mở tệp từ $appLabel" })
                }
                isIndeterminate -> {
                    putExtra("oi_template", OriginIslandConstants.TEMPLATE_BASE) // 4
                    putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_PROGRESS) // 2
                    putExtra("oi_progress", 5)
                    putExtra("show_progress", true)
                    putExtra("progress", 5)
                    putExtra("progress_max", 100)
                    putExtra("progress_indeterminate", true)
                    putExtra("oi_left_content", "Tải...")
                    putExtra("oi_right_content", "")
                    putExtra("title", info.title.take(28))
                    putExtra("text", "$appLabel · Đang chuẩn bị tệp...")
                }
                else -> {
                    putExtra("oi_template", OriginIslandConstants.TEMPLATE_BASE) // 4
                    putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_PROGRESS) // 2
                    putExtra("oi_progress", pct)
                    putExtra("show_progress", true)
                    putExtra("progress", pct)
                    putExtra("progress_max", 100)
                    putExtra("oi_left_content", "${pct}%")
                    putExtra("oi_right_content", "")
                    putExtra("title", "${info.title.take(26)} (${pct}%)")
                    putExtra("text", subText.ifBlank { "$appLabel · Đang tải xuống" })
                    if (!info.speedText.isNullOrBlank()) {
                        putExtra("subtext", "Tốc độ: ${info.speedText}")
                    }
                }
            }
        }

        context.startService(intent)

        // Nếu đã hoàn tất, tự động dọn dẹp đảo sau 3.5 giây
        if (isCompleted) {
            handler.postDelayed({
                context.startService(
                    Intent(context, PlaygroundService::class.java)
                        .setAction(PlaygroundService.ACTION_CANCEL)
                        .putExtra("id", id)
                        .putExtra("oi_scene", "NAVIGATION")
                )
            }, 3500L)
        }
    }

    fun clear(id: Int) {
        lastPostTimeMap.remove(id)
        lastProgressMap.remove(id)
    }
}
