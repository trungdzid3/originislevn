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
import android.graphics.drawable.Icon
import android.os.Handler
import android.os.Looper
import android.service.notification.StatusBarNotification
import androidx.core.content.ContextCompat
import com.originisle.android.R
import com.originisle.android.island.OriginIslandConstants
import com.originisle.android.island.PlaygroundService
import com.originisle.android.log.CastLog
import com.originisle.android.service.IconCache
import com.originisle.android.tiktok.TikTokStreakActionReceiver
import com.originisle.android.tiktok.TikTokStreakDetector

/**
 * Thẻ hiển thị chuỗi trò chuyện TikTok (TikTok Streak) trên Đảo Origin.
 * Cho phép người dùng chạm hoặc bấm nút "Gửi lửa" để tự động copy emoji chuỗi và mở chat TikTok.
 */
object TikTokStreakCard {

    const val ID = 35012
    private const val HOLD_MS = 4_500L
    const val COLOR_STREAK = "#FFFF5E00"

    private val mainHandler = Handler(Looper.getMainLooper())
    private var autoCancelRunnable: Runnable? = null

    fun createGlowingFlameBadge(context: Context, size: Int = 128): Icon {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val center = size / 2f
        val radius = size / 2f - 2f

        // Nền tròn tối (#FF18181A)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF18181A")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(center, center, radius, bgPaint)

        // Viền cam sáng (#4DFF5E00)
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4DFF5E00")
            style = Paint.Style.STROKE
            strokeWidth = size * 0.04f
        }
        canvas.drawCircle(center, center, radius - strokePaint.strokeWidth / 2f, strokePaint)

        // Vector R.drawable.ic_tiktok_streak_fire căn giữa với màu cam rực rỡ (#FFFF5E00)
        val drawable = ContextCompat.getDrawable(context, R.drawable.ic_tiktok_streak_fire)?.mutate()
        if (drawable != null) {
            val pad = (size * 0.22f).toInt()
            drawable.setBounds(pad, pad, size - pad, size - pad)
            drawable.setTint(Color.parseColor("#FFFF5E00"))
            drawable.setTintMode(PorterDuff.Mode.SRC_IN)
            drawable.draw(canvas)
        }
        return Icon.createWithBitmap(bmp)
    }

    fun post(
        context: Context,
        info: TikTokStreakDetector.TikTokStreakInfo,
        sbn: StatusBarNotification? = null,
    ) {
        val app = context.applicationContext

        val targetPi = info.contentIntent ?: fallbackClickIntent(app, info.sourcePkg)

        // PendingIntent trỏ tới BroadcastReceiver để copy tin nhắn và mở app TikTok
        val actionIntent = Intent(app, TikTokStreakActionReceiver::class.java).apply {
            action = TikTokStreakActionReceiver.ACTION_SEND_TIKTOK_STREAK
            putExtra(TikTokStreakActionReceiver.EXTRA_TARGET_INTENT, targetPi)
            putExtra(TikTokStreakActionReceiver.EXTRA_SOURCE_PKG, info.sourcePkg)
            putExtra("cast_id", ID)
        }
        val streakPi = PendingIntent.getBroadcast(
            app,
            ID,
            actionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val flameBadgeIcon = createGlowingFlameBadge(app)
        IconCache.activeSmallIcons[ID] = flameBadgeIcon
        IconCache.activeLargeIcons[ID] = flameBadgeIcon

        val chipText = if (info.streakDays > 0) "🔥 ${info.streakDays} ngày" else "🔥 Giữ chuỗi"
        val buttonTitle = "🔥 Gửi lửa"
        val notifAction = Notification.Action.Builder(null, buttonTitle, streakPi).build()

        val intent = Intent(app, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", ID)
            putExtra("oi_scene", "NAVIGATION")
            putExtra("oi_template", OriginIslandConstants.TEMPLATE_BUTTONS) // 8
            putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT) // 6
            putExtra("title", "TikTok · Chuỗi 🔥")
            putExtra("text", "${info.friendName}: ${info.message}")
            putExtra("subtext", "Chạm hoặc bấm $buttonTitle để gửi lửa TikTok")
            putExtra("oi_left_content", info.friendName.take(10))
            putExtra("oi_right_content", chipText)
            putExtra("status_chip_text", chipText)
            putExtra("oi_show_left_icon", true)
            putExtra("oi_left_icon", flameBadgeIcon)
            putExtra("oi_large_icon", flameBadgeIcon)
            putExtra("icon_res", R.mipmap.ic_launcher_round)
            putExtra("is_ongoing", false)
            putExtra("oi_auto_dismiss_ms", HOLD_MS)
            putExtra("oi_force_show", true)
            putExtra("oi_show_notify", false)
            putExtra("is_high_priority", true)
            putExtra("oi_dismiss_when_kill", true)

            // Bảng màu cam ngọn lửa chuẩn OriginOS
            putExtra("oi_light_color", COLOR_STREAK)
            putExtra("oi_bg_color", "#33FF5E00")
            putExtra("oi_fg_color", COLOR_STREAK)

            // Cấu hình nút bấm tương tác
            putParcelableArrayListExtra("actions", arrayListOf(notifAction))
            putStringArrayListExtra("oi_button_titles", arrayListOf(buttonTitle))

            // Chạm trực tiếp vào thẻ
            putExtra("click_resp", streakPi)
            putExtra("source_content_intent", streakPi)
            putExtra("oi_capsule_click_resp", streakPi)
            putExtra("source_pkg", info.sourcePkg)
        }
        app.startService(intent)

        CastLog.add("TikTok", "Chuỗi 🔥", "hiển thị — chuỗi TikTok: ${info.friendName} ($chipText)", true, context = app)

        // Tự động hủy sau 20 giây nếu người dùng không tương tác
        autoCancelRunnable?.let { mainHandler.removeCallbacks(it) }
        val cancelTask = Runnable { clear(app) }
        autoCancelRunnable = cancelTask
        mainHandler.postDelayed(cancelTask, HOLD_MS)
    }

    fun postSample(context: Context) {
        val app = context.applicationContext
        val clickPi = fallbackClickIntent(app, "com.zhiliaoapp.musically")
        val sampleInfo = TikTokStreakDetector.TikTokStreakInfo(
            friendName = "Lan Anh",
            message = "Duy trì chuỗi với Lan Anh ngay nào!",
            streakDays = 15,
            contentIntent = clickPi,
            sourcePkg = "com.zhiliaoapp.musically",
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
