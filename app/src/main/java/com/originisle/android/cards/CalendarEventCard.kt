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
import com.originisle.android.calendar.CalendarEventDetector
import com.originisle.android.island.OriginIslandConstants
import com.originisle.android.island.PlaygroundService
import com.originisle.android.log.CastLog
import com.originisle.android.service.IconCache

/**
 * Thẻ hiển thị Nhắc Lịch & Sự kiện sắp diễn ra (Calendar & Upcoming Event) trên Đảo Origin.
 * Tương thích 100% Vivo SuperX SDK với biểu tượng tờ lịch vẽ trực tiếp bằng Canvas.
 */
object CalendarEventCard {

    const val ID = 35015
    private const val HOLD_MS = 5_000L
    const val COLOR_CALENDAR = "#FF0A84FF"

    private val mainHandler = Handler(Looper.getMainLooper())
    private var autoCancelRunnable: Runnable? = null

    /**
     * Vẽ biểu tượng tờ lịch bo góc sắc nét trên nền tròn tối #FF18181A, viền xanh #4D0A84FF,
     * gáy lịch đỏ cam #FFFF453A phía trên và thân lịch trắng kèm vạch/chấm ngày giờ ở giữa.
     * Sử dụng 100% Canvas thuần túy để Vivo SuperX SDK luôn hiển thị ổn định.
     */
    fun createCalendarBadgeIcon(context: Context, size: Int = 128): Icon {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val center = size / 2f
        val radius = size / 2f - 2f

        // 1. Nền tròn tối (#FF18181A)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF18181A")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(center, center, radius, bgPaint)

        // 2. Viền xanh lịch (#4D0A84FF)
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4D0A84FF")
            style = Paint.Style.STROKE
            strokeWidth = size * 0.04f
        }
        canvas.drawCircle(center, center, radius - strokePaint.strokeWidth / 2f, strokePaint)

        // 3. Khung tờ lịch bo góc ở trung tâm
        val calLeft = size * 0.26f
        val calTop = size * 0.27f
        val calRight = size * 0.74f
        val calBottom = size * 0.75f
        val cornerRadius = size * 0.09f
        val calRect = RectF(calLeft, calTop, calRight, calBottom)

        // Thân tờ lịch màu trắng (#FFFFFFFF)
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(calRect, cornerRadius, cornerRadius, bodyPaint)

        // 4. Phần gáy lịch phía trên màu đỏ cam (#FFFF453A), cắt gọn theo khung bo góc
        val headerBottom = calTop + (calBottom - calTop) * 0.32f
        val clipPath = Path().apply {
            addRoundRect(calRect, cornerRadius, cornerRadius, Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(clipPath)
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFFF453A")
            style = Paint.Style.FILL
        }
        canvas.drawRect(calLeft, calTop, calRight, headerBottom, headerPaint)
        canvas.restore()

        // 5. Hai khoen gáy lịch nhỏ phía trên
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = size * 0.035f
            strokeCap = Paint.Cap.ROUND
        }
        val ringTop = calTop - size * 0.03f
        val ringBottom = calTop + size * 0.04f
        val leftRingX = calLeft + (calRight - calLeft) * 0.28f
        val rightRingX = calLeft + (calRight - calLeft) * 0.72f
        canvas.drawLine(leftRingX, ringTop, leftRingX, ringBottom, ringPaint)
        canvas.drawLine(rightRingX, ringTop, rightRingX, ringBottom, ringPaint)

        // 6. Các vạch và chấm ngày giờ bên trong thân lịch
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF0A84FF")
            style = Paint.Style.STROKE
            strokeWidth = size * 0.038f
            strokeCap = Paint.Cap.ROUND
        }
        val row1Y = headerBottom + (calBottom - headerBottom) * 0.36f
        val row2Y = headerBottom + (calBottom - headerBottom) * 0.68f
        val innerLeft = calLeft + (calRight - calLeft) * 0.22f
        val innerRight1 = calLeft + (calRight - calLeft) * 0.78f
        val innerRight2 = calLeft + (calRight - calLeft) * 0.56f
        canvas.drawLine(innerLeft, row1Y, innerRight1, row1Y, linePaint)
        canvas.drawLine(innerLeft, row2Y, innerRight2, row2Y, linePaint)

        // Chấm điểm nhấn màu đỏ cam ở góc phải hàng dưới
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFFF453A")
            style = Paint.Style.FILL
        }
        val dotX = calLeft + (calRight - calLeft) * 0.74f
        canvas.drawCircle(dotX, row2Y, size * 0.028f, dotPaint)

        return Icon.createWithBitmap(bmp)
    }

    fun post(
        context: Context,
        info: CalendarEventDetector.CalendarEventInfo,
        sbn: StatusBarNotification? = null,
    ) {
        val app = context.applicationContext
        val clickPi = info.contentIntent ?: fallbackClickIntent(app, info.sourcePkg)

        val badgeIcon = createCalendarBadgeIcon(app)
        IconCache.activeSmallIcons[ID] = badgeIcon
        IconCache.activeLargeIcons[ID] = badgeIcon

        val intent = Intent(app, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", ID)
            putExtra("oi_scene", "NAVIGATION")
            putExtra("template", OriginIslandConstants.TEMPLATE_BASE) // 4
            putExtra("oi_template", OriginIslandConstants.TEMPLATE_BASE) // 4
            putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT) // 6
            putExtra("title", "Lịch hẹn · ${info.timeText}")
            putExtra("text", info.title)
            putExtra("subtext", info.detailText.ifBlank { "Chạm để mở chi tiết sự kiện" })
            putExtra("oi_left_content", info.title.take(12))
            putExtra("oi_right_content", info.chipText)
            putExtra("status_chip_text", info.chipText)
            putExtra("oi_show_left_icon", true)
            putExtra("oi_left_icon", badgeIcon)
            putExtra("oi_large_icon", badgeIcon)
            putExtra("icon_res", R.mipmap.ic_launcher_round)
            putExtra("is_ongoing", false)
            putExtra("oi_auto_dismiss_ms", HOLD_MS)
            putExtra("oi_force_show", true)
            putExtra("oi_show_notify", false)
            putExtra("is_high_priority", true)
            putExtra("oi_dismiss_when_kill", true)

            putExtra("oi_light_color", COLOR_CALENDAR)
            putExtra("oi_bg_color", "#330A84FF")
            putExtra("oi_fg_color", COLOR_CALENDAR)

            putExtra("click_resp", clickPi)
            putExtra("source_content_intent", clickPi)
            putExtra("oi_capsule_click_resp", clickPi)
            putExtra("source_pkg", info.sourcePkg)
        }
        app.startService(intent)

        CastLog.add(
            "Nhắc lịch",
            info.title,
            "hiển thị — sự kiện: ${info.title} (${info.chipText})",
            true,
            context = app,
        )

        autoCancelRunnable?.let { mainHandler.removeCallbacks(it) }
        val cancelTask = Runnable { clear(app) }
        autoCancelRunnable = cancelTask
        mainHandler.postDelayed(cancelTask, HOLD_MS)
    }

    fun postSample(context: Context) {
        val app = context.applicationContext
        val samplePkg = "com.google.android.calendar"
        val clickPi = fallbackClickIntent(app, samplePkg)
        val sampleInfo = CalendarEventDetector.CalendarEventInfo(
            title = "Họp triển khai dự án Quý 4",
            timeText = "14:30 – 15:30",
            detailText = "Phòng họp A2 · Còn 15 phút nữa bắt đầu",
            chipText = "14:30",
            sourcePkg = samplePkg,
            contentIntent = clickPi,
        )
        post(app, sampleInfo, null)
    }

    @JvmOverloads
    fun clear(context: Context, id: Int = ID) {
        autoCancelRunnable?.let {
            mainHandler.removeCallbacks(it)
            autoCancelRunnable = null
        }
        IconCache.clear(id)
        context.startService(
            Intent(context, PlaygroundService::class.java)
                .setAction(PlaygroundService.ACTION_CANCEL)
                .putExtra("id", id)
                .putExtra("oi_scene", "NAVIGATION"),
        )
    }

    fun cancel(context: Context, id: Int = ID) = clear(context, id)
}
