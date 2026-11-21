package com.originisle.android.download

import com.originisle.android.service.KeepAliveAccessibilityService
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadBrowserDetectionTest {

    @After
    fun tearDown() {
        KeepAliveAccessibilityService.currentForegroundPackage = ""
        KeepAliveAccessibilityService.instance = null
    }

    @Test
    fun isBrowserPackage_detectsSupportedBrowsersAndExcludesNonBrowsers() {
        assertTrue(DownloadDetector.isBrowserPackage("com.android.chrome"))
        assertTrue(DownloadDetector.isBrowserPackage("com.chrome.beta"))
        assertTrue(DownloadDetector.isBrowserPackage("com.microsoft.emmx"))
        assertTrue(DownloadDetector.isBrowserPackage("com.microsoft.emmx.canary"))
        assertTrue(DownloadDetector.isBrowserPackage("com.vivo.browser"))
        assertTrue(DownloadDetector.isBrowserPackage("com.brave.browser"))
        assertTrue(DownloadDetector.isBrowserPackage("org.mozilla.firefox"))
        assertTrue(DownloadDetector.isBrowserPackage("com.opera.browser"))
        assertTrue(DownloadDetector.isBrowserPackage("com.coccoc.trinhduyet"))
        assertTrue(DownloadDetector.isBrowserPackage("com.sec.android.app.sbrowser"))
        assertTrue(DownloadDetector.isBrowserPackage("com.duckduckgo.mobile.android"))
        assertTrue(DownloadDetector.isBrowserPackage("mark.via.gp"))

        assertFalse(DownloadDetector.isBrowserPackage("com.android.vending"))
        assertFalse(DownloadDetector.isBrowserPackage("com.android.providers.downloads"))
        assertFalse(DownloadDetector.isBrowserPackage("com.bbk.launcher2"))
    }

    @Test
    fun isIgnoredWindowPkg_filtersSystemUiAndKeyboards() {
        assertTrue(KeepAliveAccessibilityService.isIgnoredWindowPkg("com.android.systemui"))
        assertTrue(KeepAliveAccessibilityService.isIgnoredWindowPkg("android"))
        assertTrue(KeepAliveAccessibilityService.isIgnoredWindowPkg("com.autonavi.minimap"))
        assertTrue(KeepAliveAccessibilityService.isIgnoredWindowPkg("com.google.android.inputmethod.latin"))
        assertTrue(KeepAliveAccessibilityService.isIgnoredWindowPkg("com.custom.keyboard.app"))
        assertTrue(KeepAliveAccessibilityService.isIgnoredWindowPkg("com.google.android.permissioncontroller"))

        assertFalse(KeepAliveAccessibilityService.isIgnoredWindowPkg("com.microsoft.emmx"))
        assertFalse(KeepAliveAccessibilityService.isIgnoredWindowPkg("com.bbk.launcher2"))
    }

    @Test
    fun isBrowserCurrentlyOpen_tracksForegroundBrowserForBrowserAndDownloadManager() {
        KeepAliveAccessibilityService.currentForegroundPackage = "com.microsoft.emmx"
        assertTrue(KeepAliveAccessibilityService.isBrowserCurrentlyOpen("com.microsoft.emmx"))
        assertTrue(KeepAliveAccessibilityService.isBrowserCurrentlyOpen("com.android.providers.downloads"))
        assertFalse(KeepAliveAccessibilityService.isBrowserCurrentlyOpen("com.android.vending"))

        KeepAliveAccessibilityService.currentForegroundPackage = "com.bbk.launcher2"
        assertFalse(KeepAliveAccessibilityService.isBrowserCurrentlyOpen("com.microsoft.emmx"))
        assertFalse(KeepAliveAccessibilityService.isBrowserCurrentlyOpen("com.android.providers.downloads"))

        KeepAliveAccessibilityService.currentForegroundPackage = "com.android.vending"
        assertTrue(KeepAliveAccessibilityService.isBrowserCurrentlyOpen("com.android.vending"))
        assertFalse(KeepAliveAccessibilityService.isBrowserCurrentlyOpen("com.microsoft.emmx"))

        KeepAliveAccessibilityService.currentForegroundPackage = "com.bbk.launcher2"
        assertFalse(KeepAliveAccessibilityService.isBrowserCurrentlyOpen("com.android.vending"))
    }
}
