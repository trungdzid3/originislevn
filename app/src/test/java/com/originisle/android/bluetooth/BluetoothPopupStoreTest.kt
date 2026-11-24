package com.originisle.android.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BluetoothPopupStoreTest {

    @Test
    fun normalizeKey_macAddressAndDeviceName() {
        assertEquals("aa_bb_cc_11_22_33", BluetoothPopupStore.normalizeKey("AA:BB:CC:11:22:33"))
        assertEquals("airpods_pro_2nd_gen", BluetoothPopupStore.normalizeKey("  AirPods Pro (2nd Gen)! "))
        val fallback = BluetoothPopupStore.normalizeKey("---:::")
        assertTrue(fallback.matches(Regex("^[a-z0-9_]+$")))
    }

    @Test
    fun presetFromId_resolvesKnownAndFallback() {
        assertEquals(BluetoothDevicePreset.TWS_EARBUDS, BluetoothDevicePreset.fromId("tws"))
        assertEquals(BluetoothDevicePreset.TWS_WHITE, BluetoothDevicePreset.fromId("tws_white"))
        assertEquals(BluetoothDevicePreset.OVER_EAR, BluetoothDevicePreset.fromId("over_ear"))
        assertEquals(BluetoothDevicePreset.SPEAKER, BluetoothDevicePreset.fromId("speaker"))
        assertEquals(BluetoothDevicePreset.WATCH, BluetoothDevicePreset.fromId("watch"))
        assertEquals(BluetoothDevicePreset.TWS_EARBUDS, BluetoothDevicePreset.fromId(null))
        assertEquals(BluetoothDevicePreset.TWS_EARBUDS, BluetoothDevicePreset.fromId("unknown_id"))
    }

    @Test
    fun inferFromName_detectsOverEar() {
        assertEquals(BluetoothDevicePreset.OVER_EAR, BluetoothDevicePreset.inferFromName("Sony WH-1000XM5"))
        assertEquals(BluetoothDevicePreset.OVER_EAR, BluetoothDevicePreset.inferFromName("AirPods Max"))
        assertEquals(BluetoothDevicePreset.OVER_EAR, BluetoothDevicePreset.inferFromName("Bose QuietComfort 45"))
        assertEquals(BluetoothDevicePreset.OVER_EAR, BluetoothDevicePreset.inferFromName("Tai nghe chụp tai Baseus"))
    }

    @Test
    fun inferFromName_detectsSpeaker() {
        assertEquals(BluetoothDevicePreset.SPEAKER, BluetoothDevicePreset.inferFromName("JBL Flip 6"))
        assertEquals(BluetoothDevicePreset.SPEAKER, BluetoothDevicePreset.inferFromName("Marshall Emberton II"))
        assertEquals(BluetoothDevicePreset.SPEAKER, BluetoothDevicePreset.inferFromName("Loa Bluetooth Mini"))
    }

    @Test
    fun inferFromName_detectsWatch() {
        assertEquals(BluetoothDevicePreset.WATCH, BluetoothDevicePreset.inferFromName("Galaxy Watch 6"))
        assertEquals(BluetoothDevicePreset.WATCH, BluetoothDevicePreset.inferFromName("Xiaomi Smart Band 8"))
        assertEquals(BluetoothDevicePreset.WATCH, BluetoothDevicePreset.inferFromName("Garmin Forerunner 265"))
        assertEquals(BluetoothDevicePreset.WATCH, BluetoothDevicePreset.inferFromName("Đồng hồ thông minh"))
    }

    @Test
    fun inferFromName_detectsTwsDefault() {
        assertEquals(BluetoothDevicePreset.TWS_EARBUDS, BluetoothDevicePreset.inferFromName("AirPods Pro"))
        assertEquals(BluetoothDevicePreset.TWS_EARBUDS, BluetoothDevicePreset.inferFromName("Sony WF-1000XM5"))
        assertEquals(BluetoothDevicePreset.TWS_EARBUDS, BluetoothDevicePreset.inferFromName("Galaxy Buds2 Pro"))
        assertEquals(BluetoothDevicePreset.TWS_EARBUDS, BluetoothDevicePreset.inferFromName("Unknown BT Device"))
    }

    @Test
    fun inferFromName_detectsTwsWhite() {
        assertEquals(BluetoothDevicePreset.TWS_WHITE, BluetoothDevicePreset.inferFromName("Vivo TWS 4 White"))
        assertEquals(BluetoothDevicePreset.TWS_WHITE, BluetoothDevicePreset.inferFromName("Tai nghe trắng"))
        assertEquals(BluetoothDevicePreset.TWS_WHITE, BluetoothDevicePreset.inferFromName("Earbuds Pearl Edition"))
        assertEquals(BluetoothDevicePreset.WATCH, BluetoothDevicePreset.inferFromName("Galaxy Watch White"))
    }

    @Test
    fun popupThemeFromId_resolvesKnownAndFallback() {
        assertEquals(BluetoothPopupTheme.LIGHT, BluetoothPopupTheme.fromId("light"))
        assertEquals(BluetoothPopupTheme.SYSTEM, BluetoothPopupTheme.fromId("system"))
        assertEquals(BluetoothPopupTheme.DARK, BluetoothPopupTheme.fromId("dark"))
        assertEquals(BluetoothPopupTheme.LIGHT, BluetoothPopupTheme.fromId(null))
        assertEquals(BluetoothPopupTheme.LIGHT, BluetoothPopupTheme.fromId("unknown"))
    }
}
