package com.originisle.android.cards

import android.app.Notification
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
import androidx.core.graphics.PathParser
import com.originisle.android.R
import com.originisle.android.island.OriginIslandConstants
import com.originisle.android.island.PlaygroundService
import com.originisle.android.quickshare.QuickShareInfo
import com.originisle.android.service.IconCache
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

object QuickShareCard {

    const val ID = 35009

    private val handler = Handler(Looper.getMainLooper())
    private val lastPostTimeMap: MutableMap<Int, Long> = ConcurrentHashMap()
    private val lastProgressMap: MutableMap<Int, Int> = ConcurrentHashMap()
    private val finishedIds = ConcurrentHashMap.newKeySet<Int>()
    private val watchdogTasks: MutableMap<Int, Runnable> = ConcurrentHashMap()
    private val pendingCancelTasks: MutableMap<Int, Runnable> = ConcurrentHashMap()

    @Volatile
    private var activeTransfer: Boolean = false

    @Volatile
    private var lastActivePackage: String = "com.google.android.gms"

    @Volatile
    private var cachedOfficialIcon: Icon? = null

    fun isTransferring(): Boolean = activeTransfer

    fun getLastActivePackage(): String = lastActivePackage

    /**
     * Tạo biểu tượng Quick Share chính thức dạng Canvas Bitmap:
     * - Nền tròn màu xanh Quick Share #4285F4.
     * - Vẽ 2 cánh mũi tên xoay tròn màu trắng #FFFFFF theo dữ liệu vector chuẩn từ Google/Samsung Quick Share.
     */
    fun createQuickShareOfficialIcon(context: Context, size: Int = 192): Icon {
        cachedOfficialIcon?.let { return it }
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val scale = size / 222f
        canvas.save()
        canvas.scale(scale, scale)

        // Nền tròn màu xanh Quick Share #4285F4
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4285F4")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(111f, 111f, 111f, bgPaint)

        // Bút vẽ 2 cánh mũi tên màu trắng #FFFFFF
        val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        val pathData1 = "M183.827 110.596c-3.629 0-6.738-2.738-7.119-6.405-3.466-33.6-31.716-58.936-65.706-58.936-27.54 0-51.718 16.758-61.499 41.657h43.47l-3.517-3.502a6.312 6.312 0 0 1 0-8.956 6.388 6.388 0 0 1 9.004 0l15.17 15.098a6.314 6.314 0 0 1 0 8.961l-15.17 15.098a6.384 6.384 0 0 1-8.999 0 6.315 6.315 0 0 1 0-8.961l3.493-3.475H40.305c-2.195 0-4.589-1.168-5.95-2.885-1.363-1.725-1.93-4.34-1.406-6.467C41.801 56.011 73.9 31 111.002 31c41.361 0 75.736 30.843 79.96 71.737.404 3.914-2.46 7.418-6.396 7.82a8.026 8.026 0 0 1-.739.039Z"
        val pathData2 = "M37.437 111.354c3.983-.356 7.45 2.46 7.854 6.374.12 1.199.276 2.39.462 3.565 5.071 32.131 32.514 55.444 65.249 55.444 27.551 0 51.737-16.761 61.51-41.683h-43.509l3.545 3.528a6.316 6.316 0 0 1 0 8.961 6.385 6.385 0 0 1-8.999 0l-15.174-15.101a6.316 6.316 0 0 1 0-8.961l15.171-15.099a6.39 6.39 0 0 1 9.004 0 6.313 6.313 0 0 1 0 8.957l-3.468 3.452h52.628c2.195 0 4.593 1.168 5.949 2.885 1.364 1.725 1.931 4.324 1.407 6.458C180.225 165.97 148.124 191 111.002 191c-39.835 0-73.226-28.387-79.401-67.494a83.856 83.856 0 0 1-.564-4.34c-.4-3.913 2.468-7.41 6.4-7.812Z"

        try {
            val path1 = PathParser.createPathFromPathData(pathData1)
            val path2 = PathParser.createPathFromPathData(pathData2)
            canvas.drawPath(path1, pathPaint)
            canvas.drawPath(path2, pathPaint)
        } catch (_: Exception) {
        }
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

    private fun cancelPendingDismiss(id: Int = ID) {
        pendingCancelTasks.remove(id)?.let(handler::removeCallbacks)
        if (id != ID) {
            pendingCancelTasks.remove(ID)?.let(handler::removeCallbacks)
        }
    }

    private fun scheduleDelayedCancel(context: Context, id: Int = ID, sbnId: Int? = null, delayMs: Long) {
        cancelPendingDismiss(id)
        cancelPendingDismiss(ID)
        sbnId?.let { cancelPendingDismiss(it) }
        val cancelRunnable = Runnable {
            pendingCancelTasks.remove(id)
            pendingCancelTasks.remove(ID)
            sbnId?.let { pendingCancelTasks.remove(it) }
            context.startService(
                Intent(context, PlaygroundService::class.java)
                    .setAction(PlaygroundService.ACTION_CANCEL)
                    .putExtra("id", ID)
                    .putExtra("oi_scene", "NAVIGATION")
            )
        }
        pendingCancelTasks[id] = cancelRunnable
        pendingCancelTasks[ID] = cancelRunnable
        sbnId?.let { pendingCancelTasks[it] = cancelRunnable }
        handler.postDelayed(cancelRunnable, delayMs)
    }

    fun post(context: Context, sbn: StatusBarNotification, info: QuickShareInfo) = post(context, info, sbn)

    fun post(context: Context, info: QuickShareInfo, sbn: StatusBarNotification) {
        val sbnId = IconCache.castIdFor(sbn)
        val cardId = ID // 35009

        // Nếu có phiên nhận tệp mới (request hoặc đang truyền thực tế 1..99%), xóa trạng thái finished cũ
        if (info.isRequest || (info.progress in 1..99 && !info.isFinished) || (info.isIndeterminate && !info.isFinished)) {
            finishedIds.remove(cardId)
            finishedIds.remove(sbnId)
            cancelPendingDismiss(cardId)
            cancelPendingDismiss(sbnId)
        }

        // Nếu đã hoàn tất (đã post thẻ 100%), KHÔNG BAO GIỜ post lại thẻ để tránh vòng lặp thu/mở liên tục từ pollRunnable!
        if (finishedIds.contains(cardId) || finishedIds.contains(sbnId)) {
            return
        }

        val now = System.currentTimeMillis()
        val lastTime = lastPostTimeMap[cardId] ?: lastPostTimeMap[sbnId] ?: 0L
        val lastProg = lastProgressMap[cardId] ?: lastProgressMap[sbnId] ?: -1

        val isOngoingNotif = (sbn.notification.flags and Notification.FLAG_ONGOING_EVENT) != 0 || sbn.isOngoing
        val isFinishedEffective = info.isFinished || (info.progress == 100)
        val isFailureOrCanceled = info.isCanceled || info.isFailed
        val isTerminal = isFinishedEffective || isFailureOrCanceled

        // Throttle chống spam thông báo khi đang truyền tệp:
        // Đăng ngay nếu là yêu cầu (REQUEST), hoặc hoàn tất/hủy/lỗi, hoặc khi bắt đầu nhận tệp, hoặc sau 1000ms VÀ tiến độ đổi >= 2%
        val shouldPost = info.isRequest || isTerminal || !activeTransfer ||
            (now - lastTime >= 1000L && abs(info.progress - lastProg) >= 2)

        if (!shouldPost) {
            return
        }

        if (isFinishedEffective) {
            activeTransfer = false
            watchdogTasks.remove(sbnId)?.let(handler::removeCallbacks)
            watchdogTasks.remove(cardId)?.let(handler::removeCallbacks)
            finishedIds.add(sbnId)
            finishedIds.add(cardId)
            lastPostTimeMap.remove(sbnId)
            lastPostTimeMap.remove(cardId)
            lastProgressMap[sbnId] = 100
            lastProgressMap[cardId] = 100
        } else if (isFailureOrCanceled) {
            activeTransfer = false
            watchdogTasks.remove(sbnId)?.let(handler::removeCallbacks)
            watchdogTasks.remove(cardId)?.let(handler::removeCallbacks)
            finishedIds.remove(sbnId)
            finishedIds.remove(cardId)
            lastPostTimeMap.remove(sbnId)
            lastPostTimeMap.remove(cardId)
            lastProgressMap[sbnId] = 0
            lastProgressMap[cardId] = 0
        } else {
            lastPostTimeMap[sbnId] = now
            lastPostTimeMap[cardId] = now
            lastProgressMap[sbnId] = info.progress
            lastProgressMap[cardId] = info.progress
        }

        val n = sbn.notification
        // Quản lý Icon chuẩn Resource theo GenericCard:
        IconCache.activeSmallIcons.remove(cardId)
        if (sbnId != cardId) IconCache.activeSmallIcons.remove(sbnId)

        val appIcon: Icon = resolveShareIcon(context, sbn.packageName)
        IconCache.activeLargeIcons[cardId] = appIcon
        if (sbnId != cardId) IconCache.activeLargeIcons[sbnId] = appIcon

        val clickPi = info.contentIntent
            ?: n.contentIntent
            ?: fallbackClickIntent(context, sbn.packageName)

        val intent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", ID)
            putExtra("oi_scene", "NAVIGATION")
            putExtra("oi_show_notify", false)
            putExtra("source_pkg", sbn.packageName)
            putExtra("icon_res", R.mipmap.ic_launcher_round)
            putExtra("oi_left_icon", appIcon)
            putExtra("oi_large_icon", appIcon)
            putExtra("source_content_intent", clickPi)
            putExtra("click_resp", clickPi)
            putExtra("oi_force_show", true)
            putExtra("is_high_priority", true)
            putExtra("oi_dismiss_when_kill", true)

            // Cấu hình SuperX chuẩn Vivo OriginOS 6 theo quy ước Animation
            when {
                isFailureOrCanceled -> {
                    // Trạng thái hủy / thất bại
                    val failOrCancelText = if (info.isFailed) "Thất bại" else "Đã hủy"
                    putExtra("oi_template", OriginIslandConstants.TEMPLATE_BASE) // 4
                    putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_TEXT_ICON) // 4
                    putExtra("oi_show_right_icon", false)
                    putExtra("oi_left_content", "")
                    putExtra("oi_right_content", failOrCancelText)
                    putExtra("status_chip_text", failOrCancelText)
                    putExtra("title", if (info.isFailed) "Quick Share · Thất bại" else "Quick Share · Đã hủy")
                    putExtra("text", info.text.ifBlank { if (info.isFailed) "Lỗi truyền tệp" else "Đã hủy truyền tệp" })
                    putExtra("oi_light_color", "#FF5247")
                    putExtra("oi_bg_color", "#FF5247")
                    putExtra("oi_fg_color", "#FF5247")
                    putExtra("is_ongoing", false)
                }
                isFinishedEffective -> {
                    // Trạng thái hoàn tất truyền tệp
                    val tickIcon = Icon.createWithResource(context, R.drawable.ic_success_tick_green)
                    putExtra("oi_template", OriginIslandConstants.TEMPLATE_BASE) // 4
                    putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_TEXT_ICON) // 4
                    putExtra("oi_left_icon", tickIcon)
                    putExtra("oi_right_icon", tickIcon)
                    putExtra("oi_show_right_icon", true)
                    putExtra("oi_left_content", "")
                    putExtra("oi_right_content", "")
                    putExtra("status_chip_text", "✓")
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
                    putExtra("oi_bg_color", "#FF262626")
                    putExtra("oi_fg_color", "#FFFFFFFF")
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
                    activeTransfer = true
                    lastActivePackage = sbn.packageName
                    putExtra("oi_template", OriginIslandConstants.TEMPLATE_BASE) // 4
                    putExtra("oi_fg_color", "#2B66FF")
                    putExtra("oi_bg_color", "#4C2B66FF")
                    putExtra("is_ongoing", isOngoingNotif)

                    if (info.progress <= 0 || info.isIndeterminate) {
                        val transferLabel = if (info.text.contains("kết nối", ignoreCase = true) || info.title.contains("kết nối", ignoreCase = true)) "Đang kết nối" else "Đang nhận tệp"
                        putExtra("oi_left_content", "")
                        putExtra("oi_right_content", transferLabel)
                        putExtra("status_chip_text", transferLabel)
                        putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_TEXT_ICON) // 4
                        putExtra("oi_show_right_icon", false)
                        putExtra("title", "Quick Share · $transferLabel")
                        putExtra("text", "${info.title.take(22)} · ${info.text.ifBlank { "Đang nhận tệp" }}")
                        putExtra("progress_indeterminate", true)
                    } else {
                        val pct = info.progress.coerceIn(1, 99)
                        putExtra("oi_card_ring", pct)
                        putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_PROGRESS) // 2
                        putExtra("oi_progress", pct)
                        putExtra("show_progress", true)
                        putExtra("progress", pct)
                        putExtra("progress_max", 100)
                        putExtra("oi_left_content", "$pct%")
                        putExtra("oi_right_content", "")
                        putExtra("status_chip_text", "$pct%")
                        putExtra("title", "Quick Share · $pct%")
                        putExtra("text", "${info.title.take(22)} · ${info.text.ifBlank { "Đang nhận tệp" }}")
                    }

                    if (!info.speedText.isNullOrBlank()) {
                        putExtra("subtext", "Tốc độ: ${info.speedText}")
                    }
                }
            }
        }

        context.startService(intent)

        // Tự động dọn dẹp đảo sau khi xong (3.5s) hoặc hủy/lỗi (2.5s)
        if (isFinishedEffective) {
            scheduleDelayedCancel(context, cardId, sbnId, 3500L)
        } else if (isFailureOrCanceled) {
            scheduleDelayedCancel(context, cardId, sbnId, 2500L)
        } else if (!info.isRequest) {
            // Hủy watchdog cũ nếu có
            watchdogTasks.remove(sbnId)?.let(handler::removeCallbacks)
            watchdogTasks.remove(cardId)?.let(handler::removeCallbacks)

            // Watchdog auto-dismiss cho nhánh đang truyền tệp thông thường (15s không cập nhật -> auto cancel trong im lặng)
            val watchdog = Runnable {
                watchdogTasks.remove(cardId)
                watchdogTasks.remove(sbnId)
                clear(cardId)
                clear(sbnId)
                context.startService(
                    Intent(context, PlaygroundService::class.java)
                        .setAction(PlaygroundService.ACTION_CANCEL)
                        .putExtra("id", ID)
                        .putExtra("oi_scene", "NAVIGATION")
                )
            }
            watchdogTasks[cardId] = watchdog
            watchdogTasks[sbnId] = watchdog
            handler.postDelayed(watchdog, 15000L)
        }
    }

    /**
     * Phát hoạt ảnh hoàn tất khi notification bị gỡ bỏ lúc đã truyền xong (100% hoặc isFinished).
     */
    fun postFinished(context: Context, id: Int = ID, appPkg: String, title: String = "Quick Share") {
        val lastProg = getLastProgress(id)
        if (!isFinished(id) && lastProg != 100) {
            return
        }
        if (finishedIds.contains(ID) || finishedIds.contains(id)) {
            return
        }
        activeTransfer = false
        watchdogTasks.remove(id)?.let(handler::removeCallbacks)
        watchdogTasks.remove(ID)?.let(handler::removeCallbacks)
        cancelPendingDismiss(id)
        cancelPendingDismiss(ID)
        finishedIds.add(ID)
        finishedIds.add(id)
        lastPostTimeMap.remove(id)
        lastPostTimeMap.remove(ID)
        lastProgressMap[id] = 100
        lastProgressMap[ID] = 100

        if (id != ID) {
            context.startService(
                Intent(context, PlaygroundService::class.java)
                    .setAction(PlaygroundService.ACTION_CANCEL)
                    .putExtra("id", id)
                    .putExtra("oi_scene", "NAVIGATION")
            )
        }
        IconCache.activeSmallIcons.remove(ID)
        if (id != ID) IconCache.activeSmallIcons.remove(id)
        val appIcon: Icon = resolveShareIcon(context, appPkg)
        IconCache.activeLargeIcons[ID] = appIcon
        if (id != ID) IconCache.activeLargeIcons[id] = appIcon
        val clickPi = fallbackClickIntent(context, appPkg)
        val intent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", ID)
            putExtra("oi_scene", "NAVIGATION")
            putExtra("oi_show_notify", false)
            putExtra("source_pkg", appPkg)
            putExtra("icon_res", R.mipmap.ic_launcher_round)
            putExtra("oi_left_icon", appIcon)
            putExtra("oi_large_icon", appIcon)
            putExtra("source_content_intent", clickPi)
            putExtra("click_resp", clickPi)
            putExtra("oi_force_show", true)
            putExtra("is_high_priority", true)
            putExtra("oi_dismiss_when_kill", true)
            putExtra("is_ongoing", false)
            val tickIcon = Icon.createWithResource(context, R.drawable.ic_success_tick_green)
            putExtra("oi_template", OriginIslandConstants.TEMPLATE_BASE) // 4
            putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_TEXT_ICON) // 4
            putExtra("oi_left_icon", tickIcon)
            putExtra("oi_right_icon", tickIcon)
            putExtra("oi_show_right_icon", true)
            putExtra("oi_left_content", "")
            putExtra("oi_right_content", "")
            putExtra("status_chip_text", "✓")
            putExtra("oi_light_color", "#00D084")
            putExtra("oi_bg_color", "#00D084")
            putExtra("oi_fg_color", "#00D084")
            putExtra("title", "Quick Share · Hoàn tất")
            putExtra("text", "Đã nhận tệp")
        }
        context.startService(intent)
        scheduleDelayedCancel(context, ID, id, 3500L)
    }

    /**
     * Phát hoạt ảnh hủy / lỗi khi notification bị gỡ bỏ lúc bị hủy hoặc thất bại.
     */
    fun postCanceled(context: Context, id: Int = ID, appPkg: String, isFailed: Boolean = false) {
        activeTransfer = false
        watchdogTasks.remove(id)?.let(handler::removeCallbacks)
        watchdogTasks.remove(ID)?.let(handler::removeCallbacks)
        cancelPendingDismiss(id)
        cancelPendingDismiss(ID)
        finishedIds.remove(id)
        finishedIds.remove(ID)
        lastPostTimeMap.remove(id)
        lastPostTimeMap.remove(ID)
        lastProgressMap[id] = 0
        lastProgressMap[ID] = 0

        if (id != ID) {
            context.startService(
                Intent(context, PlaygroundService::class.java)
                    .setAction(PlaygroundService.ACTION_CANCEL)
                    .putExtra("id", id)
                    .putExtra("oi_scene", "NAVIGATION")
            )
        }
        IconCache.activeSmallIcons.remove(ID)
        if (id != ID) IconCache.activeSmallIcons.remove(id)
        val appIcon: Icon = resolveShareIcon(context, appPkg)
        IconCache.activeLargeIcons[ID] = appIcon
        if (id != ID) IconCache.activeLargeIcons[id] = appIcon
        val clickPi = fallbackClickIntent(context, appPkg)
        val intent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", ID)
            putExtra("oi_scene", "NAVIGATION")
            putExtra("oi_show_notify", false)
            putExtra("source_pkg", appPkg)
            putExtra("icon_res", R.mipmap.ic_launcher_round)
            putExtra("oi_left_icon", appIcon)
            putExtra("oi_large_icon", appIcon)
            putExtra("source_content_intent", clickPi)
            putExtra("click_resp", clickPi)
            putExtra("oi_force_show", true)
            putExtra("is_high_priority", true)
            putExtra("oi_dismiss_when_kill", true)
            putExtra("is_ongoing", false)
            putExtra("oi_template", OriginIslandConstants.TEMPLATE_BASE) // 4
            putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_TEXT_ICON) // 4
            putExtra("oi_show_right_icon", false)
            putExtra("oi_left_content", "")
            val statusText = if (isFailed) "Thất bại" else "Đã hủy"
            putExtra("oi_right_content", statusText)
            putExtra("status_chip_text", statusText)
            putExtra("title", if (isFailed) "Quick Share · Thất bại" else "Quick Share · Đã hủy")
            putExtra("text", if (isFailed) "Lỗi truyền tệp" else "Đã hủy truyền tệp")
            putExtra("oi_light_color", "#FF5247")
            putExtra("oi_bg_color", "#FF5247")
            putExtra("oi_fg_color", "#FF5247")
        }
        context.startService(intent)
        scheduleDelayedCancel(context, ID, id, 2500L)
    }

    fun getLastProgress(id: Int = ID): Int = lastProgressMap[id] ?: lastProgressMap[ID] ?: -1

    fun isFinished(id: Int = ID): Boolean = finishedIds.contains(id) || finishedIds.contains(ID)

    fun clear(id: Int = ID) {
        activeTransfer = false
        watchdogTasks.remove(id)?.let(handler::removeCallbacks)
        cancelPendingDismiss(id)
        lastPostTimeMap.remove(id)
        lastProgressMap.remove(id)
        finishedIds.remove(id)
        if (id != ID) {
            watchdogTasks.remove(ID)?.let(handler::removeCallbacks)
            cancelPendingDismiss(ID)
            lastPostTimeMap.remove(ID)
            lastProgressMap.remove(ID)
            finishedIds.remove(ID)
        }
    }
}
