package com.originisle.android.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
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
    private val handler = Handler(Looper.getMainLooper())
    private val delayedForegroundCheck = Runnable {
        val prevFg = currentForegroundPackage
        val resolved = resolveLiveForegroundPackage()
        DownloadCard.onForegroundPackageChanged(applicationContext, resolved)
        val fgChanged = resolved.isNotBlank() && resolved != prevFg
        if ((DownloadCard.hasDeferredDownloads() || fgChanged || isLauncherPackage(resolved)) &&
            !isBrowserCurrentlyOpen(resolved)
        ) {
            NotificationCastListener.instance?.refreshActiveDownloadsNow()
        }
    }

    private fun scheduleDelayedForegroundCheck() {
        handler.removeCallbacks(delayedForegroundCheck)
        handler.postDelayed(delayedForegroundCheck, 50L)
        handler.postDelayed(delayedForegroundCheck, 150L)
        handler.postDelayed(delayedForegroundCheck, 300L)
        handler.postDelayed(delayedForegroundCheck, 600L)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        // Kích hoạt rebind và phân quyền scene sau khi tiến trình được hồi sinh
        runCatching { NotificationCastListener.forceRebind(this) }
        runCatching { OriginIslandBuilder.grantScenes(this) }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        handler.removeCallbacks(delayedForegroundCheck)
        if (instance === this) {
            instance = null
            currentForegroundPackage = ""
        }
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        handler.removeCallbacks(delayedForegroundCheck)
        if (instance === this) {
            instance = null
            currentForegroundPackage = ""
        }
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString().orEmpty()
        val eventType = event.eventType
        val prevFg = currentForegroundPackage
        if (pkg.isNotBlank() && !isIgnoredWindowPkg(pkg)) {
            val shouldUpdateFg = isLauncherPackage(pkg) ||
                eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED && pkg != currentForegroundPackage)
            if (shouldUpdateFg) {
                lastWindowChangedPkg = pkg
                lastWindowChangedTimeMs = System.currentTimeMillis()
                currentForegroundPackage = pkg
                if (DownloadDetector.isBrowserPackage(pkg)) {
                    lastKnownBrowserPackage = pkg
                }
            }
        }
        val isSystemUiGestureWithDeferred = pkg == "com.android.systemui" && DownloadCard.hasDeferredDownloads()
        val shouldCheckForeground = eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED ||
            isLauncherPackage(pkg) ||
            isSystemUiGestureWithDeferred ||
            (pkg.isNotBlank() && !isIgnoredWindowPkg(pkg) && pkg != prevFg)
        if (shouldCheckForeground) {
            val resolved = resolveLiveForegroundPackage()
            DownloadCard.onForegroundPackageChanged(applicationContext, resolved)
            val fgChanged = resolved.isNotBlank() && resolved != prevFg
            if (fgChanged && !isBrowserCurrentlyOpen(resolved)) {
                NotificationCastListener.instance?.refreshActiveDownloadsNow()
            }
            scheduleDelayedForegroundCheck()
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

        @Volatile
        var lastWindowChangedPkg: String = ""

        @Volatile
        var lastWindowChangedTimeMs: Long = 0L

        @Volatile
        var lastKnownBrowserPackage: String = ""

        private val IGNORED_FOREGROUND_PKGS = setOf(
            "com.android.systemui",
            "android",
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

        fun isLauncherPackage(pkg: String): Boolean {
            val lower = pkg.lowercase()
            return lower.contains("launcher") ||
                pkg == "com.bbk.launcher2" ||
                pkg == "com.vivo.launcher" ||
                pkg == "com.vivo.hiboard" ||
                pkg == "com.vivo.globalsearch"
        }

        fun resolveDownloadOwnerPackage(downloadSourcePkg: String): String {
            if (downloadSourcePkg.startsWith("com.android.providers.downloads") &&
                lastKnownBrowserPackage.isNotBlank() &&
                DownloadDetector.isBrowserPackage(lastKnownBrowserPackage)
            ) {
                return lastKnownBrowserPackage
            }
            return downloadSourcePkg
        }

        private fun matchesDownloadSource(pkg: String, downloadSourcePkg: String): Boolean {
            if (DownloadDetector.isBrowserPackage(downloadSourcePkg)) {
                return pkg.equals(downloadSourcePkg, ignoreCase = true) || DownloadDetector.isBrowserPackage(pkg)
            }
            if (downloadSourcePkg.startsWith("com.android.providers.downloads")) {
                return DownloadDetector.isBrowserPackage(pkg)
            }
            if (downloadSourcePkg == "com.android.vending" || DownloadDetector.isDownloadPackage(downloadSourcePkg)) {
                return pkg.equals(downloadSourcePkg, ignoreCase = true)
            }
            return false
        }

        fun onDownloadStartedInApp(pkg: String) {
            if (pkg.isBlank() || isIgnoredWindowPkg(pkg)) return
            if (currentForegroundPackage.isBlank()) {
                currentForegroundPackage = pkg
                lastWindowChangedPkg = pkg
                lastWindowChangedTimeMs = System.currentTimeMillis()
            }
            if (DownloadDetector.isBrowserPackage(pkg)) {
                lastKnownBrowserPackage = pkg
            }
        }

        fun resolveLiveForegroundPackage(): String {
            val svc = instance ?: return currentForegroundPackage
            var livePkg = ""
            if (lastWindowChangedPkg.isNotBlank() &&
                isLauncherPackage(lastWindowChangedPkg) &&
                System.currentTimeMillis() - lastWindowChangedTimeMs <= 3_000L
            ) {
                var hasActiveNonLauncherApp = false
                runCatching {
                    val activeAppWindows = svc.windows.orEmpty()
                        .filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isActive }
                    for (w in activeAppWindows) {
                        val root = w.root ?: continue
                        val pkg = try {
                            root.packageName?.toString().orEmpty()
                        } finally {
                            runCatching { root.recycle() }
                        }
                        if (pkg.isNotBlank() && !isIgnoredWindowPkg(pkg) && !isLauncherPackage(pkg)) {
                            hasActiveNonLauncherApp = true
                            break
                        }
                    }
                }
                if (!hasActiveNonLauncherApp) {
                    livePkg = lastWindowChangedPkg
                }
            }
            if (livePkg.isBlank()) {
                runCatching {
                    val sortedWindows = svc.windows.orEmpty()
                        .filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION && (it.isActive || it.isFocused) }
                        .sortedWith(
                            compareByDescending<AccessibilityWindowInfo> { it.isActive }
                                .thenByDescending { it.isFocused }
                                .thenByDescending { it.layer }
                        )
                    for (w in sortedWindows) {
                        val root = w.root ?: continue
                        val pkg = try {
                            root.packageName?.toString().orEmpty()
                        } finally {
                            runCatching { root.recycle() }
                        }
                        if (pkg.isNotBlank() && !isIgnoredWindowPkg(pkg)) {
                            livePkg = pkg
                            break
                        }
                    }
                }
            }
            if (livePkg.isBlank()) {
                runCatching {
                    svc.rootInActiveWindow?.let { root ->
                        val pkg = try {
                            root.packageName?.toString().orEmpty()
                        } finally {
                            runCatching { root.recycle() }
                        }
                        if (pkg.isNotBlank() && !isIgnoredWindowPkg(pkg)) {
                            livePkg = pkg
                        }
                    }
                }
            }
            if (livePkg.isBlank() &&
                lastWindowChangedPkg.isNotBlank() &&
                !isIgnoredWindowPkg(lastWindowChangedPkg) &&
                System.currentTimeMillis() - lastWindowChangedTimeMs <= 4_000L
            ) {
                livePkg = lastWindowChangedPkg
            }
            if (livePkg.isNotBlank()) {
                currentForegroundPackage = livePkg
                if (DownloadDetector.isBrowserPackage(livePkg)) {
                    lastKnownBrowserPackage = livePkg
                }
            }
            return currentForegroundPackage
        }

        fun isBrowserCurrentlyOpen(downloadSourcePkg: String): Boolean {
            val svc = instance
            if (svc != null) {
                val activePkgs = mutableListOf<String>()
                val inactivePkgs = mutableListOf<String>()
                var hasNullRootActiveAppWindow = false
                runCatching {
                    val sortedWindows = svc.windows.orEmpty()
                        .filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
                        .sortedWith(
                            compareByDescending<AccessibilityWindowInfo> { it.isActive }
                                .thenByDescending { it.isFocused }
                                .thenByDescending { it.layer }
                        )
                    for (w in sortedWindows) {
                        val isWindowActive = w.isActive || w.isFocused
                        val root = w.root
                        if (root == null) {
                            if (isWindowActive) {
                                hasNullRootActiveAppWindow = true
                            }
                            continue
                        }
                        val pkg = try {
                            root.packageName?.toString().orEmpty()
                        } finally {
                            runCatching { root.recycle() }
                        }
                        if (pkg.isNotBlank() && !isIgnoredWindowPkg(pkg)) {
                            if (isWindowActive) {
                                activePkgs.add(pkg)
                            } else {
                                inactivePkgs.add(pkg)
                            }
                        }
                    }
                }
                if (activePkgs.isNotEmpty()) {
                    if (activePkgs.any { isLauncherPackage(it) } ||
                        activePkgs.none { matchesDownloadSource(it, downloadSourcePkg) }
                    ) {
                        currentForegroundPackage = activePkgs.first()
                        return false
                    }
                } else {
                    if (inactivePkgs.any { matchesDownloadSource(it, downloadSourcePkg) }) {
                        if (isLauncherPackage(lastWindowChangedPkg)) {
                            currentForegroundPackage = lastWindowChangedPkg
                        }
                        return false
                    }
                    if (hasNullRootActiveAppWindow && isLauncherPackage(lastWindowChangedPkg)) {
                        currentForegroundPackage = lastWindowChangedPkg
                        return false
                    }
                }
            }

            val fg = resolveLiveForegroundPackage()
            if (fg.isBlank() || isLauncherPackage(fg)) return false
            return matchesDownloadSource(fg, downloadSourcePkg)
        }
    }
}
