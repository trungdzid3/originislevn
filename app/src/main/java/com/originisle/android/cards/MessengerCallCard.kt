package com.originisle.android.cards

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Icon
import android.os.SystemClock
import android.service.notification.StatusBarNotification
import androidx.core.content.ContextCompat
import com.originisle.android.R
import com.originisle.android.call.MessengerCallDetector
import com.originisle.android.call.MessengerCallInfo
import com.originisle.android.island.OriginIslandConstants
import com.originisle.android.island.PlaygroundService
import com.originisle.android.log.CastLog
import com.originisle.android.service.IconCache

/**
 * Thẻ Đảo Động (OriginIsland / SuperX) chuyên biệt cho cuộc gọi Facebook Messenger (`com.facebook.orca`).
 * Hiển thị ảnh đại diện người gọi bo tròn kèm huy hiệu Messenger, phân biệt cuộc gọi đến / đang đàm thoại,
 * và cung cấp các nút tương tác trực tiếp (Từ chối / Trả lời hoặc Kết thúc / Mở app).
 */
object MessengerCallCard {

    const val ID = 35013

    @Volatile private var lastCaller: String = ""
    @Volatile private var lastIncoming: Boolean? = null
    @Volatile private var lastPostMs: Long = 0L
    @Volatile private var active: Boolean = false

    fun isActive(): Boolean = active

    fun post(context: Context, sbn: StatusBarNotification, info: MessengerCallInfo) =
        post(context, info, sbn)

    fun post(context: Context, info: MessengerCallInfo, sbn: StatusBarNotification) {
        val now = SystemClock.elapsedRealtime()
        val isSameStateAndCaller = (info.callerName == lastCaller && info.isIncoming == lastIncoming)
        if (active && isSameStateAndCaller && (now - lastPostMs) < 900L && PlaygroundService.activeCardIds.contains(ID)) {
            return
        }

        val forceShow = !active || !isSameStateAndCaller
        lastCaller = info.callerName
        lastIncoming = info.isIncoming
        lastPostMs = now
        active = true

        // Nếu trước đó thông báo Messenger lọt vào thẻ thường (legacyId), thu hồi thẻ thường để tránh trùng lặp
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
        val largeBitmap = roundAvatarWithMessengerBadge(context, info.avatar)
        val pillIcon = Icon.createWithBitmap(pillBitmap)
        val largeIcon = Icon.createWithBitmap(largeBitmap)

        IconCache.activeSmallIcons[ID] = pillIcon
        IconCache.activeLargeIcons[ID] = largeIcon
        IconCache.activeLargeBitmaps[ID] = largeBitmap

        val clickPi = info.contentIntent
            ?: info.answerIntent
            ?: sbn.notification.contentIntent
            ?: fallbackClickIntent(context, MessengerCallDetector.MESSENGER_PKG)

        val resolvedText = if (info.isIncoming) {
            if (info.isVideo) "Messenger · Cuộc gọi video đến" else "Messenger · Cuộc gọi thoại đến"
        } else {
            if (info.isVideo) "Messenger · Đang gọi video" else "Messenger · Đang trong cuộc gọi"
        }

        val intent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", ID)
            putExtra("oi_scene", "NAVIGATION")
            putExtra("source_pkg", MessengerCallDetector.MESSENGER_PKG)
            putExtra("source_app", "Messenger")
            putExtra("title", info.callerName)
            putExtra("text", resolvedText)
            putExtra("subtext", "Messenger")
            putExtra("icon_res", R.mipmap.ic_launcher_round)
            putExtra("oi_left_icon", pillIcon)
            putExtra("oi_large_icon", largeIcon)
            putExtra("oi_show_left_icon", true)
            putExtra("is_ongoing", true)
            putExtra("oi_force_show", forceShow)
            putExtra("oi_show_notify", false)
            putExtra("oi_dismiss_when_kill", true)

            putExtra("oi_template", OriginIslandConstants.TEMPLATE_BUTTONS) // 8
            putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT) // 6
            putExtra("oi_left_content", info.callerName.take(14))

            if (info.isIncoming) {
                val chip = if (info.isVideo) "Video đến" else "Gọi đến"
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
                putExtra("oi_right_content", "Đang gọi")
                putExtra("status_chip_text", "Đang gọi")
                putExtra("oi_fg_color", "#FF0084FF")
                putExtra("oi_bg_color", "#330084FF")
                putExtra("oi_light_color", "#FF0084FF")

                val endPi = info.declineIntent ?: clickPi
                val openPi = info.contentIntent ?: info.answerIntent ?: clickPi
                val actions = arrayListOf(
                    Notification.Action.Builder(null, "Kết thúc", endPi).build(),
                    Notification.Action.Builder(null, "Mở app", openPi).build(),
                )
                putParcelableArrayListExtra("actions", actions)
                putStringArrayListExtra("oi_button_titles", arrayListOf("Kết thúc", "Mở app"))
            }

            putExtra("source_content_intent", clickPi)
            putExtra("click_resp", clickPi)
        }

        context.startService(intent)
        CastLog.add("Messenger Call", info.callerName, "hiển thị — ${info.callerName} ($resolvedText)", true)
    }

    /**
     * Hủy thẻ cuộc gọi Messenger trên Đảo Động.
     */
    fun clear(context: Context) {
        active = false
        lastCaller = ""
        lastIncoming = null
        lastPostMs = 0L
        IconCache.clear(ID)
        val cancelIntent = Intent(context, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_CANCEL
            putExtra("id", ID)
            putExtra("oi_scene", "NAVIGATION")
        }
        context.startService(cancelIntent)
    }

    /**
     * Tạo cuộc gọi mẫu từ "Nguyễn Văn A" để thử nghiệm hiển thị trên Đảo Động.
     */
    fun postSample(context: Context, isIncoming: Boolean = true, isVideo: Boolean = false) {
        val app = context.applicationContext
        val clickPi = fallbackClickIntent(app, MessengerCallDetector.MESSENGER_PKG)
        val sampleInfo = MessengerCallInfo(
            callerName = "Nguyễn Văn A",
            isVideo = isVideo,
            isIncoming = isIncoming,
            answerIntent = clickPi,
            declineIntent = clickPi,
            contentIntent = clickPi,
            avatar = null,
        )

        val pillBitmap = createRoundPillAvatar(app, null)
        val largeBitmap = roundAvatarWithMessengerBadge(app, null)
        val pillIcon = Icon.createWithBitmap(pillBitmap)
        val largeIcon = Icon.createWithBitmap(largeBitmap)

        IconCache.activeSmallIcons[ID] = pillIcon
        IconCache.activeLargeIcons[ID] = largeIcon
        IconCache.activeLargeBitmaps[ID] = largeBitmap

        active = true
        lastCaller = sampleInfo.callerName
        lastIncoming = sampleInfo.isIncoming
        lastPostMs = SystemClock.elapsedRealtime()

        val resolvedText = if (isIncoming) {
            if (isVideo) "Messenger · Cuộc gọi video đến" else "Messenger · Cuộc gọi thoại đến"
        } else {
            if (isVideo) "Messenger · Đang gọi video" else "Messenger · Đang trong cuộc gọi"
        }

        val intent = Intent(app, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", ID)
            putExtra("oi_scene", "NAVIGATION")
            putExtra("source_pkg", MessengerCallDetector.MESSENGER_PKG)
            putExtra("source_app", "Messenger")
            putExtra("title", sampleInfo.callerName)
            putExtra("text", resolvedText)
            putExtra("subtext", "Messenger")
            putExtra("icon_res", R.mipmap.ic_launcher_round)
            putExtra("oi_left_icon", pillIcon)
            putExtra("oi_large_icon", largeIcon)
            putExtra("oi_show_left_icon", true)
            putExtra("is_ongoing", true)
            putExtra("oi_force_show", true)
            putExtra("oi_show_notify", false)
            putExtra("oi_dismiss_when_kill", true)

            putExtra("oi_template", OriginIslandConstants.TEMPLATE_BUTTONS)
            putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT)
            putExtra("oi_left_content", sampleInfo.callerName.take(14))

            if (isIncoming) {
                val chip = if (isVideo) "Video đến" else "Gọi đến"
                putExtra("oi_right_content", chip)
                putExtra("status_chip_text", chip)
                putExtra("oi_fg_color", "#FF29E683")
                putExtra("oi_bg_color", "#3329E683")
                putExtra("oi_light_color", "#FF29E683")

                val actions = arrayListOf(
                    Notification.Action.Builder(null, "Từ chối", clickPi).build(),
                    Notification.Action.Builder(null, "Trả lời", clickPi).build(),
                )
                putParcelableArrayListExtra("actions", actions)
                putStringArrayListExtra("oi_button_titles", arrayListOf("Từ chối", "Trả lời"))
            } else {
                putExtra("oi_right_content", "Đang gọi")
                putExtra("status_chip_text", "Đang gọi")
                putExtra("oi_fg_color", "#FF0084FF")
                putExtra("oi_bg_color", "#330084FF")
                putExtra("oi_light_color", "#FF0084FF")

                val actions = arrayListOf(
                    Notification.Action.Builder(null, "Kết thúc", clickPi).build(),
                    Notification.Action.Builder(null, "Mở app", clickPi).build(),
                )
                putParcelableArrayListExtra("actions", actions)
                putStringArrayListExtra("oi_button_titles", arrayListOf("Kết thúc", "Mở app"))
            }

            putExtra("source_content_intent", clickPi)
            putExtra("click_resp", clickPi)
        }

        app.startService(intent)
        CastLog.add("Messenger Call", sampleInfo.callerName, "mẫu thử nghiệm — ${sampleInfo.callerName} ($resolvedText)", true)
    }

    private fun ensureSoftwareBitmap(bitmap: Bitmap): Bitmap {
        return if (bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: bitmap
        } else {
            bitmap
        }
    }

    private fun loadMessengerAppBitmap(context: Context, size: Int = 96): Bitmap? = runCatching {
        val drawable = context.packageManager.getApplicationIcon(MessengerCallDetector.MESSENGER_PKG)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        bmp
    }.getOrNull()

    private fun createDefaultCallAvatarBitmap(context: Context, size: Int = 160): Bitmap {
        loadMessengerAppBitmap(context, size)?.let { messengerBmp ->
            return cropCircleBitmap(messengerBmp, size)
        }
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF0084FF")
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
     * Vẽ ảnh đại diện người gọi bo tròn 160x160 kèm huy hiệu ứng dụng Messenger nhỏ ở góc dưới bên phải.
     * Nếu không có ảnh đại diện, sử dụng icon Messenger hoặc vòng tròn xanh Messenger (#FF0084FF) kèm biểu tượng cuộc gọi.
     */
    private fun roundAvatarWithMessengerBadge(context: Context, avatar: Bitmap?): Bitmap {
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

            val badgeBmp = loadMessengerAppBitmap(context, 56) ?: createMessengerBadgeFallback(56)
            val cx = 126f
            val cy = 126f
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FF181818")
                style = Paint.Style.FILL
            }
            canvas.drawCircle(cx, cy, 28f, borderPaint)

            val badgeSize = 48
            val circularBadge = cropCircleBitmap(badgeBmp, badgeSize)
            canvas.drawBitmap(circularBadge, cx - badgeSize / 2f, cy - badgeSize / 2f, null)

            output
        }.getOrElse {
            createDefaultCallAvatarBitmap(context, totalSize)
        }
    }

    private fun createMessengerBadgeFallback(size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF0084FF")
            style = Paint.Style.FILL
        }
        val r = size / 2f
        canvas.drawCircle(r, r, r, paint)

        val boltPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val path = Path().apply {
            moveTo(r * 0.95f, r * 0.45f)
            lineTo(r * 0.65f, r * 1.05f)
            lineTo(r * 0.98f, r * 1.05f)
            lineTo(r * 0.88f, r * 1.55f)
            lineTo(r * 1.35f, r * 0.95f)
            lineTo(r * 1.02f, r * 0.95f)
            close()
        }
        canvas.drawPath(path, boltPaint)
        return bmp
    }
}
