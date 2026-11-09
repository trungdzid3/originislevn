package com.originisle.android.cards

import android.app.KeyguardManager
import android.app.Notification
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.service.notification.StatusBarNotification
import com.originisle.android.R
import com.originisle.android.island.OriginIslandConstants
import com.originisle.android.island.PlaygroundService
import com.originisle.android.service.CopyCodeReceiver
import com.originisle.android.service.IconCache

/**
 * Thẻ thông báo mã OTP / mã xác thực đăng nhập ưu tiên cao trên Đảo Origin theo cấu trúc Ngân hàng (Bank-Style).
 * - Sao chép 100% cấu trúc kỹ thuật thành công của PaymentCard.postBankTransaction.
 * - Sử dụng TEMPLATE_BASE (4) và TEMPLATE_RIGHT_ISLAND_TEXT_ICON (4) ổn định tuyệt đối.
 * - Tuyệt đối không truyền Notification.Action để tránh Vivo SystemUI từ chối icon cross-package (SUPERX_VALUE = false).
 * - Tự động sao chép mã OTP vào Clipboard ngay lập tức và hỗ trợ chạm thẻ để sao chép lại.
 */
object OtpCard {

    const val ID = 35001

    /** Thời gian hiển thị thẻ OTP trên Đảo (15 giây). */
    private const val HOLD_MS = 15_000L

    /** Khoảng thời gian chống trùng lặp giữa NotificationListener và AccessibilityService (8 giây). */
    private const val DEDUP_WINDOW_MS = 8_000L

    const val COLOR_OTP = "#00D084"

    @Volatile
    private var lastPostedCode: String = ""

    @Volatile
    private var lastPostedTime: Long = 0L

    private val mainHandler = Handler(Looper.getMainLooper())
    private var autoCancelRunnable: Runnable? = null
    private var clearClipRunnable: Runnable? = null

    @JvmOverloads
    fun post(
        context: Context,
        sbn: StatusBarNotification,
        code: String,
        appLabel: String = "",
    ) {
        val pkg = sbn.packageName
        val extras = sbn.notification?.extras
        val notifTitle = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()
        val sender = when {
            !notifTitle.isNullOrBlank() -> notifTitle
            appLabel.isNotBlank() -> appLabel.trim()
            else -> resolveAppLabel(context, pkg)
        }
        postInternal(context, pkg, sender, code)
    }

    fun postFromAccessibility(
        context: Context,
        pkg: String,
        senderTitle: String,
        code: String,
    ) {
        val sender = senderTitle.trim().ifBlank { resolveAppLabel(context, pkg) }
        postInternal(context, pkg, sender, code)
    }

    private fun postInternal(
        context: Context,
        pkg: String,
        sender: String,
        code: String,
    ) {
        val cleanCode = code.trim()
        if (cleanCode.isEmpty()) return

        val now = System.currentTimeMillis()
        if (cleanCode == lastPostedCode && (now - lastPostedTime) < DEDUP_WINDOW_MS) {
            return
        }
        lastPostedCode = cleanCode
        lastPostedTime = now

        val app = context.applicationContext

        // Tự động sao chép ngay mã OTP vào bộ nhớ tạm kèm cờ nhạy cảm và tự xóa sau 60s
        runCatching {
            val cm = app.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText("OTP", cleanCode).apply {
                description.extras = PersistableBundle().apply {
                    putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                }
            }
            cm?.setPrimaryClip(clip)
            clearClipRunnable?.let { mainHandler.removeCallbacks(it) }
            val clearTask = Runnable {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    runCatching { cm?.clearPrimaryClip() }
                }
            }
            clearClipRunnable = clearTask
            mainHandler.postDelayed(clearTask, 60_000L)
        }

        val prefs = app.getSharedPreferences("experimental_prefs", Context.MODE_PRIVATE)
        val redactOnLock = prefs.getBoolean("cast_lockscreen_redact_sensitive", true)
        val isLocked = (app.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)?.isDeviceLocked == true
        val shouldMask = redactOnLock && isLocked
        val displayCode = if (shouldMask) "••••••" else cleanCode

        IconCache.activeSmallIcons.remove(ID)
        val appIcon = IconCache.safeAppIcon(app, pkg)
        val fallbackIcon = Icon.createWithResource(app, R.mipmap.ic_launcher_round)
        val leftIcon = appIcon ?: fallbackIcon

        val copyPendingIntent = PendingIntent.getBroadcast(
            app,
            ID,
            Intent(app, CopyCodeReceiver::class.java)
                .putExtra(CopyCodeReceiver.EXTRA_CODE, cleanCode)
                .putExtra("cast_id", ID),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val intent = Intent(app, PlaygroundService::class.java).apply {
            action = PlaygroundService.ACTION_START
            putExtra("id", ID)
            putExtra("oi_scene", "NAVIGATION")
            putExtra("template", OriginIslandConstants.TEMPLATE_BASE) // 4
            putExtra("oi_template", OriginIslandConstants.TEMPLATE_BASE) // 4
            putExtra("oi_right_template", OriginIslandConstants.TEMPLATE_RIGHT_ISLAND_TEXT_ICON) // 4
            putExtra("title", "Mã OTP: $displayCode")
            putExtra("text", "$sender • Chạm vào thẻ để sao chép")
            putExtra(
                "subtext",
                if (shouldMask) "Đã tự động lưu mã OTP vào bộ nhớ tạm (Mở khóa để xem)" else "Đã tự động lưu $cleanCode vào bộ nhớ tạm",
            )
            putExtra("oi_left_content", "OTP")
            putExtra("oi_right_content", if (shouldMask) "•••" else cleanCode)
            putExtra("status_chip_text", if (shouldMask) "•••" else cleanCode)
            putExtra("oi_show_right_icon", false)
            putExtra("oi_show_notify", false)
            putExtra("icon", leftIcon)
            putExtra("oi_large_icon", leftIcon)
            putExtra("oi_left_icon", leftIcon)
            putExtra("icon_res", R.mipmap.ic_launcher_round)
            putExtra("oi_generating_status", 0)
            putExtra("oi_icon_status_type", OriginIslandConstants.ICON_STATUS_SUCCESS)
            putExtra("oi_bg_color", COLOR_OTP)
            putExtra("oi_fg_color", COLOR_OTP)
            putExtra("oi_light_color", COLOR_OTP)
            putExtra("oi_force_show", true)
            putExtra("oi_dismiss_when_kill", true)
            putExtra("click_resp", copyPendingIntent)
            putExtra("copy_content", cleanCode)
            putExtra("source_pkg", pkg)
        }
        app.startService(intent)

        autoCancelRunnable?.let { mainHandler.removeCallbacks(it) }
        val cancelTask = Runnable { cancel(app, ID) }
        autoCancelRunnable = cancelTask
        mainHandler.postDelayed(cancelTask, HOLD_MS)
    }

    @JvmOverloads
    fun cancel(context: Context, id: Int = ID) {
        context.startService(
            Intent(context, PlaygroundService::class.java)
                .setAction(PlaygroundService.ACTION_CANCEL)
                .putExtra("id", id)
                .putExtra("oi_scene", "NAVIGATION"),
        )
    }

    private fun resolveAppLabel(context: Context, pkg: String): String =
        runCatching {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(pkg, 0)
            pm.getApplicationLabel(info).toString()
        }.getOrDefault("Tin nhắn")
}
