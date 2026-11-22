package com.originisle.android.download

import com.originisle.android.service.KeepAliveAccessibilityService
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadBrowserDetectionTest {

    @After
    fun tearDown() {
        KeepAliveAccessibilityService.currentForegroundPackage = ""
        KeepAliveAccessibilityService.lastKnownBrowserPackage = ""
        KeepAliveAccessibilityService.lastWindowChangedPkg = ""
        KeepAliveAccessibilityService.lastWindowChangedTimeMs = 0L
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
        assertFalse(KeepAliveAccessibilityService.isIgnoredWindowPkg("com.autonavi.minimap"))
        assertTrue(KeepAliveAccessibilityService.isIgnoredWindowPkg("com.google.android.inputmethod.latin"))
        assertTrue(KeepAliveAccessibilityService.isIgnoredWindowPkg("com.custom.keyboard.app"))
        assertTrue(KeepAliveAccessibilityService.isIgnoredWindowPkg("com.google.android.permissioncontroller"))

        assertFalse(KeepAliveAccessibilityService.isIgnoredWindowPkg("com.microsoft.emmx"))
        assertFalse(KeepAliveAccessibilityService.isIgnoredWindowPkg("com.bbk.launcher2"))
    }

    @Test
    fun isLauncherPackage_detectsVivoAndGenericLaunchers() {
        assertTrue(KeepAliveAccessibilityService.isLauncherPackage("com.bbk.launcher2"))
        assertTrue(KeepAliveAccessibilityService.isLauncherPackage("com.vivo.launcher"))
        assertFalse(KeepAliveAccessibilityService.isLauncherPackage("com.android.chrome"))
    }

    @Test
    fun resolveDownloadOwnerPackage_fallsBackToLastKnownBrowserForSystemDownloadManager() {
        KeepAliveAccessibilityService.lastKnownBrowserPackage = "com.android.chrome"
        assertEquals(
            "com.android.chrome",
            KeepAliveAccessibilityService.resolveDownloadOwnerPackage("com.android.providers.downloads"),
        )
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

    @Test
    fun onDownloadStartedInApp_setsForegroundWhenEmptyAndTracksLastKnownBrowser() {
        KeepAliveAccessibilityService.onDownloadStartedInApp("com.android.chrome")
        assertEquals("com.android.chrome", KeepAliveAccessibilityService.currentForegroundPackage)
        assertEquals("com.android.chrome", KeepAliveAccessibilityService.lastWindowChangedPkg)
        assertEquals("com.android.chrome", KeepAliveAccessibilityService.lastKnownBrowserPackage)

        KeepAliveAccessibilityService.currentForegroundPackage = "com.bbk.launcher2"
        KeepAliveAccessibilityService.onDownloadStartedInApp("com.microsoft.emmx")
        assertEquals("com.bbk.launcher2", KeepAliveAccessibilityService.currentForegroundPackage)
        assertEquals("com.microsoft.emmx", KeepAliveAccessibilityService.lastKnownBrowserPackage)
    }
}
