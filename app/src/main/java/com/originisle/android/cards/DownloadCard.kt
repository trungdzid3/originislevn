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

object DownloadCard {

    private val handler = Handler(Looper.getMainLooper())

    fun post(context: Context, info: DownloadInfo, sbn: StatusBarNotification) {
        val id = IconCache.castIdFor(sbn)

        val icon = Icon.createWithResource(context, R.mipmap.ic_launcher_round)
        val clickPi = info.contentIntent
            ?: sbn.notification.contentIntent
            ?: fallbackClickIntent(context, sbn.packageName)

        val chipText = when {
            info.isFinished -> "Xong ✓"
            info.progress > 0 -> "${info.progress}%"
            !info.speedText.isNullOrBlank() -> info.speedText
            info.isIndeterminate && info.statusText.contains("cài đặt") -> "Cài đặt"
            else -> "Đang tải"
        }

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

            // Cấu hình SuperX chuẩn OriginOS 6 (TEMPLATE_BASE + CAPSULE_TEXT)
            putExtra("oi_template", OriginIslandConstants.TEMPLATE_BASE)
            putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT)
            putExtra("status_chip_text", chipText)
            putExtra("oi_left_content", info.title)
            putExtra("oi_right_content", chipText)

            putExtra("oi_progress", info.progress)
            putExtra("show_progress", true)
            putExtra("progress", info.progress)
            putExtra("progress_max", 100)

            putExtra("oi_left_icon", icon)
            putExtra("oi_large_icon", icon)
            putExtra("icon_res", R.mipmap.ic_launcher_round)
            putExtra("oi_fg_color", "#FF2B66FF") // Vivo Origin Blue
            putExtra("oi_force_show", true)      // Ép đảo hiển thị viên thuốc ngay lập tức
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
}
