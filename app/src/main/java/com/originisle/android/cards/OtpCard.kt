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
import com.originisle.android.service.CopyCodeReceiver
import com.originisle.android.service.IconCache
import java.util.Locale

/**
 * Thẻ thông báo mã OTP / mã xác thực đăng nhập ưu tiên cao trên Đảo Origin theo cấu trúc Ngân hàng (Bank-Style).
 * - Sao chép 100% cấu trúc kỹ thuật của PaymentCard.postBankTransaction.
 * - Sử dụng TEMPLATE_BASE (4) và TEMPLATE_RIGHT_ISLAND_TEXT_ICON (4) ổn định tuyệt đối.
 * - oi_show_right_icon = false để dành trọn diện tích viên nang bên phải cho mã OTP.
 * - Tone màu xanh bảo mật Emerald / Bank-Style (#00D084).
 * - Chạm để sao chép mã OTP vào clipboard qua [CopyCodeReceiver].
 */
object OtpCard {

    /** Thời gian tối đa lưu giữ thẻ OTP (2 phút) để tránh mã quá hạn nằm lâu trên Đảo. */
    private const val HOLD_MS = 120_000L

    const val COLOR_OTP = "#00D084"

    private val handler = Handler(Looper.getMainLooper())

    fun post(context: Context, sbn: StatusBarNotification, code: String, appLabel: String) {
        val app = context.applicationContext
        val id = IconCache.castIdFor(sbn)
        IconCache.activeSmallIcons.remove(id)
        val appIcon = IconCache.safeAppIcon(app, sbn.packageName)

        val isVi = Locale.getDefault().language == "vi"
        val copyActionLabel = if (isVi) "Sao chép mã" else "Copy code"

        val copy = PendingIntent.getBroadcast(
            app, id,
            Intent(app, CopyCodeReceiver::class.java)
                .putExtra(CopyCodeReceiver.EXTRA_CODE, code)
                .putExtra("cast_id", id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val copyAction = Notification.Action.Builder(
            IconCache.safeResourceIcon(app, R.mipmap.ic_launcher_round), copyActionLabel, copy,
        ).build()

        val fallbackIcon = Icon.createWithResource(app, R.mipmap.ic_launcher_round)
        val leftIcon = appIcon ?: fallbackIcon

        val intent = Intent(app, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", id)
            putExtra("is_ongoing", false)
            putExtra("oi_scene", "NAVIGATION")
            putExtra("oi_template", OriginIslandConstants.TEMPLATE_BASE) // 4 (Chuẩn cơ bản không bao giờ bị lỗi)
            putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_TEXT_ICON) // 4
            putExtra("oi_show_right_icon", false) // BẮT BUỘC: không để hiện icon bên phải để dành trọn diện tích cho mã OTP
            putExtra("oi_left_content", "OTP")
            putExtra("oi_right_content", code)
            putExtra("status_chip_text", code)
            putExtra("title", spaced(code))
            putExtra("text", "Chạm để sao chép mã OTP")
            putExtra("subtext", "Mã xác thực · $appLabel")
            putExtra("source_app", appLabel)
            putExtra("source_pkg", sbn.packageName)
            putExtra("oi_bg_color", COLOR_OTP)
            putExtra("oi_fg_color", COLOR_OTP)
            putExtra("oi_light_color", COLOR_OTP)
            putExtra("oi_icon_status_type", OriginIslandConstants.ICON_STATUS_SUCCESS)
            putExtra("icon_res", R.mipmap.ic_launcher_round)
            putExtra("oi_left_icon", leftIcon)
            putExtra("oi_large_icon", leftIcon)
            putParcelableArrayListExtra("actions", arrayListOf(copyAction))
            putStringArrayListExtra("oi_button_titles", arrayListOf(copyActionLabel))
            putExtra("action_title", copyActionLabel)
            putExtra("click_resp", copy)
            putExtra("source_content_intent", copy)
            putExtra("oi_force_show", true)
            putExtra("oi_island_show_time", 15)
            putExtra("oi_keep_duration", 30)
            putExtra("oi_dismiss_when_kill", true)
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
