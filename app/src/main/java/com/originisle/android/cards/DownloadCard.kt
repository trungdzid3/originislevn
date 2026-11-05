package com.originisle.android.cards

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Handler
import android.os.Looper
import android.service.notification.StatusBarNotification
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

    fun post(context: Context, info: DownloadInfo, sbn: StatusBarNotification) {
        val id = IconCache.castIdFor(sbn)
        val now = System.currentTimeMillis()
        val lastTime = lastPostTimeMap[id] ?: 0L
        val lastProg = lastProgressMap[id] ?: -1

        // Throttle chống spam thông báo từ trình duyệt/chợ ứng dụng:
        // Chỉ post nếu hoàn tất, hoặc trôi qua >= 500ms, hoặc tiến độ thay đổi >= 2%
        val shouldPost = info.isFinished ||
            (now - lastTime >= 500L) ||
            (abs(info.progress - lastProg) >= 2)

        if (!shouldPost) {
            return
        }

        if (info.isFinished) {
            lastPostTimeMap.remove(id)
            lastProgressMap.remove(id)
        } else {
            lastPostTimeMap[id] = now
            lastProgressMap[id] = info.progress
        }

        IconCache.activeSmallIcons.remove(id)
        val clickPi = info.contentIntent
            ?: sbn.notification.contentIntent
            ?: fallbackClickIntent(context, sbn.packageName)

        val fallbackIcon = Icon.createWithResource(context, R.mipmap.ic_launcher_round)
        val appIcon = IconCache.safeAppIcon(context, sbn.packageName) ?: fallbackIcon

        val chipText = if (info.isFinished) "Xong ✓" else "${info.progress}%"

        val displayStatus = when {
            info.isFinished -> info.statusText
            !info.speedText.isNullOrBlank() && !info.statusText.contains(info.speedText) -> {
                "${info.statusText} · ${info.speedText}"
            }
            else -> info.statusText
        }

        val intent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", id)
            putExtra("oi_scene", "NAVIGATION")
            putExtra("title", info.title)
            putExtra("text", displayStatus)
            if (!info.speedText.isNullOrBlank()) {
                putExtra("subtext", "Tốc độ: ${info.speedText}")
            }
            putExtra("source_pkg", sbn.packageName)

            putExtra("oi_template", OriginIslandConstants.TEMPLATE_BASE)
            putExtra("oi_card_ring", info.progress.coerceIn(0, 100))
            putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_PROGRESS)
            putExtra("oi_left_content", "")
            putExtra("status_chip_text", "${info.progress}%")
            putExtra("oi_right_content", "${info.progress}%")

            putExtra("show_progress", true)
            putExtra("progress", info.progress.coerceIn(0, 100))
            putExtra("progress_max", 100)
            putExtra("oi_large_icon", appIcon)
            putExtra("oi_left_icon", appIcon)
            putExtra("icon_res", R.mipmap.ic_launcher_round)

            putExtra("oi_force_show", true)
            putExtra("is_high_priority", true)
            putExtra("oi_dismiss_when_kill", true)
            putExtra("is_ongoing", !info.isFinished)
            putExtra("click_resp", clickPi)
            putExtra("source_content_intent", clickPi)
        }

        context.startService(intent)

        // Nếu đã hoàn tất, tự động dọn dẹp đảo sau 3.5 giây
        if (info.isFinished) {
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
