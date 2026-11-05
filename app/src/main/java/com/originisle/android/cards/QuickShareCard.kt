package com.originisle.android.cards

import android.content.Context
import android.content.Intent
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

    private const val TEMPLATE_ISLAND_TEXT_SWAP = OriginIslandConstants.TEMPLATE_PRIORITY_INFO // 1
    private const val TEMPLATE_RIGHT_ISLAND_NONE = 0

    private val handler = Handler(Looper.getMainLooper())
    private val lastPostTimeMap: MutableMap<Int, Long> = ConcurrentHashMap()
    private val lastProgressMap: MutableMap<Int, Int> = ConcurrentHashMap()

    fun post(context: Context, sbn: StatusBarNotification, info: QuickShareInfo) = post(context, info, sbn)

    fun post(context: Context, info: QuickShareInfo, sbn: StatusBarNotification) {
        val id = IconCache.castIdFor(sbn)
        val now = System.currentTimeMillis()
        val lastTime = lastPostTimeMap[id] ?: 0L
        val lastProg = lastProgressMap[id] ?: -1

        // Throttle chống spam thông báo khi đang truyền tệp:
        // Đăng ngay nếu là yêu cầu (REQUEST), hoặc hoàn tất, hoặc sau 500ms, hoặc tiến độ đổi >= 2%
        val shouldPost = info.isRequest || info.isFinished ||
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

        val intent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", id)
            putExtra("oi_scene", "NAVIGATION")
            putExtra("source_pkg", sbn.packageName)
            putExtra("icon_res", R.drawable.ic_download)
            putExtra("source_content_intent", clickPi)
            putExtra("click_resp", clickPi)
            putExtra("oi_force_show", true)
            putExtra("is_high_priority", true)
            putExtra("oi_dismiss_when_kill", true)
            putExtra("oi_show_notify", false)

            // Cấu hình SuperX chuẩn Vivo OriginOS 6 theo mẫu demo hoạt động tốt
            when {
                info.isRequest -> {
                    // Trạng thái yêu cầu nhận tệp (REQUEST)
                    putExtra("oi_template", OriginIslandConstants.TEMPLATE_BASE) // 4
                    putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT) // 6
                    putExtra("oi_left_content", "")
                    putExtra("oi_right_content", "Nhận tệp")
                    putExtra("status_chip_text", "Nhận tệp")
                    putExtra("oi_island_show_time", 15)
                    putExtra("is_ongoing", true)
                    putExtra("title", info.title)
                    putExtra("text", info.text)
                    if (info.actions.isNotEmpty()) {
                        putParcelableArrayListExtra("actions", ArrayList(info.actions))
                        val btnTitles = if (info.actions.size >= 2) {
                            arrayListOf(
                                info.actions[0].title?.toString() ?: "Từ chối",
                                info.actions[1].title?.toString() ?: "Chấp nhận",
                            )
                        } else {
                            arrayListOf("Từ chối", "Chấp nhận")
                        }
                        putStringArrayListExtra("oi_button_titles", btnTitles)
                    } else {
                        putStringArrayListExtra("oi_button_titles", arrayListOf("Từ chối", "Chấp nhận"))
                    }
                }
                info.isFinished -> {
                    // Trạng thái hoàn tất truyền tệp
                    putExtra("oi_template", TEMPLATE_ISLAND_TEXT_SWAP) // 1
                    putExtra("oi_right_template", TEMPLATE_RIGHT_ISLAND_NONE) // 0
                    putExtra("oi_left_content", "Xong")
                    putExtra("oi_right_content", "Quick Share")
                    putExtra("title", "Quick Share · Hoàn tất")
                    putExtra("text", info.text.ifBlank { "Đã hoàn tất truyền tệp" })
                    putExtra("is_ongoing", false)
                }
                else -> {
                    // Trạng thái đang truyền/kết nối tệp (PROGRESS / INDETERMINATE)
                    val pct = if (info.isIndeterminate) 10 else info.progress.coerceIn(1, 99)
                    putExtra("oi_template", OriginIslandConstants.TEMPLATE_BASE) // 4
                    putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_PROGRESS) // 2
                    putExtra("oi_progress", pct)
                    putExtra("show_progress", true)
                    putExtra("progress", pct)
                    putExtra("progress_max", 100)
                    if (info.isIndeterminate) putExtra("progress_indeterminate", true)
                    putExtra("oi_left_content", "${pct}%")
                    putExtra("oi_right_content", "")
                    putExtra("status_chip_text", "${pct}%")
                    putExtra("title", "Quick Share · ${if (info.isIndeterminate) "Đang kết nối" else "$pct%"}")
                    putExtra("text", "${info.title.take(22)} · ${info.text.ifBlank { "Đang truyền tệp" }}")
                    if (!info.speedText.isNullOrBlank()) {
                        putExtra("subtext", "Tốc độ: ${info.speedText}")
                    }
                    putExtra("is_ongoing", true)
                    if (info.actions.isNotEmpty()) {
                        putParcelableArrayListExtra("actions", ArrayList(info.actions))
                    }
                }
            }
        }

        context.startService(intent)

        // Nếu đã hoàn tất, tự động dọn dẹp đảo sau 3.5 giây
        if (!info.isRequest && info.isFinished) {
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
