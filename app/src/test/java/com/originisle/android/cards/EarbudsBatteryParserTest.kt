package com.originisle.android.cards

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EarbudsBatteryParserTest {

    @Test
    fun parseBatteryText_googleFastPairVietnameseStandard_extractsLeftRightAndCase() {
        val telemetry = EarbudsCard.parseBatteryText(
            title = "heobeos plus",
            text = "Tai nghe bên trái 50% Hộp sạc 20% Tai nghe bên phải 100%",
        )

        assertNotNull(telemetry)
        assertEquals(50, telemetry!!.left)
        assertEquals(100, telemetry.right)
        assertEquals(20, telemetry.case)
        assertTrue(telemetry.hasDetails)
    }

    @Test
    fun parseBatteryText_googleFastPairWithUnicodeBidiInvisibleChars_extractsLeftRightAndCase() {
        val telemetry = EarbudsCard.parseBatteryText(
            title = "heobeos plus",
            text = "Tai nghe bên trái\u00A0\u206850%\u2069 Hộp sạc\u00A0\u206820%\u2069 Tai nghe bên phải\u00A0\u2068100%\u2069",
        )

        assertNotNull(telemetry)
        assertEquals(50, telemetry!!.left)
        assertEquals(100, telemetry.right)
        assertEquals(20, telemetry.case)
        assertTrue(telemetry.hasDetails)
    }

    @Test
    fun mergeTelemetry_preservesDetailedLeftRightCaseWhenIncomingOnlyHasMainHfp() {
        val existing = EarbudsCard.EarbudsTelemetry(left = 50, right = 100, case = 20)
        val incoming = EarbudsCard.EarbudsTelemetry(main = 50)

        val merged = EarbudsCard.mergeTelemetry(existing, incoming)

        assertEquals(50, merged.left)
        assertEquals(100, merged.right)
        assertEquals(20, merged.case)
        assertEquals(50, merged.main)
        assertTrue(merged.hasDetails)
    }

    @Test
    fun parseBatteryText_galaxyFit3BackgroundOrDisconnected_returnsNullNeverParsesAs3Percent() {
        val backgroundNotif = EarbudsCard.parseBatteryText(
            title = "Galaxy Fit 3",
            text = "Galaxy Fit 3 — Đang chạy nền",
        )
        assertNull(backgroundNotif)

        val disconnectedNotif = EarbudsCard.parseBatteryText(
            title = "Galaxy Fit 3",
            text = "Không thể kết nối với Galaxy Fit 3",
        )
        assertNull(disconnectedNotif)
    }

    @Test
    fun parseBatteryText_xiaomiEarbudsRemoteViewsJoinedText_extractsLeftRightAndCase() {
        val viJoined = EarbudsCard.parseBatteryText(
            title = "Xiaomi Buds",
            text = "Trái 80% Phải 90% Hộp sạc 50%",
        )
        assertNotNull(viJoined)
        assertEquals(80, viJoined!!.left)
        assertEquals(90, viJoined.right)
        assertEquals(50, viJoined.case)
        assertTrue(viJoined.hasDetails)

        val enJoined = EarbudsCard.parseBatteryText(
            title = "Xiaomi Buds",
            text = "Left 85% Right 90%",
        )
        assertNotNull(enJoined)
        assertEquals(85, enJoined!!.left)
        assertEquals(90, enJoined.right)
        assertEquals(-1, enJoined.case)
        assertTrue(enJoined.hasDetails)
    }

    @Test
    fun shouldSuppressReconnectPopup_blocksAlreadyConnectedUnlessShowingOrNewLowBattery() {
        assertTrue(
            EarbudsCard.shouldSuppressReconnectPopup(
                wasAlreadyConnectedBefore = true,
                isShowing = false,
                isNewLowBattery = false,
                lastShownMs = 0L,
                nowMs = 20_000L,
            ),
        )
        assertFalse(
            EarbudsCard.shouldSuppressReconnectPopup(
                wasAlreadyConnectedBefore = true,
                isShowing = true,
                isNewLowBattery = false,
                lastShownMs = 15_000L,
                nowMs = 20_000L,
            ),
        )
        assertFalse(
            EarbudsCard.shouldSuppressReconnectPopup(
                wasAlreadyConnectedBefore = true,
                isShowing = false,
                isNewLowBattery = true,
                lastShownMs = 15_000L,
                nowMs = 20_000L,
            ),
        )
    }

    @Test
    fun shouldSuppressReconnectPopup_allowsFreshConnectAfterDisconnectAndRespectsCooldown() {
        assertFalse(
            EarbudsCard.shouldSuppressReconnectPopup(
                wasAlreadyConnectedBefore = false,
                isShowing = false,
                isNewLowBattery = false,
                lastShownMs = 0L,
                nowMs = 20_000L,
            ),
        )
        assertTrue(
            EarbudsCard.shouldSuppressReconnectPopup(
                wasAlreadyConnectedBefore = false,
                isShowing = false,
                isNewLowBattery = false,
                lastShownMs = 10_000L,
                nowMs = 24_999L,
            ),
        )
        assertFalse(
            EarbudsCard.shouldSuppressReconnectPopup(
                wasAlreadyConnectedBefore = false,
                isShowing = false,
                isNewLowBattery = false,
                lastShownMs = 10_000L,
                nowMs = 25_000L,
            ),
        )
    }
}
