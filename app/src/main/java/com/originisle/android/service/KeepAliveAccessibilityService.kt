package com.originisle.android.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.originisle.android.cards.DownloadCard
import com.originisle.android.cards.EarbudsCard
import com.originisle.android.download.DownloadDetector
import com.originisle.android.island.OriginIslandBuilder

/**
 * Service Hỗ trợ Tiếp cận (AccessibilityService) Đa nhiệm:
 * 1. Duy trì tiến trình (Keep-Alive) không bị OriginOS thu hồi.
 * 2. Kênh đọc pin Samsung Galaxy Fit 3: Tự động quét và trích xuất mức % pin khi người dùng
 *    mở hoặc tương tác với ứng dụng Galaxy Wearable hoặc Galaxy Fit3 Plugin.
 * 3. Theo dõi gói ứng dụng tiền cảnh để tự động ẩn/hiện tiến trình tải xuống trình duyệt trên Đảo.
 */
class KeepAliveAccessibilityService : AccessibilityService() {

    private var lastScannedLevel = -1
    private var lastScanTime = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        // Kích hoạt rebind và phân quyền scene sau khi tiến trình được hồi sinh
        runCatching { NotificationCastListener.forceRebind(this) }
        runCatching { OriginIslandBuilder.grantScenes(this) }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if (instance === this) {
            instance = null
        }
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        if (instance === this) {
            instance = null
        }
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString().orEmpty()
        if (pkg.isNotBlank() && !isIgnoredWindowPkg(pkg)) {
            if (pkg != currentForegroundPackage || event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                val changed = pkg != currentForegroundPackage
                currentForegroundPackage = pkg
                if (changed) {
                    DownloadCard.onForegroundPackageChanged(applicationContext, pkg)
                }
            }
        }
        if (pkg.isBlank()) return

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
        if (!trimmed.contains('%')) return -1
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
        if (!EarbudsCard.isDeviceHardwareConnected(applicationContext, "Galaxy Fit 3", isWearable = true)) {
            Log.d(TAG, "Fit 3 hardware not connected, ignoring a11y battery scan")
            return
        }
        val now = System.currentTimeMillis()
        if (level == lastScannedLevel && now - lastScanTime < 5_000L) return
        lastScannedLevel = level
        lastScanTime = now
        Log.i(TAG, "Accessibility detected Fit 3 battery: $level%")

        EarbudsCard.saveCachedBattery(applicationContext, "Galaxy Fit 3", level)
        EarbudsCard.saveCachedBattery(applicationContext, "Galaxy Fit3", level)
        val prefs = applicationContext.getSharedPreferences("wearable_battery_cache", Context.MODE_PRIVATE)
        prefs.edit().putInt("wearable_last_known_battery", level).apply()

        if (EarbudsCard.isShowing(isWearable = true, nameOrAddress = "Galaxy Fit 3")) {
            EarbudsCard.onBatteryUpdated(
                applicationContext,
                "Galaxy Fit 3",
                EarbudsCard.EarbudsTelemetry(main = level),
                isWearable = true
            )
        }
    }

    override fun onInterrupt() = Unit

    companion object {
        private const val TAG = "KeepAliveA11y"
        private val BATTERY_LABELED_REGEX = Regex("""(?:pin|battery|mức\s*pin)\s*[:：\-]?\s*(\d{1,3})\s*%""", RegexOption.IGNORE_CASE)
        private val BATTERY_PCT_REGEX = Regex("""(\d{1,3})\s*%""")

        @Volatile
        var instance: KeepAliveAccessibilityService? = null

        @Volatile
        var currentForegroundPackage: String = ""

        private val IGNORED_FOREGROUND_PKGS = setOf(
            "com.android.systemui",
            "android",
            "com.autonavi.minimap",
            "com.vivo.upslide",
            "com.vivo.smartmultiwindow",
            "com.vivo.fingerprint",
            "com.vivo.secime.service",
            "com.google.android.inputmethod.latin",
            "com.TouchPal.vivo",
            "com.baidu.input_vivo",
            "com.sohu.inputmethod.sogou.vivo",
        )

        fun isIgnoredWindowPkg(pkg: String): Boolean {
            val lower = pkg.lowercase()
            return pkg in IGNORED_FOREGROUND_PKGS ||
                lower.contains("inputmethod") ||
                lower.contains("keyboard") ||
                lower.contains("permissioncontroller")
        }

        fun isBrowserCurrentlyOpen(downloadSourcePkg: String): Boolean {
            val livePkg = instance?.let { svc ->
                runCatching {
                    svc.rootInActiveWindow?.let { root ->
                        val p = root.packageName?.toString().orEmpty()
                        runCatching { root.recycle() }
                        p
                    }
                }.getOrNull()
            }.orEmpty()
            if (livePkg.isNotBlank() && !isIgnoredWindowPkg(livePkg)) {
                currentForegroundPackage = livePkg
            }
            val fg = currentForegroundPackage
            if (fg.isBlank()) return false
            if (DownloadDetector.isBrowserPackage(downloadSourcePkg)) {
                return fg.equals(downloadSourcePkg, ignoreCase = true) || DownloadDetector.isBrowserPackage(fg)
            }
            if (downloadSourcePkg.startsWith("com.android.providers.downloads")) {
                return DownloadDetector.isBrowserPackage(fg)
            }
            if (downloadSourcePkg == "com.android.vending" || DownloadDetector.isDownloadPackage(downloadSourcePkg)) {
                return fg.equals(downloadSourcePkg, ignoreCase = true)
            }
            return false
        }
    }
}
