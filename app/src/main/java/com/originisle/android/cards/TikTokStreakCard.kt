package com.originisle.android.cards

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Handler
import android.os.Looper
import android.service.notification.StatusBarNotification
import com.originisle.android.R
import com.originisle.android.island.OriginIslandConstants
import com.originisle.android.island.PlaygroundService
import com.originisle.android.tiktok.TikTokStreakActionReceiver
import com.originisle.android.tiktok.TikTokStreakDetector

/**
 * Thẻ hiển thị chuỗi trò chuyện TikTok (TikTok Streak) trên Đảo Origin.
 * Cho phép người dùng chạm hoặc bấm nút "Gửi lửa" để tự động copy emoji chuỗi và mở chat TikTok.
 */
object TikTokStreakCard {

    const val ID = 35012
    private const val HOLD_MS = 20_000L
    const val COLOR_STREAK = "#FFFF5E00"

    private val mainHandler = Handler(Looper.getMainLooper())
    private var autoCancelRunnable: Runnable? = null

    fun post(
        context: Context,
        info: TikTokStreakDetector.TikTokStreakInfo,
        sbn: StatusBarNotification,
    ) {
        val app = context.applicationContext

        // PendingIntent trỏ tới BroadcastReceiver để copy tin nhắn và mở app TikTok
        val actionIntent = Intent(app, TikTokStreakActionReceiver::class.java).apply {
            action = TikTokStreakActionReceiver.ACTION_SEND_TIKTOK_STREAK
            putExtra(TikTokStreakActionReceiver.EXTRA_TARGET_INTENT, info.contentIntent)
            putExtra(TikTokStreakActionReceiver.EXTRA_SOURCE_PKG, info.sourcePkg)
            putExtra("cast_id", ID)
        }
        val streakPi = PendingIntent.getBroadcast(
            app,
            ID,
            actionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val fireIcon = Icon.createWithResource(app, R.drawable.ic_tiktok_streak_fire)

        // Nút hành động "Gửi lửa" / "Duy trì"
        val buttonTitle = if (info.streakDays > 0) "Duy trì" else "Gửi lửa"
        val notifAction = Notification.Action.Builder(fireIcon, buttonTitle, streakPi).build()

        val intent = Intent(app, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", ID)
            putExtra("oi_scene", "NAVIGATION")
            putExtra("oi_template", OriginIslandConstants.TEMPLATE_BUTTONS) // 8
            putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_TEXT_ICON) // 4
            putExtra("title", "TikTok · Chuỗi 🔥")
            putExtra("text", "${info.friendName}: ${info.message}")
            putExtra("subtext", "Chạm hoặc bấm $buttonTitle để gửi lửa TikTok")
            putExtra("oi_left_content", "Chuỗi 🔥")
            putExtra("oi_right_content", "🔥 Chuỗi")
            putExtra("status_chip_text", "🔥 Chuỗi")
            putExtra("oi_show_right_icon", true)
            putExtra("oi_show_notify", false)
            putExtra("oi_force_show", true)
            putExtra("is_high_priority", true)
            putExtra("oi_dismiss_when_kill", true)

            // Icon biểu trưng ngọn lửa rực sáng
            putExtra("icon", fireIcon)
            putExtra("oi_left_icon", fireIcon)
            putExtra("oi_large_icon", fireIcon)
            putExtra("icon_res", R.drawable.ic_tiktok_streak_fire)

            // Bảng màu cam ngọn lửa chuẩn TikTok
            putExtra("oi_light_color", COLOR_STREAK)
            putExtra("oi_bg_color", COLOR_STREAK)
            putExtra("oi_fg_color", COLOR_STREAK)
            putExtra("oi_icon_status_type", OriginIslandConstants.ICON_STATUS_SUCCESS)

            // Cấu hình nút bấm tương tác
            putParcelableArrayListExtra("actions", arrayListOf(notifAction))
            putStringArrayListExtra("oi_button_titles", arrayListOf(buttonTitle))
            putParcelableArrayListExtra("oi_button_actions", arrayListOf(streakPi))
            putParcelableArrayListExtra("oi_button_icons", arrayListOf(fireIcon))

            // Chạm trực tiếp vào thẻ
            putExtra("click_resp", streakPi)
            putExtra("source_pkg", info.sourcePkg)
            putExtra("source_content_intent", info.contentIntent)
        }
        app.startService(intent)

        // Tự động hủy sau 20 giây nếu người dùng không tương tác
        autoCancelRunnable?.let { mainHandler.removeCallbacks(it) }
        val cancelTask = Runnable { clear(app) }
        autoCancelRunnable = cancelTask
        mainHandler.postDelayed(cancelTask, HOLD_MS)
    }

    @JvmOverloads
    fun clear(context: Context, id: Int = ID) {
        autoCancelRunnable?.let {
            mainHandler.removeCallbacks(it)
            autoCancelRunnable = null
        }
        context.startService(
            Intent(context, PlaygroundService::class.java)
                .setAction(PlaygroundService.ACTION_CANCEL)
                .putExtra("id", id)
                .putExtra("oi_scene", "NAVIGATION"),
        )
    }

    fun cancel(context: Context, id: Int = ID) = clear(context, id)
}
