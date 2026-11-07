package com.originisle.android.cards

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Icon
import android.os.SystemClock
import android.service.notification.StatusBarNotification
import androidx.core.content.ContextCompat
import com.originisle.android.R
import com.originisle.android.call.ZaloCallDetector
import com.originisle.android.call.ZaloCallInfo
import com.originisle.android.island.OriginIslandConstants
import com.originisle.android.island.PlaygroundService
import com.originisle.android.log.CastLog
import com.originisle.android.service.IconCache

/**
 * Thẻ Đảo Động (OriginIsland / SuperX) chuyên biệt cho cuộc gọi Zalo (`com.zing.zalo`).
 * Hỗ trợ hiển thị ảnh đại diện người gọi kèm huy hiệu Zalo, trạng thái cuộc gọi thoại/video đến
 * và cuộc gọi đang diễn ra cùng các nút tương tác trực tiếp (Từ chối / Trả lời / Kết thúc / Mở cuộc gọi).
 */
object ZaloCallCard {

    const val ID = 35011

    @Volatile private var lastCaller: String = ""
    @Volatile private var lastState: ZaloCallDetector.CallState? = null
    @Volatile private var lastPostMs: Long = 0L
    @Volatile private var active: Boolean = false

    fun isActive(): Boolean = active

    fun post(context: Context, sbn: StatusBarNotification, info: ZaloCallInfo) =
        post(context, info, sbn)

    fun post(context: Context, info: ZaloCallInfo, sbn: StatusBarNotification) {
        val now = SystemClock.elapsedRealtime()
        val isSameStateAndCaller = (info.callerName == lastCaller && info.state == lastState)
        if (active && isSameStateAndCaller && (now - lastPostMs) < 900L && PlaygroundService.activeCardIds.contains(ID)) {
            return
        }

        val forceShow = !active || !isSameStateAndCaller
        lastCaller = info.callerName
        lastState = info.state
        lastPostMs = now
        active = true

        // Nếu trước đó thông báo Zalo lọt vào thẻ thường (legacyId), thu hồi thẻ thường để tránh trùng lặp
        val legacyId = IconCache.castIdFor(sbn)
        if (legacyId != ID && PlaygroundService.activeCardIds.contains(legacyId)) {
            IconCache.clear(legacyId)
            context.startService(
                Intent(context, PlaygroundService::class.java).apply {
                    action = PlaygroundService.ACTION_CANCEL
                    putExtra("id", legacyId)
                    putExtra("oi_scene", "NAVIGATION")
                },
            )
        }

        val pillBitmap = createRoundPillAvatar(context, info.avatar)
        val largeBitmap = roundAvatarWithZaloBadge(context, info.avatar)
        val pillIcon = Icon.createWithBitmap(pillBitmap)
        val largeIcon = Icon.createWithBitmap(largeBitmap)

        IconCache.activeSmallIcons[ID] = pillIcon
        IconCache.activeLargeIcons[ID] = largeIcon
        IconCache.activeLargeBitmaps[ID] = largeBitmap

        val clickPi = info.contentIntent
            ?: info.answerIntent
            ?: sbn.notification.contentIntent
            ?: fallbackClickIntent(context, ZaloCallDetector.ZALO_PKG)

        val resolvedText = info.statusText.ifBlank {
            if (info.state == ZaloCallDetector.CallState.INCOMING) {
                if (info.isVideoCall) "Zalo · Cuộc gọi video đến" else "Zalo · Cuộc gọi thoại đến"
            } else {
                "Zalo · Đang trong cuộc gọi"
            }
        }

        val intent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", ID)
            putExtra("oi_scene", "NAVIGATION")
            putExtra("source_pkg", ZaloCallDetector.ZALO_PKG)
            putExtra("source_app", "Zalo")
            putExtra("title", info.callerName)
            putExtra("text", resolvedText)
            putExtra("subtext", "Zalo")
            putExtra("icon_res", R.mipmap.ic_launcher_round)
            putExtra("oi_left_icon", pillIcon)
            putExtra("oi_large_icon", largeIcon)
            putExtra("oi_show_left_icon", true)
            putExtra("is_ongoing", true)
            putExtra("oi_force_show", if (forceShow) true else true)
            putExtra("oi_show_notify", false)
            putExtra("oi_dismiss_when_kill", true)

            if (info.state == ZaloCallDetector.CallState.INCOMING) {
                putExtra("oi_template", OriginIslandConstants.TEMPLATE_BUTTONS) // 8
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT) // 6
                val chip = if (info.isVideoCall) "Video đến" else "Gọi đến"
                putExtra("oi_left_content", info.callerName.take(14))
                putExtra("oi_right_content", chip)
                putExtra("status_chip_text", chip)
                putExtra("oi_fg_color", "#FF29E683")
                putExtra("oi_bg_color", "#3329E683")
                putExtra("oi_light_color", "#FF29E683")

                val declinePi = info.declineIntent ?: clickPi
                val answerPi = info.answerIntent ?: info.contentIntent ?: clickPi
                val actions = arrayListOf(
                    Notification.Action.Builder(null, "Từ chối", declinePi).build(),
                    Notification.Action.Builder(null, "Trả lời", answerPi).build(),
                )
                putParcelableArrayListExtra("actions", actions)
                putStringArrayListExtra("oi_button_titles", arrayListOf("Từ chối", "Trả lời"))
            } else {
                putExtra("oi_template", OriginIslandConstants.TEMPLATE_BUTTONS) // 8
                putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT) // 6
                putExtra("oi_left_content", info.callerName.take(14))
                putExtra("oi_right_content", "Đang gọi")
                putExtra("status_chip_text", "Đang gọi")
                putExtra("oi_fg_color", "#FF0068FF")
                putExtra("oi_bg_color", "#330068FF")
                putExtra("oi_light_color", "#FF0068FF")

                val endPi = info.declineIntent ?: clickPi
                val openPi = info.contentIntent ?: info.answerIntent ?: clickPi
                val actions = arrayListOf(
                    Notification.Action.Builder(null, "Kết thúc", endPi).build(),
                    Notification.Action.Builder(null, "Mở cuộc gọi", openPi).build(),
                )
                putParcelableArrayListExtra("actions", actions)
                putStringArrayListExtra("oi_button_titles", arrayListOf("Kết thúc", "Mở cuộc gọi"))
            }

            putExtra("source_content_intent", clickPi)
            putExtra("click_resp", clickPi)
        }

        context.startService(intent)
        CastLog.add("Zalo Call", info.callerName, "hiển thị — ${info.callerName} ($resolvedText)", true)
    }

    fun clear(context: Context) {
        active = false
        lastCaller = ""
        lastState = null
        lastPostMs = 0L
        IconCache.clear(ID)
        val cancelIntent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_CANCEL
            putExtra("id", ID)
            putExtra("oi_scene", "NAVIGATION")
        }
        context.startService(cancelIntent)
    }

    private fun ensureSoftwareBitmap(bitmap: Bitmap): Bitmap {
        return if (bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: bitmap
        } else {
            bitmap
        }
    }

    private fun loadZaloAppBitmap(context: Context, size: Int = 96): Bitmap? = runCatching {
        val drawable = context.packageManager.getApplicationIcon(ZaloCallDetector.ZALO_PKG)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        bmp
    }.getOrNull()

    private fun createDefaultCallAvatarBitmap(context: Context, size: Int = 160): Bitmap {
        loadZaloAppBitmap(context, size)?.let { zaloBmp ->
            return cropCircleBitmap(zaloBmp, size)
        }
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF0068FF")
            style = Paint.Style.FILL
        }
        val radius = size / 2f
        canvas.drawCircle(radius, radius, radius, bgPaint)

        runCatching {
            val callDrawable = ContextCompat.getDrawable(context, R.drawable.ic_call)?.mutate()
            if (callDrawable != null) {
                val pad = (size * 0.25f).toInt()
                callDrawable.setBounds(pad, pad, size - pad, size - pad)
                callDrawable.setTint(Color.WHITE)
                callDrawable.setTintMode(PorterDuff.Mode.SRC_IN)
                callDrawable.draw(canvas)
            }
        }
        return bmp
    }

    private fun cropCircleBitmap(src: Bitmap, targetSize: Int = 160): Bitmap {
        return runCatching {
            val soft = ensureSoftwareBitmap(src)
            val output = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            val r = targetSize / 2f
            canvas.drawCircle(r, r, r, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(
                soft,
                Rect(0, 0, soft.width, soft.height),
                RectF(0f, 0f, targetSize.toFloat(), targetSize.toFloat()),
                paint,
            )
            output
        }.getOrDefault(src)
    }

    private fun createRoundPillAvatar(context: Context, avatar: Bitmap?): Bitmap {
        val size = 128
        if (avatar != null && avatar.width > 0 && avatar.height > 0) {
            return cropCircleBitmap(avatar, size)
        }
        return createDefaultCallAvatarBitmap(context, size)
    }

    /**
     * Vẽ ảnh đại diện người gọi bo tròn 160x160 kèm huy hiệu ứng dụng Zalo nhỏ ở góc dưới bên phải.
     * Nếu không có ảnh đại diện, sử dụng icon Zalo hoặc vòng tròn xanh Zalo (#FF0068FF) kèm biểu tượng cuộc gọi.
     */
    private fun roundAvatarWithZaloBadge(context: Context, avatar: Bitmap?): Bitmap {
        val totalSize = 160
        if (avatar == null || avatar.width <= 0 || avatar.height <= 0) {
            return createDefaultCallAvatarBitmap(context, totalSize)
        }

        return runCatching {
            val output = Bitmap.createBitmap(totalSize, totalSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)

            val avatarSize = 152
            val circleAvatar = cropCircleBitmap(avatar, avatarSize)
            canvas.drawBitmap(circleAvatar, 0f, 0f, null)

            loadZaloAppBitmap(context, 56)?.let { zaloBmp ->
                val cx = 126f
                val cy = 126f
                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#FF181818")
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(cx, cy, 28f, borderPaint)

                val badgeSize = 48
                val circularBadge = cropCircleBitmap(zaloBmp, badgeSize)
                canvas.drawBitmap(circularBadge, cx - badgeSize / 2f, cy - badgeSize / 2f, null)
            }

            output
        }.getOrElse {
            createDefaultCallAvatarBitmap(context, totalSize)
        }
    }
}
