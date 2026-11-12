package com.originisle.android.cards

import org.junit.Assert.assertEquals
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
}
