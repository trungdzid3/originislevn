package com.originisle.android.service

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.content.Context
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat
import com.originisle.android.cards.EarbudsCard
import com.originisle.android.cards.OtpCard
import com.originisle.android.island.OriginIslandBuilder
import com.originisle.android.otp.OtpDetector

/**
 * Service Hỗ trợ Tiếp cận (AccessibilityService) Đa nhiệm:
 * 1. Duy trì tiến trình (Keep-Alive) không bị OriginOS thu hồi.
 * 2. Bắt mã OTP trực tiếp từ TYPE_NOTIFICATION_STATE_CHANGED để vượt qua cơ chế ẩn nội dung nhạy cảm
 *    (FLAG_OTP_REDACTED / Bảo vệ mã xác minh SMS) trên Android 15/16 & Vivo OriginOS 6.
 * 3. Kênh 3 đọc pin Samsung Galaxy Fit 3: Tự động quét và trích xuất mức % pin khi người dùng
 *    mở hoặc tương tác với ứng dụng Galaxy Wearable hoặc Galaxy Fit3 Plugin.
 */
class KeepAliveAccessibilityService : AccessibilityService() {

    private var lastScannedLevel = -1
    private var lastScanTime = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Kích hoạt rebind và phân quyền scene sau khi tiến trình được hồi sinh
        runCatching { NotificationCastListener.forceRebind(this) }
        runCatching { OriginIslandBuilder.grantScenes(this) }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: return

        if (event.eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            handleNotificationEvent(event, pkg)
            return
        }

        val isTargetPkg = pkg.contains("watchmanager", ignoreCase = true) ||
            pkg.contains("fit3plugin", ignoreCase = true) ||
            pkg.contains("wearable", ignoreCase = true)
        if (!isTargetPkg) return

        val root = rootInActiveWindow ?: return
        runCatching {
            scanForBattery(root)
        }.also {
            runCatching { root.recycle() }
        }
    }

    private fun handleNotificationEvent(event: AccessibilityEvent, pkg: String) {
        if (pkg == packageName ||
            pkg == "com.autonavi.minimap" ||
            pkg == "com.android.systemui" ||
            pkg == "android" ||
            !OtpDetector.isLikelyMessagingOrOtpPackage(pkg)
        ) {
            return
        }

        if (!isSmartOtpEnabled(applicationContext)) return

        val textParts = mutableListOf<String>()
        val actionTitles = mutableListOf<String>()
        var notifTitle: String? = null

        val notification = event.parcelableData as? Notification
        if (notification != null) {
            runCatching {
                NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)?.let { style ->
                    style.messages.asReversed().forEach { msg ->
                        msg.text?.toString()?.takeIf { it.isNotBlank() }?.let { textParts.add(it) }
                    }
                }
            }

            val extras = notification.extras
            if (extras != null) {
                notifTitle = runCatching {
                    extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()
                }.getOrNull()

                listOf(
                    Notification.EXTRA_BIG_TEXT,
                    Notification.EXTRA_TEXT,
                    Notification.EXTRA_SUB_TEXT,
                    Notification.EXTRA_TITLE,
                ).forEach { key ->
                    runCatching { extras.getCharSequence(key)?.toString() }
                        .getOrNull()?.takeIf { it.isNotBlank() }?.let { textParts.add(it) }
                }
            }

            runCatching { notification.tickerText?.toString() }
                .getOrNull()?.takeIf { it.isNotBlank() }?.let { textParts.add(it) }

            runCatching {
                notification.actions?.forEach { action ->
                    action.title?.toString()?.takeIf { it.isNotBlank() }?.let { actionTitles.add(it) }
                }
            }
        }

        val eventText = event.text
            ?.mapNotNull { it?.toString()?.takeIf { s -> s.isNotBlank() } }
            ?.joinToString(" ")
            .orEmpty()
        if (eventText.isNotBlank()) {
            textParts.add(eventText)
        }

        val combinedText = textParts.distinct().joinToString("\n")
        if (combinedText.isBlank() && actionTitles.isEmpty()) return

        val code = OtpDetector.find(combinedText, actionTitles)
        if (code != null) {
            val senderTitle = notifTitle?.takeIf { it.isNotBlank() } ?: resolveAppLabel(pkg)
            Log.i(TAG, "Accessibility intercepted OTP from $pkg ($senderTitle)")
            OtpCard.postFromAccessibility(applicationContext, pkg, senderTitle, code)
        }
    }

    private fun isSmartOtpEnabled(context: Context): Boolean {
        val prefNames = listOf(
            "${context.packageName}_preferences",
            "origin_isle_prefs",
            "prefs",
        )
        for (name in prefNames) {
            val sp = context.getSharedPreferences(name, Context.MODE_PRIVATE)
            if (sp.contains("smart_otp")) return sp.getBoolean("smart_otp", true)
            if (sp.contains("otp_enabled")) return sp.getBoolean("otp_enabled", true)
            if (sp.contains("pref_smart_otp")) return sp.getBoolean("pref_smart_otp", true)
        }
        return true
    }

    private fun resolveAppLabel(pkg: String): String =
        runCatching {
            val pm = applicationContext.packageManager
            val info = pm.getApplicationInfo(pkg, 0)
            pm.getApplicationLabel(info).toString()
        }.getOrDefault("Tin nhắn")

    private fun scanForBattery(node: AccessibilityNodeInfo?) {
        if (node == null) return

        val text = node.text?.toString()
        val desc = node.contentDescription?.toString()
        val candidate = when {
            !text.isNullOrBlank() -> text
            !desc.isNullOrBlank() -> desc
            else -> null
        }

        if (candidate != null) {
            val level = parseBattery(candidate)
            if (level in 0..100) {
                updateFit3Battery(level)
                return
            }
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i)
            if (child != null) {
                scanForBattery(child)
                runCatching { child.recycle() }
            }
        }
    }

    private fun parseBattery(str: String): Int {
        val trimmed = str.trim()
        val labeledMatch = BATTERY_LABELED_REGEX.find(trimmed)
        if (labeledMatch != null) {
            val num = labeledMatch.groupValues[1].toIntOrNull()
            if (num != null && num in 0..100) return num
        }
        val pctMatch = BATTERY_PCT_REGEX.find(trimmed)
        if (pctMatch != null) {
            val num = pctMatch.groupValues[1].toIntOrNull()
            if (num != null && num in 0..100) return num
        }
        return -1
    }

    private fun updateFit3Battery(level: Int) {
        val now = System.currentTimeMillis()
        if (level == lastScannedLevel && now - lastScanTime < 5_000L) return
        lastScannedLevel = level
        lastScanTime = now
        Log.i(TAG, "Accessibility detected Fit 3 battery: $level%")

        EarbudsCard.saveCachedBattery(applicationContext, "Galaxy Fit 3", level)
        EarbudsCard.saveCachedBattery(applicationContext, "Galaxy Fit3", level)
        val prefs = applicationContext.getSharedPreferences("wearable_battery_cache", Context.MODE_PRIVATE)
        prefs.edit().putInt("wearable_last_known_battery", level).apply()

        EarbudsCard.onBatteryUpdated(
            applicationContext,
            "Galaxy Fit 3",
            EarbudsCard.EarbudsTelemetry(main = level),
            isWearable = true
        )
    }

    override fun onInterrupt() = Unit

    companion object {
        private const val TAG = "KeepAliveA11y"
        private val BATTERY_LABELED_REGEX = Regex("""(?:pin|battery|mức pin)\s*[:：\-]?\s*(\d{1,3})\s*%?""", RegexOption.IGNORE_CASE)
        private val BATTERY_PCT_REGEX = Regex("""(\d{1,3})\s*%""")
    }
}
