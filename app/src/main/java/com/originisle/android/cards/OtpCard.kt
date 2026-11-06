package com.originisle.android.cards

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.service.notification.StatusBarNotification
import com.originisle.android.R
import com.originisle.android.island.OriginIslandConstants
import com.originisle.android.island.PlaygroundService
import com.originisle.android.service.CopyCodeReceiver
import com.originisle.android.service.IconCache
import java.util.Locale

/**
 * Thẻ thông báo mã OTP / mã xác thực đăng nhập ưu tiên cao trên Đảo Origin.
 * - Mã hiển thị lớn trên thẻ và trong viên thuốc (capsule).
 * - Nút bấm "Sao chép mã" với [CopyCodeReceiver] tự động sao chép mã vào clipboard và thu gọn thẻ.
 * - Màu chữ trắng tương phản hoàn hảo trên nền Teal đặc trưng OriginOS 6 (#00BFA5), sửa lỗi trùng màu nền đen.
 */
object OtpCard {

    /** Thời gian tối đa lưu giữ thẻ OTP (2 phút) để tránh mã quá hạn nằm lâu trên Đảo. */
    private const val HOLD_MS = 120_000L

    private const val COLOR_TEAL_ORIGIN_OS = 0xFF00BFA5.toInt()

    private val handler = Handler(Looper.getMainLooper())

    fun post(context: Context, sbn: StatusBarNotification, code: String, appLabel: String) {
        val app = context.applicationContext
        val id = IconCache.castIdFor(sbn)
        IconCache.activeSmallIcons.remove(id)
        val appIcon = IconCache.safeAppIcon(app, sbn.packageName)

        val isVi = Locale.getDefault().language == "vi"
        val copyLabel = if (isVi) "Sao chép mã" else "Copy code"

        val copy = PendingIntent.getBroadcast(
            app, id,
            Intent(app, CopyCodeReceiver::class.java)
                .putExtra(CopyCodeReceiver.EXTRA_CODE, code)
                .putExtra("cast_id", id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val copyAction = Notification.Action.Builder(
            IconCache.safeResourceIcon(app, R.mipmap.ic_launcher_round), copyLabel, copy,
        ).build()

        val fgColorHex = String.format("#%08X", Color.WHITE) // #FFFFFFFF
        val bgColorHex = String.format("#%08X", COLOR_TEAL_ORIGIN_OS) // #FF00BFA5

        val intent = Intent(app, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", id)
            putExtra("is_ongoing", true)
            putExtra("oi_scene", "NAVIGATION")
            putExtra("oi_template", OriginIslandConstants.TEMPLATE_BUTTONS) // 8
            putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_CAPSULE_TEXT) // 6
            putExtra("title", spaced(code))
            putExtra("text", "Mã xác thực · $appLabel")
            putExtra("source_app", appLabel)
            putExtra("source_pkg", sbn.packageName)
            putExtra("oi_left_content", "OTP")
            putExtra("oi_right_content", code)
            putExtra("status_chip_text", code)
            putExtra("icon_res", R.mipmap.ic_launcher_round)
            appIcon?.let {
                putExtra("oi_left_icon", it)
                putExtra("oi_large_icon", it)
            }
            putParcelableArrayListExtra("actions", arrayListOf(copyAction))
            putStringArrayListExtra("oi_button_titles", arrayListOf(copyLabel))
            putExtra("action_title", copyLabel)
            putExtra("source_content_intent", copy)
            putExtra("click_resp", copy)
            putExtra("oi_force_show", true)
            putExtra("oi_island_show_time", 15)
            putExtra("oi_keep_duration", 30)
            putExtra("oi_dismiss_when_kill", true)

            // Khắc phục dứt điểm lỗi chữ đen trùng màu nền đen
            putExtra("oi_fg_color", fgColorHex)
            putExtra("oi_bg_color", bgColorHex)
            putExtra("oi_light_color", bgColorHex)
        }
        app.startService(intent)

        handler.postDelayed({ cancel(app, id) }, HOLD_MS)
    }

    fun cancel(context: Context, id: Int) {
        context.startService(
            Intent(context, PlaygroundService::class.java)
                .setAction(PlaygroundService.ACTION_CANCEL)
                .putExtra("id", id)
                .putExtra("oi_scene", "NAVIGATION"),
        )
    }

    /** "482913" -> "482 913" giúp người dùng đọc nhanh từ thẻ thông báo. */
    private fun spaced(code: String): String =
        if (code.length >= 6) code.chunked((code.length + 1) / 2).joinToString(" ") else code
}
